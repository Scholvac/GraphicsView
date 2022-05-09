package de.sos.gv.gta;

import java.util.HashMap;
import java.util.Map;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gvc.Utils.TmpVars;

public class GTUtils {
	private static final Map<CoordinateReferenceSystem, MathTransform>		sTransformToWGS84Map = new HashMap<>();

	public static LatLonBox getLatLonBox(final DirectPosition p1, final DirectPosition p2, final CoordinateReferenceSystem crs, final LatLonBox _store) {
		try (TmpVars tv = TmpVars.get()){
			if (crs == DefaultGeographicCRS.WGS84) {
				tv.doubles[4] = p1.getOrdinate(0);
				tv.doubles[5] = p1.getOrdinate(1);
				tv.doubles[6] = p2.getOrdinate(0);
				tv.doubles[7] = p2.getOrdinate(1);
			}else {
				final MathTransform transform = getTransformToWGS84(crs);
				tv.doubles[0] = p1.getOrdinate(0);
				tv.doubles[1] = p1.getOrdinate(1);
				tv.doubles[2] = p2.getOrdinate(0);
				tv.doubles[3] = p2.getOrdinate(1);
				transform.transform(tv.doubles, 0, tv.doubles, 4, 2);
			}
			if (_store == null)
				return new LatLonBox(tv.doubles[5], tv.doubles[4], tv.doubles[7], tv.doubles[6]).correct();
			return _store.setAndCorrect(tv.doubles[5], tv.doubles[4], tv.doubles[7], tv.doubles[6]).correct();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static LatLonPoint directPositionWGS84ToLatLonPoint(final DirectPosition dp, final LatLonPoint store) {
		if (store == null)
			return new LatLonPoint(dp.getOrdinate(1), dp.getOrdinate(0)); //gt WGS is lon/lat, we use lat/lon
		return store.set(dp.getOrdinate(1), dp.getOrdinate(0));
	}
	public static LatLonPoint directPositionToLatLonPoint(final DirectPosition dp, final CoordinateReferenceSystem crs, final LatLonPoint store) {
		if (crs == DefaultGeographicCRS.WGS84) {
			return directPositionWGS84ToLatLonPoint(dp, store);
		}
		final MathTransform transform = getTransformToWGS84(crs);
		try (TmpVars tv = TmpVars.get()){
			transform.transform(dp.getCoordinate(), 0, tv.doubles, 0, 1);
			if (store == null)
				return new LatLonPoint(tv.doubles[1], tv.doubles[0]);
			return store.set(tv.doubles[1], tv.doubles[0]);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public static MathTransform getTransformToWGS84(final CoordinateReferenceSystem srcCRS) {
		if (sTransformToWGS84Map.containsKey(srcCRS))
			return sTransformToWGS84Map.get(srcCRS);
		MathTransform t = null;
		try {
			t = CRS.findMathTransform(srcCRS, DefaultGeographicCRS.WGS84);
		} catch (final FactoryException e) {
			e.printStackTrace();
		}
		sTransformToWGS84Map.put(srcCRS, t);
		return t;
	}

	public static LatLonBox getLatLonBox(final Envelope env, final CoordinateReferenceSystem crs, final LatLonBox store) {
		return getLatLonBox(env.getLowerCorner(), env.getUpperCorner(), crs, store);
	}

	public static LatLonBox getLatLonBox(final Envelope env) {
		return getLatLonBox(env, env.getCoordinateReferenceSystem(), null);
	}


}
