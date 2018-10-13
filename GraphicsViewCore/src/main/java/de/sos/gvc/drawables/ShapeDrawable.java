package de.sos.gvc.drawables;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

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
				style.applyFillPaint(g, ctx);
				g.fill(shape);
			}
			if (style.hasLinePaint()) {
				style.applyLinePaint(g, ctx);
				if (shape instanceof Rectangle2D || shape instanceof Line2D) {
					g.draw(shape);
				}else {
					float scale = (float)ctx.getScale();
					Stroke ls = style.getLineStroke();
					if (ls == null || scale > 1) 
						ls = new BasicStroke(scale);
					g.setStroke(ls);
					
					if (scale > 1) {
						g.setStroke(new BasicStroke(1));	
						g.draw(shape);
					}else {
						g.draw(shape);
					}
					
					
						
				}
			}
		}
	}
	
}
