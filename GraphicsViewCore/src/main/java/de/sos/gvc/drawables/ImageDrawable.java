package de.sos.gvc.drawables;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import de.sos.gvc.IDrawContext;
import de.sos.gvc.styles.DrawableStyle;

public class ImageDrawable extends AbstractDrawable {

	private BufferedImage 		mImage;
	private Rectangle2D 		mBoundingBox;

	private double 				mScaleX;
	private double 				mScaleY;
	private double 				mOffsetX;
	private double 				mOffsetY;

	public ImageDrawable(final Rectangle2D bb, final BufferedImage image) {
		mImage = image;
		mBoundingBox = bb;

		final double iw = mImage.getWidth();
		final double ih = mImage.getHeight();
		final double iw2 = iw / 2.0;
		final double ih2 = ih / 2.0;

		final double bbw = mBoundingBox.getWidth();
		final double bbh = mBoundingBox.getHeight();

		mScaleX = bbw / iw;
		mScaleY = bbh / ih;
		mOffsetX = -iw2 * mScaleX;
		mOffsetY = ih * mScaleY;
	}

	public BufferedImage getImage() {return mImage;}
	public Rectangle2D getBoundingBox() {return mBoundingBox;}

	@Override
	public void paintItem(final Graphics2D g, final DrawableStyle style, final IDrawContext ctx) {
		final AffineTransform t = new AffineTransform();
		t.translate(mOffsetX/1.0, mOffsetY/2.0);
		t.scale(mScaleX, -mScaleY);
		g.drawImage(mImage, t, null);
	}

}