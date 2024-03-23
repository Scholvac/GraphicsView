package de.sos.gvc.styles;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import de.sos.gvc.IDrawContext;

/**
 *
 * @author scholvac
 *
 */
public class DrawableStyle {

	private String 			name = null;
	private Paint			mFillPaint = null;

	private Stroke			mLineStroke = null;
	private Paint			mLinePaint = null;

	private Font			mFont = null;

	private BufferedImage 	mTexture;
	private TexturePaint	mTexturePaint;
	private Rectangle2D.Double 	mTextureAnchorRect;


	public DrawableStyle() { this(null); }

	public DrawableStyle(final String name) {
		this(name, null, null, null);
	}

	public DrawableStyle(final String name, final Paint linePaint, final Stroke lineStroke, final Paint fillPaint) {
		this.name = name;
		this.mLinePaint = linePaint;
		this.mLineStroke = lineStroke;
		this.mFillPaint = fillPaint;
	}

	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}

	public void setFont(final Font f) { this.mFont = f; }
	public Font getFont() { return mFont;}



	public Paint getFillPaint() {
		return mFillPaint;
	}
	public void setFillPaint(final Paint fillPaint) {
		this.mFillPaint = fillPaint;
	}
	public void setTexture(final BufferedImage bimg) {
		mTexture = bimg;
	}
	public BufferedImage getTexture() {
		return mTexture;
	}
	public boolean hasTexture() {
		return mTexture != null;
	}


	public Stroke getLineStroke() {
		return mLineStroke;
	}
	public void setLineStroke(final Stroke lineStroke) {
		this.mLineStroke = lineStroke;
	}

	public Paint getLinePaint() {
		return mLinePaint;
	}
	public void setLinePaint(final Paint linePaint) {
		this.mLinePaint = linePaint;
	}

	public boolean hasFillPaint() {
		return getFillPaint() != null || getTexture() != null;
	}
	public boolean hasLinePaint() {
		return getLinePaint() != null;
	}

	protected TexturePaint getTexturePaint(final Shape shape) {
		if (mTexturePaint != null) { //check if we still have the same values
			if (mTexturePaint.getImage() != mTexture)
				mTexturePaint = null;
			else {
				final Rectangle2D b = shape.getBounds2D();

				if (mTextureAnchorRect.x != b.getX() || mTextureAnchorRect.y != b.getY() || mTextureAnchorRect.width != b.getWidth() || mTextureAnchorRect.height != -b.getHeight())
					mTexturePaint = null;
			}
		}
		if (mTexturePaint == null) {
			final Rectangle2D b = shape.getBounds2D();
			mTexturePaint = new TexturePaint(getTexture(), new Rectangle2D.Double(b.getX(), b.getY(), b.getWidth(), -b.getHeight()));
			mTextureAnchorRect = (Rectangle2D.Double)mTexturePaint.getAnchorRect();
		}
		return mTexturePaint;
	}

	public void applyFillPaint(final Graphics2D g, final IDrawContext ctx, final Shape shape) {
		if (hasTexture()) {
			final TexturePaint tp = getTexturePaint(shape);
			g.setPaint(tp);
		}else {
			final Paint fillPaint = getFillPaint();
			if (fillPaint != null) {
				g.setPaint(fillPaint);
			}
		}
	}
	public void applyLinePaint(final Graphics2D g, final IDrawContext ctx, final Shape shape) {
		final Stroke lineStroke = getLineStroke();
		if (lineStroke != null) {
			if (lineStroke instanceof ScaledStroke) {
				final double scale = ctx.getScale();
				//				if (scale > 1)
				//					scale = 1. / scale;
				((ScaledStroke) lineStroke).setScale(scale);
			}
			g.setStroke(lineStroke);
		}
		final Paint linePaint = getLinePaint();
		if (linePaint != null) {
			g.setPaint(linePaint);
		}
	}




	public Rectangle2D getBounds(final Graphics g, final String text) {
		if (g == null) return null;
		Font f = getFont(); if (f == null) f = g.getFont();
		final FontMetrics fm = g.getFontMetrics(f);
		if (fm == null) return null;
		return fm.getStringBounds(text, g);
	}

	public Rectangle2D getBounds(final String text) {
		//we have to create a new graphics context
		final Canvas c = new Canvas();
		Font f = getFont(); if (f == null) f = c.getFont();
		final FontMetrics fm = c.getFontMetrics(f);
		if (fm == null) return null;
		final int w = fm.stringWidth(text);
		final int h = fm.getHeight();
		return new Rectangle2D.Double(-w / 2.0, -h / 2.0, w, h);
	}


}
