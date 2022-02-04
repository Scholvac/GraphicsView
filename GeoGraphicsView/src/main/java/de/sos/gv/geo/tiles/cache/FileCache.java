package de.sos.gv.geo.tiles.cache;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gv.geo.tiles.cache.impl.AbstractTileCacheCascade;

public class FileCache extends AbstractTileCacheCascade implements ITileCache {

	public static long folderSize(File directory) {
		long length = 0;
		File[] files = directory.listFiles();
		if (files == null || files.length == 0)
			return 0;
		for (File file : files) {
			if (file.isFile())
				length += file.length();
			else
				length += folderSize(file);
		}
		return length;
	}

	private static String fileName(final TileInfo info) {
		return new StringBuffer()
				.append("_").append(info.tileX())
				.append("_").append(info.tileY())
				.append("_").append(info.tileZ())
				.append(".png")
				.toString();
	}

	private File 					mDirectory;
	private long					mSize;
	private long 					mMaxDirectorySize = 100*1024*1024;

	private ExecutorService			mSerializerES = Executors.newFixedThreadPool(1);

	public FileCache(final File dir, long maxDirectorySize) {
		mMaxDirectorySize = maxDirectorySize;
		if ( dir.exists()== false)
			dir.mkdirs();
		setDirectory(dir);
	}



	@Override
	protected void internalRelease(TileInfo info) {}



	@Override
	protected BufferedImage internalGet(TileInfo info) {
		final File file = new File(mDirectory, fileName(info));
		if (file.exists())
			try {
				return ImageIO.read(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		return null;
	}

	@Override
	protected BufferedImage getFromNextCache(TileInfo info) {
		BufferedImage bimg = super.getFromNextCache(info);
		if (bimg != null) {
			mSerializerES.execute(() -> {
				internalAdd(info, bimg);
			});
		}
		return bimg;
	}



	@Override
	protected void internalAdd(TileInfo info, BufferedImage image) {
		final File file = new File(mDirectory, fileName(info));
		if (file.exists() == false) {
			try {
				ImageIO.write(image, "PNG", file);
				internalAddCacheEntry(info, file);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		evictIfRequired(info);
	}

	private void internalAddCacheEntry(TileInfo info, File file) {
		final CacheEntry e = new CacheEntry(file, info);
		mEntries.put(info.getHash(), e);
		mSize += e.getSize();
	}

	@Override
	protected boolean requiresEviction() {
		return mSize >= mMaxDirectorySize;
	}

	static class CacheEntry {
		final File 		file;
		final TileInfo	info;

		private CacheEntry(File file, TileInfo info) {
			this.file = file;
			this.info = info;
		}

		static CacheEntry create(File f) {
			if (f == null) return null;
			final String name = f.getName();
			final TileInfo info = TileInfo.fromHash(name.substring(1, name.length()-4)); //remove _ and .png
			if (info == null) return null;
			return new CacheEntry(f, info);
		}

		public long getSize() {
			return file.length();
		}

	}
	private final Map<String, CacheEntry> 	mEntries = Collections.synchronizedMap(new HashMap<>());
	public void setDirectory(final File directory) {
		if (directory != mDirectory) {
			mDirectory = directory;

			mEntries.clear();
			mSize = 0;
			mSerializerES.execute(() -> {
				File[] files = mDirectory.listFiles();
				for (int i = 0; i < files.length; i++) {
					final CacheEntry cache = CacheEntry.create(files[i]);
					if (cache != null) {
						mEntries.put(cache.info.getHash(), cache);
						mSize += cache.getSize();
					}
				}
			});
		}
	}

	@Override
	protected TileInfo getTileToEvict(LatLonBox area) {
		final Point2D.Double center = GeoUtils.getXY(area.getCenter());

		double maxDist = 0;
		CacheEntry maxCache = null;
		ArrayList<CacheEntry> tmpCollection = new ArrayList<>(mEntries.values());
		for (CacheEntry e : tmpCollection) {
			final double d = GeoUtils.squareDistance(center, e.info.getXYCenter());
			if (d > maxDist) {
				maxCache = e;
				maxDist = d;
			}
		}
		return maxCache != null ? maxCache.info : null;

	}

	@Override
	protected BufferedImage evict(TileInfo ti) {
		if (ti == null)
			return null;
		final String hash = ti.getHash();
		final CacheEntry ce = mEntries.remove(hash);
		if (ce != null) {
			long size = ce.getSize();
			if (ce.file.delete())
				mSize -= size;
		}
		return null; //TODO: no need to load the image into memory to get it cleaned up right after that; the File cache is the last cache.
	}



}
