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

	public LatLonBox(final LatLonPoint ll, final LatLonPoint ur) {
		this(ll, ur, false);
	}
	public LatLonBox(final LatLonPoint ll, final LatLonPoint ur, final boolean use) {
		mLowerLeft = use ? ll : new LatLonPoint(ll);
		mUpperRight = use ? ur : new LatLonPoint(ur);
	}

	public LatLonBox add(final LatLonPoint ll) {
		return add(ll.getLatitude(), ll.getLongitude());
	}

	public LatLonBox setAndCorrect(final double lat1, final double lon1, final double lat2, final double lon2) {
		set(lat1, lon1, lat2, lon2);
		return correct();
	}
	private LatLonBox add(final double latitude, final double longitude) {
		final double n = getNorth();
		final double s = getSouth();
		if (latitude > n) mUpperRight.setLatitude(latitude);
		if (latitude < s) mLowerLeft.setLatitude(latitude);

		final double e = getEast();
		final double w = getWest();
		if (longitude < w) mLowerLeft.setLongitude(longitude);
		if (longitude > e) mUpperRight.setLongitude(longitude);
		return this;
	}
	public LatLonBox set(final double lat1, final double lon1, final double lat2, final double lon2) {
		mLowerLeft.set(lat1, lon1);
		mUpperRight.set(lat2, lon2);
		return this;
	}
	public void set(final LatLonPoint _ll, final LatLonPoint _ur) {
		mLowerLeft.set(_ll);
		mUpperRight.set(_ur);
	}
	public LatLonBox correct() {
		final double n = getNorth();
		final double s = getSouth();
		if (s > n) {
			mUpperRight.setLatitude(s);
			mLowerLeft.setLatitude(n);
		}
		final double e = getEast();
		final double w = getWest();
		if (e < w) {
			mUpperRight.setLongitude(w);
			mLowerLeft.setLongitude(e);
		}
		return this;
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



	public LatLonPoint getUpperLeft(final LatLonPoint store) { return store == null ? new LatLonPoint(getNorth(), getWest()) : store.set(getNorth(), getWest());}
	public LatLonPoint getLowerLeft(final LatLonPoint store) { return store == null ? new LatLonPoint(mLowerLeft) : store.set(mLowerLeft);}
	public LatLonPoint getUpperRight(final LatLonPoint store){ return store == null ? new LatLonPoint(mUpperRight) : store.set(mUpperRight);}
	public LatLonPoint getLowerRight(final LatLonPoint store){ return store == null ? new LatLonPoint(getSouth(), getEast()) : store.set(getSouth(), getEast());}


	public LatLonPoint getUpperLeft() { return new LatLonPoint(getNorth(), getWest());}
	public LatLonPoint getLowerLeft() { return mLowerLeft;}
	public LatLonPoint getUpperRight(){ return mUpperRight;}
	public LatLonPoint getLowerRight(){ return new LatLonPoint(getSouth(), getEast());}


	public LatLonPoint getCenter(final LatLonPoint store) {
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
		final double x = minX + width * 0.5;
		final double y = minY + height * 0.5;
		if (store == null)
			return new LatLonPoint(y, x);
		store.set(y, x);
		return store;
	}
	public LatLonPoint getCenter() {
		return getCenter(null);
	}

	public LatLonBox setAndCorrect(final LatLonPoint _ll, final LatLonPoint _ur) {
		set(_ll, _ur);
		return correct();
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "LL = [%1.5f, %1.5f]; UR = [%1.5f, %1.5f]", mLowerLeft.getLatitude(), mLowerLeft.getLongitude(), mUpperRight.getLatitude(), mUpperRight.getLongitude());
	}
	public String getVerticesString() {
		final LatLonPoint[] v = getVertices();
		final StringBuilder out = new StringBuilder("[");
		for (final LatLonPoint p : v) {
			out.append(String.format(Locale.US, "[%1.5f, %1.5f],", p.getLongitude(), p.getLatitude()));
		}
		final LatLonPoint p = v[0];
		out.append(String.format(Locale.US, "[%1.5f, %1.5f]]", p.getLongitude(), p.getLatitude()));
		return out.toString();
	}




}
