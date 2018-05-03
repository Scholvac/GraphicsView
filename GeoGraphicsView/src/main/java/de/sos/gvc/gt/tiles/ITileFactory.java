package de.sos.gvc.gt.tiles;

import java.awt.geom.Rectangle2D;
import java.util.Collection;

import de.sos.gvc.gt.tiles.cache.CacheData;
import de.sos.gvc.gt.tiles.cache.ICacheDataFactory;

/**
 * 
 * @author scholvac
 *
 */
public interface ITileFactory<DESC extends ITileDescription> {

	public Collection<DESC> getTileDescriptions(LatLonBoundingBox area, Rectangle2D viewArea/*in Pixel*/);
	
	public ITileLoader<DESC> createTileLoader();

	/**
	 * This method is called if a tile, created by this ITileFactory has been unloaded
	 * @param tile
	 */
	public void notifyParentUnloadedTile(LazyTileItem<DESC> tile);

	/**
	 * @note if the PARENT_CACHE_VALUE is not known you may use the cacheFactory to create an BufferedImage
	 * @param data the cache entry of the parent cache
	 * @param cacheFactory the cache factory of the parent cache
	 */
	public <PARENT_CACHE_VALUE> void notifyParentUnloadedTile(CacheData<DESC, PARENT_CACHE_VALUE> data,
			ICacheDataFactory<DESC, PARENT_CACHE_VALUE> cacheFactory);
}
