package de.sos.gv.ge.model.geom;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Geometry extends AbstractGeometry implements IGeometry {

	public static IGeometry rectangle(final double width, final double height) {
		final Geometry geom = new Geometry(GeometryType.Polygon);
		geom.addPoint(0, new Point2D.Double(-width, height));
		geom.addPoint(1, new Point2D.Double( width, height));
		geom.addPoint(2, new Point2D.Double( width,-height));
		geom.addPoint(3, new Point2D.Double(-width,-height));
		//		geom.addPoint(0, new Point2D.Double(-width, height));
		return geom;
	}


	private GeometryType			mType;

	private ArrayList<IGeometry>	mSubGeometries = null;
	private ArrayList<Point2D>		mPoints = new ArrayList<>();



	Geometry() {
		mType = GeometryType.Point;
	}
	public Geometry(final GeometryType type) {
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
	public IGeometry getGeometry(final int idx) {
		if (idx == 0) return this;
		return mSubGeometries.get(idx); //nullpointer and array out of bounds exception possible
	}

	@Override
	public Point2D getPoint(final int idx) {
		return mPoints.get(idx);
	}

	@Override
	public GeometryType getType() {
		return mType;
	}
	@Override
	public void setType(final GeometryType type) {
		mType = type;
	}

	@Override
	public void addPoint(final int idx, final Point2D point) {
		final Point2D p0 = mPoints.isEmpty() == false ? mPoints.get(0) : null;
		if (p0 != null && p0.getX()  == point.getX() && p0.getY() == point.getY())
			return; //should only affect the last point in a polygon and linear ring, otherwise the geometry is wrong?
		mPoints.add(idx, point);
		firePointChanged(null, point);

	}


	@Override
	public Point2D removePoint(final int idx) {
		final Point2D old = mPoints.remove(idx);
		firePointChanged(old, null);
		return old;
	}

	@Override
	public Point2D replacePoint(final int idx, final Point2D point) {
		System.out.println("Set Point: " + point);
		final Point2D oldPoint = mPoints.set(idx, point);
		firePointChanged(oldPoint, point);
		return oldPoint;
	}

	@Override
	public void addGeometry(final int idx, final IGeometry geometry) {
		if (mSubGeometries == null)
			mSubGeometries = new ArrayList<>();
		mSubGeometries.add(idx, geometry);
	}

	@Override
	public IGeometry removeGeometry(final int idx) {
		final IGeometry r = mSubGeometries.remove(idx);
		if (mSubGeometries.isEmpty())
			mSubGeometries = null;
		return r;
	}


	@Override
	public void applyTransform(final AffineTransform transform) {
		if (mPoints != null && mPoints.isEmpty() == false) {
			for (int i = 0; i < mPoints.size(); i++) {
				final Point2D p = mPoints.get(i);
				transform.transform(p, p);
			}
			firePointsChanged(null, mPoints);
		}
		if (mSubGeometries != null && mSubGeometries.isEmpty() == false) {
			for (final IGeometry sub : mSubGeometries)
				sub.applyTransform(transform);
		}
	}




}
