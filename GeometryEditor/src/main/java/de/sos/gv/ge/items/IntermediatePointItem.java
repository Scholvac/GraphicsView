package de.sos.gv.ge.items;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import de.sos.gv.ge.MathUtils;
import de.sos.gv.ge.model.geom.IGeometry;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.IDrawable;
import de.sos.gvc.drawables.ShapeDrawable;
import de.sos.gvc.handler.MouseDelegateHandler.DelegateMouseEvent;
import de.sos.gvc.styles.DrawableStyle;

/**
 * An intermedia point is represented through a triangle on the edge between two vertices of an geometry. 
 * The IntermediatePoint is used for two purposes: 
 * 	1) indicate the direction of the edge (v_{n} -|>- v_{n+1}) 
 * 	2) split the edge. 
 * The IntermediatePoint implements a mouse listener like the ContourPointItem and creates (and insert) a new Vertex, as soon as it is moved. 
 *  
 * @author sschweigert
 *
 */
public class IntermediatePointItem extends GraphicsItem implements MouseMotionListener , MouseListener {

	private static final DrawableStyle 	sNormalStyle = new DrawableStyle("DefaultIntermediatePointStyle", Color.BLACK, new BasicStroke(1), Color.GRAY);
	private static final DrawableStyle 	sActiveStyle = new DrawableStyle("ActiveIntermediatePointStyle", Color.BLUE, new BasicStroke(1), Color.GREEN);
	
	private static final DrawableStyle 	sIntermediateStyle = new DrawableStyle("ActiveContourPointStyle", Color.RED, new BasicStroke(1), null);
	
	private static final Shape			sBaseShape;
	
	static {
		GeneralPath gp = new GeneralPath();
		gp.moveTo(-2.5f, -2.5f);
		gp.lineTo(0, 5.f);
		gp.lineTo(2.5f, -2.5f);
		gp.lineTo(-2.5, -2.5f);
		gp.closePath();
		sBaseShape = gp;
	}
	
	private IGeometry 			mGeometry;
	private Point2D 			mOldLocation;
	
	private int					mIndex0;
	private int					mIndex1;
	
	class IntermediatePointDrawable extends ShapeDrawable implements IDrawable {
		public IntermediatePointDrawable(IShapeProvider shape) {
			super(shape);
		}

		@Override
		public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
			super.paintItem(g, style, ctx);
			
			
			if (mOldLocation != null) {
				Point2D ill = scene2Local(mOldLocation);
				
				Arc2D.Double arc = new Arc2D.Double(ill.getX()-5, ill.getY()-5, 10, 10, 0, 360, Arc2D.CHORD);
				sIntermediateStyle.applyLinePaint(g, ctx, arc);
				g.draw(arc);
			
				Point2D prevPoint = mGeometry.getPoint(mIndex0);
				Point2D nextPoint = mGeometry.getPoint(mIndex1);
				if (prevPoint != null) {
					Point2D pp = scene2Local(prevPoint);
					g.draw(new Line2D.Double(0, 0, pp.getX(), pp.getY()));
				}
				if (nextPoint != null) {
					Point2D np = scene2Local(nextPoint);
					g.draw(new Line2D.Double(0, 0, np.getX(), np.getY()));
				}
			}
		}
		
	}
	
	public IntermediatePointItem(IGeometry geom, int idx0, int idx1) {
		super(sBaseShape);
		mGeometry = geom;
		mIndex0 = idx0;
		mIndex1 = idx1;
		
		setStyle(sNormalStyle);
		setDrawable(new IntermediatePointDrawable(this));
		
		setMouseMotionSupport(this);
		setMouseSupport(this);
		setSelectable(false);
	}
	
	@Override
	public void draw(Graphics2D g, IDrawContext ctx) {
		setScale(ctx.getScale());
		super.draw(g, ctx);
	}

	/**
	 * Updates its scene-location and rotation and shall be called if either the geometry or one of the controll points changes
	 * @note this method shall only be called if the item has been added to its parent, otherwise the scene-location calculation would not work
	 */
	public void update() {
		Point2D p0 = mGeometry.getPoint(mIndex0);
		Point2D p1 = mGeometry.getPoint(mIndex1);
		setLocalLocation(MathUtils.getIntermediatePosition(p0, p1));
		setRotation(MathUtils.getRotation(p0, p1));		
	}
	
	
	
	public int getIndex0() { return mIndex0; }
	public int getIndex1() { return mIndex1; }
	
	
	


	@Override
	public void mousePressed(MouseEvent e) {
		setStyle(sActiveStyle);
		Point2D sl = getSceneLocation();
		mOldLocation = new Point2D.Double(sl.getX(), sl.getY());
		if (e instanceof DelegateMouseEvent)
			((DelegateMouseEvent) e).addPermanentMouseMotionListener(this); 
		e.consume();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (e.isConsumed() == false) {
			 setSceneLocation(getView().getSceneLocation(e.getPoint()));
			 e.consume();
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		setStyle(sNormalStyle);
		
		//add the new position of this vertex item
		mGeometry.addPoint(mIndex1, getCenter());//get center is the position in local coordinates.
		
		mOldLocation = null;
		if (e instanceof DelegateMouseEvent)
			((DelegateMouseEvent) e).removePermanentMouseMotionListener(this);
		e.consume();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		e.getComponent().setCursor(new Cursor(Cursor.MOVE_CURSOR));
		e.consume();
	}


	@Override
	public void mouseExited(MouseEvent e) {
		e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		e.consume();
	}

	

	@Override
	public void mouseMoved(MouseEvent e) {}
	@Override
	public void mouseClicked(MouseEvent e) {}


}
