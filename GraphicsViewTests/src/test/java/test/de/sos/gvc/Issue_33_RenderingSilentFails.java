package test.de.sos.gvc;

import java.awt.image.BufferedImage;

import org.junit.Test;

import de.sos.gvc.ImageCompareUtil;
import de.sos.gvc.TestGraphicsView;
import de.sos.gvc.TestItem;
import de.sos.gvc.TestItem.FillColorStyles;

public class Issue_33_RenderingSilentFails {

	/**
	 * Assert that a view transform listener do not break the whole rendering process
	 */
	@Test
	public void test_NoFatalErrorWith_ViewTransformListener() {
		final TestGraphicsView view = TestGraphicsView.create(200, 200);
		view.addTestItem(TestItem.createStar(0, 0, 50, FillColorStyles.BLUE));
		view.viewAllTestItems(1.1);

		final BufferedImage refImage = view.getBufferedImage(true);

		view.addViewTransformListener(ctx -> {
			throw new NullPointerException("Just an excpetion");
		});

		final BufferedImage testImage = view.getBufferedImage(true);
		ImageCompareUtil.assertEquals("noSilentFail", refImage, testImage);
	}
}
