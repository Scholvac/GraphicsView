package de.sos.gv.ge.items;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import de.sos.gv.ge.menu.MenuManager;
import de.sos.gv.ge.model.geom.IGeometry;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.IDrawable;
import de.sos.gvc.drawables.ShapeDrawable;
import de.sos.gvc.handler.MouseDelegateHandler.DelegateMouseEvent;
import de.sos.gvc.styles.DrawableStyle;

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
	private MenuManager 		mMenuManager;

	
	class ContourPointDrawable extends ShapeDrawable implements IDrawable {
		
		public ContourPointDrawable(IShapeProvider shape) {
			super(shape);
		}

		@Override
		public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
			super.paintItem(g, style, ctx);
			
			
			if (mOldLocation != null) {
				Point2D ill = mOldLocation;
				
				Arc2D.Double arc = new Arc2D.Double(ill.getX()-5, ill.getY()-5, 10, 10, 0, 360, Arc2D.CHORD); 
				sIntermediateStyle.applyLinePaint(g, ctx, arc);
				g.draw(arc);
			
				if (mPrevPoint != null) {
					g.draw(new Line2D.Double(0, 0, mPrevPoint.getX(), mPrevPoint.getY()));
				}
				if (mNextPoint != null) {
					g.draw(new Line2D.Double(0, 0, mNextPoint.getX(), mNextPoint.getY()));
				}
			}
		}
		
	}
	
	public ContourPointItem(MenuManager mm, IGeometry geom, int idx) {
		super(new Arc2D.Double(-5, -5, 10, 10, 0, 360, Arc2D.CHORD));
		mGeometry = geom;
		mIndex = idx;
		mMenuManager = mm;

		setStyle(sNormalStyle);
		setDrawable(new ContourPointDrawable(this));
		
		setMouseMotionSupport(this);
		setMouseSupport(this);
		setSelectable(false);
	}

	
	@Override
	public void draw(Graphics2D g, IDrawContext ctx) {
		setScale(ctx.getScale());
		super.draw(g, ctx);
	}
	
	
	
	
	
	
	
	public int getIndex() { return mIndex; }
	
	


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
		if (SwingUtilities.isRightMouseButton(e)) {
			JPopupMenu pm = new JPopupMenu();
			mMenuManager.fillContourItemMenu(this, mGeometry, pm);
			pm.show(getView(), e.getX(), e.getY());
			e.consume();
		}
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
		
		//set the new position of this vertex item
		mGeometry.replacePoint(mIndex, getCenter()); //get center is the position in local coordinates.
		
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
