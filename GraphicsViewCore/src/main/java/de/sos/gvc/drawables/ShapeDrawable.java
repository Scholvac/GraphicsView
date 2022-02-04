package de.sos.gvc.drawables;

import java.awt.Graphics2D;
import java.awt.Shape;

import de.sos.gvc.IDrawContext;
import de.sos.gvc.styles.DrawableStyle;


/**
 *
 * @author scholvac
 *
 */
public class ShapeDrawable extends AbstractDrawable
{

	public interface IShapeProvider {
		public Shape getShape();
	}
	static class StaticShapeProvider implements IShapeProvider {
		private final Shape mShape;
		public StaticShapeProvider(Shape s) { mShape = s; }
		@Override
		public Shape getShape() {
			return mShape;
		}
	}

	private IShapeProvider	 			mShape;

	private TransformedStroke			mTransformedStroke = null; //will be initialized if needed.

	public ShapeDrawable(Shape shape) {
		this(new StaticShapeProvider(shape));
	}
	public ShapeDrawable(IShapeProvider shape) {
		mShape = shape;
	}





	@Override
	public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
		Shape shape = mShape.getShape();
		if (shape == null) return ;

		drawStyled(g, shape, style, ctx);

//		if (style == null) {
//			g.draw(shape);
//		}else {
//			if (style.hasFillPaint()) {
//				style.applyFillPaint(g, ctx);
//				g.fill(shape);
//			}
//			if (style.hasLinePaint()) {
//				style.applyLinePaint(g, ctx);
//				if (shape instanceof Line2D) {
//					g.draw(shape);
//				}else {
//
//					if (style.getLineStroke() != null) {
//						if (mTransformedStroke == null) {
//							try {
//								mTransformedStroke = new TransformedStroke(style.getLineStroke(), g.getTransform());
//							} catch (NoninvertibleTransformException e) {
//								e.printStackTrace();
//								mTransformedStroke = null;
//							}
//						}else {
//							try {
//								mTransformedStroke.set(style.getLineStroke(), g.getTransform());
//							} catch (NoninvertibleTransformException e) {
//								e.printStackTrace();
//								mTransformedStroke = null;
//							}
//						}
//					}else
//						mTransformedStroke = null;
//
//
//
//					Stroke os = g.getStroke();
//					float scale = (float)ctx.getScale();
//					Stroke ls = mTransformedStroke;
//					if (ls == null)
//						if (scale > 1)
//							ls = new BasicStroke(1);//scale);
//						else
//							ls = new BasicStroke((float) (scale));
//
//					g.setStroke(ls);
//					g.draw(shape);
//
//
//					g.setStroke(os);
//				}
//			}
//		}
	}

}
