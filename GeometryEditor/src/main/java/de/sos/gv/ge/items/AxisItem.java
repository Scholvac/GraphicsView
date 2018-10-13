package de.sos.gv.ge.items;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.styles.DrawableStyle;

public class AxisItem extends GraphicsItem 
{
	
	
	private static Shape getArrowShape() {
		Path2D p = new GeneralPath();
		p.moveTo(0, -30);
		p.lineTo(0, 27.0);
		p.lineTo(-03, 27.0);
		p.lineTo(0, 32);
		p.lineTo(03, 27);
		p.lineTo(0, 27);
		p.closePath();
		return p;
	}
	
	public AxisItem() {
		super(new Rectangle2D.Double(-1, -1, 2, 2));
		setDrawable(null);
		setSelectable(false);
		setZOrder(5000);
		
		GraphicsItem northArrow = new GraphicsItem(getArrowShape());
		DrawableStyle northStyle = new DrawableStyle("NorthStyle", Color.GREEN, new BasicStroke(1.9f), Color.GREEN);
		northArrow.setStyle(northStyle);
		
		GraphicsItem eastArrow = new GraphicsItem(getArrowShape());
		eastArrow.setRotation(90);
		DrawableStyle eastStyle = new DrawableStyle("EastStyle", Color.RED, new BasicStroke(1.9f), Color.RED);
		eastArrow.setStyle(eastStyle);
		
		addItem(northArrow);
		addItem(eastArrow);
	}
	
	@Override
	public void draw(Graphics2D g, IDrawContext ctx) {
		setScale(ctx.getScale());
		super.draw(g, ctx);
	}

	
}
