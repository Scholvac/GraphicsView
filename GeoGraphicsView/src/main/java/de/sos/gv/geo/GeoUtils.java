package de.sos.gv.geo;

import java.awt.geom.Point2D;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsView;

public class GeoUtils {
	public static final double HALF_PI = Math.PI / 2.0;
	public static final double TWO_PI = Math.PI * 2.0;
	public static final double TO_RAD = Math.PI / 180.;

	//	https://docs.microsoft.com/en-us/bingmaps/articles/bing-maps-tile-system?redirectedfrom=MSDN
	public static final double EARTH_RADIUS = 6378137;
	public static final double EARTH_CIRCUMFERENCE = TWO_PI * EARTH_RADIUS;
	public static final double MIN_LATITUDE = -85.05112878;
	public static final double MAX_LATITUDE =  85.05112878;
	public static final double MIN_LONGITUDE = -180;
	public static final double MAX_LONGITUDE =  180;





	static LatLonPoint GoogleBingtoWGS84Mercator (final double x, final double y, final LatLonPoint store) {
		final double lon = x / 20037508.34 * 180;
		double lat = y / 20037508.34 * 180;

		lat = 180/Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180)) - Math.PI / 2);
		return store.set(lat, lon);
	}
	static Point2D.Double WGS84toGoogleBing(final double lat, final double lon, final Point2D.Double store) {
		// https://alastaira.wordpress.com/2011/01/23/the-google-maps-bing-maps-spherical-mercator-projection/
		store.x = lon * 20037508.34 / 180;
		store.y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
		store.y = store.y * 20037508.34 / 180;
		return store;
	}

	public static LatLonPoint getLatLon(final double mercatorX, final double mercatorY) {
		return getLatLon(mercatorX, mercatorY, null);
	}

	public static LatLonPoint getLatLon(final double mercatorX, final double mercatorY, LatLonPoint store) {
		if (store == null) store = new LatLonPoint();
		return GoogleBingtoWGS84Mercator(mercatorX, mercatorY, store);
	}
	public static LatLonPoint getLatLon(final Point2D xy) {
		return getLatLon(xy.getX(), xy.getY());
	}

	private static final Point2D.Double _distanceStore1 = new Point2D.Double();
	private static final Point2D.Double _distanceStore2 = new Point2D.Double();
	public static double distance(final LatLonPoint p1, final LatLonPoint p2) {
		synchronized (_distanceStore1) {
			getXY(p1, _distanceStore1);
			getXY(p2, _distanceStore2);
			return distance(_distanceStore1, _distanceStore2);
		}
	}
	private static final double[] sCorrectionFactors = {1.00336, 1.00336, 1.00337, 1.00336, 1.00340, 1.00338, 1.00343, 1.00345, 1.00347, 1.00350, 1.00346, 1.00350, 1.00366, 1.00370, 1.00361, 1.00365, 1.00370, 1.00389, 1.00394, 1.00386, 1.00405, 1.00399, 1.00405, 1.00412, 1.00419, 1.00453, 1.00434, 1.00443, 1.00451, 1.00460, 1.00495, 1.00478, 1.00488, 1.00498, 1.00508, 1.00545, 1.00529, 1.00541, 1.00552, 1.00564, 1.00576, 1.00562, 1.00628, 1.00641, 1.00601, 1.00615, 1.00629, 1.00696, 1.00711, 1.00673, 1.00688, 1.00703, 1.00772, 1.00788, 1.00752, 1.00769, 1.00786, 1.00856, 1.00874, 1.00839, 1.00857, 1.00876, 1.00948, 1.00967, 1.00934, 1.00954, 1.00974, 1.01048, 1.01069, 1.01036, 1.01058, 1.01080, 1.01155, 1.01178, 1.01147, 1.01170, 1.01194, 1.01271, 1.01295, 1.01266, 1.01290, 1.01315, 1.01341, 1.01366, 1.01392, 1.01419, 1.01445, 1.01472, 1.01499, 1.01527, 1.01555, 1.01583, 1.01612, 1.01641, 1.01670, 1.01699, 1.01729, 1.01759, 1.01790, 1.01821, 1.01852, 1.01884, 1.01915, 1.01948, 1.01980, 1.02013, 1.02046, 1.02080, 1.02113, 1.02148, 1.02182, 1.02217, 1.02252, 1.02288, 1.02323, 1.02360, 1.02396, 1.02433, 1.02470, 1.02508, 1.02546, 1.02584, 1.02622, 1.02661, 1.02700, 1.02740, 1.02780, 1.02820, 1.02861, 1.02902, 1.02943, 1.02985, 1.03027, 1.03069, 1.03003, 1.03155, 1.03089, 1.03242, 1.03286, 1.03222, 1.03375, 1.03311, 1.03466, 1.03512, 1.03449, 1.03605, 1.03542, 1.03699, 1.03747, 1.03685, 1.03843, 1.03782, 1.03941, 1.03990, 1.03930, 1.04090, 1.04031, 1.04192, 1.04243, 1.04185, 1.04347, 1.04399, 1.04232, 1.04285, 1.04559, 1.04613, 1.04667, 1.04501, 1.04556, 1.04832, 1.04888, 1.04944, 1.04779, 1.04836, 1.05115, 1.05172, 1.05230, 1.05067, 1.05125, 1.05407, 1.05467, 1.05527, 1.05364, 1.05425, 1.05709, 1.05770, 1.05832, 1.05671, 1.05734, 1.06021, 1.06084, 1.06148, 1.05989, 1.06053, 1.06342, 1.06408, 1.06474, 1.06316, 1.06382, 1.06674, 1.06742, 1.06810, 1.06653, 1.06722, 1.07017, 1.07086, 1.07156, 1.07001, 1.07071, 1.07369, 1.07441, 1.07513, 1.07359, 1.07432, 1.07732, 1.07806, 1.07880, 1.07727, 1.07802, 1.08106, 1.08182, 1.08259, 1.08107, 1.08184, 1.08491, 1.08569, 1.08648, 1.08497, 1.08577, 1.08886, 1.08967, 1.09048, 1.08899, 1.08981, 1.09063, 1.09376, 1.09229, 1.09312, 1.09396, 1.09481, 1.09797, 1.09651, 1.09737, 1.09823, 1.09910, 1.10230, 1.10085, 1.10173, 1.10262, 1.10351, 1.10674, 1.10531, 1.10621, 1.10712, 1.10804, 1.11130, 1.10989, 1.11082, 1.11175, 1.11269, 1.11599, 1.11459, 1.11554, 1.11650, 1.11747, 1.12080, 1.11942, 1.12040, 1.12138, 1.12237, 1.12575, 1.12437, 1.12538, 1.12639, 1.12741, 1.13082, 1.12946, 1.13049, 1.13153, 1.13257, 1.13602, 1.13468, 1.13574, 1.13680, 1.13788, 1.14136, 1.14003, 1.14112, 1.14221, 1.14331, 1.14684, 1.14553, 1.14664, 1.14776, 1.14889, 1.14759, 1.15116, 1.15231, 1.15346, 1.15461, 1.15333, 1.15694, 1.15812, 1.15930, 1.16048, 1.15922, 1.16287, 1.16407, 1.16528, 1.16650, 1.16525, 1.16895, 1.17018, 1.17142, 1.17267, 1.17144, 1.17518, 1.17644, 1.17771, 1.17899, 1.17778, 1.18406, 1.18036, 1.18166, 1.18798, 1.18428, 1.19063, 1.18693, 1.18826, 1.19464, 1.19095, 1.19735, 1.19366, 1.19503, 1.20147, 1.19778, 1.20425, 1.20056, 1.20197, 1.20847, 1.20479, 1.21132, 1.20764, 1.20907, 1.21565, 1.21197, 1.21857, 1.21489, 1.21636, 1.22300, 1.21933, 1.22599, 1.22232, 1.22383, 1.23054, 1.22687, 1.23361, 1.22994, 1.23149, 1.23827, 1.23460, 1.24141, 1.23775, 1.23933, 1.24618, 1.24253, 1.24941, 1.24575, 1.24738, 1.25430, 1.25065, 1.25761, 1.25396, 1.25562, 1.25730, 1.25898, 1.26067, 1.26237, 1.26407, 1.26579, 1.26751, 1.26925, 1.27099, 1.27274, 1.27450, 1.27626, 1.27804, 1.27982, 1.28162, 1.28342, 1.28523, 1.28705, 1.28888, 1.29072, 1.29257, 1.29443, 1.29630, 1.29817, 1.30006, 1.30195, 1.30386, 1.30577, 1.30770, 1.30963, 1.31157, 1.31353, 1.31549, 1.31746, 1.31945, 1.32144, 1.32344, 1.32545, 1.32748, 1.32951, 1.33156, 1.33361, 1.33567, 1.33775, 1.33983, 1.34193, 1.34404, 1.34615, 1.34828, 1.35042, 1.35257, 1.35473, 1.35691, 1.35909, 1.36128, 1.36349, 1.36571, 1.36793, 1.37017, 1.37243, 1.37469, 1.37696, 1.37925, 1.38155, 1.38386, 1.38618, 1.38852, 1.39086, 1.39322, 1.39559, 1.39797, 1.40037, 1.40278, 1.40520, 1.40763, 1.41008, 1.41254, 1.41501, 1.41749, 1.41999, 1.42250, 1.42503, 1.42756, 1.43012, 1.43268, 1.43526, 1.43785, 1.44046, 1.44307, 1.44571, 1.44836, 1.45102, 1.45369, 1.45638, 1.45909, 1.46181, 1.46454, 1.46107, 1.47005, 1.47283, 1.46937, 1.47843, 1.47498, 1.48409, 1.48695, 1.48351, 1.49270, 1.48927, 1.49852, 1.50146, 1.49803, 1.50737, 1.50395, 1.51335, 1.51637, 1.51296, 1.52245, 1.51905, 1.52859, 1.53169, 1.52831, 1.53794, 1.53456, 1.54426, 1.54745, 1.54409, 1.55388, 1.55052, 1.56038, 1.56366, 1.56031, 1.57027, 1.56694, 1.57696, 1.58033, 1.57701, 1.58713, 1.58382, 1.59401, 1.59748, 1.59419, 1.60448, 1.60120, 1.61157, 1.61514, 1.61187, 1.62234, 1.61909, 1.62963, 1.63331, 1.63007, 1.64073, 1.63751, 1.64824, 1.65203, 1.64882, 1.65967, 1.65648, 1.66740, 1.67131, 1.66813, 1.67918, 1.67602, 1.68715, 1.69117, 1.68803, 1.69928, 1.69616, 1.70750, 1.71164, 1.70854, 1.72001, 1.71692, 1.72848, 1.73275, 1.72969, 1.74138, 1.73834, 1.75011, 1.75452, 1.75151, 1.76342, 1.76042, 1.77244, 1.77699, 1.77402, 1.78617, 1.78322, 1.79547, 1.80017, 1.79725, 1.80965, 1.80675, 1.81926, 1.82411, 1.82124, 1.83390, 1.83105, 1.83601, 1.84100, 1.84602, 1.85895, 1.85616, 1.86128, 1.86644, 1.87163, 1.88484, 1.88212, 1.88741, 1.89274, 1.89811, 1.91161, 1.90895, 1.91443, 1.91994, 1.92549, 1.93930, 1.93671, 1.94238, 1.94809, 1.95383, 1.96796, 1.96545, 1.97131, 1.97722, 1.98317, 1.99763, 1.99520, 2.00128, 2.00740, 2.01356, 2.02836, 2.02602, 2.03232, 2.03866, 2.04505, 2.06022, 2.05797, 2.06450, 2.07108, 2.07771, 2.09325, 2.09111, 2.09788, 2.10471, 2.11159, 2.12753, 2.12549, 2.13253, 2.13961, 2.14675, 2.16311, 2.16120, 2.16850, 2.17586, 2.18328, 2.20008, 2.19829, 2.20588, 2.21353, 2.22124, 2.23850, 2.23685, 2.24475, 2.25270, 2.26073, 2.27847, 2.28665, 2.27546, 2.28371, 2.31161, 2.32007, 2.32859, 2.31738, 2.32597, 2.35459, 2.36340, 2.37228, 2.36106, 2.37001, 2.39938, 2.40856, 2.41782, 2.40660, 2.41594, 2.44609, 2.45568, 2.46535, 2.45412, 2.46388, 2.49486, 2.50487, 2.51497, 2.50376, 2.51395, 2.54581, 2.55627, 2.56683, 2.55564, 2.56630, 2.59908, 2.61003, 2.62108, 2.60992, 2.62108, 2.65484, 2.66630, 2.67787, 2.66676, 2.67845, 2.71325, 2.72527, 2.73740, 2.72635, 2.73861, 2.77451, 2.78712, 2.79985, 2.78888, 2.80175, 2.83882, 2.85207, 2.86545, 2.85456, 2.86810, 2.90641, 2.92034, 2.93441, 2.92365, 2.93789, 2.97752, 2.99219, 3.00702, 2.99639, 3.01140, 3.05244, 3.06791, 3.08355, 3.07308, 3.08893, 3.13148, 3.14781, 3.16432, 3.15406, 3.17079, 3.21497, 3.23223, 3.24969, 3.23966, 3.25738, 3.30329, 3.32157, 3.34006, 3.33031, 3.34908, 3.39686, 3.41625, 3.43587, 3.42644, 3.44637, 3.49617, 3.51676, 3.53761, 3.52856, 3.54975, 3.60174, 3.62365, 3.64585, 3.63724, 3.65982, 3.71417, 3.73754, 3.76122, 3.75313, 3.77723, 3.83415, 3.85912, 3.88443, 3.87695, 3.90273, 3.96246, 3.98919, 4.01630, 4.00953, 4.03718, 4.09997, 4.12866, 4.15778, 4.15183, 4.18155, 4.24771, 4.27858, 4.30992, 4.30494, 4.33696, 4.40683, 4.44014, 4.47397, 4.47013, 4.50474, 4.57871, 4.61475, 4.65137, 4.64887, 4.68639, 4.76491, 4.80403, 4.80276, 4.84288, 4.88369, 4.92520, 5.00990, 5.01044, 5.05419, 5.09874, 5.14409, 5.23463, 5.23730, 5.28521, 5.33402, 5.38376, 5.48090, 5.48612, 5.53880, 5.59252, 5.64731, 5.75194, 5.76022, 5.81842, 5.87782, 5.93846, 6.05167, 6.06364, 6.12826, 6.19429, 6.26177, 6.38487, 6.40132, 6.47347, 6.54730, 6.62285, 6.75745, 6.77936, 6.86046, 6.94354, 7.02868, 7.17678, 7.20545, 7.29725, 7.39143, 7.48810, 7.65221, 7.68932, 7.79407, 7.90173, 8.01244, 8.19578, 8.24352, 8.36416, 8.48841, 8.61644, 8.82320, 8.88454, 9.02498, 9.16996, 9.31970, 9.55543, 9.63446, 9.79998, 9.97133, 10.14880, 10.42106, 10.52351, 10.72149, 10.92708, 11.14075, 11.46011};
	public static double getScaleCorrectionFromLatitude(final double lat) {
		double y = lat;
		if (Math.abs(y) > 85)
			y = 85;
		int idx = (int)(y * 10);
		if (idx < 0) idx = -idx; //the problem is symetric
		return sCorrectionFactors[idx];
	}
	private static final LatLonPoint _llpScaleCorrectionFromY = new LatLonPoint();
	public static double getScaleCorrectionFromY(double y) {
		GoogleBingtoWGS84Mercator(0, y, _llpScaleCorrectionFromY);
		y = _llpScaleCorrectionFromY.getLatitude();
		if (Math.abs(y) > 85) y = 85;
		int idx = (int)(y * 10);
		if (idx < 0) idx = -idx; //the problem is symetric
		return sCorrectionFactors[idx];
	}

	public static double distance(final Point2D.Double p1, final Point2D.Double p2) {
		final double x = p2.x - p1.x;
		final double y = p2.y - p1.y;
		final double cy = p1.y + y*0.5;
		final double cf = getScaleCorrectionFromY(cy);
		return Math.sqrt(x*x + y*y) / cf;
	}

	public static Point2D.Double getXY(final LatLonPoint llp) {
		return WGS84toGoogleBing(llp.getLatitude(), llp.getLongitude(), new Point2D.Double());
	}
	public static Point2D.Double getXY(final double lat, final double lon) {
		return WGS84toGoogleBing(lat, lon, new Point2D.Double());
	}
	public static Point2D.Double getXY(final LatLonPoint llp, final Point2D.Double store) {
		return WGS84toGoogleBing(llp.getLatitude(), llp.getLongitude(), store != null ? store : new Point2D.Double());
	}
	public static Point2D.Double getXY(final double lat, final double lon, final Point2D.Double store) {
		return WGS84toGoogleBing(lat, lon, store != null ? store : new Point2D.Double());
	}



	public static void setViewCenter(final GraphicsView view, final LatLonPoint llp) {
		final Point2D.Double m = getXY(llp);
		view.setCenter(m.x, -m.y);
	}

	public static void setGeoPosition(final GraphicsItem item, final LatLonPoint llp) {
		final Point2D.Double m = getXY(llp);
		item.setCenter(m);
	}
	public static void setGeoPosition(final GraphicsItem item, final double lat, final double lon) {
		final Point2D.Double m = getXY(lat, lon);
		item.setCenter(m);
	}

	public static double squareDistance(final Point2D.Double p1, final Point2D.Double p2) {
		final double x = p2.x - p1.x;
		final double y = p2.y - p1.y;
		return x*x + y*y;
	}
	public static Point2D.Double[] getXY(final LatLonPoint[] vertices) {
		final Point2D.Double[] out = new Point2D.Double[vertices.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = getXY(vertices[i], null);
		}
		return out;
	}


}
