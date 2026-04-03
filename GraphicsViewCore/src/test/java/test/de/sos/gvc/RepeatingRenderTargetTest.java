package test.de.sos.gvc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.rt.IRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;
import de.sos.gvc.storage.ListStorage;
import de.sos.gvc.styles.DrawableStyle;

public class RepeatingRenderTargetTest {

	public static void main(final String[] args) throws IOException {

		//		final ImageComparison ic = new ImageComparison("b_expected_0.png", "c_actual_0.png");
		//		ic.setAllowingPercentOfDifferentPixels(0);
		//		ic.setDrawExcludedRectangles(false);
		//
		//		ic.setPixelToleranceLevel(0);
		//		final ImageComparisonResult res = ic.compareImages();
		//		assertEquals(ImageComparisonState.MISMATCH, res.getImageComparisonState());

		run(1000);
	}
	static void run(final int c) throws IOException {
		final BufferedImage refImg = createView().getBufferedImage(true);
		for (int i = 0; i < c; i++) {
			System.out.println("Compare: " + i);
			final BufferedImage bimg = createView().getBufferedImage(true);
			final ImageComparison ic = new ImageComparison(refImg, bimg);
			ic.setAllowingPercentOfDifferentPixels(0);
			ic.setDrawExcludedRectangles(false);

			ic.setPixelToleranceLevel(0);
			final ImageComparisonResult res = ic.compareImages();
			if (res.getImageComparisonState() != ImageComparisonState.MATCH) {
				res.writeResultTo(new File("a_error_" + i + ".png"));
				ImageIO.write(res.getActual(), "PNG", new File("c_actual_" + i + ".png"));
				ImageIO.write(res.getExpected(), "PNG", new File("b_expected_" + i + ".png"));
			}
			assertEquals(ImageComparisonState.MATCH, res.getImageComparisonState());

		}
	}


	static TestGraphicsView createView() {
		final TestGraphicsView view = TestGraphicsView.create(800, 600);

		int x = -350;
		int y = -350;
		final DrawableStyle testStyle = new DrawableStyle(null, null,null, Color.BLUE);
		for (int i = 0; i < 9; i++) {
			final GraphicsItem item = new GraphicsItem(new Rectangle2D.Double(-40, -20, 80, 40));
			item.setStyle(testStyle);
			item.setCenter(x, y);
			x += 350 - 25;
			if (x > 400){
				y += 350 - 25;
				x = -350;
			}
			view.addTestItem(item);
		}
		view.setScale(1.4);
		return view;
	}

	public static class TestGraphicsView extends GraphicsView {


		public static TestGraphicsView create(final int width, final int height) {
			final BufferedImageRenderTarget rt = new BufferedImageRenderTarget(width, height, false);
			rt.setClearColor(Color.GREEN);

			final TestGraphicsView view = new TestGraphicsView(rt);
			view.enableRepaintTrigger(true);
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
}
