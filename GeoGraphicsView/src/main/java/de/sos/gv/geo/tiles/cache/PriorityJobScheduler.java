package de.sos.gv.geo.tiles.cache;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

import de.sos.gv.geo.tiles.impl.Utils.SimpleThreadFactory;

public class PriorityJobScheduler {

	public interface IJob extends Runnable {
		public long getCreationTime();
		public String getHash();
	}

	private final ExecutorService 					mPriorityJobScheduler;
	private final ExecutorService 					mPriorityJobPoolExecutor;
	private final PriorityBlockingQueue<IJob>	 	mPriorityQueue;
	private boolean 								mAlive = true;

	public PriorityJobScheduler(final String threadName, final Integer poolSize, final Integer queueSize) {

		mPriorityJobScheduler		= Executors.newSingleThreadExecutor(new SimpleThreadFactory(threadName+"_Scheduler", true));
		mPriorityJobPoolExecutor 	= Executors.newFixedThreadPool(poolSize, new SimpleThreadFactory(threadName+"_Worker", true));
		mPriorityQueue 				= new PriorityBlockingQueue<>(queueSize, Comparator.comparing(IJob::getCreationTime).reversed());

		mPriorityJobScheduler.execute(() -> {
			while (mAlive) {
				try {
					mPriorityJobPoolExecutor.execute(mPriorityQueue.take());
				} catch (final InterruptedException e) {
					break;
				}
			}
		});
	}

	public void scheduleJob(final IJob job) {
		mPriorityQueue.add(job);
	}
	public void remove(final String hash) {
		mPriorityQueue.removeIf(tj -> hash.equals(tj.getHash())); //no need to download or load the image anymore...
	}
	public void stop() {
		mAlive = false;
		mPriorityJobScheduler.shutdown();
	}

	public void clear() {
		mPriorityQueue.clear();
	}


}