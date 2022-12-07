package de.sos.gvc.rt;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.RenderManager;

public class JPanelRenderTarget extends JPanel implements IRenderTarget{

	private GraphicsView mView;
	private RenderManager mLastManager;

	public JPanelRenderTarget() {
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				mView.resetViewTransform();
			}
		});
	}


	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public void setGraphicsView(final GraphicsView view) {
		mView = view;
	}

	@Override
	public void requestRepaint() {
		repaint();
	}
	@Override
	public void proposeRepaint(final RenderManager renderManager) {
		mLastManager = renderManager;
		repaint();//forward request to awt
	}
	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);
		if (mLastManager != null) {
			mLastManager.doPaint((Graphics2D)g);
		}else
			mView.doPaint((Graphics2D) g);
	}
}
