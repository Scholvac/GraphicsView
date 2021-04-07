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
public interface ITileProvider<DESC extends ITileDescription> {

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

	/**
	 * writes the information of the tile description into a single line string. 
	 * @note this string may be used as name for files
	 * @param desc
	 * @return
	 */
	public String getStringDescription(DESC desc);
	/**
	 * restores a tile description from a string
	 * @note this will be always called with the result obtained by <code>getStringDescription(DESC d)</code>
	 * @param substring
	 * @return
	 */
	public DESC createDescriptionFromString(String substring);
}
