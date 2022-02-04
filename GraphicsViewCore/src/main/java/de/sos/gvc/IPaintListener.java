package de.sos.gvc;

import java.awt.Graphics2D;

/**
 *
 * @author scholvac
 *
 */
public interface IPaintListener {

	public void prePaint(Graphics2D graphics, IDrawContext context);

	public void postPaint(Graphics2D graphics, IDrawContext context);
}
