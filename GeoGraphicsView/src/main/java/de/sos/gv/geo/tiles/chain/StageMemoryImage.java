package de.sos.gv.geo.tiles.chain;

import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;

/**
 * L1 cache stage — stores decoded {@link TilePayload.Image} objects in heap memory.
 *
 * <p>This is the fastest stage in the pipeline: a hit delivers a paint-ready
 * {@link java.awt.image.BufferedImage} with no I/O and no decode step.
 * Memory usage is estimated as {@code width × height × 4 bytes} (ARGB).
 *
 * <p>When the budget is exceeded the least-recently-used image is evicted.
 * If an {@link EvictionListener} is set (done automatically by {@link TileChain}),
 * the evicted image is re-encoded as PNG and demoted to the next stage.
 *
 * <p>Touch this class if the memory-estimation formula needs adjusting
 * (e.g. for non-ARGB formats) or if a different eviction policy is required.
 */
public class StageMemoryImage implements TileStage, SupportsEvictionListener {
	private final long maxBytes;
	private long curBytes = 0L;
	private final ReentrantLock lock = new ReentrantLock();
	private final LinkedHashMap<String, TilePayload.Image> lru = new LinkedHashMap<String, TilePayload.Image>(16,0.75f,true);
	private volatile EvictionListener evictionListener;

	public StageMemoryImage(final long maxBytes){ this.maxBytes = maxBytes; }

	@Override public EnumSet<TilePayload.Kind> provides(){ return EnumSet.of(TilePayload.Kind.IMAGE); }
	@Override public EnumSet<TilePayload.Kind> accepts(){ return EnumSet.of(TilePayload.Kind.IMAGE); }

	@Override
	public CompletableFuture<Optional<TilePayload>> get(final TileId id, final CancellationToken ct){
		return CompletableFuture.supplyAsync(() -> {
			if (ct!=null && ct.isCancelled()) throw new CancellationException();
			lock.lock();
			try {
				final TilePayload.Image img = lru.get(id.cacheKey());
				return img != null ? Optional.<TilePayload>of(img) : Optional.<TilePayload>empty();
			} finally { lock.unlock(); }
		});
	}

	@Override
	public CompletableFuture<Void> put(final TileId id, final TilePayload payload, final CancellationToken ct){
		if (payload.kind()!=TilePayload.Kind.IMAGE) return CompletableFuture.completedFuture(null);
		return CompletableFuture.runAsync(() -> {
			if (ct!=null && ct.isCancelled()) throw new CancellationException();
			final TilePayload.Image p = (TilePayload.Image) payload;
			final long est = ((long)p.value().getWidth()) * p.value().getHeight() * 4L;
			lock.lock();
			try {
				final TilePayload.Image prev = lru.remove(id.cacheKey());
				if (prev != null) curBytes -= ((long)prev.value().getWidth()) * prev.value().getHeight() * 4L;
				lru.put(id.cacheKey(), p);
				curBytes += est;
				while (curBytes > maxBytes && !lru.isEmpty()) {
					final Map.Entry<String, TilePayload.Image> eldest = lru.entrySet().iterator().next();
					final BufferedImage bi = eldest.getValue().value();
					curBytes -= ((long)bi.getWidth()) * bi.getHeight() * 4L;
					// Evict with degrade callback
					final TilePayload.Image evicted = eldest.getValue();
					final String key = eldest.getKey();
					lru.remove(key);
					if (evictionListener != null)
						evictionListener.onEvict(key, evicted);
				}
			} finally { lock.unlock(); }
		});
	}

	@Override
	public CompletableFuture<Void> invalidate(final TileId id){
		return CompletableFuture.runAsync(() -> {
			lock.lock();
			try {
				final TilePayload.Image prev = lru.remove(id.cacheKey());
				if (prev != null)
					curBytes -= ((long)prev.value().getWidth()) * prev.value().getHeight() * 4L;
			} finally { lock.unlock(); }
		});
	}

	@Override public long currentSizeBytes(){ return curBytes; }
	@Override public long maxSizeBytes(){ return maxBytes; }

	@Override public void setEvictionListener(final EvictionListener l){ this.evictionListener = l; }
}
