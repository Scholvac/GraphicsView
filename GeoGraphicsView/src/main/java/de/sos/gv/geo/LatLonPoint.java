package de.sos.gv.geo;

public class LatLonPoint {

	public static final double NORTH_POLE = 90.0;
	public static final double SOUTH_POLE = -NORTH_POLE;
	public static final double DATELINE = 180.0;
	public static final double LON_RANGE = 360.0;


	private double			mLatitude = Double.NaN;
	private double			mLongitue = Double.NaN;

	public LatLonPoint() {
		this(0,0);
	}
	public LatLonPoint(final LatLonPoint other) {
		this(other.mLatitude, other.mLongitue);
	}
	public LatLonPoint(final double lat, final double lon) {
		mLatitude = lat;
		mLongitue = lon;
	}

	public void setLongitude(final double lon) { mLongitue = lon; }
	public void setLatitude(final double lat) { mLatitude = lat; }
	public double getLongitude() { return mLongitue; }
	public double getLatitude() { return mLatitude; }


	public final static double wrapedLongitude(double lon) {
		if ((lon < -DATELINE) || (lon > DATELINE)) {
			lon += DATELINE;
			lon = lon % LON_RANGE;
			lon = (lon < 0) ? DATELINE + lon : -DATELINE + lon;
		}
		return lon;
	}
	public static double normalizedLatitude(double lat) {
		if (lat > NORTH_POLE) {
			lat = NORTH_POLE;
		}
		if (lat < SOUTH_POLE) {
			lat = SOUTH_POLE;
		}
		return lat;
	}
	public LatLonPoint set(final double lat, final double lon) {
		mLatitude = lat;
		mLongitue = lon;
		return this;
	}
	public LatLonPoint set(final LatLonPoint ll) {
		mLatitude = ll.mLatitude;
		mLongitue = ll.mLongitue;
		return this;
	}

	@Override
	public String toString() {
		return "LatLonPoint[lat=" + mLatitude + ",lon=" + mLongitue + "]";
	}

}
