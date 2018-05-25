package de.sos.gvc.drawables;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import de.sos.gvc.IDrawContext;

public class ImageDrawable extends AbstractDrawable {

		private BufferedImage 		mImage;
		private Rectangle2D 		mBoundingBox;

		public ImageDrawable(Rectangle2D bb, BufferedImage image) {
			mImage = image;
			mBoundingBox = bb;
		}
		
		public BufferedImage getImage() {return mImage;}
		public Rectangle2D getBoundingBox() {return mBoundingBox;}
	
		@Override
		public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
			int x = (int)mBoundingBox.getX();
			int y = (int)mBoundingBox.getY();
			int w = (int)mBoundingBox.getWidth();
			int h = (int)-mBoundingBox.getHeight();
			g.drawImage(mImage, x, (y-h), w, h, null);
		}
		
	}