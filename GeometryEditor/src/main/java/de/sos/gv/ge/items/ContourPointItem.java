package de.sos.gv.ge.items;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
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

	private int mIndex;

	private IGeometry 			mGeometry;
	private Point2D 			mPrevPoint;
	private Point2D 			mNextPoint;
	private Point2D 			mOldLocation;
	private MenuManager 		mMenuManager;
	private GeometryItem 		mGeometryItem;


	class ContourPointDrawable extends ShapeDrawable implements IDrawable {

		public ContourPointDrawable(final IShapeProvider shape) {
			super(shape);
		}

		@Override
		public void paintItem(final Graphics2D g, final DrawableStyle style, final IDrawContext ctx) {
			super.paintItem(g, style, ctx);

			if (mOldLocation != null) {
				final Point2D ill = scene2Local(mOldLocation);

				final Arc2D.Double arc = new Arc2D.Double(ill.getX()-5, ill.getY()-5, 10, 10, 0, 360, Arc2D.CHORD);
				Styles.IntermediateStyle.applyLinePaint(g, ctx, arc);
				g.draw(arc);

				final Point2D.Double c = (Point2D.Double) mGeometryItem.getCenter(); //parent center
				if (mPrevPoint != null) {
					final Point2D pp = scene2Local(mPrevPoint);
					g.draw(new Line2D.Double(0, 0, c.x + pp.getX(), c.y + pp.getY()));
				}
				if (mNextPoint != null) {
					final Point2D np = scene2Local(mNextPoint);
					g.draw(new Line2D.Double(0, 0, c.x + np.getX(), c.y+np.getY()));
				}
			}
		}

	}

	public ContourPointItem(final MenuManager mm, final GeometryItem parent, final int idx) {
		super(new Arc2D.Double(-5, -5, 10, 10, 0, 360, Arc2D.CHORD));
		mGeometry = parent.getGeometry();
		mGeometryItem = parent;
		mIndex = idx;
		mMenuManager = mm;

		setStyle(Styles.NormalStyle);
		setDrawable(new ContourPointDrawable(this));

		setMouseMotionSupport(this);
		setMouseSupport(this);
		setSelectable(false);
	}

	private boolean allowsManipulation() {
		return mGeometryItem.allowsManipulation();
	}

	@Override
	public void draw(final Graphics2D g, final IDrawContext ctx) {
		setScale(ctx.getScale());
		super.draw(g, ctx);
	}

	public int getIndex() { return mIndex; }

	@Override
	public void mouseDragged(final MouseEvent e) {
		if (allowsManipulation() && e.isConsumed() == false) {
			final Point2D sl = getView().getSceneLocation(e.getPoint());
			setSceneLocation(sl);
			e.consume();
		}
	}


	@Override
	public void mouseMoved(final MouseEvent e) {}


	@Override
	public void mouseClicked(final MouseEvent e) {
		if (allowsManipulation() && SwingUtilities.isRightMouseButton(e)) {
			final JPopupMenu pm = new JPopupMenu();
			mMenuManager.fillContourItemMenu(this, mGeometry, pm);
			pm.show(getView().getComponent(), e.getX(), e.getY());
			e.consume();
		}
	}


	@Override
	public void mousePressed(final MouseEvent e) {
		if (allowsManipulation()) {
			setStyle(Styles.ActiveStyle);
			mPrevPoint = mGeometry.getPreviousPoint(mIndex);
			mNextPoint = mGeometry.getNextPoint(mIndex);


			final Point2D sl = getSceneLocation();
			mOldLocation = new Point2D.Double(sl.getX(), sl.getY());
			if (e instanceof DelegateMouseEvent)
				((DelegateMouseEvent) e).addPermanentMouseMotionListener(this);
			e.consume();
		}
	}


	@Override
	public void mouseReleased(final MouseEvent e) {
		if (allowsManipulation()) {
			setStyle(Styles.NormalStyle);

			//set the new position of this vertex item
			markDirty();
			updateWorldTransform();
			System.out.println("Center of index: " + mIndex + " = " + getCenter());
			mGeometry.replacePoint(mIndex, getCenter()); //get center is the position in local coordinates.

			mNextPoint = mPrevPoint = mOldLocation = null;
			if (e instanceof DelegateMouseEvent)
				((DelegateMouseEvent) e).removePermanentMouseMotionListener(this);
			e.consume();
		}
	}


	@Override
	public void mouseEntered(final MouseEvent e) {
		if (allowsManipulation()) {
			e.getComponent().setCursor(new Cursor(Cursor.MOVE_CURSOR));
			e.consume();
		}
	}


	@Override
	public void mouseExited(final MouseEvent e) {
		if (allowsManipulation()) {
			e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			e.consume();
		}
	}

}
