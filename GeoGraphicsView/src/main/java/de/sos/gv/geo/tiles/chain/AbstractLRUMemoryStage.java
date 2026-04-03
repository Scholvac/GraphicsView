package de.sos.gv.geo.tiles.chain;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;

/**
 * Shared LRU eviction logic for in-memory {@link TileStage} implementations.
 *
 * <p>Concrete subclasses only need to declare:
 * <ul>
 *   <li>{@link #kind()}   — which {@link TilePayload.Kind} this stage stores</li>
 *   <li>{@link #cast}     — unchecked cast from {@link TilePayload} to {@code T}</li>
 *   <li>{@link #sizeOf}   — heap size estimate for one entry in bytes</li>
 * </ul>
 *
 * <p>Existing subclasses: {@link StageMemoryImage} (L1, decoded images),
 * {@link StageMemoryBytes} (L2, encoded bytes).
 *
 * <p>The LRU map uses access-order so the iterator always yields the least-recently-used
 * entry first. Both {@code get} and {@code put} update access order.
 * All map and counter access is guarded by {@link #mLock}.
 */
abstract class AbstractLRUMemoryStage<T extends TilePayload> implements TileStage {

	private final long mMaxBytes;
	private long mCurBytes = 0L;
	private final ReentrantLock mLock = new ReentrantLock();
	// access-order = true → iterator yields LRU entry first
	private final LinkedHashMap<String, T> mLru = new LinkedHashMap<>(16, 0.75f, true);
	private volatile EvictionListener mEvictionListener;

	protected AbstractLRUMemoryStage(final long maxBytes) {
		mMaxBytes = maxBytes;
	}

	/** The single {@link TilePayload.Kind} this stage stores. */
	protected abstract TilePayload.Kind kind();

	/** Unchecked cast — only called after verifying {@code payload.kind() == kind()}. */
	protected abstract T cast(TilePayload payload);

	/** Heap-size estimate for one stored entry, in bytes. */
	protected abstract long sizeOf(T value);

	// -----------------------------------------------------------------------------------------
	// TileStage
	// -----------------------------------------------------------------------------------------

	@Override public EnumSet<TilePayload.Kind> provides() { return EnumSet.of(kind()); }
	@Override public EnumSet<TilePayload.Kind> accepts()  { return EnumSet.of(kind()); }

	@Override
	public CompletableFuture<Optional<TilePayload>> get(final TileId id, final CancellationToken ct) {
		return CompletableFuture.supplyAsync(() -> {
			if (ct != null && ct.isCancelled()) throw new CancellationException();
			mLock.lock();
			try {
				final T v = mLru.get(id.cacheKey());
				return v != null ? Optional.<TilePayload>of(v) : Optional.empty();
			} finally { mLock.unlock(); }
		});
	}

	@Override
	public CompletableFuture<Void> put(final TileId id, final TilePayload payload, final CancellationToken ct) {
		if (payload.kind() != kind()) return CompletableFuture.completedFuture(null);
		return CompletableFuture.runAsync(() -> {
			if (ct != null && ct.isCancelled()) throw new CancellationException();
			final T p = cast(payload);
			final long size = sizeOf(p);
			mLock.lock();
			try {
				final T prev = mLru.remove(id.cacheKey());
				if (prev != null) mCurBytes -= sizeOf(prev);
				mLru.put(id.cacheKey(), p);
				mCurBytes += size;
				while (mCurBytes > mMaxBytes && !mLru.isEmpty()) {
					final Map.Entry<String, T> eldest = mLru.entrySet().iterator().next();
					final String key = eldest.getKey();
					final T evicted = eldest.getValue();
					mLru.remove(key);
					mCurBytes -= sizeOf(evicted);
					if (mEvictionListener != null)
						mEvictionListener.onEvict(key, evicted);
				}
			} finally { mLock.unlock(); }
		});
	}

	@Override
	public CompletableFuture<Void> invalidate(final TileId id) {
		return CompletableFuture.runAsync(() -> {
			mLock.lock();
			try {
				final T prev = mLru.remove(id.cacheKey());
				if (prev != null) mCurBytes -= sizeOf(prev);
			} finally { mLock.unlock(); }
		});
	}

	@Override public long currentSizeBytes() { return mCurBytes; }
	@Override public long maxSizeBytes()     { return mMaxBytes; }

	@Override
	public void setEvictionListener(final EvictionListener l) { mEvictionListener = l; }
}
