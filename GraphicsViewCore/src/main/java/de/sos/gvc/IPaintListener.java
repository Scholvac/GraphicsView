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

	public static abstract class PaintAdapter implements IPaintListener {
		@Override
		public void prePaint(final Graphics2D graphics, final IDrawContext context) {
			// TODO overwrite method
		}
		@Override
		public void postPaint(final Graphics2D graphics, final IDrawContext context) {
			// TODO overwrite method
		}

	}
}
