package de.sos.gvc;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.DoubleStream;

import de.sos.gvc.GraphicsScene.IItemFilter;
import de.sos.gvc.GraphicsScene.ShapeSelectionFilter;

/**
 *
 * @author scholvac
 *
 */
public class Utils {

	public static Rectangle2D transform(final Rectangle2D rect, final AffineTransform transform, final Rectangle2D store) {
		double mix = rect.getMinX();
		double max = rect.getMaxX();
		double miy = rect.getMinY();
		double may = rect.getMaxY();
		//build all 4 vertices
		final double[] vertices = {
				mix, may, //ul
				max, may, //ur
				max, miy, //lr
				mix, miy  //ll
		};
		final double[] newVertices = new double[8];
		transform.transform(vertices, 0, newVertices, 0, 4);
		mix = miy = Double.MAX_VALUE;
		max = may = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i+=2) {
			final double x = newVertices[i], y = newVertices[i+1];
			mix = Math.min(x, mix);
			max = Math.max(x, max);
			miy = Math.min(y, miy);
			may = Math.max(y, may);
		}
		final double w = max - mix;
		final double h = may - miy;
		if (store == null)
			return new Rectangle2D.Double(mix, miy, w, h);
		store.setRect(mix, miy, w, h);
		return store;
	}
	public static Rectangle2D transform(final Rectangle2D rect, final AffineTransform transform) {
		return transform(rect, transform, null);
	}

	public static Rectangle2D inverseTransform(final Rectangle2D rect, final AffineTransform transform) {
		double mix = rect.getMinX();
		double max = rect.getMaxX();
		double miy = rect.getMinY();
		double may = rect.getMaxY();
		//build all 4 vertices
		final double[] vertices = {
				mix, may, //ul
				max, may, //ur
				max, miy, //lr
				mix, miy  //ll
		};
		final double[] newVertices = new double[8];
		try {
			transform.inverseTransform(vertices, 0, newVertices, 0, 4);
		} catch (final NoninvertibleTransformException e) {
			e.printStackTrace();
			return null;
		}
		mix = miy = Double.MAX_VALUE;
		max = may = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i+=2) {
			final double x = newVertices[i], y = newVertices[i+1];
			mix = Math.min(x, mix);
			max = Math.max(x, max);
			miy = Math.min(y, miy);
			may = Math.max(y, may);
		}
		final double w = max - mix;
		final double h = may - miy;
		return new Rectangle2D.Double(mix, miy, w, h);
	}

	public static Point2D[] getVertices(final Rectangle2D rect) {
		final double mix = rect.getMinX();
		final double max = rect.getMaxX();
		final double miy = rect.getMinY();
		final double may = rect.getMaxY();

		return new Point2D[] {
				new Point2D.Double(mix, may),
				new Point2D.Double(max, may),
				new Point2D.Double(max, miy),
				new Point2D.Double(mix, miy)
		};
	}

	public static List<Rectangle2D> verticesToRectangle(final List<Point2D[]> verticesList) {
		final ArrayList<Rectangle2D> out = new ArrayList<>();
		for (final Point2D[] vertices : verticesList) {
			out.add(verticesToRectangle(vertices));
		}
		return out;
	}

	public static Rectangle2D verticesToRectangle(final Point2D[] vertices) {
		double mix = Double.MAX_VALUE, miy = Double.MAX_VALUE;
		double max = -Double.MAX_EXPONENT, may = -Double.MAX_VALUE;
		for (int i = 0; i < vertices.length; i++) {
			final double x = vertices[i].getX(), y = vertices[i].getY();
			mix = Math.min(x, mix);
			max = Math.max(x, max);
			miy = Math.min(y, miy);
			may = Math.max(y, may);
		}
		final double w = max - mix;
		final double h = may - miy;
		return new Rectangle2D.Double(mix, miy, w, h);
	}


	/** Temporary variables, to avoid creation of often used instances.
	 *
	 * @author sschweigert
	 *
	 * \source this concept has been inspired / copied from jMonkeyEngine - TempVars.class
	 */
	public static class TmpVars implements AutoCloseable {

		private boolean 						isUsed = false;

		public final GraphicsItem[]				itemStack = new GraphicsItem[32]; //assume that we have no more than 32 hierarchie levels in the scenegraph
		public final double[]					doubles = new double[64];
		public final int[]						ints = new int[64];
		public final boolean[]					booleans = new boolean[64];

		private static final int STACK_SIZE = 5;

		private static class TmpVarsStack {
			int 				index = 0;
			TmpVars[] 			tempVars = new TmpVars[STACK_SIZE];
		}
		/**
		 * ThreadLocal to store a TmpVarsStack for each thread.
		 * This ensures each thread has a single TempVarsStack that is
		 * used only in method calls in that thread.
		 */
		private static final ThreadLocal<TmpVarsStack> varsLocal = new ThreadLocal<TmpVarsStack>() {
			@Override
			public TmpVarsStack initialValue() {
				return new TmpVarsStack();
			}
		};

		public static TmpVars get() {
			final TmpVarsStack stack = varsLocal.get();
			TmpVars instance = stack.tempVars[stack.index];
			if (instance == null) {
				// Create new
				instance = new TmpVars();
				// Put it in there
				stack.tempVars[stack.index] = instance;
			}
			stack.index++;
			instance.isUsed = true;
			return instance;
		}

		/**
		 * Releases this instance of TmpVars.
		 * Once released, the contents of the TmpVars are undefined.
		 * The TmpVars must be released in the opposite order that they are retrieved,
		 * e.g. Acquiring vars1, then acquiring vars2, vars2 MUST be released
		 * first otherwise an exception will be thrown.
		 */
		public void release() {
			if (!isUsed) {
				throw new IllegalStateException("This instance of TempVars was already released!");
			}
			isUsed = false;
			final TmpVarsStack stack = varsLocal.get();
			// Return it to the stack
			stack.index--;
			// Check if it is actually there
			if (stack.tempVars[stack.index] != this) {
				throw new IllegalStateException("An instance of TempVars has not been released in a called method!");
			}
		}

		@Override
		public void close() throws Exception {
			release();
		}
	}

	public static GraphicsItem getBestFit(final GraphicsView view, final Point viewPoint, final double epsilon, final IItemFilter ...filter ) {
		final Point2D scene = view.getSceneLocation(viewPoint, null);
		final double eps = epsilon * view.getScaleX();
		return getBestFit(view.getScene(), scene, eps, filter);
	}
	public static GraphicsItem getBestFit(final GraphicsScene scene, final Point2D scenePoint, final double epsilonInMeter, final IItemFilter...filter) {
		final Rectangle2D r = new Rectangle2D.Double(scenePoint.getX() - epsilonInMeter/2, scenePoint.getY() - epsilonInMeter/2, epsilonInMeter, epsilonInMeter);
		final ShapeSelectionFilter ssf = new ShapeSelectionFilter(scenePoint, epsilonInMeter, true);
		final List<GraphicsItem> items = scene.getAllItems(r, IItemFilter.combound(ssf, filter));
		if (items == null || items.isEmpty())
			return null;

		//sort by Z-Order
		items.sort( Comparator.comparing(GraphicsItem::getZOrder).reversed() );
		return items.get(0);
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//					Vector operations
	///////////////////////////////////////////////////////////////////////////////////////////

	public static Point2D.Double centerOf(final Point2D.Double start, final Point2D.Double end) {
		final Point2D.Double vector = direction(start, end);
		final double length = length(vector);
		mul(vector, .5/length, vector);
		return vector;
	}
	public static Point2D.Double mul(final Point2D.Double vector, final double factor, final Point2D.Double result) {
		result.x = vector.x * factor;
		result.y = vector.y * factor;
		return result;
	}
	public static Point2D.Double add(final Point2D.Double center, final Point2D.Double double1) {
		return add(center, double1, new Point2D.Double());
	}
	public static Point2D.Double add(final Point2D.Double vector, final Point2D.Double v2, final Point2D.Double result) {
		result.x = vector.x + v2.x;
		result.y = vector.y + v2.y;
		return result;
	}
	public static double length(final Point2D.Double v) {
		return Math.sqrt(lengthSq(v));
	}
	public static double lengthSq(final Point2D.Double v) {
		return v.x*v.x + v.y*v.y;
	}
	public static Point2D.Double normalize(final Point2D.Double vector, final Point2D.Double result) {
		return mul(vector, 1.0/length(vector), result);
	}
	public static Point2D.Double direction(final Point2D.Double start, final Point2D.Double end) {
		return direction(start, end, new Point2D.Double());
	}
	public static Point2D.Double direction(final Point2D.Double start, final Point2D.Double end, final Point2D.Double result) {
		result.x = end.x-start.x;
		result.y = end.y-start.y;
		return result;
	}
	public static double angleBetweenDeg(final Point2D.Double v2, final Point2D.Double v1) {
		final double[] v12 = {v2.getX() - v1.getX(), v2.getY() - v1.getY()};
		final double deg = Math.toDegrees(Math.acos(v12[1] / Math.sqrt(v12[0]*v12[0] + v12[1]*v12[1])));
		return v12[0]<0.0 ? -deg : deg;
	}


	///////////////////////////////////////////////////////////////////////////////////////////
	//					Geometry
	///////////////////////////////////////////////////////////////////////////////////////////

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

	public static Arc2D.Double createArc2D(final double radius){
		return new Arc2D.Double(-radius, -radius, 2*radius, 2*radius, 0, 360, Arc2D.CHORD);
	}


	///////////////////////////////////////////////////////////////////////////////////////////
	//					Statistic
	///////////////////////////////////////////////////////////////////////////////////////////

	public static class WindowStat {
		final double[] 		mBuffer;
		int					mIndex = 0;
		double				mSum = 0;
		int					mCount = 0;

		public WindowStat(final int windowSize) {
			mBuffer = new double[windowSize];
		}

		public void accept(final double v) {
			mSum -= mBuffer[mIndex];
			mSum += v;
			mBuffer[mIndex] = v;
			mCount++;
			if (mIndex++ >= mBuffer.length-1)
				mIndex = 0;
		}
		public double avg() {
			final double c = mCount > mBuffer.length ? mBuffer.length : mCount;
			return mSum / c;
		}
		public double sum() { return mSum;}
		public double min() {return DoubleStream.of(mBuffer).min().getAsDouble();}
		public double max() {return DoubleStream.of(mBuffer).max().getAsDouble();}
		public int windowSize() { return mBuffer.length;}
		public int count() { return mCount;}

		@Override
		public String toString() {
			return String.format(
					"%s{count=%d, sum=%f, min=%f, average=%f, max=%f}",
					this.getClass().getSimpleName(), count(), sum(), min(), avg(), max());
		}
	}
}
