package de.sos.gvc.gt;

import java.awt.geom.Point2D;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.gt.proj.AbstractGCT;
import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.proj.MercatorMeterGCT;

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


	public static Point2D getPosition(LatLonPoint llp) {
		return mMercator.forward(llp);
	}
	
	public static Point2D getPosition(double lat, double lon) {
		return mMercator.forward(lat, lon);
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


	
	
}
