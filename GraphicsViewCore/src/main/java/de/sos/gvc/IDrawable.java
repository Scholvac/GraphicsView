package de.sos.gvc;

import java.awt.Graphics2D;

import de.sos.gvc.styles.DrawableStyle;

/**
 *
 * @author scholvac
 *
 */
public interface IDrawable {

	public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx);
}
