package de.sos.gv.geo.tiles.impl;

import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import de.sos.gv.geo.tiles.TileItem;
import de.sos.gv.geo.tiles.TileItem.TileStatus;
import de.sos.gv.geo.tiles.cache.ITileCache;

public class TileWorker implements Runnable {

	public static class TileWorkerItem {
		public final TileItem 		item;
		public final long			time;
		public int					numTries = 0;


		public TileWorkerItem(final TileItem it, int tries) {
			item = it;
			time = System.currentTimeMillis();
			numTries = tries;
		}
	}

	private long								mPollTimeoutSec = 1;
	private boolean								mAlive = true;
	private final BlockingQueue<TileWorkerItem>	mTileQueue;
	private final ITileCache					mTileCache;
	private final BufferedImage					mErrorImage;


	public TileWorker(final ITileCache cache, final BlockingQueue<TileWorkerItem> queue, final BufferedImage ei) {
		mTileQueue = queue;
		mTileCache = cache;
		mErrorImage = ei;
	}


	@Override
	public void run() {
		mAlive = true;
		while(mAlive) {
			TileWorkerItem item = null;
			try {
				item = mTileQueue.poll(mPollTimeoutSec, TimeUnit.SECONDS);
				if (item != null)
					handleTile(item);
			} catch (Throwable e) {
				e.printStackTrace();
				if (item != null)
					handleTileError(item);
			}
		}

	}

	private void handleTile(TileWorkerItem item) {
		BufferedImage img = mTileCache.load(item.item.getInfo());
		if (img != null) {
			item.item.setImage(TileStatus.FINISHED, img);
		}else{
			item.numTries--;
			if (item.numTries == 0) {
				handleTileError(item);
			} else
				try {
					mTileQueue.put(item);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	}

	private void handleTileError(TileWorkerItem item) {
		item.item.setImage(TileStatus.ERROR, mErrorImage);
	}

	public void setAlive(boolean b) {
		mAlive = b;
	}



}
