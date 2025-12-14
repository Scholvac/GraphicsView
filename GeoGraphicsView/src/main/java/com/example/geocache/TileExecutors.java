package com.example.geocache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class TileExecutors {
	public final ExecutorService diskIO;
	public final ExecutorService decodeCPU;

	public TileExecutors(){
		final int cores = Math.max(2, Runtime.getRuntime().availableProcessors());

		this.diskIO = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(512), (ThreadFactory) r -> new Thread(r,"tiles-disk-"+System.nanoTime()));
		this.decodeCPU = new ThreadPoolExecutor(Math.max(2, cores/2), cores, 30, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(256), (ThreadFactory) r -> new Thread(r,"tiles-decode-"+System.nanoTime()));
	}
	public void shutdown(){ diskIO.shutdown(); decodeCPU.shutdown(); }
}
