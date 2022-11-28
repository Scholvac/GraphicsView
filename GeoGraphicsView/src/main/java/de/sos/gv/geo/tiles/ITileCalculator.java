package de.sos.gv.geo.tiles;

import java.awt.Rectangle;

import de.sos.gv.geo.LatLonBox;

public interface ITileCalculator {

	public int[][] calculateTileCoordinates(LatLonBox area, final int imgWidth);
}
