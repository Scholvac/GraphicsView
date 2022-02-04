package de.sos.gv.geo.tiles;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.tiles.cache.ITileCache;
import de.sos.gv.geo.tiles.impl.OSMTileCalculator;
import de.sos.gv.geo.tiles.impl.TileWorker;
import de.sos.gv.geo.tiles.impl.TileWorker.TileWorkerItem;
import de.sos.gvc.log.GVLog;

public class TileFactory {

	private static final Logger					LOG = GVLog.getLogger(TileInfo.class);

	private ITileCalculator						mCalculator = new OSMTileCalculator();

	private final BlockingQueue<TileWorkerItem>	mTileQueue = new PriorityBlockingQueue<>(1000, (a, b) -> -Long.compare(a.time, b.time));
	private final List<TileWorker>				mWorkerThreads = new ArrayList<>();

	private BufferedImage 						mLoadingImage;
	private BufferedImage 						mErrorImage;

	private ITileCache							mTileCache;
	private String								mThreadName;
	private int									mThreadCount;
	private int									mNameCounter = 0;

	private int 								mMaxTries = 5;



	public TileFactory(ITileCache cache) {
		this(cache, "TileFactoryWorker", 4);
	}
	public TileFactory(ITileCache cache, final String threadName, final int threadCount) {
		try {
			mLoadingImage = ImageIO.read(getClass().getClassLoader().getResource("timer.png"));
			mErrorImage = ImageIO.read(getClass().getClassLoader().getResource("error404.png"));
		} catch (IOException e) {
			e.printStackTrace();
			mLoadingImage = new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR);
		}
		setTileCache(cache);
		createExecutor(threadName, threadCount);
	}

	public void setTileCache(ITileCache cache) { mTileCache = cache;}
	public void setNumberOfThreads(final int count) { createExecutor(mThreadName, count);}
	public void setThreadName(final String name) { createExecutor(name, mThreadCount);}

	private void createExecutor(final String name, final int count) {
		LOG.info("Update Tileworker");
		mWorkerThreads.forEach(it -> it.setAlive(false));
		mWorkerThreads.clear(); //may not yet finished, but that does not matter...

		for (int i = 0; i < count; i++) {
			TileWorker tw = new TileWorker(mTileCache, mTileQueue, mErrorImage);
			Thread t = new Thread(tw, "TileWorker_" + mNameCounter++);
			t.setDaemon(true);
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
			LOG.info("Create TileWorker: " + t.getName());
			mWorkerThreads.add(tw);
		}
	}



	public int[][] getRequiredTileInfos(final LatLonBox area, final Rectangle viewBounds) {
		return mCalculator.calculateTileCoordinates(area, viewBounds);
	}


	public TileItem load(int[] tileInfo) {
		TileInfo ti = new TileInfo(tileInfo);
		TileItem tile = new TileItem(ti, mLoadingImage);
		TileWorkerItem twi = new TileWorkerItem(tile, mMaxTries);
		mTileQueue.add(twi);

		return tile;
	}

	public void release(TileItem item) {
		mTileCache.release(item.getInfo(), item.getImage());
	}
	public void check(Collection<TileItem> values) {
		//		for (TileItem item : values) {
		//			if (item.getStatus() == TileStatus.FINISHED) {
		//				if (item.getImage() == mLoadingImage) {
		//					System.out.println("Error: " + item);
		//				}
		//			}else {
		//				System.out.println(item);
		//				if (item.getStatus() == TileStatus.ERROR) {
		//					if (item.getImage() != mErrorImage) {
		//						System.out.println("UPS");
		//					}
		//				}
		//			}
		//		}
	}
}
