package de.sos.gvc.gt.tiles.osm;

import javax.management.MXBean;

import de.sos.gvc.gt.proj.HashCodeUtil;
import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.tiles.ITileDescription;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;


/**
 * 
 * @author scholvac
 *
 */
public class OSMTileDescription implements ITileDescription{

	private int					mTileX;
	private int					mTileY;
	private int					mZoom;
	
	private LatLonPoint			mCenter;
	private LatLonBoundingBox	mBounds;
	
	private int					mHash;
	
	public OSMTileDescription(int x, int y, int zoom) {
		mTileX = x; mTileY = y; mZoom = zoom;
		
		 mHash = 54;
		 mHash = HashCodeUtil.hash(mHash, x);
		 mHash = HashCodeUtil.hash(mHash, y);
		 mHash = HashCodeUtil.hash(mHash, zoom);
	}
		
	public int getTileX() { return mTileX; }
	public int getTileY() { return mTileY; }
	public int getZoom() { return mZoom; }
	
	@Override
	public int getIdentifier() {
		return mHash;
	}

	@Override
	public LatLonPoint getCenter() {
		if (mCenter == null) {
			mCenter = OSMTileFactory.tileCenter(mTileX, mTileY, mZoom);
		}
		return mCenter;
	}

	@Override
	public LatLonBoundingBox getBounds() {
		if (mBounds == null) {
			mBounds = OSMTileFactory.tile2boundingBox(mTileX, mTileY, mZoom);
		}
		return mBounds;
	}

	@Override
	public int hashCode() {
		return mHash;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OSMTileDescription == false) return false;
		OSMTileDescription o = (OSMTileDescription)obj;
		return mTileX == o.mTileX && mTileY == o.mTileY && mZoom == o.mZoom;
	}
	
	@Override
	public String toString() {
		return "Tile[" + mZoom + " / " + mTileX + " / " + mTileY + "]";
	}
}
