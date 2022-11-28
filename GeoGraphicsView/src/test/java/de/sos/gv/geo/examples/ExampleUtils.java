package de.sos.gv.geo.examples;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author scholvac
 *
 */
public class ExampleUtils {

	public static Shape wkt2Shape(final String wkt) {
		final int idx1 = wkt.lastIndexOf("(")+1;
		final int idx2 = wkt.indexOf(")");
		final String coords1 = wkt.substring(idx1, idx2);
		final String coordArr[] = coords1.split(",");
		final GeneralPath path = new GeneralPath();
		final String fc[] = coordArr[0].split(" ");
		final double fx = Float.parseFloat(fc[0]);
		final double fy = Float.parseFloat(fc[1]);
		path.moveTo(fx, fy);

		for (int i = 1; i < coordArr.length; i++) {
			final String c[] = coordArr[i].trim().split(" ");
			final float cx = Float.parseFloat(c[0]);
			final float cy = Float.parseFloat(c[1]);
			path.lineTo(cx, cy);
		}
		if (coordArr[0].trim().equals(coordArr[coordArr.length-1].trim()))
			path.closePath();
		return path;
	}

	static enum WKTGeometryType {
		POLYGON, POINT, LINESTRING
	}
	static class WKTGeom {
		Shape shape;
		WKTGeometryType type;
	}
	static class WKTCollection {
		WKTGeom[] geometries;
	}

	public static WKTCollection parseWKTGeometryCollection(final InputStream stream) throws IOException {
		final byte[] buffer = new byte[stream.available()];
		stream.read(buffer);
		final String str = new String(buffer);
		return parseWKTGeometryCollection(str);
	}
	public static WKTCollection parseWKTGeometryCollection(final String content) {
		final String firstWord = content.substring(0, content.indexOf(' '));
		WKTGeom[] geoms;
		if ("geometrycollection".equalsIgnoreCase(firstWord)) {
			geoms = parseCollection(content.substring(content.indexOf('(')+1, content.lastIndexOf(')')));
		}else {
			geoms = new WKTGeom[] {parseGeometry(content)};
		}
		final WKTCollection coll = new WKTCollection();
		coll.geometries = geoms;
		return coll;
	}

	private static WKTGeom[] parseCollection(final String content) {
		final String[] geometries = content.split("\\),");
		final WKTGeom[] out = new WKTGeom[geometries.length];
		for (int i = 0; i < geometries.length; i++) {
			out[i] = parseGeometry(geometries[i]);
		}
		return out;
	}

	public static WKTGeom parseGeometry(final String wkt) {
		final int i1 = wkt.indexOf('(');
		final String firstWord = wkt.substring(0, i1).trim();
		final WKTGeom out = new WKTGeom();
		out.type = WKTGeometryType.valueOf(firstWord.toUpperCase());
		if (out.type == WKTGeometryType.POLYGON)
			out.shape = wkt2Shape(wkt);
		return out;
	}


}
