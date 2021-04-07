package de.sos.gvc.gt.tiles.cache.factories;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.sos.gvc.gt.tiles.cache.CacheData;
import de.sos.gvc.gt.tiles.cache.ICacheDataFactory;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;


/**
 * 
 * @author scholvac
 *
 */
public class FileDataFactory<DESC extends OSMTileDescription> implements ICacheDataFactory<DESC, File> {

	private File mDirectory;

	public FileDataFactory(String directory) throws IOException {
		this(new File(directory));
	}
	
	public FileDataFactory(File directory) throws IOException {
		mDirectory = directory;
		if (mDirectory != null) {
			if (mDirectory.exists() && mDirectory.isDirectory() == false) throw new IOException("Require a directory to write the tile images into");
		}
		if (!mDirectory.exists()) {
			if (!mDirectory.mkdirs()) throw new IOException("Failed to create the directory: " + mDirectory); 
		}
	}

	@Override
	public CacheData<DESC, File> createCacheData(DESC tile, byte[] imgData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedImage createImageFromCache(CacheData<DESC, File> data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getMemoryConsumption(CacheData<DESC, File> data) {
		if (data.imageData.exists() == false) return 0;
		return data.imageData.length();
	}

	@Override
	public <PARENT_CACHE_VALUE> CacheData<DESC, File> createCacheDataFromOtherCache(CacheData<DESC, PARENT_CACHE_VALUE> parentEntry, ICacheDataFactory<DESC, PARENT_CACHE_VALUE> cacheFactory) {
		BufferedImage bimg = null;
		if (parentEntry.imageData != null && parentEntry.imageData instanceof BufferedImage) {
			bimg = (BufferedImage)parentEntry.imageData;
		}else if (cacheFactory != null)
			bimg = cacheFactory.createImageFromCache(parentEntry);
		if (bimg != null) {
			try {
				File targetFile = getFile(parentEntry.tile);
				if (targetFile.exists() == false) {
					if (targetFile.getParentFile().exists() == false)
						targetFile.getParentFile().mkdirs();
					ImageIO.write(bimg, "PNG", targetFile);
				}
				return new CacheData<DESC, File>(parentEntry.tile, targetFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}			
		return null;
	}

	public File getFile(DESC t) {
		return new File(mDirectory.getAbsolutePath() + "/" + getFileName(t));
	}
	public String getFileName(DESC t) {
		return ""+t.getZoom() + "/"+t.getTileX() + "/"+t.getTileY() + ".png";
	}

//	@Override
//	public CacheData<DESC, File> createCacheData(DESC tile, File imgData) {
//		return new CacheData<DESC, File>(tile, imgData);
//	}
//
//	@Override
//	public BufferedImage createImageFromCache(CacheData<DESC, File> data) {
//		if (data != null) {
//			try {
//				return ImageIO.read(new ByteArrayInputStream(data.imageData));
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		return null;
//	}
//
//	@Override
//	public long getMemoryConsumption(CacheData<DESC, File> data) {
//		return data.imageData.length;
//	}
//
//	@Override
//	public <PARENT_CACHE_VALUE> CacheData<DESC, File> createCacheDataFromOtherCache(
//			CacheData<DESC, PARENT_CACHE_VALUE> parentEntry, ICacheDataFactory<DESC, PARENT_CACHE_VALUE> cacheFactory) {
//		BufferedImage bimg = null;
//		if (parentEntry.imageData != null && parentEntry.imageData instanceof BufferedImage) {
//			bimg = (BufferedImage)parentEntry.imageData;
//		}else if (cacheFactory != null)
//			bimg = cacheFactory.createImageFromCache(parentEntry);
//		if (bimg != null) {
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			try {
//				ImageIO.write(bimg, "PNG", baos);
//				return new CacheData<DESC, File>(parentEntry.tile, baos.toByteArray());
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}			
//		return null;
//	}

}
