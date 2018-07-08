package de.sos.gvc.styles;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

import de.sos.gvc.IDrawContext;

/**
 * 
 * @author scholvac
 *
 */
public class DrawableStyle {
	
	private String 	name = null;
	private Paint	fillPaint = null;
	
	private Stroke	lineStroke = null;
	private Paint	linePaint = null;
	
	private Font	font = null;
	
	public DrawableStyle() { this(null); }
	
	public DrawableStyle(String name) {
		this(name, null, null, null);
	}

	public DrawableStyle(String name, Paint linePaint, Stroke lineStroke, Paint fillPaint) {
		this.name = name;
		this.linePaint = linePaint;
		this.lineStroke = lineStroke;
		this.fillPaint = fillPaint;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public void setFont(Font f) { this.font = f; }
	public Font getFont() { return font;}
	
	
	
	public Paint getFillPaint() {
		return fillPaint;
	}
	public void setFillPaint(Paint fillPaint) {
		this.fillPaint = fillPaint;
	}
	

	
	public Stroke getLineStroke() {
		return lineStroke;
	}
	public void setLineStroke(Stroke lineStroke) {
		this.lineStroke = lineStroke;
	}

	public Paint getLinePaint() {
		return linePaint;
	}
	public void setLinePaint(Paint linePaint) {
		this.linePaint = linePaint;
	}






	public boolean hasFillPaint() {
		return fillPaint != null;
	}
	public boolean hasLinePaint() {
		return linePaint != null;
	}
	public void applyFillPaint(Graphics2D g, IDrawContext ctx) {
		if (fillPaint != null) {
			g.setPaint(fillPaint);
		}
	}
	public void applyLinePaint(Graphics2D g, IDrawContext ctx) {
		if (lineStroke != null) {
			if (lineStroke instanceof ScaledStroke) {
				double scale = ctx.getScale();
//				if (scale > 1)
//					scale = 1. / scale;
				((ScaledStroke) lineStroke).setScale(scale);
			}
			g.setStroke(lineStroke);
		}
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
