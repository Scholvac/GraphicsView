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
		public StaticShapeProvider(final Shape s) { mShape = s; }
		@Override
		public Shape getShape() {
			return mShape;
		}
	}

	private IShapeProvider	 			mShape;

	private TransformedStroke			mTransformedStroke = null; //will be initialized if needed.

	public ShapeDrawable(final Shape shape) {
		this(new StaticShapeProvider(shape));
	}
	public ShapeDrawable(final IShapeProvider shape) {
		mShape = shape;
	}





	@Override
	public void paintItem(final Graphics2D g, final DrawableStyle style, final IDrawContext ctx) {
		final Shape shape = mShape.getShape();
		if (shape == null) return ;

		drawStyled(g, shape, style, ctx);
	}

}
