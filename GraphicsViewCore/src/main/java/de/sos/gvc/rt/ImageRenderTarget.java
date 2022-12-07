package de.sos.gvc.rt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.RenderManager;

public abstract class ImageRenderTarget<ImageType extends Image> implements IRenderTarget {

	/** Simple JPanel, that 'just' renders the image of the rendertarget.
	 *
	 * @author scholvac
	 *
	 */
	private class ImagePanel extends JPanel {
		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);
			synchronized (mSyncObject) {
				final Image img = getImage();
				if (img != null)
					g.drawImage(img, 0, 0, null);
			}
		}
	}

	public interface IImageRenderListener {
		public void imageCreated(final Image renderTarget);
		public void preRender(final Image renderTarget);
		public void postRender(final Image renderTarget);
	}
	public static abstract class ImageRenderAdapter implements IImageRenderListener{
		@Override
		public void imageCreated(final Image renderTarget) { }
		@Override
		public void preRender(final Image renderTarget) { }
		@Override
		public void postRender(final Image renderTarget) { }
	}

	private final Boolean		mSyncObject = true;//object used to synchronize the image panel and the draw method
	private GraphicsView		mView;
	private int 				mImageType;
	private int 				mWidth = 1;
	private int					mHeight = 1;
	private Rectangle			mRectangle;
	private boolean				mAllowResize	= true;
	/** If not null, the image is cleared with this color, during pre-paint event */
	private Color				mClearColor 	= null;

	private List<IImageRenderListener> mRenderListener = null;

	private  ImageType			mImage;
	protected Component 		mComponent;

	private ComponentAdapter 	mResizeListener = new ComponentAdapter() {
		@Override
		public void componentResized(final ComponentEvent e) {
			final Dimension size = e.getComponent().getSize();
			resetImage(e.getComponent().getWidth(), e.getComponent().getHeight());
			mView.triggerRepaint();
		}
	};


	protected ImageRenderTarget(final int width, final int height, final int type, final boolean enableComponent) {
		mImageType = type;
		mWidth = width;
		mHeight = height;
		if (enableComponent) {
			mComponent = new ImagePanel();
			setAllowResize(mAllowResize);
		}
	}

	//	public GraphicsView getView() { return mView;}

	protected abstract ImageType createNewImage(final int width, final int height, final int type);

	public void setAllowResize(final boolean allow) {
		if (allow && mComponent != null) {
			mComponent.addComponentListener(mResizeListener);
		}else if (!allow && mComponent != null)
			mComponent.removeComponentListener(mResizeListener);
	}
	public void setClearColor(final Color color) {
		mClearColor = color;
	}

	@Override
	public void setGraphicsView(final GraphicsView view) {
		mView = view;
	}

	/** Adds a new listener that gets notified if an image is going to be renderered and after it has been rendered */
	public void addRenderListener(final IImageRenderListener listener) {
		if (listener != null){
			if (mRenderListener == null)mRenderListener = new ArrayList<>();
			mRenderListener.add(listener);
		}
	}
	public void removeRenderListener(final IImageRenderListener listener) {
		if (listener != null){
			if (mRenderListener != null && mRenderListener.remove(listener)) {
				if (mRenderListener.isEmpty())
					mRenderListener = null;
			}
		}
	}

	@Override
	public void proposeRepaint(final RenderManager renderManager) {
		synchronized (mSyncObject) {
			final ImageType imgTarget = getImage();
			final Graphics2D g2d = getGraphics2D(imgTarget);

			if ( mClearColor != null) {
				g2d.setColor(mClearColor);
				g2d.fillRect(0, 0, mWidth, mHeight);
			}

			if (mRenderListener != null) {
				mRenderListener.forEach(it -> it.preRender(imgTarget));
			}

			if (renderManager != null)
				renderManager.doPaint(g2d);
			else
				mView.doPaint(g2d);

			if (mRenderListener != null)
				mRenderListener.forEach(it -> it.postRender(imgTarget));

			g2d.dispose();

			if (mComponent != null)
				mComponent.repaint();
		}

	}

	@Override
	public void requestRepaint() {
		proposeRepaint(null);
	}

	protected Graphics2D getGraphics2D(final Image imgTarget) {
		final Graphics2D g2d = (Graphics2D) imgTarget.getGraphics();
		return g2d;
	}
	public ImageType getImage() {
		if (mImage == null) {
			mRectangle = null;
			if (mView != null)
				mView.resetViewTransform();
			mImage = createNewImage(getWidth(), getHeight(), getImageType());
			if (mRenderListener != null)
				mRenderListener.forEach(li -> li.imageCreated(mImage));
		}
		return mImage;
	}
	protected void resetImage(final int width, final int height) {
		mWidth = width; mHeight = height;
		mImage = null;
	}

	@Override
	public Rectangle getVisibleRect() {
		if (mRectangle == null)
			mRectangle = new Rectangle(0, 0, getWidth(), getHeight());
		return mRectangle;
	}

	@Override
	public int getWidth() {
		return mWidth;
	}

	@Override
	public int getHeight() {
		return mHeight;
	}

	public int getImageType() {
		return mImageType;
	}

	@Override
	public Component getComponent() {
		return mComponent;
	}

	protected GraphicsConfiguration getGraphicsConfiguration() {
		final Component comp = getComponent();
		if (comp != null) {
			final GraphicsConfiguration gc = comp.getGraphicsConfiguration();
			if (gc != null)
				return gc;
		}
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
	}

	public void clear(final Color clearColor) {
		final Graphics2D g2d = getGraphics2D(getImage());
		g2d.setColor(clearColor);
		g2d.fillRect(0, 0, getWidth(), getHeight());
	}


	public static class BufferedImageRenderTarget extends ImageRenderTarget<BufferedImage>{
		public BufferedImageRenderTarget(final boolean enableComponent) {
			this(1, 1, enableComponent);
		}
		public BufferedImageRenderTarget(final int width, final int height, final boolean enableComponent) {
			this(width, height, -1, enableComponent);
		}
		public BufferedImageRenderTarget(final int width, final int height, final int type, final boolean enableComponent) {
			super(width, height, type, enableComponent);
		}

		@Override
		protected BufferedImage createNewImage(final int width, final int height, final int type) {
			final BufferedImage img = type > 0 ? new BufferedImage(width, height, type) : new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			img.setAccelerationPriority(1);
			return img;
		}

	}

	public static class VolatileImageRenderTarget extends ImageRenderTarget<VolatileImage>{
		public VolatileImageRenderTarget(final boolean enableComponent) {
			this(1, 1, enableComponent);
		}
		public VolatileImageRenderTarget(final int width, final int height, final boolean enableComponent) {
			super(width, height, -1, enableComponent);
		}

		@Override
		protected VolatileImage createNewImage(final int width, final int height, final int type) {
			final VolatileImage img = type > 0 ? getGraphicsConfiguration().createCompatibleVolatileImage(width, height, type) : getGraphicsConfiguration().createCompatibleVolatileImage(width, height);
			img.setAccelerationPriority(1);
			return img;
		}
		@Override
		public VolatileImage getImage() {
			final VolatileImage img = super.getImage();
			if (img.validate(getGraphicsConfiguration()) == VolatileImage.IMAGE_INCOMPATIBLE) {
				resetImage(getWidth(), getHeight());
				return super.getImage();
			}
			return img;
		}
		@Override
		protected Graphics2D getGraphics2D(final Image imgTarget) {
			// TODO Auto-generated method stub
			return super.getGraphics2D(imgTarget);
		}
	}
}
