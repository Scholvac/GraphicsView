package com.example.geocache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.example.geocache.Cancellation.CancellationToken;
import com.example.geocache.TilePayload.Encoded;
import com.example.geocache.TilePayload.Image;

public class TileChain {
	private final List<TileStage> stages; // 0 = fastest, last = slowest
	private final ImageIOTranscoder transcoder;
	private final TileExecutors execs;
	private final ConcurrentHashMap<String, Group> inFlight = new ConcurrentHashMap<String, Group>();
	// Tracks background move/encode operations triggered by evictions/promotions
	private final java.util.Set<CompletableFuture<?>> backgroundOps = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<CompletableFuture<?>, Boolean>());


	private static final class Group {
		final AtomicInteger subscribers = new AtomicInteger(0);
		final CompletableFuture<TilePayload.Image> future;
		Group(final CompletableFuture<TilePayload.Image> f){ this.future = f; }
	}

	public TileChain(final List<TileStage> stages, final ImageIOTranscoder transcoder, final TileExecutors execs){
		this.stages = Collections.unmodifiableList(new ArrayList<TileStage>(stages));
		this.transcoder = transcoder;
		this.execs = execs;
		wireEvictions();
	}

	/**
	 * Returns a CompletableFuture that completes when all currently active
	 * tile requests (the Group futures in 'inFlight') have finished —
	 * whether they complete normally, fail, or are cancelled.
	 *
	 * New requests started after this call are not tracked by the returned future.
	 */
	public CompletableFuture<Void> whenIdle() {
		// Take a snapshot of all active Group futures
		final java.util.List<CompletableFuture<?>> list = new java.util.ArrayList<>();
		for (final Group g : inFlight.values())
			list.add(g.future);
		if (list.isEmpty())
			return CompletableFuture.completedFuture(null);
		return CompletableFuture.allOf(list.toArray(new CompletableFuture<?>[0]));
	}

	/** Completes when all currently pending background operations (eviction demotions, etc.) are finished. */
	public CompletableFuture<Void> whenBackgroundIdle() {
		final CompletableFuture<?>[] snap = backgroundOps.toArray(new CompletableFuture<?>[0]);
		return snap.length == 0 ? CompletableFuture.completedFuture(null) : CompletableFuture.allOf(snap);
	}

	/**
	 * Returns a CompletableFuture that completes only when the chain is truly idle —
	 * meaning all currently active requests are finished, and no new requests
	 * have appeared in the meantime.
	 *
	 * ⚠️ Warning: if new requests keep being submitted continuously,
	 * this method may never complete.
	 */
	public CompletableFuture<Void> whenTrulyIdle() {
		return whenIdle().thenCompose(v -> inFlight.isEmpty()
				? CompletableFuture.completedFuture(null)
						: whenTrulyIdle());
	}
	// Wire eviction degradation: stage i -> first lower stage that accepts the payload
	private void wireEvictions(){
		for (int i=0;i<stages.size();i++){
			final int from = i;
			final TileStage s = stages.get(i);
			if (s instanceof SupportsEvictionListener)
				((SupportsEvictionListener)s).setEvictionListener((key, payload) -> {
					// find first lower stage that accepts payload.kind
					for (int j=from+1;j<stages.size();j++){
						final TileStage lower = stages.get(j);
						if (lower.accepts().contains(payload.kind())) {
							// Move down: put to lower
							final TileId id = keyToId(key);
							if (id != null)
								trackBackground(lower.put(id, payload, CancellationToken.none()));
							return;
						} else if (payload.kind()==TilePayload.Kind.IMAGE && lower.accepts().contains(TilePayload.Kind.ENCODED)) {
							// Re-encode to bytes then store
							final TileId id = keyToId(key);
							if (id != null)
								transcoder.encodePng((TilePayload.Image)payload, CancellationToken.none())
								.thenCompose(enc -> trackBackground(lower.put(id, enc, CancellationToken.none())));
							return;
						}
					}
					// else drop on floor (no lower stage)
				});
		}
	}

	private <T> CompletableFuture<T> trackBackground(final CompletableFuture<T> f) {
		backgroundOps.add(f);
		f.whenComplete((x, e) -> backgroundOps.remove(f));
		return f;
	}


	// Parse key back to TileId (our keys are style/z/x/y.ext)
	private TileId keyToId(final String key){
		try {
			final String[] parts = key.split("/");
			final String style = parts[0];
			final int z = Integer.parseInt(parts[1]);
			final int x = Integer.parseInt(parts[2]);
			final String[] rest = parts[3].split("\\.");
			final int y = Integer.parseInt(rest[0]);
			final String ext = rest[1];
			return new TileId(z,x,y,style,ext);
		} catch (final Exception e){ return null; }
	}

	public CompletableFuture<TilePayload.Image> getImage(final TileId id, final CancellationToken ct){
		final String key = id.cacheKey();

		final Group g = inFlight.compute(key, (k, existing) -> {
			if (existing != null) {
				existing.subscribers.incrementAndGet();
				return existing;
			}
			final CompletableFuture<TilePayload.Image> core = resolveSingleCopy(id, ct);
			final Group created = new Group(core);
			created.subscribers.set(1);
			return created;
		});

		final CompletableFuture<TilePayload.Image> perCaller = new CompletableFuture<TilePayload.Image>();
		g.future.whenComplete((img, ex) -> {
			if (!perCaller.isCancelled())
				if (ex != null)
					perCaller.completeExceptionally(ex);
				else
					perCaller.complete(img);
		});
		perCaller.whenComplete((r, ex) -> {
			final int left = g.subscribers.decrementAndGet();
			if (perCaller.isCancelled() && left <= 0)
				g.future.cancel(true);
			if (left <= 0)
				inFlight.remove(key, g);
		});

		if (ct != null && ct.isCancelled())
			perCaller.cancel(true);
		return perCaller;
	}

	// Single-copy policy: after resolving from stage i, MOVE it to the highest accepting stage (index t)
	private CompletableFuture<TilePayload.Image> resolveSingleCopy(final TileId id, final CancellationToken ct){
		// Step 1: search stages in order
		CompletableFuture<LocatedPayload> cursor = CompletableFuture.completedFuture(new LocatedPayload(-1, null));

		for (int i=0;i<stages.size();i++){
			final int idx = i;
			final TileStage s = stages.get(i);
			cursor = cursor.thenCompose((Function<LocatedPayload, CompletableFuture<LocatedPayload>>) prev -> {
				if (ct!=null && ct.isCancelled())
					return cancelledLocated();
				if (prev.payload != null)
					return CompletableFuture.completedFuture(prev);
				return s.get(id, ct).thenApply(opt -> {
					if (opt.isPresent()) return new LocatedPayload(idx, opt.get());
					return new LocatedPayload(-1, null);
				});
			});
		}

		// Step 2: if found at i with payload P, move to highest accepting stage
		return cursor.thenCompose((Function<LocatedPayload, CompletableFuture<Image>>) loc -> {
			if (ct!=null && ct.isCancelled())
				return cancelled();
			if (loc.payload == null)
				return failed(new NoSuchElementException("Tile not found"));

			// Determine target
			final int target = highestAcceptingIndex(loc.payload.kind());
			// Decode to Image if needed for target access
			CompletableFuture<TilePayload.Image> imgFuture;
			if (loc.payload.kind()==TilePayload.Kind.IMAGE)
				imgFuture = CompletableFuture.completedFuture((TilePayload.Image)loc.payload);
			else
				imgFuture = transcoder.decode((TilePayload.Encoded)loc.payload, ct);

			// After we have an image, move to target if it accepts IMAGE, else if target only accepts ENCODED, re-encode
			return imgFuture.thenCompose((Function<Image, CompletableFuture<Image>>) img -> {
				final TileStage targetStage = stages.get(target);
				CompletableFuture<Void> move;
				if (targetStage.accepts().contains(TilePayload.Kind.IMAGE))
					move = targetStage.put(id, img, ct).thenRun(() -> stages.get(loc.index).invalidate(id));
				else
					// re-encode PNG and move bytes
					move = transcoder.encodePng(img, ct).thenCompose((Function<Encoded, CompletableFuture<Void>>) enc -> stages.get(target).put(id, enc, ct).thenRun(() -> stages.get(loc.index).invalidate(id)));
				return move.thenApply(v -> img);
			});
		});
	}

	private int highestAcceptingIndex(final TilePayload.Kind kind){
		for (int i=0;i<stages.size();i++)
			if (stages.get(i).accepts().contains(kind) || (kind==TilePayload.Kind.ENCODED && stages.get(i).accepts().contains(TilePayload.Kind.IMAGE)))
				return i;
		return stages.size()-1; // fallback
	}

	private static final class LocatedPayload {
		final int index;
		final TilePayload payload;
		LocatedPayload(final int index, final TilePayload payload){ this.index=index; this.payload=payload; }
	}

	private static <T> CompletableFuture<T> failed(final Throwable t){ final CompletableFuture<T> f=new CompletableFuture<T>(); f.completeExceptionally(t); return f; }
	private static <T> CompletableFuture<T> cancelled(){ final CompletableFuture<T> f=new CompletableFuture<T>(); f.cancel(true); return f; }
	private static CompletableFuture<LocatedPayload> cancelledLocated(){ final CompletableFuture<LocatedPayload> f=new CompletableFuture<LocatedPayload>(); f.cancel(true); return f; }
}
