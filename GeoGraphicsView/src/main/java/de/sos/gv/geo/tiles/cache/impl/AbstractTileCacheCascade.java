package de.sos.gv.geo.tiles.cache.impl;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.function.Supplier;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gv.geo.tiles.cache.ITileCache;

public abstract class AbstractTileCacheCascade implements ITileCache {

	private ITileCache 			mNextCache;


	public ITileCache getFollowUpCache() { return mNextCache;}
	public void setFollowUpCache(ITileCache cache) { mNextCache = cache;}


	@Override
	public BufferedImage load(TileInfo info) {
		final BufferedImage bimg = internalGet(info);
		if (bimg != null) {
			internalRelease(info);
			return bimg;
		}
		return getFromNextCache(info);
	}
	protected BufferedImage getFromNextCache(TileInfo info) {
		return mNextCache != null ? mNextCache.load(info) : null;
	}

	@Override
	public void release(TileInfo key, BufferedImage image) {
		internalAdd(key, image);
		evictIfRequired(key);
	}

	protected void evictIfRequired(final TileInfo addedTile) {
		int count = 2;
		while(--count > 0 && requiresEviction()) {
			TileInfo ti = getTileToEvict(addedTile.getLatLonBounds());
			if (ti == null || ti.compareTo(addedTile) == 0)
				return ;
			BufferedImage img = evict(ti);
			if (mNextCache != null && mNextCache instanceof IForwardCache)
				((IForwardCache)mNextCache).add(ti, img);
		}
	}

	protected abstract TileInfo getTileToEvict(LatLonBox latLonBounds);
	protected abstract BufferedImage evict(TileInfo ti);

	protected abstract boolean requiresEviction();
	protected abstract void internalRelease(TileInfo info);
	protected abstract BufferedImage internalGet(TileInfo info);
	protected abstract void internalAdd(TileInfo key, BufferedImage image);


	public static TileInfo getTileByMaxDistance(final LatLonBox area, final Collection<Supplier<TileInfo>> tileSupplier) {
		if (tileSupplier == null || area == null)
			return null;
		final Point2D.Double center = GeoUtils.getXY(area.getCenter());

		double maxDist = 0;
		TileInfo maxCache = null;
		for (Supplier<TileInfo> sub : tileSupplier) {
			final TileInfo info = sub.get();
			final double d = GeoUtils.squareDistance(center, info.getXYCenter());
			if (d > maxDist) {
				maxCache = info;
				maxDist = d;
			}
		}
		return maxCache;
	}
}