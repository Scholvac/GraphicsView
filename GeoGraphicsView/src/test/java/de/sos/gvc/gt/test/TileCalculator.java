package de.sos.gvc.gt.test;

import static org.junit.Assert.assertEquals;

import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;

public class TileCalculator {

	public static void main(String[] args) {
		TileCalculator tc = new TileCalculator();
		tc.testBremerhaven();
		tc.testWorld();
		
	}

	private void testBremerhaven() {
		LatLonPoint ul = new LatLonPoint.Double(53.52589, 8.62223);
		LatLonPoint lr = new LatLonPoint.Double(53.51660, 8.65886);
		
		int imgWidth = 1900;
		int expectedZoom = 16;
		
		int expectedTileMinX = 34337, expectedTileMaxX = 34344;
		int expectedTileMinY = 21188, expectedTileMaxY = 21191;
	
		
		int zoom = new OSMTileFactory().calculateZoom(new LatLonBoundingBox(ul, lr), imgWidth);
		assertEquals(expectedZoom, zoom);
		int[][] tileAdresses = OSMTileFactory.getTileNumbers(ul, lr, zoom);
		int tminx = Integer.MAX_VALUE, tminy = Integer.MAX_VALUE;
		int tmaxx = -Integer.MAX_VALUE, tmaxy = -Integer.MAX_VALUE;
		for (int i = 0; i < tileAdresses.length; i++) {
			tminx = Math.min(tminx, tileAdresses[i][0]);
			tmaxx = Math.max(tmaxx, tileAdresses[i][0]);
			tminy = Math.min(tminy, tileAdresses[i][1]);
			tmaxy = Math.max(tmaxy, tileAdresses[i][1]);
		}
		assertEquals(expectedTileMaxX, tmaxx);
		assertEquals(expectedTileMinX, tminx);
		assertEquals(expectedTileMaxY, tmaxy);
		assertEquals(expectedTileMinY, tminy);
	}
	
	
	
	
	private void testWorld() {
		LatLonPoint ul = new LatLonPoint.Double(-104.34810, 77.41615);
		LatLonPoint lr = new LatLonPoint.Double( 154.75347, -10.02165);
		
		int imgWidth = 1900;
		int expectedZoom = 4;
		
		int expectedTileMinX = 7, expectedTileMaxX = 11;
		int expectedTileMinY = 0, expectedTileMaxY = 15;
	
		
		int zoom = new OSMTileFactory().calculateZoom(new LatLonBoundingBox(ul, lr), imgWidth);
		assertEquals(zoom, expectedZoom);
		int[][] tileAdresses = OSMTileFactory.getTileNumbers(ul, lr, zoom);
		int tminx = Integer.MAX_VALUE, tminy = Integer.MAX_VALUE;
		int tmaxx = -Integer.MAX_VALUE, tmaxy = -Integer.MAX_VALUE;
		for (int i = 0; i < tileAdresses.length; i++) {
			tminx = Math.min(tminx, tileAdresses[i][0]);
			tmaxx = Math.max(tmaxx, tileAdresses[i][0]);
			tminy = Math.min(tminy, tileAdresses[i][1]);
			tmaxy = Math.max(tmaxy, tileAdresses[i][1]);
		}
		assertEquals(expectedTileMaxX, tmaxx);
		assertEquals(expectedTileMinX, tminx);
		assertEquals(expectedTileMaxY, tmaxy);
		assertEquals(expectedTileMinY, tminy);
	}

}

