package de.sos.gvc.gt.tiles.osm;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;

import de.sos.gvc.gt.GeoUtils;
import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.tiles.ITileLoader;
import de.sos.gvc.gt.tiles.ITileProvider;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;
import de.sos.gvc.gt.tiles.LazyTileItem;
import de.sos.gvc.gt.tiles.cache.CacheData;
import de.sos.gvc.gt.tiles.cache.ICacheDataFactory;


/**
 * 
 * @author scholvac
 *
 */
public class OSMTileFactory implements ITileProvider<OSMTileDescription> {
	
	//usefull links: 	http://tools.geofabrik.de/map/#16/53.5208/8.6397&type=Geofabrik_Standard&grid=1
	//					https://gis.stackexchange.com/questions/19632/how-to-calculate-the-optimal-zoom-level-to-display-two-or-more-points-on-a-map
	//					https://wiki.openstreetmap.org/wiki/Zoom_levels
	//					https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames

	
	String				mBaseURL = null;
	
	public OSMTileFactory() {
		this("http://tile.openstreetmap.org/");
	}
	public OSMTileFactory(String baseURL) {
		mBaseURL = baseURL;
	}
	
	public String getBaseURL() { return mBaseURL; }
	
	@Override
	public Collection<OSMTileDescription> getTileDescriptions(LatLonBoundingBox area, Rectangle2D viewArea) {
		int zoom = calculateZoom(area, viewArea.getWidth());
		if (zoom < 0)
			return new ArrayList<>();
		int[][] tiles = getTileNumbers(area.getUpperLeft(), area.getLowerRight(), zoom+1);
		ArrayList<OSMTileDescription> out = new ArrayList<>();
		for (int i = 0; i < tiles.length; i++) {
			out.add(new OSMTileDescription(tiles[i][0], tiles[i][1], tiles[i][2]));
		}
		return out;
	}
	
		
	
	public static LatLonPoint tileCenter(int tileX, int tileY, int zoom) {
		double lat = tile2lat(tileY, zoom);
		double lon = tile2lon(tileX, zoom);
		return new LatLonPoint.Double(lat, lon);
	}
	
	
	public static LatLonBoundingBox tile2boundingBox(final int x, final int y, final int zoom) {
		double n = tile2lat(y, zoom);
		double s = tile2lat(y + 1, zoom);
		double w = tile2lon(x, zoom);
		double e = tile2lon(x + 1, zoom);
		return new LatLonBoundingBox(n, e, s, w);
	}
	
	
	public int calculateZoom(LatLonBoundingBox area, double imgWidth) {
		Point2D mul = GeoUtils.getPosition(area.getUpperLeft());
		Point2D mur = GeoUtils.getPosition(area.getUpperRight());
		double distanceMeter = Math.abs(mul.getX() - mur.getX());
		double reqMeterPerPixel = distanceMeter / imgWidth;
		for (int i = 0; i < 20; i++) {
			double zoomMeterPerPixel = ( 156543.03 * Math.cos(area.getLowerLeft().getRadLat()) ) / Math.pow(2, i);//Exact length of the equator (according to wikipedia) is 40075.016686 km in WGS-84. At zoom 0, one pixel would equal 156543.03 meters (assuming a tile size of 256 px):
			if (zoomMeterPerPixel <= reqMeterPerPixel)
				return i-1;
		}
		return -1;
	}

	public static int[][] getTileNumbers(LatLonPoint ul, LatLonPoint lr, int zoom) {
		int ul_x = lon2tileNumberX(ul.getLongitude(), zoom);
		int ul_y = lat2tileNumberY(ul.getLatitude(), zoom);
		int lr_x = lon2tileNumberX(lr.getLongitude(), zoom);
		int lr_y = lat2tileNumberY(lr.getLatitude(), zoom);
		
		if (ul_x > lr_x) {
			int t = ul_x;
			ul_x = lr_x; lr_x = t;
		}
		if (ul_y > lr_y) {
			int t = ul_y;
			ul_y = lr_y; lr_y = t;
		}
		
		int num = Math.abs((lr_x-ul_x+1) * (lr_y - ul_y+1));
		int[][] out = new int[num][]; int idx = 0;
		for (int x = ul_x; x <= lr_x; x++) {
			for (int y = ul_y; y <= lr_y; y++) {
				out[idx++] = new int[] {x,y,zoom};
			}
		}
		return out;
	}
	
	
	public static double lon2tileX(double lon, int zoom) {
		return (lon + 180) / 360 * (1 << zoom);
	}
	public static int lon2tileNumberX(double lon, int zoom) {
		int xtile = (int) Math.floor(lon2tileX(lon, zoom));
		if (xtile < 0)
			xtile = 0;
		if (xtile >= (1 << zoom))
			xtile = ((1 << zoom) - 1);
		return xtile;				
	}
	public static double tile2lon(int x, int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}

	public static int lat2tileNumberY(double lat, int zoom) {
		int ytile = (int) Math.floor(lat2tileY(lat, zoom));
		if (ytile < 0)
			ytile = 0;
		if (ytile >= (1 << zoom))
			ytile = ((1 << zoom) - 1);
		return ytile;
	}
	public static double lat2tileY(double lat, int zoom) {
		return (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom);
	}
	public static double tile2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}
	

	@Override
	public ITileLoader<OSMTileDescription> createTileLoader() {
		return new OSMTileDownloader(mBaseURL);
	}


	@Override
	public void notifyParentUnloadedTile(LazyTileItem<OSMTileDescription> tile) {
		//Nothing to do here - we do not want to delete tiles from OSM or they and the rest of the world will become angry :)
	}


	@Override
	public <PARENT_CACHE_VALUE> void notifyParentUnloadedTile(CacheData<OSMTileDescription, PARENT_CACHE_VALUE> data,
			ICacheDataFactory<OSMTileDescription, PARENT_CACHE_VALUE> cacheFactory) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getStringDescription(OSMTileDescription desc) {
		return "OSM_" + desc.getZoom() + "_" + desc.getTileX() + "_" + desc.getTileY();
	}
	@Override
	public OSMTileDescription createDescriptionFromString(String substring) {
		String[] str = substring.split("_");
		if (str.length != 4) return null;
		if (str[0].equals("OSM") == false) return null;
		int z = Integer.parseInt(str[1]);
		int x = Integer.parseInt(str[2]);
		int y = Integer.parseInt(str[3]);
		return new OSMTileDescription(x, y, z);
	}

}
