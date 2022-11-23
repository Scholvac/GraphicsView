package de.sos.gvc.handler;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import de.sos.gvc.IGraphicsView;
import de.sos.gvc.IGraphicsViewHandler;

/**
 * Default view drag and scroll behaviour
 * - if the mouse is dragged (and the event has not been consumed before) the views center is changed accordingly
 * - if the mouse wheel is used (and the event has not been consumed before) the view is scaled accordingly
 * @author scholvac
 *
 */
public class DefaultViewDragHandler implements IGraphicsViewHandler, MouseListener, MouseMotionListener, MouseWheelListener {

	private IGraphicsView mView;

	protected Point mLastScreenPosition; //used to remember the last position, for dx / dy calculation during drag

	@Override
	public void install(final IGraphicsView view) {
		mView = view;
		mView.addMouseListener(this);
		mView.addMouseMotionListener(this);
		mView.addMouseWheelListener(this);
	}

	@Override
	public void uninstall(final IGraphicsView view) {
		if (mView == view)
			mView = null;
		mView.removeMouseListener(this);
		mView.removeMouseMotionListener(this);
		mView.removeMouseWheelListener(this);
	}



	@Override
	public void mouseClicked(final MouseEvent e) { }

	@Override
	public void mousePressed(final MouseEvent e) {
		if (!e.isConsumed()) {
			mLastScreenPosition = e.getPoint();
		}
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		mLastScreenPosition = null;
	}

	@Override
	public void mouseEntered(final MouseEvent e) { }
	@Override
	public void mouseExited(final MouseEvent e) {}

	@Override
	public void mouseDragged(final MouseEvent e) {
		if (!e.isConsumed() && mLastScreenPosition != null) {
			final double dx = e.getPoint().getX() - mLastScreenPosition.getX();
			final double dy = e.getPoint().getY() - mLastScreenPosition.getY();
			final double angle_radian = Math.toRadians(-mView.getRotationDegrees());
			//rotation matrix
			final double cos = Math.cos(angle_radian);
			final double sin = Math.sin(angle_radian);
			final double dxx = dx * cos + dy * -sin;
			final double dyy = dx * sin + dy * cos;


			mLastScreenPosition.setLocation(e.getPoint());
			final double scaleX = mView.getScaleX();
			final double scaleY = mView.getScaleY();
			final double x = mView.getCenterX();
			final double y = mView.getCenterY();
			final double xx = x - dxx * scaleX;
			final double yy = y - dyy * scaleY;
			mView.setCenter(xx, yy);
		}else {
			mLastScreenPosition = null;
		}
	}

	@Override
	public void mouseMoved(final MouseEvent e) {}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {
		if (e.isConsumed())
			return ;
		final int dir = e.getWheelRotation();
		if (dir == 0)
			return ; //this may happen if the "mouse" accumulates until the first 'click', see MouseWheelEvent doc.
		//scale x and y with same ratio
		final double factor = dir < 0 ? 0.8 : 1.2;
		final double scaleX = mView.getScaleX() * factor;
		final double scaleY = mView.getScaleY() * factor;
		mView.setScale(scaleX, scaleY);
	}

	protected IGraphicsView getView() {
		return mView;
	}
}
