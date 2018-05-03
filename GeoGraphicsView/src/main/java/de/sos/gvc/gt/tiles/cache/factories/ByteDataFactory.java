package de.sos.gvc.gt.tiles.cache.factories;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.sos.gvc.gt.tiles.ITileDescription;
import de.sos.gvc.gt.tiles.cache.CacheData;
import de.sos.gvc.gt.tiles.cache.ICacheDataFactory;


/**
 * 
 * @author scholvac
 *
 */
public class ByteDataFactory<DESC extends ITileDescription> implements ICacheDataFactory<DESC, byte[]> {

	@Override
	public CacheData<DESC, byte[]> createCacheData(DESC tile, byte[] imgData) {
		return new CacheData<DESC, byte[]>(tile, imgData);
	}

	@Override
	public BufferedImage createImageFromCache(CacheData<DESC, byte[]> data) {
		if (data != null) {
			try {
				return ImageIO.read(new ByteArrayInputStream(data.imageData));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public long getMemoryConsumption(CacheData<DESC, byte[]> data) {
		return data.imageData.length;
	}

	@Override
	public <PARENT_CACHE_VALUE> CacheData<DESC, byte[]> createCacheDataFromOtherCache(
			CacheData<DESC, PARENT_CACHE_VALUE> parentEntry, ICacheDataFactory<DESC, PARENT_CACHE_VALUE> cacheFactory) {
		BufferedImage bimg = null;
		if (parentEntry.imageData != null && parentEntry.imageData instanceof BufferedImage) {
			bimg = (BufferedImage)parentEntry.imageData;
		}else if (cacheFactory != null)
			bimg = cacheFactory.createImageFromCache(parentEntry);
		if (bimg != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(bimg, "PNG", baos);
				return new CacheData<DESC, byte[]>(parentEntry.tile, baos.toByteArray());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}			
		return null;
	}

}
