package test.de.sos.gvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import org.junit.Test;

import de.sos.gvc.GraphicsView.IViewTransformListener;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.ImageCompareUtil;
import de.sos.gvc.TestGraphicsView;
import de.sos.gvc.TestItem;
import de.sos.gvc.TestItem.FillColorStyles;

public class Issue_33_RenderingSilentFailsTest {

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
		assertTrue(view.hadRenderExceptions());
		assertEquals(1, view.getRenderExceptions().size());
		assertTrue(view.getRenderExceptions().get(0) instanceof NullPointerException);
	}



	/**
	 * Assert that view transform listener exceptions are collected and can be queried
	 */
	@Test
	public void test_Collect_ViewTransformListenerExceptions() {
		final TestGraphicsView view = TestGraphicsView.create(200, 200);
		view.addTestItem(TestItem.createStar(0, 0, 50, FillColorStyles.BLUE));
		view.viewAllTestItems(1.1);

		final BufferedImage refImage = view.getBufferedImage(true);
		assertFalse(view.hadRenderExceptions());

		final IViewTransformListener listener = ctx -> {
			throw new NullPointerException("Just an excpetion");
		};
		view.addViewTransformListener(listener);
		final BufferedImage testImage = view.getBufferedImage(true);
		ImageCompareUtil.assertEquals("noSilentFail", refImage, testImage);
		assertTrue(view.hadRenderExceptions());
		assertEquals(1, view.getRenderExceptions().size());
		assertTrue(view.getRenderExceptions().get(0) instanceof NullPointerException);

		//check that the exception list is cleared for the next rendering
		view.removeViewTransformListener(listener);
		final BufferedImage testImage2 = view.getBufferedImage(true);
		assertFalse(view.hadRenderExceptions());
	}
}
