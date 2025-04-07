package de.sos.gv.geo.tiles.cache;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileInfo;

public class ThreadedTileProvider implements ITileImageProvider {

	private ThreadLocal<ITileImageProvider> 		sThreadProvider = new ThreadLocal<>();

	private Supplier<ITileImageProvider>			mSupplier = null;

	public ThreadedTileProvider(final Supplier<ITileImageProvider> supplier) {
		mSupplier = supplier;
	}

	protected ITileImageProvider getLocalProvider() {
		ITileImageProvider downloader = sThreadProvider.get();
		if (downloader == null) {
			synchronized (mSupplier) {
				sThreadProvider.set(downloader = mSupplier.get());
			}
		}
		return downloader;
	}
	@Override
	public BufferedImage load(final TileInfo info) {
		return getLocalProvider().load(info);
	}

	@Override
	public void free(final TileInfo info, final BufferedImage img) {
		getLocalProvider().free(info, img);
	}

}
