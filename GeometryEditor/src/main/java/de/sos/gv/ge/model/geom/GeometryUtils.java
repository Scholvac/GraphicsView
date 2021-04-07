package de.sos.gv.ge.model.geom;

import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import de.sos.gv.ge.model.geom.IGeometry.GeometryType;

public class GeometryUtils {

	public static Shape createShape(IGeometry geom) {
		if (geom.numGeometries() != 1) {
			return createMultiShape(geom);
		}
		if (geom.numPoints() <= 1)
			return new Arc2D.Double(0, 0, 1, 1, 0, 360, Arc2D.CHORD); 
		assert(geom.numPoints() >= 1);
		
		if (geom.getType() == GeometryType.Point || geom.numPoints() == 1)
			return new Arc2D.Double(geom.getPoint(0).getX(), geom.getPoint(0).getY(), 1, 1, 0, 360, Arc2D.CHORD);
		
		Path2D path = new Path2D.Double();
		Point2D p = geom.getPoint(0);
		path.moveTo(p.getX(), p.getY());
		for (int i = 1; i < geom.numPoints(); i++) {
			p = geom.getPoint(i);
			path.lineTo(p.getX(), p.getY());
		}
		if (geom.getType() == GeometryType.LinearRing || geom.getType() == GeometryType.Polygon)
			path.closePath();
		
		return path;
	}

	public static Shape createMultiShape(IGeometry geom) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	public static IGeometry geometryFromWKT(String wkt) {
		int idx1 = wkt.lastIndexOf("(")+1;
		int idx2 = wkt.indexOf(")");
		String coords1 = wkt.substring(idx1, idx2);
		String coordArr[] = coords1.split(",");
		String type = wkt.substring(0, idx1-2).trim();
		GeometryType gt = GeometryType.Point;
		if (type.equals("POLYGON")) gt = GeometryType.Polygon;
		if (type.equals("LINESTRING")) gt = GeometryType.LineString;
		if (type.equals("LINEARRING")) gt = GeometryType.LinearRing;
		
		Geometry geom = new Geometry(gt);
		for (int i = 0; i < coordArr.length; i++) {
			String c[] = coordArr[i].trim().split(" ");
			double cx = Double.parseDouble(c[0]);
			double cy = Double.parseDouble(c[1]);
			geom.appendPoint(new Point2D.Double(cx, cy));
		}
		return geom;
	}
}
