package de.sos.gvc.gt.proj;

import java.awt.geom.Point2D;

import net.jafama.FastMath;

/**
 * Convert between mercator meters and lat/lon degrees.
 * 
 * http://johndeck.blogspot.com/2005_09_01_johndeck_archive.html
 * http://search.cpan.org/src/RRWO/GPS-Lowrance-0.31/lib/Geo/Coordinates/MercatorMeters.pm
 */
public class MercatorMeterGCT extends AbstractGCT implements
        GeoCoordTransformation {
    
    public final static MercatorMeterGCT INSTANCE = new MercatorMeterGCT();
    
    // TODO: better names?
    private double latfac;
    private double lonfac;
    
    public MercatorMeterGCT() {
        latfac = Planet.wgs84_earthPolarRadiusMeters_D;
        lonfac = Planet.wgs84_earthPolarRadiusMeters_D;
    }

    public MercatorMeterGCT(double latfac, double lonfac) {
        this.latfac = latfac;
        this.lonfac = lonfac;
    }

    @Override
	public Point2D forward(double lat, double lon, Point2D ret) {

    	lat = LatLonPoint.normalizeLatitude(lat);
    	lon = LatLonPoint.wrapLongitude(lon);

        double latrad = FastMath.toRadians(lat);
        double lonrad = FastMath.toRadians(lon);

        double lat_m = latfac
                * FastMath.log(FastMath.tan(((latrad + ProjMath.HALF_PI_D) / 2d)));
        double lon_m = lonfac * lonrad;

        ret.setLocation(lon_m, lat_m);

        return ret;
    }

    @Override
	public LatLonPoint inverse(double lon_m, double lat_m, LatLonPoint ret) {
        double latrad = (2d * FastMath.atan(FastMath.exp(lat_m / latfac))) - ProjMath.HALF_PI_D;
        double lonrad = lon_m / lonfac;

        double lat = FastMath.toDegrees(latrad);
        double lon = FastMath.toDegrees(lonrad);

        lat = LatLonPoint.normalizeLatitude(lat);
        lon = LatLonPoint.wrapLongitude(lon);

        ret.setLatLon(lat, lon);

        return ret;
    }

}
