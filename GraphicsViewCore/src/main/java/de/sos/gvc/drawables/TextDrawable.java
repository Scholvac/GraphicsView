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
	public TextDrawable(String txt) {
		this(new Parameter<>("Label", "Label", true, txt));
	}
	public TextDrawable(IParameter<String> labelProperty) {
		mLabel = labelProperty;
	}
	@Override
	public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
		
		if (mLabel == null || mLabel.get() == null) return ;
		
		AffineTransform at = g.getTransform();
		g.scale(1, -1);
		FontMetrics fm = null;
		if (style != null && style.getFont() != null) {
			g.setFont(style.getFont());
			fm = g.getFontMetrics(style.getFont());
		}else
			fm = g.getFontMetrics();//use default font and metric or that from the last time
		
		if (style != null && style.hasLinePaint())
			style.applyLinePaint(g, ctx);
		Rectangle2D r = fm.getStringBounds(mLabel.get(), g);
		double x = -r.getWidth() / 2.0;//r.getMinX();
		double y = r.getHeight() / 2.0; //r.getMinY();
		
		
		g.drawString(mLabel.get(), (float)x, (float)y);
		g.setTransform(at);
	}
	public String getText() {
		if (mLabel == null || mLabel.get() == null) return "";
		return mLabel.get();
	}
	
	

}
