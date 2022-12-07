package de.sos.gvc.drawables;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import de.sos.gvc.IDrawContext;
import de.sos.gvc.styles.DrawableStyle;

public class ImageDrawable extends AbstractDrawable {

	protected BufferedImage 	mImage;
	protected Rectangle2D 		mBoundingBox;
	protected AffineTransform 	mTransform;


	public ImageDrawable(final Rectangle2D bb, final BufferedImage image) {
		mImage = image;
		mBoundingBox = bb;
	}

	public BufferedImage getImage() {return mImage;}
	public Rectangle2D getBoundingBox() {return mBoundingBox;}

	protected AffineTransform getTransform() {
		if (mTransform == null) {
			final double iw = mImage.getWidth();
			final double ih = mImage.getHeight();
			final double iw2 = iw / 2.0;
			final double ih2 = ih / 2.0;

			final double bbw = mBoundingBox.getWidth();
			final double bbh = mBoundingBox.getHeight();

			final double scaleX = bbw / iw;
			final double scaleY = bbh / ih;
			final double offsetX = -iw2 * scaleX;
			final double offsetY = ih2 * scaleY;

			final AffineTransform t = new AffineTransform();
			t.translate(offsetX, offsetY);
			t.scale(scaleX, -scaleY); //-scaleY = take care of the inverted y-axis from GraphicsView
			mTransform = t;
		}
		return mTransform;
	}
	@Override
	public void paintItem(final Graphics2D g, final DrawableStyle style, final IDrawContext ctx) {
		final AffineTransform transform = getTransform();
		final Image image = getImage();

		g.drawImage(image, transform, null);
	}

}