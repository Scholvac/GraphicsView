package de.sos.gv.geo.tiles.cache.impl;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gv.geo.tiles.cache.ITileCache;

public class TileImageProviderCache implements ITileCache {
	private final Supplier<ITileImageProvider>		mProvider;

	private static final ThreadLocal<ITileImageProvider> tlProvider = new ThreadLocal<>();

	public TileImageProviderCache(final Supplier<ITileImageProvider> provider) {
		mProvider = provider;
	}
	@Override
	public BufferedImage load(TileInfo info) {
		if (tlProvider.get() == null)
			tlProvider.set(mProvider.get());
		final ITileImageProvider downloader = tlProvider.get();
		return downloader.provide(info);
	}
	@Override
	public void release(TileInfo key, BufferedImage image) {
		//do nothing
	}
}