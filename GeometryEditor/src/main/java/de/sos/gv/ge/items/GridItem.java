package de.sos.gv.ge.items;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.IDrawable;
import de.sos.gvc.drawables.TextDrawable;
import de.sos.gvc.param.IParameter;
import de.sos.gvc.param.Parameter;
import de.sos.gvc.styles.DrawableStyle;

public class GridItem extends GraphicsItem {

	IParameter<Integer>		mMinColumns = new Parameter<>("MinColumns", "Minimum columns to be drawn by the grid", true, 5);
	IParameter<Integer>		mMinRows = new Parameter<>("MinRows", "Minimum rows to be drawn by the grid", true, 5);
	
	
	
	class GridDrawable implements IDrawable {
		IParameter<String>	mLabelProperty = new Parameter<>("Scale", "Scale", true, "X=1 Y=1");
		TextDrawable mTextDrawable = new TextDrawable(mLabelProperty);
		@Override
		public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
			Rectangle2D rect = ctx.getView().getVisibleSceneRect();
			double width = rect.getWidth();
			double height = rect.getHeight();
			
			double minColumns = mMinColumns.get();
			double step = width / minColumns;
			int decimalPower = getDezimalPower(step);
			step = Math.pow(10, decimalPower);
			String legend = "X=" + step + "[m] /";
			
			double start = rect.getX();
			start = ((int)(start / step)-1) * step;
			
			double scale = ctx.getScale();
			Stroke stroke = new BasicStroke(1.0f*(float)scale);
			Stroke oldStroke = g.getStroke();
			g.setStroke(stroke);
			Color oldColor = g.getColor();
			g.setColor(Color.LIGHT_GRAY.darker());
			
			for (double x = start; x < rect.getMaxX(); x += step) {
				g.draw(new Line2D.Double(x, rect.getMinY(), x, rect.getMaxY()));
			}
			
			double minRows = mMinRows.get();
			step = height / minRows;
			decimalPower = getDezimalPower(step);
			step = Math.pow(10, decimalPower);
			legend += " Y=" + step + "[m]";
			start = rect.getMinY();
			start = ((int)(start / step)-1) * step;
			
			for (double y = start; y < rect.getMaxY(); y += step)
				g.draw(new Line2D.Double(rect.getMinX(), y, rect.getMaxX(), y));
			
			
			AffineTransform at = g.getTransform();
			g.setTransform(new AffineTransform());
			Font oldFont = g.getFont();
//			g.setFont(oldFont.deriveFont(oldFont.getSize() * (float)scale));
			g.setColor(Color.RED);
			Rectangle vr = g.getClipBounds();
			g.drawString(legend, (float)((vr.getX()+1)), (float)vr.getMaxY()-1);
			
//			mLabelProperty.set(legend);
//			DrawableStyle txtStyle = new DrawableStyle();
//			txtStyle.setLinePaint(Color.GREEN);
////			g.setColor(Color.GREEN);
//			txtStyle.setFont(g.getFont().deriveFont(g.getFont().getSize() * (float)scale));
//			mTextDrawable.paintItem(g, txtStyle, ctx);
			
			g.setTransform(at);
			g.setFont(oldFont);
			g.setColor(oldColor);
			g.setStroke(oldStroke);
			
			
		}
	}
	
	
	public GridItem() {
		super(new Rectangle2D.Double(-10, -10, 20, 20));
		setDrawable(new GridDrawable());		
		setSelectable(false);
	}
	
	
	@Override
	public void draw(Graphics2D g, IDrawContext ctx) {
		setShape(ctx.getView().getVisibleSceneRect()); //we always draw into the whole view
		super.draw(g, ctx);
	}
	
	int getDezimalPower(double val) {
		if (val > 1) {
			int power = 0;
			while(val > 10) { 
				val /= 10;
				power++;
			}
			return power;
		}else {
			int power = 0;
			while(val < 1) {
				val *= 10;
				power --;
			}
			return power;
		}
	}
}
