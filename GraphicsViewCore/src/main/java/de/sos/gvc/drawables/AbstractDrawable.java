package de.sos.gvc.drawables;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;

import de.sos.gvc.IDrawContext;
import de.sos.gvc.IDrawable;
import de.sos.gvc.styles.DrawableStyle;

/**
 *
 * @author scholvac
 *
 */
public abstract class AbstractDrawable implements IDrawable {
	protected TransformedStroke 			mTransformedStroke = null;

	protected void drawStyled(final Graphics2D g, final Shape shape, final DrawableStyle style, final IDrawContext ctx) {
		if (style == null) {
			g.draw(shape);
		}else {
			if (style.hasFillPaint()) {
				style.applyFillPaint(g, ctx, shape);
				g.fill(shape);
			}
			if (style.hasLinePaint()) {
				style.applyLinePaint(g, ctx, shape);
				if (shape instanceof Line2D) {
					g.draw(shape);
				}else {
					if (style.getLineStroke() != null) {
						if (mTransformedStroke == null) {
							try {
								mTransformedStroke = new TransformedStroke(style.getLineStroke(), g.getTransform());
							} catch (final NoninvertibleTransformException e) {
								e.printStackTrace();
								mTransformedStroke = null;
							}
						}else {
							try {
								mTransformedStroke.set(style.getLineStroke(), g.getTransform());
							} catch (final NoninvertibleTransformException e) {
								e.printStackTrace();
								mTransformedStroke = null;
							}
						}
					}else
						mTransformedStroke = null;

					final Stroke os = g.getStroke();
					final float scale = (float)ctx.getScale();
					Stroke ls = mTransformedStroke;
					if (ls == null)
						if (scale > 1)
							ls = new BasicStroke(1);//scale);
						else
							ls = new BasicStroke(scale);

					g.setStroke(ls);
					g.draw(shape);


					g.setStroke(os);
				}
			}
		}
	}

}
