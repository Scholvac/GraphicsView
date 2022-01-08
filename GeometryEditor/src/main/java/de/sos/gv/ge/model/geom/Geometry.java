package de.sos.gv.ge.model.geom;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Geometry implements IGeometry {

	public static IGeometry rectangle(double width, double height) {
		Geometry geom = new Geometry(GeometryType.Polygon);
		geom.addPoint(0, new Point2D.Double(-width, height));
		geom.addPoint(1, new Point2D.Double( width, height));
		geom.addPoint(2, new Point2D.Double( width,-height));
		geom.addPoint(3, new Point2D.Double(-width,-height));
//		geom.addPoint(0, new Point2D.Double(-width, height));
		return geom;
	}
	 
	
	private GeometryType mType;
	
	private ArrayList<IGeometry>	mSubGeometries = null;
	private ArrayList<Point2D>		mPoints = new ArrayList<>();
	
	private PropertyChangeSupport	mPCS = null;

	Geometry() {
		mType = GeometryType.Point;
	}
	public Geometry(GeometryType type) {
		mType = type;
	}
	
	@Override
	public void addListener(PropertyChangeListener listener) {
		if (mPCS == null) mPCS = new PropertyChangeSupport(this);
		mPCS.addPropertyChangeListener(listener);		
	}

	@Override
	public void removeListener(PropertyChangeListener listener) {
		if (mPCS != null)
			mPCS.removePropertyChangeListener(listener);
		if (!mPCS.hasListeners(null))
			mPCS = null;
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
	public void setType(GeometryType type) {
		mType = type;
	}

	@Override
	public void addPoint(int idx, Point2D point) {
		Point2D p0 = mPoints.isEmpty() == false ? mPoints.get(0) : null;
		if (p0 != null && p0.getX()  == point.getX() && p0.getY() == point.getY())
			return; //should only affect the last point in a polygon and linear ring, otherwise the geometry is wrong?
		mPoints.add(idx, point);
		if (mPCS != null) mPCS.firePropertyChange("Points", null, point);
	}

	@Override
	public Point2D removePoint(int idx) {
		Point2D old = mPoints.remove(idx);
		if (mPCS != null) mPCS.firePropertyChange("Points", old, null);
		return old;
	}

	@Override
	public Point2D replacePoint(int idx, Point2D point) {
		System.out.println("Set Point: " + point);
		Point2D oldPoint = mPoints.set(idx, point);
		if (mPCS != null) {
			mPCS.firePropertyChange("Points", oldPoint, point);
		}
		return oldPoint;
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


	@Override
	public void applyTransform(AffineTransform transform) {
		if (mPoints != null && mPoints.isEmpty() == false) {
			for (int i = 0; i < mPoints.size(); i++) {
				Point2D p = mPoints.get(i);
				transform.transform(p, p);
			}
			if (mPCS != null) mPCS.firePropertyChange("Points", null, mPoints);
		}
		if (mSubGeometries != null && mSubGeometries.isEmpty() == false) {
			for (IGeometry sub : mSubGeometries)
				sub.applyTransform(transform);
		}
	}




}
