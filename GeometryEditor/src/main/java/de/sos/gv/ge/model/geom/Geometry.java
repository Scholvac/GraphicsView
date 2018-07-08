package de.sos.gv.ge.model.geom;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Geometry implements IGeometry {

	private GeometryType mType;
	
	private ArrayList<IGeometry>	mSubGeometries = null;
	private ArrayList<Point2D>		mPoints = new ArrayList<>();

	public Geometry(GeometryType type) {
		mType = type;
	}
	
	@Override
	public int numGeometries() {
		if (mSubGeometries != null)
			return mSubGeometries.size();
		return 1; 
	}

	@Override
	public int numPoints() {
		return mPoints.size();
	}

	@Override
	public IGeometry getGeometry(int idx) {
		if (idx == 0) return this;
		return mSubGeometries.get(idx); //nullpointer and array out of bounds exception possible
	}

	@Override
	public Point2D getPoint(int idx) {
		return mPoints.get(idx);
	}

	@Override
	public GeometryType getType() {
		return mType;
	}

	@Override
	public void addPoint(int idx, Point2D point) {
		Point2D p0 = mPoints.isEmpty() == false ? mPoints.get(0) : null;
		if (p0 != null && p0.getX()  == point.getX() && p0.getY() == point.getY())
			return; //should only affect the last point in a polygon and linear ring, otherwise the geometry is wrong?
		mPoints.add(idx, point);
	}

	@Override
	public Point2D removePoint(int idx) {
		return mPoints.remove(idx);
	}

	@Override
	public Point2D replacePoint(int idx, Point2D point) {
		return mPoints.set(idx, point);
	}

	@Override
	public void addGeometry(int idx, IGeometry geometry) {
		if (mSubGeometries == null) 
			mSubGeometries = new ArrayList<>();
		mSubGeometries.add(idx, geometry);
	}

	@Override
	public IGeometry removeGeometry(int idx) {
		IGeometry r = mSubGeometries.remove(idx);
		if (mSubGeometries.isEmpty())
			mSubGeometries = null;
		return r;
	}
	
	 

}
