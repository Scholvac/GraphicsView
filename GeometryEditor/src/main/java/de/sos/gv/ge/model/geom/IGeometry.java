package de.sos.gv.ge.model.geom;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import de.sos.gv.ge.model.geom.IGeometry.GeometryType;

public interface IGeometry {

	public enum GeometryType {
		Point, 
		LineString, 
		LinearRing, 
		Polygon
	}
	
	void addListener(PropertyChangeListener listener);
	void removeListener(PropertyChangeListener listener);
	
	
	int numGeometries();
	int numPoints();
	
	IGeometry getGeometry(int idx);
	Point2D getPoint(int idx);
	
	GeometryType getType();
	void setType(GeometryType type);
	
	void addPoint(int idx, Point2D point);
	Point2D removePoint(int idx);
	

	/**
	 * Replaces the values of the point at the given index
	 * @param idx index of point to be manipulated
	 * @param point new values of the point
	 * @return the old values of the point
	 */
	Point2D replacePoint(int idx, Point2D point);
	
	void addGeometry(int idx, IGeometry geometry);
	IGeometry removeGeometry(int idx);
	
	void applyTransform(AffineTransform transform);
	
	default void appendPoint(Point2D point) {
		addPoint(numPoints(), point);
	}
	
	/**
	 * returns the point with idx-1 in case its an polygon or linear ring. In this case the method takes care about the circle constellation (0 == last)
	 * @param idx
	 * @return
	 */
	default Point2D getPreviousPoint(int idx) {
		int i = getPreviousIndex(idx);
		if (i < 0) return null;
		return getPoint(i);
	}
	
	/**
	 * returns the point with idx+1 in case its an polygon or linear ring. In this case the method takes care about the circle constellation (0 == last)
	 * @param idx
	 * @return
	 */
	default Point2D getNextPoint(int idx) {
		int i = getNextIndex(idx);
		if (i < 0) return null;
		return getPoint(i);
	}
	
	/**
	 * returns the next index idx+1. In case its an polygon or linear ring. In this case the method takes care about the circle constellation (0 == last)
	 * @param idx
	 * @return may return -1 if there is no previous point (e.g. for points and linerString)
	 */
	default int getPreviousIndex(int idx) {
		int i = idx-1;
		if (i < 0) {
			if (getType() == GeometryType.Polygon || getType() == GeometryType.LinearRing)
				i = numPoints()-1;
		}
		return i;
	}
	
	/**
	 * returns the next index idx+1. In case its an polygon or linear ring. In this case the method takes care about the circle constellation (0 == last)
	 * @param idx
	 * @return returns -1 if there is no next point (e.g. for points and linearStrings)
	 */
	default int getNextIndex(int idx) {
		int i = idx+1;
		if (i >= numPoints()) {
			if (getType() == GeometryType.Polygon || getType() == GeometryType.LinearRing)
				i = 0;
			else 
				i = -1;
		}
		return i;
	}
	
	
	default List<Point2D> getAllPoints() {
		ArrayList<Point2D> out = new ArrayList<Point2D>();
		for (int i = 0; i < numPoints(); i++)
			out.add(getPoint(i));
		return out;
	}

	default void setAllPoints(List<Point2D> in) {
		while(numPoints() > 0) removePoint(0);
		for (int i = 0; i < in.size(); i++)
			addPoint(i, in.get(i));
	}

	
	default List<IGeometry> getAllSubGeombetries() {
		ArrayList<IGeometry> out = new ArrayList<IGeometry>();
		for (int i = 0; i < numGeometries()-1; i++)
			out.add(getGeometry(i));
		return out;
	}

	default void setAllSubGeometries(List<IGeometry> in) {
		//sub-geometry == 0 is the geometry itself
		while(numGeometries()>1) removeGeometry(1);
		for (int i = 0; i < in.size(); i++)
			addGeometry(i, in.get(i));
	}
	
	
}
