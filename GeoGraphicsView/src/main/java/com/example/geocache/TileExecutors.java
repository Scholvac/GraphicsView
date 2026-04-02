package com.example.geocache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TileExecutors {
	public final ExecutorService diskIO;
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
