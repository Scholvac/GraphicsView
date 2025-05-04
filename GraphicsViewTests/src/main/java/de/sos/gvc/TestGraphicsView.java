package de.sos.gvc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import de.sos.gvc.rt.IRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget.VolatileImageRenderTarget;
import de.sos.gvc.storage.ListStorage;

/**
 * A test implementation of GraphicsView that provides additional functionality for testing and visualization.
 * This class allows creating test views with specific dimensions, adding test items, and saving the rendered output.
 */
public class TestGraphicsView extends GraphicsView {

	/**
	 * Set to store all items, relevant for a test. Used (among others) to fit all items into the view
	 */
	private Set<GraphicsItem> mTestItems = new HashSet<>();

	/**
	 * Creates a new TestGraphicsView with specified dimensions.
	 *
	 * @param width  The width of the view
	 * @param height The height of the view
	 * @return A new TestGraphicsView instance
	 */
	public static TestGraphicsView create(final int width, final int height) {
		final BufferedImageRenderTarget rt = new BufferedImageRenderTarget(width, height, false);
		rt.setClearColor(Color.BLACK);

		final TestGraphicsView view = new TestGraphicsView(rt);
		view.enableRepaintTrigger(false);
		return view;
	}

	/**
	 * Private constructor that initializes the view with a given render target.
	 *
	 * @param rt The render target for this view
	 */
	private TestGraphicsView(final ImageRenderTarget<?> rt) {
		super(new GraphicsScene(new ListStorage(false)), rt );
	}



	/**
	 * Adds a test item to the scene.
	 *
	 * @param item The GraphicsItem to add to the scene
	 */
	public void addTestItem(final GraphicsItem item) {
		if (mTestItems.add(item));
		getScene().addItem(item);
	}

	/**
	 * Marks the view as clean.
	 * This method is made public to allow external access for testing purposes.
	 *
	 * @see GraphicsView#markViewAsClean()
	 */
	@Override
	public void markViewAsClean() {
		super.markViewAsClean();
	}

	/**
	 * Writes the current view to a file.
	 *
	 * @param repaint  If true, forces a repaint before saving
	 * @param name     The filename to save to
	 * @return The File object if successful, null otherwise
	 */
	public File writeToFile(final boolean repaint,final String name) {
		final File f = new File(name);
		try {
			if (ImageIO.write(getBufferedImage(repaint), "PNG", f))
				return f;
			return null;
		}catch(final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the current view as a new {@link BufferedImage}.<br>
	 * Note that regardless of the current render target ( {@link BufferedImageRenderTarget} or {@link VolatileImageRenderTarget})
	 * this method always return a copy of the image, to ensure not to overwrite the image with the next operation.
	 *
	 * @param repaint If true, forces a repaint before returning the image
	 * @return A new BufferedImage instance representation of the view
	 */
	public BufferedImage getBufferedImage(final boolean repaint) {
		if (repaint)
			getRenderTarget().requestRepaint();

		// note: we always return a copy of the image, to ensure that we do not overwrite the image in the next step
		final IRenderTarget img = getRenderTarget();
		if (img instanceof ImageRenderTarget)
			return toBufferedImage(((ImageRenderTarget)img).getImage());
		throw new IllegalArgumentException("Rendertarget is not an image render target");
	}

	/**
	 * Creates a new {@link BufferedImage} and paints the given image into it.
	 *
	 * @param img The Image to copy
	 * @return A new {@link BufferedImage} instance
	 */
	public static BufferedImage toBufferedImage(final Image img) {
		final int w = img.getWidth(null);
		final int h = img.getHeight(null);
		final BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = bimg.createGraphics();
		g2d.drawImage(img, 0, 0, null);
		g2d.dispose();
		return bimg;
	}

	/**
	 * Scale the view to contain all test items
	 * @param scaleXandY If true, scales both axes to maintain aspect ratio, otherwise scales independently
	 */
	public void viewAllTestItems(final boolean scaleXandY) {
		viewAllTestItems(scaleXandY, 1.0);
	}

	/**
	 * Scale the view to contain all test items by maintaining the aspect ratio
	 *
	 * @param scaleFactor Additional scaling factor applied to the calculated scale. A scaleFactor above 1 will show a bigger area, whereas a factor < 1 show less. For backwards compatibility use 1.1.
	 */
	public void viewAllTestItems(final double scaleFactor) {
		viewAllTestItems(true, scaleFactor);
	}

	/**
	 * Scale the view to contain all test items
	 *
	 * @param scaleXandY If true, scales both axes to maintain aspect ratio, otherwise scales independently
	 * @param scaleFactor Additional scaling factor applied to the calculated scale. A scaleFactor above 1 will show a bigger area, whereas a factor < 1 show less. For backwards compatibility use 1.1.
	 */
	public void viewAllTestItems(final boolean scaleXandY, final double scaleFactor) {
		final Rectangle2D r = Utils.getBoundingBox(mTestItems);
		this.setCenterAndZoom(r, scaleXandY, scaleFactor);
	}



}