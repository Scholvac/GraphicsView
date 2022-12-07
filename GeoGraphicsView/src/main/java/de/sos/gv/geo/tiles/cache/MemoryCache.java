package de.sos.gv.geo.tiles.cache;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileInfo;

public class MemoryCache implements ITileImageProvider {

	static class CacheEntry {
		final TileInfo info;
		final BufferedImage image;
		final int length; //assume BufferedImage.TYPE_INT_ARGB or ...TYPE_INT_RGB. Both are encoded with one int per pixel

		public CacheEntry(final TileInfo ti, final BufferedImage img) {
			info = ti; image = img;
			length = img.getWidth() * img.getHeight() * 4;
		}
	}
	private final Map<TileInfo, CacheEntry>		mCache = new ConcurrentHashMap<>();
	private final LinkedList<CacheEntry>		mOrder = new LinkedList<>();

	private ITileImageProvider 					mProvider;
	private Executor 							mExecutor;
	private long mMaxSize;
	private int mCurrentSize;

	public MemoryCache(final ITileImageProvider provider, final long size, final SizeUnit unit) {
		this(provider, size, unit, null);
	}
	public MemoryCache(final ITileImageProvider provider, final long size, final SizeUnit unit, final Executor executor) {
		mProvider = provider;
		mExecutor = executor;
		setMaxmimumSize(size, unit);
	}

	public void setMaxmimumSize(final long size, final SizeUnit unit) {
		mMaxSize = unit.toBytes(size);
	}

	@Override
	public CompletableFuture<BufferedImage> load(final TileInfo info) {
		final CompletableFuture<BufferedImage> cf = new CompletableFuture<>();
		CacheEntry ce = mCache.get(info);
		if (ce == null) {
			ce = loadImage(info);
			mCache.put(info, ce);
		}
		cf.complete(ce.image);
		return cf;
	}

	private CacheEntry loadImage(final TileInfo ti) {
		try {
			final BufferedImage bimg = mProvider.load(ti).get();
			final CacheEntry ce = new CacheEntry(ti, bimg);
			mOrder.add(ce);
			mCurrentSize += ce.length;
			enforceMaximumSize();
			return ce;
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	private void enforceMaximumSize() {
		while(mCurrentSize > mMaxSize) {
			//remove the oldest entry. The oldest entry is the first element of the treeset
			if (!remove(mOrder.getFirst().info))
				break;
		}
	}
	private synchronized boolean remove(final TileInfo info) {
		final CacheEntry ce = mCache.remove(info);
		if (ce != null) {
			mOrder.removeFirstOccurrence(ce);
			mCurrentSize -= ce.length;
			return true;
		}
		return false;
	}
	@Override
	public void free(final TileInfo info, final BufferedImage img) {
		remove(info);
	}

}
