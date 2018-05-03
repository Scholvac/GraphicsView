package de.sos.gvc.gt.tiles.cache.factories;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
public class BufferedImageFactory<DESC extends ITileDescription> implements ICacheDataFactory<DESC, BufferedImage> {

	@Override
	public CacheData<DESC, BufferedImage> createCacheData(DESC tile, byte[] imgData) {
		try {
			return new CacheData<DESC, BufferedImage>(tile, ImageIO.read(new ByteArrayInputStream(imgData)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public BufferedImage createImageFromCache(CacheData<DESC, BufferedImage> data) {
		return data.imageData;
	}

	@Override
	public long getMemoryConsumption(CacheData<DESC, BufferedImage> data) {
		if (data != null && data.imageData != null)
			return data.imageData.getRaster().getDataBuffer().getSize() + 100;
		return 100;
	}

	@Override
	public <PARENT_CACHE_VALUE> CacheData<DESC, BufferedImage> createCacheDataFromOtherCache(
			CacheData<DESC, PARENT_CACHE_VALUE> parentEntry, ICacheDataFactory<DESC, PARENT_CACHE_VALUE> cacheFactory) {
		if (parentEntry.imageData != null && parentEntry.imageData instanceof BufferedImage)
			return new CacheData<DESC, BufferedImage>(parentEntry.tile, (BufferedImage)parentEntry.imageData);
		if (cacheFactory != null) {
			BufferedImage bimg = cacheFactory.createImageFromCache(parentEntry);
			return new CacheData<DESC, BufferedImage>(parentEntry.tile, bimg);
		}
		return null;
	}

}
