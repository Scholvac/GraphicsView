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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import de.sos.gv.ge.model.geom.IGeometry;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.IDrawable;
import de.sos.gvc.drawables.ShapeDrawable;
import de.sos.gvc.handler.MouseDelegateHandler.DelegateMouseEvent;
import de.sos.gvc.param.IParameter;
import de.sos.gvc.styles.DrawableStyle;
import de.sos.gvc.styles.ScaledStroke;

public class ContourPointItem extends GraphicsItem implements MouseMotionListener , MouseListener
{
	
	private static final DrawableStyle 	sNormalStyle = new DrawableStyle("DefaultContourPointStyle", Color.BLACK, new BasicStroke(2), Color.RED);
	private static final DrawableStyle 	sActiveStyle = new DrawableStyle("ActiveContourPointStyle", Color.BLUE, new BasicStroke(2), Color.GREEN);
	
	private static final DrawableStyle 	sIntermediateStyle = new DrawableStyle("ActiveContourPointStyle", Color.RED, new BasicStroke(2), null);
	
	private int mIndex;

	private IGeometry 			mGeometry;
	private Point2D 			mPrevPoint;
	private Point2D 			mNextPoint;
	private Point2D 			mOldLocation;

	
	class ContourPointDrawable extends ShapeDrawable implements IDrawable {
		public ContourPointDrawable(IParameter<Shape> shape) {
			super(shape);
		}

		@Override
		public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
			super.paintItem(g, style, ctx);
			
			
			if (mOldLocation != null) {
				Point2D ill = scene2Local(mOldLocation);
				
				sIntermediateStyle.applyLinePaint(g, ctx);
				g.draw(new Arc2D.Double(ill.getX()-5, ill.getY()-5, 10, 10, 0, 360, Arc2D.CHORD));
			
				if (mPrevPoint != null) {
					Point2D pp = scene2Local(mPrevPoint);
					g.draw(new Line2D.Double(0, 0, pp.getX(), pp.getY()));
				}
				if (mNextPoint != null) {
					Point2D np = scene2Local(mNextPoint);
					g.draw(new Line2D.Double(0, 0, np.getX(), np.getY()));
				}
			}
		}
		
	}
	
	public ContourPointItem(IGeometry geom, int idx) {
		super(new Arc2D.Double(-5, -5, 10, 10, 0, 360, Arc2D.CHORD));
		mGeometry = geom;
		mIndex = idx;

		setStyle(sNormalStyle);
		setDrawable(new ContourPointDrawable(getShapeProperty()));
		
		setMouseMotionSupport(this);
		setMouseSupport(this);
	}

	
	@Override
	public void draw(Graphics2D g, IDrawContext ctx) {
		setScale(ctx.getScale());
		super.draw(g, ctx);
	}


	@Override
	public void mouseDragged(MouseEvent e) {
		if (e.isConsumed() == false) {
			 setSceneLocation(getView().getSceneLocation(e.getPoint()));
			 e.consume();
		}
	}


	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mousePressed(MouseEvent e) {
		setStyle(sActiveStyle);
		mPrevPoint = mGeometry.getPreviousPoint(mIndex);
		mNextPoint = mGeometry.getNextPoint(mIndex);
		Point2D sl = getSceneLocation();
		mOldLocation = new Point2D.Double(sl.getX(), sl.getY());
		if (e instanceof DelegateMouseEvent)
			((DelegateMouseEvent) e).addPermanentMouseMotionListener(this); 
		e.consume();
	}


	@Override
	public void mouseReleased(MouseEvent e) {
		setStyle(sNormalStyle);
		
		mNextPoint = mPrevPoint = mOldLocation = null;
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
	
}
