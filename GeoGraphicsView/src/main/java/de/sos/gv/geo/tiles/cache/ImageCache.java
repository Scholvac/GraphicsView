package de.sos.gv.geo.tiles.cache;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gv.geo.tiles.cache.impl.AbstractTileCacheCascade;

public class ImageCache extends AbstractTileCacheCascade implements ITileCache {

	private static int sizeOf(final BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		return 3 * w * h;
	}

	public static class CacheEntry implements Supplier<TileInfo> {
		final TileInfo		info;
		final BufferedImage	img;

		CacheEntry(final TileInfo ti, final BufferedImage bi){
			info = ti;
			img = bi;
		}
		@Override
		public TileInfo get() {
			return info;
		}
	}


	private final Map<String, CacheEntry>		mCache = Collections.synchronizedMap(new HashMap<>());
	private long								mMaximumSize = 1024*1024*10; //10MB
	private	long								mSize = 0;


	public ImageCache(long maxSize) {
		mMaximumSize = maxSize;
	}

	@Override
	protected void internalRelease(TileInfo info) {
		CacheEntry img = mCache.remove(info.getHash());
		if (img != null) {
			mSize -= sizeOf(img.img);
		}
	}

	@Override
	protected BufferedImage internalGet(TileInfo info) {
		CacheEntry img = mCache.get(info.getHash());
		if (img != null) {
			return img.img;
		}
		return null;
	}

	@Override
	protected void internalAdd(TileInfo key, BufferedImage image) {
		if (mCache.put(key.getHash(), new CacheEntry(key, image)) == null) {
			mSize += sizeOf(image);
		}
	}
	@Override
	protected boolean requiresEviction() {
		return mSize > mMaximumSize;
	}


	@Override
	protected TileInfo getTileToEvict(LatLonBox latLonBounds) {
		return getTileByMaxDistance(latLonBounds, new ArrayList<>(mCache.values()));
	}

	@Override
	protected BufferedImage evict(TileInfo ti) {
		if (ti != null){
			CacheEntry img = mCache.remove(ti.getHash());
			if (img != null) {
				mSize -= sizeOf(img.img);
			}
			return img.img;
		}
		return null;
	}


}
