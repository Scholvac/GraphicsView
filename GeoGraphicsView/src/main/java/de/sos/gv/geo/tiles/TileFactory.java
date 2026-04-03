package de.sos.gv.geo.tiles;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;

import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.tiles.TileItem.TileStatus;
import de.sos.gv.geo.tiles.cache.PriorityJobScheduler;
import de.sos.gv.geo.tiles.cache.PriorityJobScheduler.IJob;
import de.sos.gv.geo.tiles.impl.Utils;
import de.sos.gvc.log.GVLog;

public class TileFactory implements ITileFactory {

	private static final Logger					LOG = GVLog.getLogger(TileInfo.class);

	@FunctionalInterface
	public interface IZOrderProvider {
		/** Provide the ZOrder for the given tile. No need to set the value, just return it, the factory will apply the value */
		public float provideZOrder(final TileItem item);
	}

	private ITileCalculator						mCalculator = new OSMTileCalculator();

	private final Supplier<BufferedImage>		mLoadingImageSupplier;
	private final Supplier<BufferedImage>		mErrorImageSupplier;
	private final PriorityJobScheduler			mScheduler;
	private final Map<String, TileJob>			mRunningJobs = new ConcurrentHashMap<>();

	private ITileImageProvider					mImageProvider;
	private IZOrderProvider						mZOrderProvider;

	public TileFactory(final ITileImageProvider imgProvider) {
		this(imgProvider, Runtime.getRuntime().availableProcessors());
	}
	public TileFactory(final ITileImageProvider imgProvider, final int threadCount) {
		this(imgProvider, "TileFactory", threadCount);
	}
	public TileFactory(final ITileImageProvider imgProvider, final String threadName, final int threadCount) {
		this(imgProvider, threadName, threadCount, 100);
	}
	public TileFactory(final ITileImageProvider imgProvider, final String threadName, final int threadCount, final int queueSize) {
		final BufferedImage loadingImage = Utils.loadImageOrEmpty("timer.png");
		final BufferedImage errorImage = Utils.loadImageOrEmpty("error404.png");
		mLoadingImageSupplier = () -> loadingImage;
		mErrorImageSupplier = () -> errorImage;
		mImageProvider = imgProvider;
		mScheduler = new PriorityJobScheduler(threadName, threadCount, queueSize);
	}

	public void setZOrder(final float fixOrder) {
		setZOrder(ti -> fixOrder);
	}
	public void setZOrder(final IZOrderProvider order) {
		mZOrderProvider = order;
	}
	public void setMaximumZoom(final int maxZoom) {
		mCalculator.setMaximumZoom(maxZoom);
	}
	public int getMaximumZoom() {
		return mCalculator.getMaximumZoom();
	}
	public void setTileCalculator(final ITileCalculator tileCalc) {
		mCalculator = tileCalc;
	}
	public ITileCalculator getTileCalculator() {
		return mCalculator;
	}

	@Override
	public int[][] getRequiredTileInfos(final LatLonBox area, final int imgWidth, final int imgHeight) {
		return mCalculator.calculateTileCoordinates(area, imgWidth);
	}

	public void setProvider(final ITileImageProvider provider) {
		mScheduler.clear();
		mImageProvider = provider;
	}

	@Override
	public TileItem load(final int[] tileInfo) {
		final TileJob job = createTileJob(tileInfo);
		mScheduler.scheduleJob(job);
		return job.getTile();
	}

	protected TileJob createTileJob(final int[] tileInfo) {
		final TileItem tile = createTile(tileInfo);
		if (mZOrderProvider != null)
			tile.setZOrder(mZOrderProvider.provideZOrder(tile));
		return new TileJob(tile);
	}

	protected TileItem createTile(final int[] tileInfo) {
		return new TileItem(new TileInfo(tileInfo), mLoadingImageSupplier.get());
	}
	@Override
	public void release(final TileItem item) {
		final String hash = item.getInfo().getHash();
		mScheduler.remove(hash);
		final TileJob running = mRunningJobs.get(hash);
		if (running != null)
			running.cancel();
	}



	///////////////////////////////////////////////
	class TileJob implements IJob {
		private final TileItem 						mItem;
		private final long							mCreationTime;//used for priority
		private volatile boolean					mCancelled = false;


		public TileJob(final TileItem item) {
			mItem = item;
			mCreationTime = System.currentTimeMillis();
		}

		public void cancel() {
			mCancelled = true;
			if (mImageProvider instanceof ICancellableTileImageProvider)
				((ICancellableTileImageProvider) mImageProvider).cancel(mItem.getInfo());
		}

		@Override
		public void run() {
			mRunningJobs.put(getHash(), this);
			try {
				final BufferedImage img = mImageProvider.load(mItem.getInfo());
				if (mCancelled)
					return;
				if (img != null)
					mItem.setImage(TileStatus.FINISHED, img);
				else {
					mItem.setImage(TileStatus.ERROR, mErrorImageSupplier.get());
				}
			} finally {
				mRunningJobs.remove(getHash(), this);
			}
		}
		@Override
		public long getCreationTime() { return mCreationTime; }
		public TileItem getTile() { return mItem; }
		@Override
		public String getHash() { return getTile().getHash(); }
	}
}
