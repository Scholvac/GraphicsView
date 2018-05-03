// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/proj/ProjMath.java,v $
// $RCSfile: ProjMath.java,v $
// $Revision: 1.9 $
// $Date: 2007/06/21 21:39:02 $
// $Author: dietrick $
// 
// **********************************************************************

package de.sos.gvc.gt.proj;

/**
 * Math functions used by projection code.
 */
public final class ProjMath {
	
	public static final float HALF_PI_F = (float)(Math.PI / 2.0);
	public static final double HALF_PI_D = Math.PI / 2.0;
	public static final double HALF_PI = HALF_PI_D;
	public static final double TWO_PI = Math.PI * 2.0;
	public static final double TWO_PI_D = TWO_PI;
	public static final float TWO_PI_F = (float)TWO_PI_D;
	
	final public static double EQUIVALENT_TOLERANCE = 0.0000001;
	

	/**
	 * North pole latitude in radians.
	 */
	public final static transient float NORTH_POLE_F = HALF_PI_F;

	/**
	 * South pole latitude in radians.
	 */
	public final static transient float SOUTH_POLE_F = -NORTH_POLE_F;

	/**
	 * North pole latitude in radians.
	 */
	public final static transient double NORTH_POLE_D = HALF_PI_D;

	/**
	 * North pole latitude in degrees.
	 */
	public final static transient double NORTH_POLE_DEG_D = 90d;

	/**
	 * South pole latitude in radians.
	 */
	public final static transient double SOUTH_POLE_D = -NORTH_POLE_D;

	/**
	 * South pole latitude in degrees.
	 */
	public final static transient double SOUTH_POLE_DEG_D = -NORTH_POLE_DEG_D;

	/**
	 * Dateline longitude in radians.
	 */
	public final static transient float DATELINE_F = (float) Math.PI;

	/**
	 * Dateline longitude in radians.
	 */
	public final static transient double DATELINE_D = Math.PI;

	/**
	 * Dateline longitude in degrees.
	 */
	public final static transient double DATELINE_DEG_D = 180d;

	/**
	 * Longitude range in radians.
	 */
	public final static transient float LON_RANGE_F = TWO_PI_F;

	/**
	 * Longitude range in radians.
	 */
	public final static transient double LON_RANGE_D = TWO_PI_D;

	/**
	 * Longitude range in degrees.
	 */
	public final static transient double LON_RANGE_DEG_D = 360d;

	// cannot construct
	private ProjMath() {
	}

	/**
	 * rounds the quantity away from 0.
	 * 
	 * @param x in value
	 * @return double
	 * @see #qint(double)
	 */
	public final static double roundAdjust(double x) {
		return qint_old(x);
	}

	/**
	 * Rounds the quantity away from 0.
	 * 
	 * @param x value
	 * @return double
	 */
	public final static double qint(double x) {
		return qint_new(x);
	}

	final private static double qint_old(double x) {
		return (((int) x) < 0) ? (x - 0.5) : (x + 0.5);
	}

	final private static double qint_new(double x) {
		// -1 or +1 away from zero
		return (x <= 0.0) ? (x - 1.0) : (x + 1.0);
	}

	/**
	 * Calculate the shortest arc distance between two lons.
	 * 
	 * @param lon1 radians
	 * @param lon2 radians
	 * @return float distance
	 */
	public final static float lonDistance(float lon1, float lon2) {
		return (float) Math.min(Math.abs(lon1 - lon2),
				((lon1 < 0) ? lon1 + Math.PI : Math.PI - lon1) + ((lon2 < 0) ? lon2 + Math.PI : Math.PI - lon2));
	}

	/**
	 * Convert between decimal degrees and scoords.
	 * 
	 * @param deg degrees
	 * @return long scoords
	 * 
	 */
	public final static long DEG_TO_SC(double deg) {
		return (long) (deg * 3600000);
	}

	/**
	 * Convert between decimal degrees and scoords.
	 * 
	 * @param sc scoords
	 * @return double decimal degrees
	 */
	public final static double SC_TO_DEG(int sc) {
		return ((sc) / (60.0 * 60.0 * 1000.0));
	}

	/**
	 * Convert radians to degrees.
	 * 
	 * @param rad radians
	 * @return double decimal degrees
	 */
	public final static double radToDeg(double rad) {
		return Math.toDegrees(rad);
	}

	/**
	 * Convert radians to degrees.
	 * 
	 * @param rad radians
	 * @return float decimal degrees
	 */
	public final static float radToDeg(float rad) {
		return (float) Math.toDegrees(rad);
	}

	/**
	 * Convert degrees to radians.
	 * 
	 * @param deg degrees
	 * @return double radians
	 */
	public final static double degToRad(double deg) {
		return Math.toRadians(deg);
	}

	/**
	 * Convert degrees to radians.
	 * 
	 * @param deg degrees
	 * @return float radians
	 */
	public final static float degToRad(float deg) {
		return (float) Math.toRadians(deg);
	}

	/**
	 * Generate a hashCode value for a lat/lon pair.
	 * 
	 * @param lat latitude
	 * @param lon longitude
	 * @return int hashcode
	 * 
	 */
	public final static int hashLatLon(float lat, float lon) {
		if (lat == -0f)
			lat = 0f;// handle negative zero (anything else?)
		if (lon == -0f)
			lon = 0f;
		int tmp = Float.floatToIntBits(lat);
		int hash = (tmp << 5) | (tmp >> 27);// rotate the lat bits
		return hash ^ Float.floatToIntBits(lon);// XOR with lon
	}

	/**
	 * Converts an array of decimal degrees float lat/lons to float radians in
	 * place.
	 * 
	 * @param degs float[] lat/lons in decimal degrees
	 * @return float[] lat/lons in radians
	 */
	public final static float[] arrayDegToRad(float[] degs) {
		for (int i = 0; i < degs.length; i++) {
			degs[i] = degToRad(degs[i]);
		}
		return degs;
	}

	/**
	 * Converts an array of radian float lat/lons to decimal degrees in place.
	 * 
	 * @param rads float[] lat/lons in radians
	 * @return float[] lat/lons in decimal degrees
	 */
	public final static float[] arrayRadToDeg(float[] rads) {
		for (int i = 0; i < rads.length; i++) {
			rads[i] = radToDeg(rads[i]);
		}
		return rads;
	}

	/**
	 * Converts an array of decimal degrees double lat/lons to double radians in
	 * place.
	 * 
	 * @param degs double[] lat/lons in decimal degrees
	 * @return double[] lat/lons in radians
	 */
	public final static double[] arrayDegToRad(double[] degs) {
		for (int i = 0; i < degs.length; i++) {
			degs[i] = degToRad(degs[i]);
		}
		return degs;
	}

	/**
	 * Converts an array of radian double lat/lons to decimal degrees in place.
	 * 
	 * @param rads double[] lat/lons in radians
	 * @return double[] lat/lons in decimal degrees
	 */
	public final static double[] arrayRadToDeg(double[] rads) {
		for (int i = 0; i < rads.length; i++) {
			rads[i] = radToDeg(rads[i]);
		}
		return rads;
	}

	/**
	 * @deprecated use normalizeLatitude instead.
	 */
	@Deprecated
	public final static float normalize_latitude(float lat, float epsilon) {
		return normalizeLatitude(lat, epsilon);
	}

	/**
	 * Normalizes radian latitude. Normalizes latitude if at or exceeds epsilon
	 * distance from a pole.
	 * 
	 * @param lat float latitude in radians
	 * @param epsilon epsilon (&gt;= 0) radians distance from pole
	 * @return float latitude (-PI/2 &lt;= phi &lt;= PI/2)
	 * @see com.bbn.openmap.proj.coords.LatLonPoint#normalizeLatitude(double)
	 */
	public final static float normalizeLatitude(float lat, float epsilon) {
		if (lat > NORTH_POLE_F - epsilon) {
			return NORTH_POLE_F - epsilon;
		} else if (lat < SOUTH_POLE_F + epsilon) {
			return SOUTH_POLE_F + epsilon;
		}
		return lat;
	}

	/**
	 * @deprecated use normalizeLatitude instead.
	 */
	@Deprecated
	public final static double normalize_latitude(double lat, double epsilon) {
		return normalizeLatitude(lat, epsilon);
	}

	/**
	 * Normalizes radian latitude. Normalizes latitude if at or exceeds epsilon
	 * distance from a pole.
	 * 
	 * @param lat double latitude in radians
	 * @param epsilon epsilon (&gt;= 0) radians distance from pole
	 * @return double latitude (-PI/2 &lt;= phi &lt;= PI/2)
	 * @see com.bbn.openmap.proj.coords.LatLonPoint#normalizeLatitude(double)
	 */
	public final static double normalizeLatitude(double lat, double epsilon) {
		if (lat > NORTH_POLE_D - epsilon) {
			return NORTH_POLE_D - epsilon;
		} else if (lat < SOUTH_POLE_D + epsilon) {
			return SOUTH_POLE_D + epsilon;
		}
		return lat;
	}

	/**
	 * @deprecated use wrapLongitde instead.
	 */
	@Deprecated
	public final static float wrap_longitude(float lon) {
		return wrapLongitude(lon);
	}

	/**
	 * Sets radian longitude to something sane.
	 * 
	 * @param lon float longitude in radians
	 * @return float longitude (-PI &lt;= lambda &lt; PI)
	 */
	public final static float wrapLongitude(float lon) {
		if ((lon < -DATELINE_F) || (lon > DATELINE_F)) {
			lon += DATELINE_F;
			lon %= LON_RANGE_F;
			lon += (lon < 0) ? DATELINE_F : -DATELINE_F;
		}
		return lon;
	}

	/**
	 * @deprecated use wrapLongitude instead.
	 */
	@Deprecated
	public final static double wrap_longitude(double lon) {
		return wrapLongitude(lon);
	}

	/**
	 * Sets radian longitude to something sane.
	 * 
	 * @param lon double longitude in radians
	 * @return double longitude (-PI &lt;= lambda &lt; PI)
	 */
	public final static double wrapLongitude(double lon) {
		if ((lon < -DATELINE_D) || (lon > DATELINE_D)) {
			lon += DATELINE_D;
			lon %= LON_RANGE_D;
			lon += (lon < 0) ? DATELINE_D : -DATELINE_D;
		}
		return lon;
	}

	/**
	 * Sets degree longitude to something sane.
	 * 
	 * @param lon double longitude in degrees
	 * @return double longitude (-180 &lt;= lambda &lt; 180)
	 */
	public final static double wrapLongitudeDeg(double lon) {
		if ((lon < -DATELINE_DEG_D) || (lon > DATELINE_DEG_D)) {
			lon += DATELINE_DEG_D;
			lon %= LON_RANGE_DEG_D;
			lon += (lon < 0) ? DATELINE_DEG_D : -DATELINE_DEG_D;
		}
		return lon;
	}

	/**
	 * Converts units (km, nm, miles, etc) to decimal degrees for a spherical
	 * planet. This does not check for arc distances &gt; 1/2 planet
	 * circumference, which are better represented as (2pi - calculated arc).
	 * 
	 * @param u units float value
	 * @param uCircumference units circumference of planet
	 * @return float decimal degrees
	 */
	public final static float sphericalUnitsToDeg(float u, float uCircumference) {
		return 360f * (u / uCircumference);
	}

	/**
	 * Converts units (km, nm, miles, etc) to arc radians for a spherical
	 * planet. This does not check for arc distances &gt; 1/2 planet
	 * circumference, which are better represented as (2pi - calculated arc).
	 * 
	 * @param u units float value
	 * @param uCircumference units circumference of planet
	 * @return float arc radians
	 */
	public final static float sphericalUnitsToRad(float u, float uCircumference) {
		return TWO_PI_F * (u / uCircumference);
	}

	/**
	 * @deprecated use geocentricLatitude instead.
	 */
	@Deprecated
	public final static float geocentric_latitude(float lat, float lon) {
		return geocentricLatitude(lat, lon);
	}

	/**
	 * Calculate the geocentric latitude given a geographic latitude. According
	 * to John Synder: <br>
	 * "The geographic or geodetic latitude is the angle which a line
	 * perpendicular to the surface of the ellipsoid at the given point makes
	 * with the plane of the equator. ...The geocentric latitude is the angle
	 * made by a line to the center of the ellipsoid with the equatorial plane".
	 * ( <i>Map Projections --A Working Manual </i>, p 13)
	 * <p>
	 * Translated from Ken Anderson's lisp code <i>Freeing the Essence of
	 * Computation </i>
	 * 
	 * @param lat float geographic latitude in radians
	 * @param flat float flatening factor
	 * @return float geocentric latitude in radians
	 * @see #geographic_latitude
	 */
	public final static float geocentricLatitude(float lat, float flat) {
		float f = 1.0f - flat;
		return (float) Math.atan((f * f) * (float) Math.tan(lat));
	}

	/**
	 * @deprecated use geographicLoatitude instead.
	 */
	@Deprecated
	public final static float geographic_latitude(float lat, float lon) {
		return geographicLatitude(lat, lon);
	}

	/**
	 * Calculate the geographic latitude given a geocentric latitude. Translated
	 * from Ken Anderson's lisp code <i>Freeing the Essence of Computation </i>
	 * 
	 * @param lat float geocentric latitude in radians
	 * @param flat float flatening factor
	 * @return float geographic latitude in radians
	 * @see #geocentric_latitude
	 */
	public final static float geographicLatitude(float lat, float flat) {
		float f = 1.0f - flat;
		return (float) Math.atan((float) Math.tan(lat) / (f * f));
	}

	


	/*
	 * public static void main(String[] args) { float degs =
	 * sphericalUnitsToRad( Planet.earthEquatorialRadius/2,
	 * Planet.earthEquatorialRadius); Debug.output("degs = " + degs); float
	 * LAT_DEC_RANGE = 90.0f; float LON_DEC_RANGE = 360.0f; float lat, lon; for
	 * (int i = 0; i < 100; i++) { lat =
	 * com.bbn.openmap.LatLonPoint.normalize_latitude(
	 * (float)Math.random()*LAT_DEC_RANGE); lon =
	 * com.bbn.openmap.LatLonPoint.wrap_longitude(
	 * (float)Math.random()*LON_DEC_RANGE); Debug.output( "(" + lat + "," + lon
	 * + ") : (" + degToRad(lat) + "," + degToRad(lon) + ") : (" +
	 * radToDeg(degToRad(lat)) + "," + radToDeg(degToRad(lon)) + ")"); } }
	 */

	/**
	 * Generic test for seeing if an left longitude value and a right longitude
	 * value seem to constitute crossing the dateline.
	 * 
	 * @param leftLon the leftmost longitude, in decimal degrees. Expected to
	 *            represent the location of the left side of a map window.
	 * @param rightLon the rightmost longitude, in decimal degrees. Expected to
	 *            represent the location of the right side of a map window.
	 * @param projScale the projection scale, considered if the two values are
	 *            very close to each other and leftLon less than rightLon.
	 * @return true if it seems like these two longitude values represent a
	 *         dateline crossing.
	 */
	public static boolean isCrossingDateline(double leftLon, double rightLon, float projScale) {
		// if the left longitude is greater than the right, we're obviously
		// crossing the dateline. If they are approximately equal, we could be
		// showing the whole earth, but only if the scale is significantly
		// large. If the scale is small, we could be really zoomed in.
		return ((leftLon > rightLon)
				|| (ProjMath.approximately_equal(leftLon, rightLon, .001f) && projScale > 1000000f));
	}


	
	

    /**
     * Checks if a ~= b. Use this to test equality of floating point
     * numbers.
     * <p>
     * 
     * @param a double
     * @param b double
     * @param epsilon the allowable error
     * @return boolean
     */
    final public static boolean approximately_equal(double a, double b,
                                                    double epsilon) {
        return (Math.abs(a - b) <= epsilon);
    }

    /**
     * Checks if a ~= b. Use this to test equality of floating point
     * numbers against EQUIVALENT_TOLERANCE.
     * <p>
     * 
     * @param a double
     * @param b double
     * @return boolean
     */
    final public static boolean approximately_equal(double a, double b) {
        return (Math.abs(a - b) <= EQUIVALENT_TOLERANCE);
    }
    
    /**
     * Checks if a ~= b. Use this to test equality of floating point
     * numbers.
     * <p>
     * 
     * @param a float
     * @param b float
     * @param epsilon the allowable error
     * @return boolean
     */
    final public static boolean approximately_equal(float a, float b,
                                                    float epsilon) {
        return (Math.abs(a - b) <= epsilon);
    }

	  /**
     * Return sign of number.
     * 
     * @param x int
     * @return int sign -1, 1
     */
    public static final int sign(int x) {
        return (x < 0) ? -1 : 1;
    }

    /**
     * Hyperbolic arcsin.
     * <p>
     * Hyperbolic arc sine: log (x+sqrt(1+x^2))
     * 
     * @param x float
     * @return float asinh(x)
     */
    public static final float asinh(float x) {
        return (float) Math.log(x + Math.sqrt(x * x + 1));
    }

    /**
     * Hyperbolic arcsin.
     * <p>
     * Hyperbolic arc sine: log (x+sqrt(1+x^2))
     * 
     * @param x double
     * @return double asinh(x)
     */
    public static final double asinh(double x) {
        return Math.log(x + Math.sqrt(x * x + 1));
    }
}