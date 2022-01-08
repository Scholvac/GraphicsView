package de.sos.gvc.gt.tiles;

import java.awt.geom.Point2D;
import java.util.Locale;

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
		return String.format(Locale.US, "LL = [%1.5f, %1.5f]; UR = [%1.5f, %1.5f]", lowerLeft.getLatitude(), lowerLeft.getLongitude(), upperRight.getLatitude(), upperRight.getLongitude());
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
	
	public boolean contains(final LatLonPoint llpoint) {
		final double x = llpoint.getLongitude();
		final double y = llpoint.getLatitude();
		
		final double x0 = lowerLeft.getLongitude();
		final double y0 = lowerLeft.getLatitude();
		final double x1 = upperRight.getLongitude();
		final double y1 = upperRight.getLatitude();
		
		return 	x >= x0 &&
				y >= y0 &&
				x <= x1 &&
				y <= y1;
	}
	
	public boolean contains(final LatLonBoundingBox llbb) {
		final double tx0 = lowerLeft.getLongitude();
		final double ty0 = lowerLeft.getLatitude();
		final double tx1 = upperRight.getLongitude();
		final double ty1 = upperRight.getLatitude();
		
		final double ox0 = llbb.getWest();
		final double ox1 = llbb.getEast();
		final double oy0 = llbb.getSouth();
		final double oy1 = llbb.getNorth();
	
		return 	ox0 >= tx0 &&
				oy0 >= ty0 &&
				ox1 <= tx1 &&
				oy1 <= ty1;
	}
	
	public boolean intersects(final LatLonBoundingBox llbb) {
		final double tx0 = lowerLeft.getLongitude();
		final double ty0 = lowerLeft.getLatitude();
		final double tx1 = upperRight.getLongitude();
		final double ty1 = upperRight.getLatitude();
		
		final double ox0 = llbb.getWest();
		final double ox1 = llbb.getEast();
		final double oy0 = llbb.getSouth();
		final double oy1 = llbb.getNorth();
	
		return 	ox1 > tx0 &&
				oy1 > ty0 &&
				ox0 < tx1 &&
				oy0 < ty1;
	}

	public boolean containsOrIntersects(final LatLonBoundingBox llbb) {
		final double tx0 = lowerLeft.getLongitude();
		final double ty0 = lowerLeft.getLatitude();
		final double tx1 = upperRight.getLongitude();
		final double ty1 = upperRight.getLatitude();
		
		final double ox0 = llbb.getWest();
		final double ox1 = llbb.getEast();
		final double oy0 = llbb.getSouth();
		final double oy1 = llbb.getNorth();
	
		return 
//				(	ox0 <= tx0 && //contains
//					oy0 <= ty0 &&
//					ox1 >= tx1 &&
//					oy1 >= ty1
//				) ||
				(	ox1 > tx0 && //intersects
					oy1 > ty0 &&
					ox0 < tx1 &&
					oy0 < ty1
				);
	}


}

//53.529999, 8.641456 
//53.523495, 8.641542