package de.sos.gv.ge;

import java.awt.geom.Point2D;

public class MathUtils {

	public static double getLength(final Point2D p0, final Point2D p1) {
		final double dx = p1.getX() - p0.getX(), dy = p1.getY() - p0.getY();
		final double l = Math.sqrt(dx*dx+dy*dy);
		return l;
	}


	public static Point2D getIntermediatePosition(final Point2D p0, final Point2D p1) {
		double dx = p1.getX() - p0.getX(), dy = p1.getY() - p0.getY();
		final double l = Math.sqrt(dx*dx+dy*dy);
		dx /= l; dy /= l;
		return new Point2D.Double(p0.getX() + dx * 0.5 * l, p0.getY() + dy * 0.5 * l);
	}


	public static double getRotation(final Point2D p0, final Point2D p1) {
		final double dx = p1.getX() - p0.getX(), dy = p1.getY() - p0.getY();
		double l = Math.sqrt(dx*dx+dy*dy);
		if(l < 1e-6f)
			l = 1e-6f;
		double f = dy / l; //dot product with 0,1
		if (f < -1) f = -1; if (f > 1) f = 1; //clamp(f, -1, 1);
		double angle = Math.acos(f);

		final double cross = -1 * dx; //crossproduct with 0,1
		if (cross>0)
			angle = Math.PI*2.0- angle;
		return Math.toDegrees(-angle);
	}
}
