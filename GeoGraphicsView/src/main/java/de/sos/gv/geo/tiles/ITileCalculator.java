package de.sos.gv.geo.tiles;

import de.sos.gv.geo.LatLonBox;

public interface ITileCalculator {

	int[][] calculateTileCoordinates(LatLonBox area, final int imgWidth);

	void setMaximumZoom(int maxZoom);
	int getMaximumZoom();
}
