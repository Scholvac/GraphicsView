package de.sos.gvc.handler;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import de.sos.gvc.GraphicsView;
import de.sos.gvc.IGraphicsViewHandler;

/**
 * Default view drag and scroll behaviour
 * - if the mouse is dragged (and the event has not been consumed before) the views center is changed accordingly
 * - if the mouse wheel is used (and the event has not been consumed before) the view is scaled accordingly
 * @author scholvac
 *
 */
public class DefaultViewDragHandler implements IGraphicsViewHandler, MouseListener, MouseMotionListener, MouseWheelListener {

	private GraphicsView mView;

	private Point mLastScreenPosition; //used to remember the last position, for dx / dy calculation during drag

	@Override
	public void install(GraphicsView view) {
		mView = view;
		mView.addMouseListener(this);
		mView.addMouseMotionListener(this);
		mView.addMouseWheelListener(this);
	}

	@Override
	public void uninstall(GraphicsView view) {
		if (mView == view)
			mView = null;
		mView.removeMouseListener(this);
		mView.removeMouseMotionListener(this);
		mView.removeMouseWheelListener(this);
	}



	@Override
	public void mouseClicked(MouseEvent e) { }

	@Override
	public void mousePressed(MouseEvent e) {
		if (!e.isConsumed()) {
			mLastScreenPosition = e.getPoint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mLastScreenPosition = null;
	}

	@Override
	public void mouseEntered(MouseEvent e) { }
	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!e.isConsumed() && mLastScreenPosition != null) {
			double dx = e.getPoint().getX() - mLastScreenPosition.getX();
			double dy = e.getPoint().getY() - mLastScreenPosition.getY();
			double angle_radian = Math.toRadians(-mView.getRotationDegrees());
			//rotation matrix
			double cos = Math.cos(angle_radian);
			double sin = Math.sin(angle_radian);
			double dxx = dx * cos + dy * -sin;
			double dyy = dx * sin + dy * cos;


			mLastScreenPosition.setLocation(e.getPoint());
			double scaleX = mView.getScaleX();
			double scaleY = mView.getScaleY();
			double x = mView.getCenterX();
			double y = mView.getCenterY();
			double xx = x - dxx * scaleX;
			double yy = y - dyy * scaleY;
			mView.setCenter(xx, yy);
		}else {
			mLastScreenPosition = null;
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
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



}
