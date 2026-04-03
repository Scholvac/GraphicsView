package de.sos.gv.geo.tiles.chain;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Holds the two thread pools shared across the entire cache pipeline.
 *
 * <ul>
 *   <li>{@link #diskIO}    — for all blocking operations: network downloads and disk reads/writes.
 *                            Sized 4–8 threads so multiple tiles can be fetched in parallel.</li>
 *   <li>{@link #decodeCPU} — for CPU-bound PNG/JPEG decode and encode via {@link ImageIOTranscoder}.
 *                            Sized to the number of available cores.</li>
 * </ul>
 *
 * <p>Always pass {@link #diskIO} to {@link StageWeb} and {@link #decodeCPU} to {@link ImageIOTranscoder}.
 * Swapping them causes thread-pool starvation under load.
 *
 * <p>Call {@link #shutdown()} on application exit. When using {@link TileChainImageProvider#build}
 * or the {@link TileChainImageProvider.Builder}, this is handled automatically via
 * {@link TileChainImageProvider#shutdown()}.
 */
public final class TileExecutors {
	/** Blocking I/O pool — network and disk. Pass to {@link StageWeb}. */
	public final ExecutorService diskIO;
	/** CPU decode/encode pool. Pass to {@link ImageIOTranscoder}. */
	public final ExecutorService decodeCPU;

	public TileExecutors(){
		final int cores = Math.max(2, Runtime.getRuntime().availableProcessors());
		final int ioThreads = Math.max(4, Math.min(cores, 8));

		// Blocking web requests benefit from a small fixed IO pool with real parallelism.
		this.diskIO = Executors.newFixedThreadPool(ioThreads, r -> new Thread(r, "tiles-io-" + System.nanoTime()));
		this.decodeCPU = Executors.newFixedThreadPool(Math.max(2, cores), r -> new Thread(r, "tiles-decode-" + System.nanoTime()));
	}
	public void shutdown(){ diskIO.shutdown(); decodeCPU.shutdown(); }
}
