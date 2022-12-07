package de.sos.gv.geo.tiles.cache;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.imageio.ImageIO;

import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileInfo;

public class FileCache implements ITileImageProvider {

	static class CacheEntry implements Comparable<CacheEntry> {
		final TileInfo 	tile;
		final File 		file;
		public CacheEntry(final TileInfo t, final File f) { tile = t; file = f;}
		@Override
		public int compareTo(final CacheEntry o) {
			return Long.compare(file.lastModified(), o.file.lastModified());
		}
	}
	private final Map<TileInfo, CacheEntry>			mCache = new ConcurrentHashMap<>();
	private TreeSet<CacheEntry>						mOrder = new TreeSet<>();

	private ITileImageProvider 						mProvider;
	private Executor 								mExecutor;
	private File 									mDirectory;

	private long 									mCurrentSize;
	private long mMaxSize;

	public FileCache(final ITileImageProvider provider, final File directory, final long size, final SizeUnit unit) {
		this(provider, directory, size, unit, null);
	}
	public FileCache(final ITileImageProvider provider, final File directory, final long size, final SizeUnit unit, final Executor executor) {
		mProvider = provider;
		mDirectory = directory;
		mExecutor = executor;
		setMaxmimumSize(size, unit);

		asyncPopulateCache();
	}

	private void asyncPopulateCache() {
		if (mExecutor != null)
			mExecutor.execute(this::populateCache);
		else
			populateCache();
	}
	private void populateCache() {
		if (mDirectory.exists()) {
			//fill the cache with existing files, to get the size calculation correct.
			final File[] files = mDirectory.listFiles();
			for (final File f : mDirectory.listFiles()) {
				addFile(f, false);
			}
			enforceMaximumSize();
		}
	}
	private CacheEntry addFile(final File f, final boolean enforceSize) {
		final String fn = f.getName();
		final TileInfo ti = TileInfo.fromHash(fn.substring(0, fn.lastIndexOf('.')));
		return add(ti, f, enforceSize);
	}
	private synchronized CacheEntry add(final TileInfo ti, final File file, final boolean enforceSize) {
		final CacheEntry ce = new CacheEntry(ti, file);
		mCache.put(ti, ce);
		mOrder.add(ce);
		mCurrentSize += file.length();
		if (enforceSize)
			enforceMaximumSize();
		return ce;
	}
	private void enforceMaximumSize() {
		while(mCurrentSize > mMaxSize && mCurrentSize > 0) {
			//remove the oldest entry. The oldest entry is the first element of the treeset
			remove(mOrder.first().tile);
		}
	}
	private void remove(final TileInfo ti) {
		final CacheEntry ce = mCache.remove(ti);
		if (ce != null) {
			mOrder.remove(ce);
			mCurrentSize -= ce.file.length();
			ce.file.delete();//we do not check, as we can not change it anyways.
		}
	}

	public void setMaxmimumSize(final long size, final SizeUnit unit) {
		mMaxSize = unit.toBytes(size);
		enforceMaximumSize();
	}

	private BufferedImage loadFromFile(final File file) {
		try {
			return ImageIO.read(file);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private File saveToFile(final File file, final BufferedImage bufferedImage) {
		try {
			if (file.getParentFile().exists() == false)
				file.getParentFile().mkdirs();
			ImageIO.write(bufferedImage, "PNG", file);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return file;
	}
	private File getFile(final TileInfo info) {
		return new File(mDirectory, info.getHash() + ".png");
	}
	protected void remove(final TileInfo key, final File value) {
		if (value != null && value.exists()) {
			value.delete();
		}
	}
	@Override
	public CompletableFuture<BufferedImage> load(final TileInfo info) {
		final CompletableFuture<BufferedImage> image = new CompletableFuture<>();
		CacheEntry ce = mCache.get(info);
		if (ce == null) {
			ce = saveToFile(info);
			mCache.put(info, ce);
		}
		image.complete(loadFromFile(ce.file));
		enforceMaximumSize();
		return image;
	}

	private CacheEntry saveToFile(final TileInfo ti) {
		try {
			return mProvider.load(ti).thenApply(img -> {
				if (img != null) {
					final File file = getFile(ti);
					saveToFile(file, img);
					return addFile(file, false);
				}
				return null;
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	@Override
	public void free(final TileInfo info, final BufferedImage img) {
	}

}
