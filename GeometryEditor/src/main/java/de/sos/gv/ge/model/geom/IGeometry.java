package de.sos.gv.ge.model.geom;

import java.awt.geom.Point2D;

public interface IGeometry {

	public enum GeometryType {
		Point, 
		LineString, 
		LinearRing, 
		Polygon
	}
	
	int numGeometries();
	int numPoints();
	
	IGeometry getGeometry(int idx);
	Point2D getPoint(int idx);
	
	GeometryType getType();
	
	void addPoint(int idx, Point2D point);
	Point2D removePoint(int idx);
	Point2D replacePoint(int idx, Point2D point);
	
	void addGeometry(int idx, IGeometry geometry);
	IGeometry removeGeometry(int idx);
	
	default void appendPoint(Point2D point) {
		addPoint(numPoints(), point);
	}
	
	/**
	 * returns the point with idx-1 in case its an polygon or linear ring. In this case the method takes care about the circle constelation (0 == last)
	 * @param idx
	 * @return
	 */
	default Point2D getPreviousPoint(int idx) {
		int i = idx-1;
		if (i < 0) {
			if (getType() == GeometryType.Polygon || getType() == GeometryType.LinearRing)
				i = numPoints()-1;
		}
		if (i < 0) return null;
		return getPoint(i);
	}
	
	/**
	 * returns the point with idx+1 in case its an polygon or linear ring. In this case the method takes care about the circle constelation (0 == last)
	 * @param idx
	 * @return
	 */
	default Point2D getNextPoint(int idx) {
		int i = idx+1;
		if (i >= numPoints()) {
			if (getType() == GeometryType.Polygon || getType() == GeometryType.LinearRing)
				i = 0;
		}
		if (i >= numPoints()) return null;
		return getPoint(i);
	}
	
	
	
}
