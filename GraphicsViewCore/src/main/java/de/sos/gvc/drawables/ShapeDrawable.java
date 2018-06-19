package de.sos.gvc.drawables;

import java.awt.Graphics2D;
import java.awt.Shape;

import de.sos.gvc.IDrawContext;
import de.sos.gvc.param.IParameter;
import de.sos.gvc.param.Parameter;
import de.sos.gvc.styles.DrawableStyle;


/**
 * 
 * @author scholvac
 *
 */
public class ShapeDrawable extends AbstractDrawable 
{

	private IParameter<Shape> 			mShape;
	
	public ShapeDrawable(Shape shape) {
		this(new Parameter<>("Shape", "", true, shape));
	}
	public ShapeDrawable(IParameter<Shape> shape) {
		mShape = shape;
	}
	
	@Override
	public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
		Shape shape = mShape.get();
		if (shape == null) return ;
		
		if (style == null) {
			g.draw(shape);
		}else {
			if (style.hasFillPaint()) {
				style.applyFillPaint(g);
				g.fill(shape);
			}
			if (style.hasLinePaint()) {
				style.applyLinePaint(g);
				g.draw(shape);
			}
		}
	}
	
}
