package de.sos.gvc.gt.tiles;

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



	public LatLonPoint getCenter() {
		double sn = getNorth() - getSouth();
		double we = getEast() - getWest();
		return new LatLonPoint.Double(getSouth() + sn / 2.0, getEast() + we / 2);
	}
	
}

//53.529999, 8.641456 
//53.523495, 8.641542