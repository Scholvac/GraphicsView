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
					//if the shape contains curves, the default behavior of the BasicStroke will lead the 
					//shape to grow with the line with. (Thats ok, but...)
					//however if the linewith exceeds the size of the shape itself, the line will no longer be
					//connected with its original shape, e.g. the drawn shape is bigger as the orginal and does not have any intersection at all. 
					//to avoid this effect, we create the stroked shape 
					Stroke ls = style.getLineStroke();
					if (ls == null) ls = new BasicStroke((float) (1 * ctx.getScale()));
					g.setStroke(new BasicStroke(1));
					g.draw(new BasicStroke(1).createStrokedShape(shape));
				}
			}
		}
	}
	
}
