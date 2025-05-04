package de.sos.gvc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import de.sos.gvc.rt.IRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;
import de.sos.gvc.storage.ListStorage;

public class TestGraphicsView extends GraphicsView {


	public static TestGraphicsView create(final int width, final int height) {
		final BufferedImageRenderTarget rt = new BufferedImageRenderTarget(width, height, false);
		rt.setClearColor(Color.BLACK);

		final TestGraphicsView view = new TestGraphicsView(rt);
		view.enableRepaintTrigger(false);
		return view;
	}

	private TestGraphicsView(final ImageRenderTarget<?> rt) {
		super(new GraphicsScene(new ListStorage(false)), rt );
	}

	public void addTestItem(final GraphicsItem item) {
		getScene().addItem(item);
	}

	//		@Override
	//		protected ScheduledExecutorService createRepaintScheduler() {
	//			return Executors.newScheduledThreadPool(1);
	//		}

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

	public BufferedImage getBufferedImage(final boolean repaint) {
		if (repaint)
			getRenderTarget().requestRepaint();
		final IRenderTarget img = getRenderTarget();
		if (img instanceof BufferedImageRenderTarget)
			return ((BufferedImageRenderTarget)img).getImage();
		if (img instanceof ImageRenderTarget)
			return toBufferedImage(((ImageRenderTarget)img).getImage());
		throw new IllegalArgumentException("Rendertarget is not an image render target");
	}
	public static BufferedImage toBufferedImage(final Image img) {
		if (img instanceof BufferedImage)
			return (BufferedImage) img;
		final int w = img.getWidth(null);
		final int h = img.getHeight(null);
		final BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = bimg.createGraphics();
		g2d.drawImage(img, 0, 0, null);
		g2d.dispose();
		return bimg;
	}

}