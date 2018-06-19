package de.sos.gvc.gt.tiles;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.drawables.AbstractDrawable;
import de.sos.gvc.gt.proj.MercatorMeterGCT;
import de.sos.gvc.styles.DrawableStyle;

/**
 * 
 * @author scholvac
 *
 */
public class LazyTileItem<DESC extends ITileDescription> extends GraphicsItem 
{
	
	public static Random rng = new Random(System.currentTimeMillis());

	private DESC 				mDescription;
	private BufferedImage		mImage = null;
		
	public static class ImageDrawable extends AbstractDrawable {

		private BufferedImage 		mImage;
		private Rectangle2D 		mBoundingBox;

		public ImageDrawable(Rectangle2D bb, BufferedImage image) {
			mImage = image;
			mBoundingBox = bb;
//			color = new Color(rng.nextInt(255), rng.nextInt(255), rng.nextInt(255));
//			if (image != null) {
//				Graphics gi = mImage.getGraphics();
//				gi.setColor(Color.GREEN);
//				int h = image.getHeight();
//				int w = image.getWidth();
//				gi.drawLine(0, h/2, w, h/2);
//				gi.drawLine(w/2, 0, w/2, h);
//				gi.drawRect(1, 1, image.getWidth()-1, image.getHeight()-1);
//				gi.setColor(Color.RED);
//				gi.fillRect(w/2-5, h/2-5, 10, 10);
//			}			
		}
	
		@Override
		public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
			int x = (int)mBoundingBox.getX();
			int y = (int)mBoundingBox.getY();
			int w = (int)mBoundingBox.getWidth();
			int h = (int)-mBoundingBox.getHeight()-(int)(2*ctx.getScale());
			g.drawImage(mImage, x, (y-h), w+(int)(2*ctx.getScale()), h, null);
		}
		
	}
	

	public LazyTileItem(DESC tile, BufferedImage bimg) {
		super(createShape(tile));
		setZOrder(10.0f);
		setSelectable(false);
		mDescription = tile;
		mImage = bimg;
		setDrawable(new ImageDrawable((Rectangle2D) getShape(), bimg));
	}

	private static MercatorMeterGCT sMercator = new MercatorMeterGCT();
	private static Shape createShape(ITileDescription tile) {
		LatLonBoundingBox llbb = tile.getBounds();
		Point2D ll = sMercator.forward(llbb.getLowerLeft());
		Point2D ur = sMercator.forward(llbb.getUpperRight());
		double x = ll.getX(), y = ll.getY();
		double w = ur.getX() - ll.getX(), h = ur.getY() - ll.getY();
		return new Rectangle2D.Double(x, y, w, h);
	}

	public DESC getDescription() {
		return mDescription;
	}

	public void setImage(BufferedImage img) {
		synchronized (mImage) {
			mImage = img;
			setDrawable(new ImageDrawable((Rectangle2D) getShape(), img)); //this marks the item as dirty -> repaint
		}		
	}
	public BufferedImage getImage() { return mImage; }
	@Override
	public Shape getShape() {
		return super.getShape();
	}

}
