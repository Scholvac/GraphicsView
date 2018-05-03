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
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.isConsumed() == false)
			mLastScreenPosition = e.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mLastScreenPosition = null;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (e.isConsumed() == false && mLastScreenPosition != null) {
			double dx = e.getPoint().getX() - mLastScreenPosition.getX();
			double dy = e.getPoint().getY() - mLastScreenPosition.getY();
			mLastScreenPosition.setLocation(e.getPoint());
			double scaleX = mView.getScaleX();
			double scaleY = mView.getScaleY();
			double x = mView.getCenterX();
			double y = mView.getCenterY();
			mView.setCenter(x - dx * scaleX, y - dy * scaleY);
		}else {
			mLastScreenPosition = null;
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.isConsumed())
			return ;
		int dir = e.getWheelRotation();
		//scale x and y with same ratio
		double factor = dir < 0 ? 0.8 : 1.2;
		double scaleX = mView.getScaleX() * factor;
		double scaleY = mView.getScaleY() * factor;
		mView.setScale(scaleX, scaleY);
	}



}
