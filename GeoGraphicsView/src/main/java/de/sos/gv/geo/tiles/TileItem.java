package de.sos.gv.geo.tiles;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.slf4j.Logger;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.drawables.AbstractDrawable;
import de.sos.gvc.log.GVLog;
import de.sos.gvc.styles.DrawableStyle;

public class TileItem extends GraphicsItem {

	public static enum TileStatus {
		LOADING,
		FINISHED,
		ERROR
	}

	private static final Logger	LOG = GVLog.getLogger(TileItem.class);

	public static class ImageDrawable extends AbstractDrawable {

		BufferedImage 			mImage;
		private Rectangle2D 	mBoundingBox;

		public ImageDrawable(Rectangle2D bb, BufferedImage image) {
			mImage = image;
			mBoundingBox = bb;
		}

		@Override
		public void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx) {
			int x = (int)mBoundingBox.getX();
			int y = (int)mBoundingBox.getY();
			int w = (int)mBoundingBox.getWidth();
			int h = (int)mBoundingBox.getHeight();
			g.drawImage(mImage, x-1, y+1, w+1, h-1, null);
		}

	}


	private final TileInfo				mInfo;
	private final ImageDrawable			mImage;
	private TileStatus					mStatus;

	public TileItem(final TileInfo info, final BufferedImage img) {
		super(info.getShape());
		setZOrder(10.0f);
		setSelectable(false);
		mInfo = info;
		setDrawable(mImage = new ImageDrawable((Rectangle2D) getShape(), img));
		setCenter(info.getXYCenter());
	}
	public synchronized void setImage(final TileStatus status, BufferedImage img) {
		LOG.trace("Set status of tile {} to {} ", mInfo.getHash(), status);
		if (img != mImage.mImage) {
			mImage.mImage = img;
			mStatus = status;
		}else
			mStatus = TileStatus.ERROR;

		markDirty();
	}
	public TileStatus getStatus() { return mStatus;}
	public BufferedImage getImage() { return mImage.mImage; }
	public String getHash() { return mInfo.getHash(); }
	public TileInfo getInfo() {return mInfo;}
}