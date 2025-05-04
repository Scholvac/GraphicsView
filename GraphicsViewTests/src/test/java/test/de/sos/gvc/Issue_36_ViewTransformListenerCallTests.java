package test.de.sos.gvc;

import static org.junit.Assert.assertEquals;

import java.util.function.Consumer;

import org.junit.Test;

import de.sos.gvc.IDrawContext;
import de.sos.gvc.TestGraphicsView;
import de.sos.gvc.TestItem;
import de.sos.gvc.TestItem.FillColorStyles;
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;

/**
 * Check that the ViewTransformListener is only called once if the view transform change and not every frame
 */
public class Issue_36_ViewTransformListenerCallTests {

	@Test
	public void test_Issue_36_calledOnlyOnce() {
		final TestGraphicsView view = TestGraphicsView.create(200, 200);
		view.addTestItem(TestItem.createStar(0, 0, 50, FillColorStyles.BLUE));
		view.viewAllTestItems(1.1);
		final int[] counter = {0};
		final Consumer<IDrawContext> listener = ctx -> {
			counter[0]++;
		};
		view.addViewTransformListener(listener);

		view.getBufferedImage(true);
		assertEquals(1, counter[0]);
		view.getBufferedImage(true); //view did not change
		assertEquals(1, counter[0]);
	}

	@Test
	public void test_Issue_36_calledAfterLocChange() {
		final TestGraphicsView view = TestGraphicsView.create(200, 200);
		view.addTestItem(TestItem.createStar(0, 0, 50, FillColorStyles.BLUE));
		view.viewAllTestItems(1.1);
		final int[] counter = {0};
		final Consumer<IDrawContext> listener = ctx -> {
			counter[0]++;
		};
		view.addViewTransformListener(listener);

		view.getBufferedImage(true);
		assertEquals(1, counter[0]);
		view.setCenter(1, 1); // The change
		view.getBufferedImage(true);
		assertEquals(2, counter[0]);
	}

	@Test
	public void test_Issue_36_calledAfterScaleChange() {
		final TestGraphicsView view = TestGraphicsView.create(200, 200);
		view.addTestItem(TestItem.createStar(0, 0, 50, FillColorStyles.BLUE));
		view.viewAllTestItems(1.1);
		final int[] counter = {0};
		final Consumer<IDrawContext> listener = ctx -> {
			counter[0]++;
		};
		view.addViewTransformListener(listener);

		view.getBufferedImage(true);
		assertEquals(1, counter[0]);
		view.setScale(2); // The change
		view.getBufferedImage(true);
		assertEquals(2, counter[0]);
	}

	@Test
	public void test_Issue_36_calledAfterRotationChange() {
		final TestGraphicsView view = TestGraphicsView.create(200, 200);
		view.addTestItem(TestItem.createStar(0, 0, 50, FillColorStyles.BLUE));
		view.viewAllTestItems(1.1);
		final int[] counter = {0};
		final Consumer<IDrawContext> listener = ctx -> {
			counter[0]++;
		};
		view.addViewTransformListener(listener);

		view.getBufferedImage(true);
		assertEquals(1, counter[0]);
		view.setRotation(2); // The change
		view.getBufferedImage(true);
		assertEquals(2, counter[0]);
	}

	@Test
	public void test_Issue_36_calledAfterSizeChange() {
		final TestGraphicsView view = TestGraphicsView.create(200, 200);
		view.addTestItem(TestItem.createStar(0, 0, 50, FillColorStyles.BLUE));
		view.viewAllTestItems(1.1);
		final int[] counter = {0};
		final Consumer<IDrawContext> listener = ctx -> {
			counter[0]++;
		};
		view.addViewTransformListener(listener);

		view.getBufferedImage(true);
		assertEquals(1, counter[0]);

		final BufferedImageRenderTarget rt = (BufferedImageRenderTarget) view.getRenderTarget();
		rt.resetImage(300, 300); // the change
		view.getBufferedImage(true);
		assertEquals(2, counter[0]);
	}
}
