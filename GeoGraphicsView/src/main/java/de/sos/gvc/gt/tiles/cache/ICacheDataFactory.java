package de.sos.gvc.gt.tiles.cache;

import java.awt.image.BufferedImage;

import de.sos.gvc.gt.tiles.ITileDescription;


/**
 * 
 * @author scholvac
 *
 */
public interface ICacheDataFactory<DESC extends ITileDescription, CACHEVALUE> {
	
	CacheData<DESC, CACHEVALUE> createCacheData(DESC tile, byte[] imgData);
	BufferedImage createImageFromCache(CacheData<DESC, CACHEVALUE> data);
	long getMemoryConsumption(CacheData<DESC, CACHEVALUE> data);
	
	<PARENT_CACHE_VALUE> CacheData<DESC, CACHEVALUE> createCacheDataFromOtherCache(CacheData<DESC, PARENT_CACHE_VALUE>  parentEntry, ICacheDataFactory<DESC, PARENT_CACHE_VALUE> cacheFactory);

}
