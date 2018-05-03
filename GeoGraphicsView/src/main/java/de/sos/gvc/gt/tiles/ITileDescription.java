package de.sos.gvc.gt.tiles;

import de.sos.gvc.gt.proj.LatLonPoint;

/**
 * 
 * @author scholvac
 *
 */
public interface ITileDescription {
	int 				getIdentifier();
	LatLonPoint			getCenter();
	LatLonBoundingBox	getBounds();
}
