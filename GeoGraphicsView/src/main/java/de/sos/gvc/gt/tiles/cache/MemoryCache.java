package de.sos.gvc.gt.tiles.cache;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.slf4j.Logger;

import de.sos.gvc.gt.tiles.ITileDescription;
import de.sos.gvc.gt.tiles.ITileFactory;
import de.sos.gvc.gt.tiles.ITileLoader;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;
import de.sos.gvc.gt.tiles.LazyTileItem;
import de.sos.gvc.gt.tiles.cache.impl.DistanceUnloadingStrategy;
import de.sos.gvc.gt.tiles.cache.impl.MemoryCacheTileLoader;
import de.sos.gvc.log.GVLog;

/**
 * 
 * @author scholvac
 *
 */
public class MemoryCache<DESC extends ITileDescription, CACHEVALUE> implements ITileFactory<DESC> {

	private Logger	LOG = GVLog.getLogger(MemoryCache.class);
	
	private ITileFactory<DESC> 									mDelegate = null;
	private ICacheDataFactory<DESC, CACHEVALUE>	 				mCacheHandler = null;
	
	private HashMap<Integer, CacheData<DESC, CACHEVALUE>> 		mTileCache = new HashMap<>();
	private HashMap<Integer, DESC> 								mDescCache = new HashMap<>();
		
	private long												mCurrentMemoryConsumption = 0;
	private long												mMaximumMemoryConsumption = 50 * 1024 * 1024; //50 MB
	
	/**
	 * Last requested area, used to priors which tiles shall be unloaded
	 */
	private LatLonBoundingBox						mLastRequest = null;
	private IUnloadStrategy 						mUnloadingStrategy;

	private String mName;
	
	
	
	public MemoryCache(ICacheDataFactory<DESC, CACHEVALUE> handler, ITileFactory<DESC> delegate) {
		this(handler, delegate, 50*1024*1024, "MemoryCache");
	}
	public MemoryCache(ICacheDataFactory<DESC, CACHEVALUE> handler, ITileFactory<DESC> delegate, long memconsumption, String name) {
		mDelegate = delegate;
		mName = name;
		if (mName != null)
			LOG = GVLog.getLogger(name);
		mCacheHandler = handler;
		mMaximumMemoryConsumption = memconsumption;
	}
	
	public boolean isInCache(int identifier) {
		return mTileCache.containsKey(identifier);
	}
	public CacheData<DESC, CACHEVALUE> findAndRemoveFromCache(int identifier) {
		CacheData<DESC, CACHEVALUE> data = mTileCache.get(identifier);
		if (data != null) {
			//remove from this cache since the image is going to an upper level cache (or view)
			mTileCache.remove(identifier);
			mDescCache.remove(identifier);
			mCurrentMemoryConsumption -= mCacheHandler.getMemoryConsumption(data);
		}
		return data;
	}
	

	/**
	 * Adds the new tileData to the memory cache and checks if we do reach the memory threshold. 
	 * If thats the case, this method also unloads old tile data. Which data will be unloaded 
	 * depends on the used "UnloadStrategie"
	 * @param out
	 */
	public void addTileDataToCache(CacheData<DESC, CACHEVALUE> data) {
		long byteSize = mCacheHandler.getMemoryConsumption(data);
		mCurrentMemoryConsumption += byteSize;
		Integer id = new Integer(data.tile.getIdentifier());
		synchronized (mTileCache) {
			mTileCache.put(id, data);
			mDescCache.put(id, data.tile);
			if (LOG.isDebugEnabled()) LOG.debug("Added Item: " + id + " to Cache");
			if (mMaximumMemoryConsumption > 0) { //otherwhise we do not restrict the memory consumption
				while( mCurrentMemoryConsumption > mMaximumMemoryConsumption) {
					int idToUnload = getUnloadingStrategy().unload(Collections.unmodifiableCollection(mDescCache.values()), mLastRequest);
					removeDataFromCache(idToUnload);
				}
			}
		}
	}






	private IUnloadStrategy getUnloadingStrategy() {
		if (mUnloadingStrategy == null)
			mUnloadingStrategy = new DistanceUnloadingStrategy();
		return mUnloadingStrategy;
	}


	private void removeDataFromCache(int idToUnload) {
		CacheData<DESC, CACHEVALUE> data = mTileCache.get(idToUnload);
		if (data != null) {
			if (LOG.isDebugEnabled()) LOG.debug("Remove Item: " + idToUnload + " From Cache");
			mTileCache.remove(idToUnload);
			mDescCache.remove(idToUnload);
			mCurrentMemoryConsumption -= mCacheHandler.getMemoryConsumption(data);
			mDelegate.notifyParentUnloadedTile(data, mCacheHandler);
		}		
	}


	@Override
	public Collection<DESC> getTileDescriptions(LatLonBoundingBox area, Rectangle2D viewArea) {
		mLastRequest = area;
		return mDelegate.getTileDescriptions(area, viewArea);
	}

	@Override
	public ITileLoader<DESC> createTileLoader() {
		return new MemoryCacheTileLoader<>(mCacheHandler, mDelegate.createTileLoader(), this);
	}

	@Override
	public void notifyParentUnloadedTile(LazyTileItem<DESC> tile) {
		CacheData<DESC, BufferedImage> oldCache = new CacheData<DESC, BufferedImage>(tile.getDescription(), tile.getImage());
		notifyParentUnloadedTile(oldCache, null);
	}


	@Override
	public <PARENT_CACHE_VALUE> void notifyParentUnloadedTile(CacheData<DESC, PARENT_CACHE_VALUE> data,
			ICacheDataFactory<DESC, PARENT_CACHE_VALUE> cacheFactory) {
		int id = data.tile.getIdentifier();
		if (isInCache(id))
			return ; //nothing to do
		CacheData<DESC, CACHEVALUE> localData = mCacheHandler.createCacheDataFromOtherCache(data, cacheFactory);
		addTileDataToCache(localData);
	}







}
