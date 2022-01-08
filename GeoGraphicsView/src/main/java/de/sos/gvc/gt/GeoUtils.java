package de.sos.gvc.gt;

import java.awt.geom.Point2D;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.gt.proj.AbstractGCT;
import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.proj.MercatorMeterGCT;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;

/**
 * 
 * @author scholvac
 *
 */
public class GeoUtils {
	
	private static AbstractGCT		mMercator = new MercatorMeterGCT();
	
	
	public static LatLonPoint getLatLon(double x, double y) {
		return mMercator.inverse(x, y);
	}
	public static LatLonPoint getLatLon(Point2D p) {
		return getLatLon(p.getX(), p.getY());
	}
	public static LatLonPoint getLatLon(final double x, final double y, LatLonPoint store) {
		return mMercator.inverse(x, y, store);
	}


	public static Point2D getPosition(LatLonPoint llp) {
		return mMercator.forward(llp);
	}
	
	public static Point2D getPosition(double lat, double lon) {
		return mMercator.forward(lat, lon);
	}
	public static Point2D getPosition(final double lat, final double lon, Point2D store) {
		return mMercator.forward(lat, lon, store);
	}

	public static void setViewCenter(GraphicsView view, LatLonPoint llp) {
		Point2D m = getPosition(llp);
		setViewCenter(view, m.getX(), m.getY());
	}

	public static void setViewCenter(GraphicsView view, double x, double y) {
		view.setCenter(x, -y); //invert to undo the YScale = -1
	}

	public static void setPosition(GraphicsItem item, LatLonPoint location) {
		item.setCenter(getPosition(location));
	}

	public static void setGeoPosition(GraphicsItem item, double lat, double lon) {
		item.setCenter(getPosition(lat, lon));
	}
	public static void setGeoPosition(GraphicsItem item, LatLonPoint llp) {
		item.setCenter(getPosition(llp));
	}
	public static Point2D getPosition(LatLonPoint ll, Point2D store) {
		return mMercator.forward(ll.getLatitude(), ll.getLongitude(), store);
	}
	public static LatLonBoundingBox getLatLonBoundingBox(double minX, double minY, double maxX, double maxY) {
		LatLonPoint ll = getLatLon(minX, maxY);
		LatLonPoint ur = getLatLon(maxX, minY);
		return new LatLonBoundingBox(ll, ur);
	}

}
