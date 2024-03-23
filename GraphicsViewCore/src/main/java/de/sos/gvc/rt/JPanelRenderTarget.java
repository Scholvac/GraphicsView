package de.sos.gvc.rt;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

import de.sos.gvc.GraphicsView;

public class JPanelRenderTarget extends JPanel implements IRenderTarget{

	private GraphicsView mView;

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
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);
		mView.doPaint((Graphics2D) g);
	}
}
