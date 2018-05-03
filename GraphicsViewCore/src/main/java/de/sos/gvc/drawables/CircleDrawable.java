package de.sos.gvc.drawables;

import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;

import de.sos.gvc.IDrawContext;

public class CircleDrawable extends AbstractDrawable {

	private Rectangle2D 	mGeometry;
	private double 			mStartDegree;
	private double 			mEndDegree;
	
	private Arc2D 			mShape = null;

	public CircleDrawable(Rectangle2D geometry, double startDeg, double endDeg) {
		mGeometry = geometry;
		mStartDegree = startDeg;
		mEndDegree = endDeg;
		mShape = null;
	}
	
	public void setGeometry(Rectangle2D rect) { mGeometry = rect; mShape = null;}
	public void setStartDegrees(double start) { mStartDegree = start; mShape = null; }
	public void setEndDegrees(double end) {mEndDegree = end; mShape = null; }
	
	@Override
	public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
		if (mShape == null) {
			mShape = new Arc2D.Double(mGeometry, mStartDegree, mEndDegree, Arc2D.CHORD);
		}
		if (style != null) {
			if (style.hasFillPaint()) {
				style.applyFillPaint(g);
				g.fill(mShape);
			}
			if (style.hasFillPaint()) {
				style.applyLinePaint(g);
				g.draw(mShape);
			}
		}else {
			g.draw(mShape);
		}
	}

}
