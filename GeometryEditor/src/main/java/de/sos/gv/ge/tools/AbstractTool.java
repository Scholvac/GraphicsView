package de.sos.gv.ge.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import de.sos.gvc.GraphicsView;
import de.sos.gvc.IGraphicsViewHandler;

public abstract class AbstractTool implements IGraphicsViewHandler, MouseListener, MouseMotionListener {

	protected GraphicsView mView;

	@Override
	public void install(GraphicsView view) {
		mView = view;
		view.addMouseListener(this);
		view.addMouseMotionListener(this);
	}

	@Override
	public void uninstall(GraphicsView view) {
		view.removeMouseListener(this);
		view.removeMouseMotionListener(this);
		mView = null;
	}

	@Override
	public void mouseDragged(MouseEvent e) { }

	@Override
	public void mouseMoved(MouseEvent e) { }

	@Override
	public void mouseClicked(MouseEvent e) { }

	@Override
	public void mousePressed(MouseEvent e) { }

	@Override
	public void mouseReleased(MouseEvent e) { }

	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) { }

}
