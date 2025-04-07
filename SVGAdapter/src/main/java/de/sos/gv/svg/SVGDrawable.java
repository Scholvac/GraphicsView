package de.sos.gv.svg;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import de.sos.gvc.IDrawContext;
import de.sos.gvc.IDrawable;
import de.sos.gvc.styles.DrawableStyle;

public class SVGDrawable implements IDrawable{

	@Override
	public void paintItem(final Graphics2D g, final DrawableStyle style, final IDrawContext ctx) {
		// TODO Auto-generated method stub
		new Shape() {

			@Override
			public boolean intersects(final double x, final double y, final double w, final double h) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean intersects(final Rectangle2D r) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public PathIterator getPathIterator(final AffineTransform at, final double flatness) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public PathIterator getPathIterator(final AffineTransform at) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Rectangle2D getBounds2D() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Rectangle getBounds() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean contains(final double x, final double y, final double w, final double h) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean contains(final double x, final double y) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean contains(final Rectangle2D r) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean contains(final Point2D p) {
				// TODO Auto-generated method stub
				return false;
			}
		};
	}



}