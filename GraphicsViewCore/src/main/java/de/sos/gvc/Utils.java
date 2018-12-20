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

	public static Rectangle2D transform(Rectangle2D rect, AffineTransform transform, Rectangle2D store) {
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
		if (store == null)
			return new Rectangle2D.Double(mix, miy, w, h);
		store.setRect(mix, miy, w, h);
		return store;
	}
	public static Rectangle2D transform(Rectangle2D rect, AffineTransform transform) {
		return transform(rect, transform, null);
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


	/** Temporary variables, to avoid creation of often used instances. 
	 * 
	 * @author sschweigert
	 *
	 * \source this concept has been inspired / copied from jMonkeyEngine - TempVars.class
	 */
	public static class TmpVars {

		private boolean 						isUsed = false;
		
		public final GraphicsItem[]				itemStack = new GraphicsItem[32]; //assume that we have no more than 32 hierarchie levels in the scenegraph

		
		
		
		
		
		
		
		
		
		
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
			 TmpVarsStack stack = varsLocal.get();
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
	        TmpVarsStack stack = varsLocal.get();
	        // Return it to the stack
	        stack.index--;
	        // Check if it is actually there
	        if (stack.tempVars[stack.index] != this) {
	            throw new IllegalStateException("An instance of TempVars has not been released in a called method!");
	        }
	    }
	}



}
