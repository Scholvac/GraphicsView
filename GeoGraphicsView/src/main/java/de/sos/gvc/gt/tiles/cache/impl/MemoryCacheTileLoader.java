package de.sos.gvc.gt.tiles.cache.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import de.sos.gvc.gt.tiles.ITileDescription;
import de.sos.gvc.gt.tiles.ITileLoader;
import de.sos.gvc.gt.tiles.cache.CacheData;
import de.sos.gvc.gt.tiles.cache.ICacheDataFactory;
import de.sos.gvc.gt.tiles.cache.MemoryCache;
import de.sos.gvc.log.GVLog;


/**
 * 
 * @author scholvac
 *
 */
public class MemoryCacheTileLoader<DESC extends ITileDescription, CACHEVALUE> implements ITileLoader<DESC> {
	
	private ICacheDataFactory<DESC, CACHEVALUE>	 		mCacheHandler;
	private ITileLoader<DESC>							mDelegateLoader;
	private MemoryCache<DESC, CACHEVALUE>				mCache;
			
	public MemoryCacheTileLoader(ICacheDataFactory<DESC, CACHEVALUE> handler,  ITileLoader<DESC> delegate, MemoryCache<DESC, CACHEVALUE> cache) {
		mCacheHandler = handler;
		mDelegateLoader = delegate;
		mCache = cache;
	}
		
	@Override
	public BufferedImage getTileImage(DESC tile) {
		CacheData<DESC, CACHEVALUE> data = mCache.findAndRemoveFromCache(tile.getIdentifier());			
		if (data != null)
			try {
				return mCacheHandler.createImageFromCache(data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return mDelegateLoader.getTileImage(tile); //try again the backup solution but this will most probabliy also fail. However if a ICompressedTileLoader uses does not use the getCompressedTileData method to create the BufferedImage, it may work out
	}
	
	public CacheData<DESC, CACHEVALUE> createCacheData(DESC tile) {
		byte[] imgData = null;
		
		//this method is by far!!! not optimal since we do compress the uncompressed image before we do return the result
		BufferedImage bimg = mDelegateLoader.getTileImage(tile);
		if (bimg != null) {
			GVLog.trace("Need to compress buffered image");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(bimg, "PNG", baos);
				imgData = baos.toByteArray();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		if (imgData != null) {
			CacheData<DESC, CACHEVALUE> out = mCacheHandler.createCacheData(tile, imgData);
			mCache.addTileDataToCache(out); //here happens the whole cache magic, e.g. memory consumption unloading ....
			return out;
		}			
		return null;
	}
}