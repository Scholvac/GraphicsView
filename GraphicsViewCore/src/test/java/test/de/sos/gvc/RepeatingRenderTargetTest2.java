package test.de.sos.gvc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
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
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;
import de.sos.gvc.styles.DrawableStyle;

public class RepeatingRenderTargetTest2 {

	public static void main(final String[] args) throws IOException {

		BufferedImage refImg = null;
		for (int i = 0; i<  500; i++) {
			System.out.println("foo + " +i);
			final GraphicsScene scene = new GraphicsScene();
			final BufferedImageRenderTarget rt = new BufferedImageRenderTarget(800, 800, false);
			rt.setAllowResize(false);
			rt.setClearColor(Color.green);

			final GraphicsView view = new GraphicsView(scene, rt) {
				//				@Override
				//				protected java.util.concurrent.ScheduledExecutorService createRepaintScheduler() {return Executors.newScheduledThreadPool(1); };
			};
			view.enableRepaintTrigger(true);
			view.setCenter(400, -250);
			view.setScale(1.);

			addItem(scene, 50, 50);
			addItem(scene, 400, 50);
			addItem(scene, 600, 50);

			addItem(scene, 50, 250);
			addItem(scene, 400, 250);
			addItem(scene, 600, 250);

			addItem(scene, 50, 550);
			addItem(scene, 400, 550);
			addItem(scene, 600, 550);

			rt.requestRepaint();
			final BufferedImage bimg = rt.getImage();
			ImageIO.write(bimg, "PNG", new File("TestFoo_" + i + ".png"));
			if (refImg == null)
				refImg = bimg;
			else {
				final ImageComparison ic = new ImageComparison(refImg, bimg);
				ic.setAllowingPercentOfDifferentPixels(0);
				ic.setDrawExcludedRectangles(false);
				ic.setPixelToleranceLevel(0);
				final ImageComparisonResult res = ic.compareImages();
				if (res.getImageComparisonState() != ImageComparisonState.MATCH)
					res.writeResultTo(new File("Error_" + i + ".png"));
				assertEquals(ImageComparisonState.MATCH, res.getImageComparisonState());
			}
			ImageIO.write(bimg, "PNG", new File("TestFoo_" + i + ".png"));

		}
	}

	private static void addItem(final GraphicsScene scene, final int i, final int j) {
		final GraphicsItem item = new GraphicsItem(new Rectangle2D.Double(-40, -20, 80, 40));
		item.setStyle(new DrawableStyle(null, null, null, Color.blue));
		scene.addItem(item);
		item.setCenter(i, j);
	}
}
