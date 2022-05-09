package de.sos.gv.gta;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import de.sos.gvc.IDrawContext;
import de.sos.gvc.drawables.AbstractDrawable;
import de.sos.gvc.styles.DrawableStyle;

public class RasterDrawable extends AbstractDrawable {
	private final Rectangle2D 		mRect;
	private final Raster 			mOriginalTile;
	private final int				mX, mY, mW, mH;

	private BufferedImage			mTileImage;
	private ColorModel 				mColorModel;


	public RasterDrawable(final Rectangle2D rect, final Raster tile, final ColorModel cm) {
		mRect = rect; mOriginalTile = tile; mColorModel = cm;
		mX = (int)mRect.getX();
		mY = (int)mRect.getY();
		mW = (int)mRect.getWidth();
		mH = (int)mRect.getHeight();
	}

	@Override
	public void paintItem(final Graphics2D g, final DrawableStyle style, final IDrawContext ctx) {
		g.setColor(Color.red);
		g.fill(mRect);
		g.drawImage(getImage(),  mX-1, -mY+1, mW+1, -mH-1, null);
	}
	public Image getImage() {
		if (mTileImage == null) {
			if (mOriginalTile instanceof WritableRaster) {
				mTileImage = new BufferedImage(mColorModel, (WritableRaster) mOriginalTile, mColorModel.isAlphaPremultiplied(), null);
			}else {
				final WritableRaster wr = mOriginalTile.createCompatibleWritableRaster();
				wr.setRect(mOriginalTile);
				mTileImage = new BufferedImage(mColorModel, wr, mColorModel.isAlphaPremultiplied(), null);
			}
		}
		return mTileImage;
	}

	public ColorModel setColorTable(final ColorModel customColorTable) {
		if (customColorTable == null)
			throw new NullPointerException("ColorModel may not be null");
		final ColorModel old = mColorModel;
		mColorModel = customColorTable;
		mTileImage = null;
		return old;
	}
}