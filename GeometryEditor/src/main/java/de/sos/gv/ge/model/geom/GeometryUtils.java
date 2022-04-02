package de.sos.gv.ge.model.geom;

import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import de.sos.gv.ge.model.geom.IGeometry.GeometryType;

public class GeometryUtils {

	public static Shape createShape(final IGeometry geom) {
		if (geom.numGeometries() != 1) {
			return createMultiShape(geom);
		}
		if (geom.numPoints() <= 1)
			return new Arc2D.Double(0, 0, 1, 1, 0, 360, Arc2D.CHORD);
		assert geom.numPoints() >= 1;

		if (geom.getType() == GeometryType.Point || geom.numPoints() == 1)
			return new Arc2D.Double(geom.getPoint(0).getX(), geom.getPoint(0).getY(), 1, 1, 0, 360, Arc2D.CHORD);

		final Path2D path = new Path2D.Double();
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

	public static Shape createMultiShape(final IGeometry geom) {
		// TODO Auto-generated method stub
		return null;
	}



	public static IGeometry geometryFromWKT(final String wkt) {
		final int idx1 = wkt.lastIndexOf("(")+1;
		final int idx2 = wkt.indexOf(")");
		final String coords1 = wkt.substring(idx1, idx2);
		final String coordArr[] = coords1.split(",");
		final String type = wkt.substring(0, idx1-2).trim();
		GeometryType gt = GeometryType.Point;
		if ("POLYGON".equals(type)) gt = GeometryType.Polygon;
		if ("LINESTRING".equals(type)) gt = GeometryType.LineString;
		if ("LINEARRING".equals(type)) gt = GeometryType.LinearRing;

		final Geometry geom = new Geometry(gt);
		for (int i = 0; i < coordArr.length; i++) {
			final String c[] = coordArr[i].trim().split(" ");
			final double cx = Double.parseDouble(c[0]);
			final double cy = Double.parseDouble(c[1]);
			geom.appendPoint(new Point2D.Double(cx, cy));
		}
		return geom;
	}

	public static String toWKT(final IGeometry geom) {
		final List<IGeometry> singleGeometries = geom.getAllSubGeombetries();
		if (singleGeometries.size() == 1) {
			return singleGeomToWKT(singleGeometries.get(0));
		}
		if (singleGeometries.isEmpty())
			return singleGeomToWKT(geom);
		final StringBuilder sb = new StringBuilder();
		sb.append(geom.getType().toWKTString()).append("(");
		sb.append(String.join(",", singleGeometries.stream().map(GeometryUtils::toWKT).collect(Collectors.toList())));
		sb.append(")");
		return sb.toString();
	}

	public static String singleGeomToWKT(final IGeometry geometry) {
		final StringBuilder sb = new StringBuilder(geometry.getType().toWKTString());
		sb.append("(");
		final ArrayList<Point2D> pointlist = new ArrayList<>(geometry.getAllPoints());
		if (geometry.isClosed())
			pointlist.add(geometry.getPoint(0));
		final String[] coords = pointlist.stream().map(p -> String.format(Locale.US, "%1.5f %1.5f", p.getX(), p.getY())).toArray(String[]::new);
		sb.append(String.join(",", coords));
		sb.append(")");
		return sb.toString();
	}


}
