package de.sos.gvc;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author scholvac
 *
 */
public class Utils {

	public static Rectangle2D transform(Rectangle2D rect, AffineTransform transform) {
		double mix = rect.getMinX();
		double max = rect.getMaxX();
		double miy = rect.getMinY();
		double may = rect.getMaxY();
		//build all 4 vertices
		double[] vertices = new double[] {
				mix, may, //ul
				max, may, //ur
				max, miy, //lr
				mix, miy  //ll
		};
		double[] newVertices = new double[8];
		transform.transform(vertices, 0, newVertices, 0, 4);
		mix = miy = Double.MAX_VALUE;
		max = may = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i+=2) {
			double x = newVertices[i], y = newVertices[i+1];
			mix = Math.min(x, mix);
			max = Math.max(x, max);
			miy = Math.min(y, miy);
			may = Math.max(y, may);
		}
		double w = max - mix;
		double h = may - miy;
		return new Rectangle2D.Double(mix, miy, w, h);
	}

	public static Rectangle2D inverseTransform(Rectangle2D rect, AffineTransform transform) {
		double mix = rect.getMinX();
		double max = rect.getMaxX();
		double miy = rect.getMinY();
		double may = rect.getMaxY();
		//build all 4 vertices
		double[] vertices = new double[] {
				mix, may, //ul
				max, may, //ur
				max, miy, //lr
				mix, miy  //ll
		};
		double[] newVertices = new double[8];
		try {
			transform.inverseTransform(vertices, 0, newVertices, 0, 4);
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
			return null;
		}
		mix = miy = Double.MAX_VALUE;
		max = may = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i+=2) {
			double x = newVertices[i], y = newVertices[i+1];
			mix = Math.min(x, mix);
			max = Math.max(x, max);
			miy = Math.min(y, miy);
			may = Math.max(y, may);
		}
		double w = max - mix;
		double h = may - miy;
		return new Rectangle2D.Double(mix, miy, w, h);
	}
	
	public static Point2D[] getVertices(Rectangle2D rect) {
		double mix = rect.getMinX();
		double max = rect.getMaxX();
		double miy = rect.getMinY();
		double may = rect.getMaxY();
		
		return new Point2D[] {
				new Point2D.Double(mix, may),
				new Point2D.Double(max, may),
				new Point2D.Double(max, miy),
				new Point2D.Double(mix, miy)
		};
	}

	public static List<Rectangle2D> verticesToRectangle(List<Point2D[]> verticesList) {
		ArrayList<Rectangle2D> out = new ArrayList<>();
		for (Point2D[] vertices : verticesList) {
			out.add(verticesToRectangle(vertices));
		}
		return out;
	}

	public static Rectangle2D verticesToRectangle(Point2D[] vertices) {
		double mix = Double.MAX_VALUE, miy = Double.MAX_VALUE;
		double max = -Double.MAX_EXPONENT, may = -Double.MAX_VALUE;
		for (int i = 0; i < vertices.length; i++) {
			double x = vertices[i].getX(), y = vertices[i].getY();
			mix = Math.min(x, mix);
			max = Math.max(x, max);
			miy = Math.min(y, miy);
			may = Math.max(y, may);
		}
		double w = max - mix;
		double h = may - miy;
		return new Rectangle2D.Double(mix, miy, w, h);
	}



}
