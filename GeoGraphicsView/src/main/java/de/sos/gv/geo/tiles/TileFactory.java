package de.sos.gv.geo.tiles;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
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


	private ITileCalculator						mCalculator = new OSMTileCalculator();

	private final Supplier<BufferedImage>		mLoadingImageSupplier;
	private final Supplier<BufferedImage>		mErrorImageSupplier;
	private final PriorityJobScheduler			mScheduler;

	private ITileImageProvider					mImageProvider;

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
		return new TileJob(tile);
	}

	protected TileItem createTile(final int[] tileInfo) {
		return new TileItem(new TileInfo(tileInfo), mErrorImageSupplier.get());
	}
	@Override
	public void release(final TileItem item) {
		mScheduler.remove(item.getInfo().getHash());
	}



	///////////////////////////////////////////////
	class TileJob implements IJob {

		private final TileItem 	mItem;
		private final long		mCreationTime;

		public TileJob(final TileItem item) {
			mItem = item;
			mCreationTime = System.currentTimeMillis(); //used for priority
		}

		@Override
		public void run() {
			mItem.setImage(TileStatus.LOADING, mLoadingImageSupplier.get());

			final CompletableFuture<BufferedImage> imgFuture = mImageProvider.load(mItem.getInfo());

			imgFuture.whenComplete((img, ex) -> {
				if (img != null)
					mItem.setImage(TileStatus.FINISHED, img);
				else {
					mItem.setImage(TileStatus.ERROR, mErrorImageSupplier.get());
					ex.printStackTrace();
				}
			});


		}
		@Override
		public long getCreationTime() { return mCreationTime; }
		public TileItem getTile() { return mItem; }
		@Override
		public String getHash() { return getTile().getHash(); }

	}
}
