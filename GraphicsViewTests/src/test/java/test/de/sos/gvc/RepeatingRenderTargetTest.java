package test.de.sos.gvc;

import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.junit.Test;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.ImageCompareUtil;
import de.sos.gvc.TestGraphicsView;
import de.sos.gvc.styles.DrawableStyle;

public class RepeatingRenderTargetTest {

	/**
	 * Create the same scene for 50 times (to keep the time in pipeline low) and compare it to the initial rendering. It shall never differ.
	 */
	@Test
	public void run50() {
		try {
			run(50);
		} catch (final IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	static void run(final int c) throws IOException {
		final BufferedImage refImg = createView().getBufferedImage(true);
		for (int i = 0; i < c; i++) {
			final BufferedImage bimg = createView().getBufferedImage(true);
			ImageCompareUtil.assertEquals("run_" + i, refImg, bimg);
		}
	}

	static TestGraphicsView createView() {
		final TestGraphicsView view = TestGraphicsView.create(200, 200);

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
		view.viewAllTestItems(true, 1.1);
		return view;
	}
}
