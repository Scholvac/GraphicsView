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
	
	
	public DrawableStyle() { this(null); }
	
	public DrawableStyle(String name) {
		this(name, null, null, null);
	}

	public DrawableStyle(String name, Paint linePaint, Stroke lineStroke, Paint fillPaint) {
		this.name = name;
		this.mLinePaint = linePaint;
		this.mLineStroke = lineStroke;
		this.mFillPaint = fillPaint;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public void setFont(Font f) { this.mFont = f; }
	public Font getFont() { return mFont;}
	
	
	
	public Paint getFillPaint() {
		return mFillPaint;
	}
	public void setFillPaint(Paint fillPaint) {
		this.mFillPaint = fillPaint;
	}
	public void setTexture(BufferedImage bimg) {
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
	public void setLineStroke(Stroke lineStroke) {
		this.mLineStroke = lineStroke;
	}

	public Paint getLinePaint() {
		return mLinePaint;
	}
	public void setLinePaint(Paint linePaint) {
		this.mLinePaint = linePaint;
	}






	public boolean hasFillPaint() {
		return getFillPaint() != null || getTexture() != null;
	}
	public boolean hasLinePaint() {
		return getLinePaint() != null;
	}
	
	
	public void applyFillPaint(Graphics2D g, IDrawContext ctx, Shape shape) {
		if (hasTexture()) {
			Rectangle2D b = shape.getBounds2D();
			if (mTexturePaint != null) { //check if we still have the same values
				if (mTexturePaint.getImage() != mTexture)
					mTexturePaint = null;
				final Rectangle2D a = mTexturePaint.getAnchorRect();
				if (a.getX() != b.getX() || a.getY() != b.getY() || a.getWidth() != b.getWidth() || a.getHeight() != -b.getHeight())
					mTexturePaint = null;
			}
			if (mTexturePaint == null) {
				mTexturePaint = new TexturePaint(getTexture(), new Rectangle2D.Double(b.getX(), b.getY(), b.getWidth(), -b.getHeight()));
			}
			g.setPaint(mTexturePaint);
		}else {
			final Paint fillPaint = getFillPaint();
			if (fillPaint != null) {
				g.setPaint(fillPaint);
			}
		}
	}
	public void applyLinePaint(Graphics2D g, IDrawContext ctx, Shape shape) {
		final Stroke lineStroke = getLineStroke();
		if (lineStroke != null) {
			if (lineStroke instanceof ScaledStroke) {
				double scale = ctx.getScale();
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
	
	
	
	
	public Rectangle2D getBounds(Graphics g, String text) {
		if (g == null) return null;
		Font f = getFont(); if (f == null) f = g.getFont();
		FontMetrics fm = g.getFontMetrics(f);
		if (fm == null) return null;
		return fm.getStringBounds(text, g);
	}
	
	public Rectangle2D getBounds(String text) {
		//we have to create a new graphics context
		Canvas c = new Canvas();
		Font f = getFont(); if (f == null) f = c.getFont();
		FontMetrics fm = c.getFontMetrics(f);
		if (fm == null) return null;
		int w = fm.stringWidth(text);
		int h = fm.getHeight();
		return new Rectangle2D.Double(-w / 2.0, -h / 2.0, w, h);
	}

	
}
