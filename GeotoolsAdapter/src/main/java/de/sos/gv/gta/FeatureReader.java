package de.sos.gv.gta;

import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.List;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import de.sos.gv.geo.GeoUtils;
import de.sos.gvc.GraphicsItem;

public class FeatureReader
{

	/** Idea: Convert all coordinates into WGS84 coordinates and use Projection from GeoUtils for WGS84 -> Mercator projection */
	private static final CoordinateReferenceSystem 		sTargetCRS = DefaultGeographicCRS.WGS84;


	public static GraphicsItem createSimpleItem(final SimpleFeature feature) {
		final Object geomObj = feature.getDefaultGeometry();


		final CoordinateReferenceSystem srcCRS = feature.getFeatureType().getGeometryDescriptor().getCoordinateReferenceSystem();
		final MathTransform transform = getTransformToWGS84(srcCRS);

		final Geometry jtsGeom = (Geometry)geomObj;
		final int ngeom = jtsGeom.getNumGeometries();

		SimpleVectorFeatureItem item = null;
		Shape shape = null;
		if (ngeom == 1) {
			shape = createShape(jtsGeom.getGeometryN(0), transform);
			item = new SimpleVectorFeatureItem(shape);
		}else {
			item = new SimpleVectorFeatureItem(createShape(jtsGeom.getGeometryN(0), transform));
			for (int i = 1; i < ngeom; i++) {
				final GraphicsItem child = new GraphicsItem( createShape(jtsGeom.getGeometryN(i), transform));
				item.addItem(child);
			}
		}
		final List<AttributeDescriptor> descriptors = feature.getFeatureType().getAttributeDescriptors();
		for (final AttributeDescriptor desc : descriptors) {
			final Object val = feature.getAttribute(desc.getName());
			if (val != null)
				item.addParameter(desc.getLocalName(), val);
		}
		return item;
	}

	private static MathTransform getTransformToWGS84(final CoordinateReferenceSystem sourceCRS) {

		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(sourceCRS, sTargetCRS, true);
			if (transform instanceof IdentityTransform)
				return null; //just check once
		} catch (final FactoryException e) {
			e.printStackTrace();
		}
		return transform;
	}

	public static Shape createShape(final Geometry geometry, final MathTransform transform) {
		if (geometry instanceof Point) {
			return createShapeFromPoint((Point)geometry, transform);
		}
		if (geometry instanceof LineString) {
			return createShapeFromLineString((LineString)geometry, transform);
		}
		if (geometry instanceof Polygon) {
			return createShapeFromPolygon((Polygon)geometry, transform);
		}
		return null;
	}

	private static Shape createShapeFromPolygon(final Polygon geometry, final MathTransform transform) {
		final Shape outerShape = createShapeFromLineString(geometry.getExteriorRing(), transform);
		if (geometry.getNumInteriorRing() == 0) {
			return outerShape;
		}
		final Area outer = new Area(outerShape);
		for (int i = 0; i < geometry.getNumInteriorRing(); i++) {
			final Shape innerShape = createShapeFromLineString(geometry.getInteriorRingN(i), transform);
			final Area innerArea = new Area(innerShape);
			outer.subtract(innerArea);
		}
		return outer;
	}

	private static Shape createShapeFromLineString(final LineString geometry, final MathTransform transform) {
		final double[] coords = getMercatorCoordinates(geometry.getCoordinates(), transform);
		final GeneralPath gp = new GeneralPath();
		gp.moveTo(coords[0], coords[1]);
		for (int i = 2; i < coords.length; i+=2) {
			gp.lineTo(coords[i+0], coords[i+1]);
		}
		if (geometry.isClosed())
			gp.closePath();
		return gp;
	}

	private static Shape createShapeFromPoint(final Point geometry, final MathTransform transform) {
		final double[] transformed = getMercatorCoordinates(geometry.getCoordinates(), transform);
		assert transformed.length == 2;
		return new Arc2D.Double(transformed[0]-0.1, transformed[1]-0.1, 0.2, 0.2, 0, 360, Arc2D.CHORD);
	}


	private static double[] getMercatorCoordinates(final Coordinate[] in, final MathTransform transform) {
		final double[] out = new double[in.length*2];
		final Point2D.Double tmp = new Point2D.Double();
		if (transform == null) {
			for (int i = 0; i < in.length; i++) {
				GeoUtils.getXY(in[i].x, in[i].y, tmp);
				out[i*2+0] = tmp.x;
				out[i*2+1] = tmp.y;
			}
		}else {
			final double[] coords = new double[in.length*2];
			for (int i = 0; i < in.length; i++){
				coords[i*2+0] = in[i].x;
				coords[i*2+1] = in[i].y;
			}
			try {
				transform.transform(coords, 0, out, 0, in.length);
				for (int i = 0; i < out.length; i+=2) {
					GeoUtils.getXY(out[i+1], out[i+0], tmp);
					out[i+0] = tmp.x;
					out[i+1] = tmp.y;
				}
				return out;
			} catch (final TransformException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
