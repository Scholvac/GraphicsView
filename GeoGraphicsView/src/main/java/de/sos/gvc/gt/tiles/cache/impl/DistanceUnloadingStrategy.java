package de.sos.gvc.gt.tiles.cache.impl;

import java.util.Collection;

import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.tiles.ITileDescription;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;
import de.sos.gvc.gt.tiles.cache.IUnloadStrategy;

/**
 * 
 * @author scholvac
 *
 */
public class DistanceUnloadingStrategy<DESC extends ITileDescription> implements IUnloadStrategy<DESC> {

	@Override
	public int unload(Collection<DESC> tileCache, LatLonBoundingBox lastArea) {
		LatLonPoint center = lastArea.getCenter();
		//Do we need to consider the zoom?
		double maxDist = 0;
		int id = 0;
		for (DESC tile : tileCache) {
			LatLonPoint tc = tile.getCenter();
			double d = tc.distanceSq(center);
			if (d > maxDist) {
				maxDist = d;
				id = tile.getIdentifier();
			}
		}
		return id;
	}


}
