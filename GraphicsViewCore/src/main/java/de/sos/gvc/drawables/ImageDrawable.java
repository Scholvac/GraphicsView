package de.sos.gvc.drawables;

import java.awt.Color;
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
		
		public ImageDrawable(Rectangle2D bb, BufferedImage image) {
			mImage = image;
			mBoundingBox = bb;
			
			double iw = mImage.getWidth();
			double ih = mImage.getHeight();
			double iw2 = iw / 2.0;
			double ih2 = ih / 2.0;
			
			double bbw = mBoundingBox.getWidth();
			double bbh = mBoundingBox.getHeight();
			
			mScaleX = bbw / iw;
			mScaleY = bbh / ih;
			mOffsetX = -iw2 * mScaleX;
			mOffsetY = ih * mScaleY;
		}
		
		public BufferedImage getImage() {return mImage;}
		public Rectangle2D getBoundingBox() {return mBoundingBox;}
	
		@Override
		public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {					
			AffineTransform t = new AffineTransform();
			t.translate(mOffsetX/1.0, mOffsetY/2.0);
			t.scale(mScaleX, -mScaleY);
			g.drawImage(mImage, t, null);
		}
		
	}