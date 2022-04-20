package de.sos.gv.geo.tiles;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.LatLonPoint;

public class TileInfo implements Comparable<TileInfo> {

	static final long[] hashOffset;

	static {
		hashOffset = new long[21];
		for (int i = 0; i <= 20; i++) {
			hashOffset[i] = (long) Math.pow(2, i);
		}
	}


	private final int							mTileX;
	private final int							mTileY;
	private final int							mTileZ;

	private	LatLonPoint							mCenter;
	private LatLonBox							mBounds;
	private Rectangle2D							mShape;
	private Double 								mXYCenter;
	private String 								mHash;

	public TileInfo(final int[] arr) {
		assert arr != null && arr.length == 3;
		mTileX = arr[0];
		mTileY = arr[1];
		mTileZ = arr[2];
	}

	public final int tileX() { return mTileX;}
	public final int tileY() { return mTileY;}
	public final int tileZ() { return mTileZ;}

	@Override
	public int compareTo(final TileInfo o) {
		int res = Integer.compare(mTileZ, o.mTileZ);
		if (res == 0)
			res = Integer.compare(mTileY, o.mTileY);
		if (res == 0)
			res = Integer.compare(mTileX, o.mTileX);
		return res;
	}

	@Override
	public int hashCode() {
		return (int)getHashCode();
	}
	public long getHashCode() {
		final long oz = hashOffset[mTileZ < 20 ? mTileZ : 20];
		final long ox = oz * mTileX + mTileY;
		final long h = oz * oz + ox;
		return h;

	}
	@Override
	public boolean equals(final Object obj) {
		if (obj == null || obj instanceof TileInfo == false)
			return false;
		final TileInfo o = (TileInfo)obj;
		if ( 	mTileX == o.mTileX &&
				mTileY == o.mTileY &&
				mTileZ == o.mTileZ)
			return true;
		return false;
	}

	public LatLonBox getLatLonBounds() {
		if (mBounds == null) {
			mBounds = OSMTileCalculator.tile2boundingBox(mTileX, mTileY, mTileZ);
		}
		return mBounds;
	}
	public LatLonPoint getWGSCenter() {
		if (mCenter == null)
			mCenter = getLatLonBounds().getCenter();
		return mCenter;
	}
	public Point2D.Double getXYCenter() {
		if (mXYCenter == null)
			mXYCenter = GeoUtils.getXY(getWGSCenter());
		return mXYCenter;
	}

	public Shape getShape() {
		if (mShape == null) {
			final LatLonBox bb = getLatLonBounds();
			bb.correct();
			final Point2D.Double ll = GeoUtils.getXY(bb.getLowerLeft());
			final Point2D.Double ur = GeoUtils.getXY(bb.getUpperRight());
			final double w = ur.getX() - ll.getX(), h = ur.getY() - ll.getY();
			mShape = new Rectangle2D.Double(-w*0.5, h*0.5, w, -h);
		}
		return mShape;
	}

	public static String getUniqueIdentifier(final int[] tileArray) {
		return new StringBuffer().append(tileArray[0]).append("_").append(tileArray[1]).append("_").append(tileArray[2]).toString();
	}

	public String getHash() {
		if (mHash == null)
			mHash = new StringBuffer().append(mTileX).append("_").append(mTileY).append("_").append(mTileZ).toString();
		return mHash;
	}

	public static TileInfo fromHash(final String hash) {
		if (hash == null || hash.isEmpty())
			return null;
		final String[] split = hash.split("_");
		if (split.length != 3)
			return null;
		try {
			return new TileInfo(new int[] {
					Integer.parseInt(split[0].trim()),
					Integer.parseInt(split[1].trim()),
					Integer.parseInt(split[2].trim())
			});
		}catch(final Exception e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return getHash();
	}

}
