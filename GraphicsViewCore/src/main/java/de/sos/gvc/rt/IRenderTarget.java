package de.sos.gvc.rt;

import java.awt.Component;
import java.awt.Rectangle;

import de.sos.gvc.GraphicsView;

public interface IRenderTarget {

	/** Creates a fix (as in non changeable) 1:1 link between render target and graphics view.
	 *
	 * This method is called directly after the RenderTarget has been assigned to
	 * the GraphicsView and before all other methods.
	 *
	 * @param view
	 */
	void setGraphicsView(final GraphicsView view);
	/** The GraphicsView or the underlaying scene is requesting a repaint.
	 *
	 * However it's up to the RenderTarget to deceide if a repaint is required
	 * or not. E.g. if the component is not visible a repaint may be skipped
	 * for performance reasons.
	 */
	void requestRepaint();

	Rectangle getVisibleRect();
	int getWidth();
	int getHeight();
	Component getComponent();
}