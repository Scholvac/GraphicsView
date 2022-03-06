package de.sos.gvc.drawables;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import de.sos.gvc.IDrawContext;
import de.sos.gvc.IDrawable;
import de.sos.gvc.param.IParameter;
import de.sos.gvc.param.Parameter;
import de.sos.gvc.styles.DrawableStyle;


/**
 *
 * @author scholvac
 *
 */
public class TextDrawable implements IDrawable {

	private IParameter<String> mLabel;


	public TextDrawable() {
		this("");
	}
	public TextDrawable(final String txt) {
		this(new Parameter<>("Label", "Label", true, txt));
	}
	public TextDrawable(final IParameter<String> labelProperty) {
		mLabel = labelProperty;
	}
	@Override
	public void paintItem(final Graphics2D g, final DrawableStyle style, final IDrawContext ctx) {

		final String label = getText();
		if (label == null || label.isEmpty())
			return ;

		final AffineTransform at = g.getTransform();
		g.scale(1, -1);
		FontMetrics fm = null;
		if (style != null && style.getFont() != null) {
			g.setFont(style.getFont());
			fm = g.getFontMetrics(style.getFont());
		}else
			fm = g.getFontMetrics();//use default font and metric or that from the last time

		final Rectangle2D r = fm.getStringBounds(label, g);
		final double x = -r.getWidth() / 2.0;//r.getMinX();
		final double y = r.getHeight() / 2.0; //r.getMinY();

		if (style != null && style.hasLinePaint())
			style.applyLinePaint(g, ctx, r);



		g.drawString(label, (float)x, (float)y);
		g.setTransform(at);
	}
	public String getText() {
		if (mLabel == null || mLabel.get() == null) return "";
		return mLabel.get();
	}



}
