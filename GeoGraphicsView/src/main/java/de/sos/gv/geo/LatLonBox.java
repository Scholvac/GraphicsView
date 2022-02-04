package de.sos.gv.geo;

import java.util.Locale;

public class LatLonBox {

	private final LatLonPoint		mLowerLeft;
	private final LatLonPoint		mUpperRight;

	public LatLonBox() {
		this(new LatLonPoint(), new LatLonPoint(), true);
	}
	public LatLonBox(final double north, final double east, final double south, final double west) {
		this(new LatLonPoint(north, east), new LatLonPoint(south, west), true);
	}

	public LatLonBox(LatLonPoint ll, LatLonPoint ur) {
		this(ll, ur, false);
	}
	public LatLonBox(LatLonPoint ll, LatLonPoint ur, boolean use) {
		mLowerLeft = use ? ll : new LatLonPoint(ll);
		mUpperRight = use ? ur : new LatLonPoint(ur);
	}



	public void set(LatLonPoint _ll, LatLonPoint _ur) {
		mLowerLeft.set(_ll);
		mUpperRight.set(_ur);
	}
	public void correct() {
		double n = getNorth();
		double s = getSouth();
		if (s > n) {
			mUpperRight.setLatitude(s);
			mLowerLeft.setLatitude(n);
		}
		double e = getEast();
		double w = getWest();
		if (e < w) {
			mUpperRight.setLongitude(w);
			mLowerLeft.setLongitude(e);
		}
	}

	public double getNorth() {	return mUpperRight.getLatitude(); }
	public double getSouth() { return mLowerLeft.getLatitude(); }
	public double getEast() { return mUpperRight.getLongitude(); }
	public double getWest() { return mLowerLeft.getLongitude(); }



	public LatLonPoint[] getVertices() {
		return new LatLonPoint[] {
				new LatLonPoint(getSouth(), getWest()),
				new LatLonPoint(getNorth(), getWest()),
				new LatLonPoint(getNorth(), getEast()),
				new LatLonPoint(getSouth(), getEast())
		};
	}



	public LatLonPoint getUpperLeft(LatLonPoint store) { return store == null ? new LatLonPoint(getNorth(), getWest()) : store.set(getNorth(), getWest());}
	public LatLonPoint getLowerLeft(LatLonPoint store) { return store == null ? new LatLonPoint(mLowerLeft) : store.set(mLowerLeft);}
	public LatLonPoint getUpperRight(LatLonPoint store){ return store == null ? new LatLonPoint(mUpperRight) : store.set(mUpperRight);}
	public LatLonPoint getLowerRight(LatLonPoint store){ return store == null ? new LatLonPoint(getSouth(), getEast()) : store.set(getSouth(), getEast());}


	public LatLonPoint getUpperLeft() { return new LatLonPoint(getNorth(), getWest());}
	public LatLonPoint getLowerLeft() { return mLowerLeft;}
	public LatLonPoint getUpperRight(){ return mUpperRight;}
	public LatLonPoint getLowerRight(){ return new LatLonPoint(getSouth(), getEast());}


	public LatLonPoint getCenter(LatLonPoint store) {
		final double n = getNorth();
		final double s = getSouth();
		final double e = getEast();
		final double w = getWest();

		final double minX = e < w ? e : w;
		final double minY = n < s ? n : s;
		final double maxX = e < w ? w : e;
		final double maxY = n < s ? s : n;

		final double width = maxX - minX;
		final double height = maxY - minY;
		final double x = minX + (width * 0.5);
		final double y = minY + (height * 0.5);
		if (store == null)
			return new LatLonPoint(y, x);
		store.set(y, x);
		return store;
	}
	public LatLonPoint getCenter() {
		return getCenter(null);
	}

	public void setAndCorrect(LatLonPoint _ll, LatLonPoint _ur) {
		set(_ll, _ur);
		correct();
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "LL = [%1.5f, %1.5f]; UR = [%1.5f, %1.5f]", mLowerLeft.getLatitude(), mLowerLeft.getLongitude(), mUpperRight.getLatitude(), mUpperRight.getLongitude());
	}
	public String getVerticesString() {
		LatLonPoint[] v = getVertices();
		StringBuilder out = new StringBuilder("[");
		for (LatLonPoint p : v) {
			out.append(String.format(Locale.US, "[%1.5f, %1.5f],", p.getLongitude(), p.getLatitude()));
		}
		LatLonPoint p = v[0];
		out.append(String.format(Locale.US, "[%1.5f, %1.5f]]", p.getLongitude(), p.getLatitude()));
		return out.toString();
	}

}
