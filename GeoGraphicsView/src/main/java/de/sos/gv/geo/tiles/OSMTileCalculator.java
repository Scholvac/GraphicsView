package de.sos.gv.geo.tiles;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.LatLonPoint;

public class OSMTileCalculator implements ITileCalculator {


	@Override
	public int[][] calculateTileCoordinates(final LatLonBox area, final int imgWidth) {
		int zoom = calculateZoom(area, imgWidth);
		if (zoom < 0){
			return null;
		}
		if (zoom < 2)
			zoom = 2;
		return calculateTileCoordinates(area.getUpperLeft(), area.getLowerRight(), zoom+1);
	}

	public static LatLonPoint tileCenter(final int tileX, final int tileY, final int zoom) {
		final double lat = tile2lat(tileY, zoom);
		final double lon = tile2lon(tileX, zoom);
		return new LatLonPoint(lat, lon);
	}


	public static LatLonBox tile2boundingBox(final int x, final int y, final int zoom) {
		final double n = tile2lat(y, zoom);
		final double s = tile2lat(y + 1, zoom);
		final double w = tile2lon(x, zoom);
		final double e = tile2lon(x + 1, zoom);
		return new LatLonBox(n, e, s, w);
	}

	private final LatLonPoint			_ul = new LatLonPoint();
	private final LatLonPoint			_ur = new LatLonPoint();
	private int calculateZoom(final LatLonBox area, final double imgWidth) {
		final double distanceMeter = GeoUtils.distance(area.getUpperLeft(_ul), area.getUpperRight(_ur));
		final double reqMeterPerPixel = distanceMeter / imgWidth;
		final double llRadLat = area.getLowerLeft().getLatitude() * GeoUtils.TO_RAD;
		final double cos_llRadLat = Math.cos(llRadLat);
		for (int i = 0; i < 20; i++) {
			final double zoomMeterPerPixel = 156543.03 * cos_llRadLat / Math.pow(2, i);//Exact length of the equator (according to wikipedia) is 40075.016686 km in WGS-84. At zoom 0, one pixel would equal 156543.03 meters (assuming a tile size of 256 px):
			if (zoomMeterPerPixel <= reqMeterPerPixel)
				return i-2;
		}
		return -1;
	}


	public static int[][] calculateTileCoordinates(final LatLonPoint ul, final LatLonPoint lr, final int zoom) {
		int ul_x = lon2tileNumberX(ul.getLongitude(), zoom);
		int ul_y = lat2tileNumberY(ul.getLatitude(), zoom);
		int lr_x = lon2tileNumberX(lr.getLongitude(), zoom);
		int lr_y = lat2tileNumberY(lr.getLatitude(), zoom);

		if (ul_x > lr_x) {
			final int t = ul_x;
			ul_x = lr_x; lr_x = t;
		}
		if (ul_y > lr_y) {
			final int t = ul_y;
			ul_y = lr_y; lr_y = t;
		}

		final int num = Math.abs((lr_x-ul_x+1) * (lr_y - ul_y+1));
		final int[][] out = new int[num][]; int idx = 0;
		for (int x = ul_x; x <= lr_x; x++) {
			for (int y = ul_y; y <= lr_y; y++) {
				out[idx++] = new int[] {x,y,zoom};
			}
		}
		return out;
	}


	public static double lon2tileX(final double lon, final int zoom) {
		return (lon + 180) / 360 * (1 << zoom);
	}
	public static int lon2tileNumberX(final double lon, final int zoom) {
		int xtile = (int) Math.floor(lon2tileX(lon, zoom));
		if (xtile < 0)
			xtile = 0;
		if (xtile >= 1 << zoom)
			xtile = (1 << zoom) - 1;
		return xtile;
	}
	public static double tile2lon(final int x, final int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}

	public static int lat2tileNumberY(final double lat, final int zoom) {
		int ytile = (int) Math.floor(lat2tileY(lat, zoom));
		if (ytile < 0)
			ytile = 0;
		if (ytile >= 1 << zoom)
			ytile = (1 << zoom) - 1;
		return ytile;
	}
	public static double lat2tileY(final double lat, final int zoom) {
		return (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom);
	}
	public static double tile2lat(final int y, final int z) {
		final double n = Math.PI - 2.0 * Math.PI * y / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}
}
