package de.sos.gv.ge.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

import de.sos.gvc.GraphicsScene;
import de.sos.gvc.IGraphicsView;
import de.sos.gvc.IGraphicsViewHandler;

public abstract class AbstractTool implements IGraphicsViewHandler, MouseListener, MouseMotionListener {

	private IGraphicsView 	mView;


	public boolean isActive() { return getView() != null; }

	@Override
	public void install(final IGraphicsView view) {
		mView = view;
		view.addMouseListener(this);
		view.addMouseMotionListener(this);

		activate();
	}

	@Override
	public void uninstall(final IGraphicsView view) {
		deactivate();

		view.removeMouseListener(this);
		view.removeMouseMotionListener(this);
		mView = null;
	}

	protected void activate() {}
	protected void deactivate() {}

	@Override
	public void mouseDragged(final MouseEvent e) { }

	@Override
	public void mouseMoved(final MouseEvent e) { }

	@Override
	public void mouseClicked(final MouseEvent e) { }

	@Override
	public void mousePressed(final MouseEvent e) { }

	@Override
	public void mouseReleased(final MouseEvent e) { }

	@Override
	public void mouseEntered(final MouseEvent e) { }

	@Override
	public void mouseExited(final MouseEvent e) { }

	protected Point2D getSceneLocation(final MouseEvent e) {
		return getView().getSceneLocation(e.getPoint());
	}

	protected IGraphicsView getView() { return mView; }
	protected GraphicsScene getScene() {
		return mView.getScene();
	}
}
