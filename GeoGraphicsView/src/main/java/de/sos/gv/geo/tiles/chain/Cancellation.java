package de.sos.gv.geo.tiles.chain;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cooperative cancellation primitives used throughout the chain.
 *
 * <p>Pattern: the caller creates a {@link CancellationSource}, passes its {@link CancellationToken}
 * into async operations, and calls {@link CancellationSource#cancel()} to request an abort.
 * Operations poll {@link CancellationToken#isCancelled()} at safe points and throw
 * {@link java.util.concurrent.CancellationException} when cancelled.
 *
 * <p>{@link TileChainImageProvider} manages this automatically per tile request.
 * Use {@link CancellationToken#none()} when cancellation is not needed.
 */
public class Cancellation {

	/** Read-side token. Passed into every async operation; polled to detect abort requests. */
	public static  final class CancellationToken {
		private final AtomicBoolean cancelled = new AtomicBoolean(false);
		public boolean isCancelled(){ return cancelled.get(); }
		void cancel(){ cancelled.set(true); }
		public static CancellationToken none(){ return new CancellationToken(); }
	}
	/** Write-side handle. Create one per request, call {@link #cancel()} to abort it. */
	public static final class CancellationSource {
		private final CancellationToken token = new CancellationToken();
		public CancellationToken token(){ return token; }
		public void cancel(){ token.cancel(); }
	}
}