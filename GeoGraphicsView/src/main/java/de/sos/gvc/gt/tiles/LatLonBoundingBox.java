package de.sos.gvc.gt.tiles;

import java.awt.geom.Point2D;

import de.sos.gvc.gt.GeoUtils;
import de.sos.gvc.gt.proj.LatLonPoint;

/**
 * 
 * @author scholvac
 *
 */
public class LatLonBoundingBox {

	private LatLonPoint			lowerLeft;
	private LatLonPoint			upperRight;
	
	public LatLonBoundingBox(LatLonPoint ll, LatLonPoint ur) {
		lowerLeft = ll;
		upperRight = ur;
		correct();
	}
	
	public LatLonBoundingBox(double north, double east, double south, double west) {
		this(new LatLonPoint.Double(north, east), new LatLonPoint.Double(south, west));
	}



	public void correct() {
		double n = getNorth();
		double s = getSouth();
		if (s > n) {
			upperRight.setLatitude(s);
			lowerLeft.setLatitude(n);
		}
		double e = getEast();
		double w = getWest();
		if (e < w) {
			upperRight.setLongitude(w);
			lowerLeft.setLongitude(e);
		}
	}



	public double getNorth() {	return upperRight.getLatitude(); }
	public double getSouth() { return lowerLeft.getLatitude(); }
	public double getEast() { return upperRight.getLongitude(); }
	public double getWest() { return lowerLeft.getLongitude(); }



	public LatLonPoint[] getVertices() {
		return new LatLonPoint[] {
				new LatLonPoint.Double(getSouth(), getWest()), 
				new LatLonPoint.Double(getNorth(), getWest()),
				new LatLonPoint.Double(getNorth(), getEast()),
				new LatLonPoint.Double(getSouth(), getEast())
		};
	}



	public LatLonPoint getUpperLeft() { return new LatLonPoint.Double(getNorth(), getWest());}
	public LatLonPoint getLowerLeft() { return lowerLeft;}
	public LatLonPoint getUpperRight(){ return upperRight;}
	public LatLonPoint getLowerRight(){ return new LatLonPoint.Double(getSouth(), getEast());
	}


	public LatLonPoint getCenter(LatLonPoint store) {
		double sn = getNorth() - getSouth();
		double we = getEast() - getWest();
		if (store == null)
			return new LatLonPoint.Double(getSouth() + sn / 2.0, getEast() + we / 2);
		store.setLatLon(getSouth() + sn / 2.0, getEast() + we / 2);
		return store;
	}
	public LatLonPoint getCenter() {
		return getCenter(null);
	}
	
	
	@Override
	public String toString() {
		return String.format("LL = [%1.5f, %1.5f]; UR = [%1.5f, %1.5f]", lowerLeft.getLatitude(), lowerLeft.getLongitude(), upperRight.getLatitude(), upperRight.getLongitude());
	}

	/** returns the width of this box in meter */
	public double getWidth() {
		Point2D ll = GeoUtils.getPosition(getLowerLeft());
		Point2D lr = GeoUtils.getPosition(getLowerRight());
		return lr.getX() - ll.getX();
	}
	/** returns the width of this box in meter */
	public double getHeight() {
		Point2D ll = GeoUtils.getPosition(getLowerLeft());
		Point2D ul = GeoUtils.getPosition(getUpperLeft());
		return ul.getY() - ll.getY();
	}


}

//53.529999, 8.641456 
//53.523495, 8.641542