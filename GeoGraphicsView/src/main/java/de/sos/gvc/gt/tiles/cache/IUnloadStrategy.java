package de.sos.gvc.gt.tiles.cache;

import java.util.Collection;

import de.sos.gvc.gt.tiles.ITileDescription;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;

/**
 * 
 * @author scholvac
 *
 */
public interface IUnloadStrategy<DESC extends ITileDescription> {

	/**
	 * Returns the identifier (ITileDescription.getIdentifier()) of the tile that shall be unloaded
	 * @param tileCache cache
	 * @param lastArea 
	 * @return
	 */
	int unload(Collection<DESC> tileCache, LatLonBoundingBox lastArea);
		
}