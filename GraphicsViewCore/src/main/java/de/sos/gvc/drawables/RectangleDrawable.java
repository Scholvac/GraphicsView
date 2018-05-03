package de.sos.gvc.drawables;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import de.sos.gvc.IDrawContext;
import de.sos.gvc.IDrawable;


/**
 * 
 * @author scholvac
 *
 */
public class RectangleDrawable implements IDrawable {

	private Rectangle2D mRectangle;
	
	public RectangleDrawable(Rectangle2D rect) {
		mRectangle = rect;
	}
	@Override
	public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
		if (style != null) {
			if (style.hasFillPaint()) {
				style.applyFillPaint(g);
				g.fill(mRectangle);
			}
			if (style.hasFillPaint()) {
				style.applyLinePaint(g);
				g.draw(mRectangle);
			}
		}else {
			g.draw(mRectangle);
		}
	}

	
}
