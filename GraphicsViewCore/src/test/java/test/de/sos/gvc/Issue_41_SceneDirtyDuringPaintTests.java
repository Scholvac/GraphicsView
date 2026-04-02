package test.de.sos.gvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.rt.IRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;

/**
 * Regression tests for issue 41.
 *
 * The original bug was that {@link GraphicsView} could lose updates when the
 * scene changed while a paint pass was already in progress. The first paint
 * had already taken its item snapshot, so the new item was not rendered in
 * that frame. At the same time, the dirty/clean bookkeeping could clear the
 * scene again without guaranteeing a follow-up repaint.
 *
 * These tests intentionally mutate the scene from inside {@link GraphicsItem#draw(Graphics2D, IDrawContext)}
 * because that is the critical timing window: the current frame cannot include
 * the new item anymore, so the only correct outcomes are:
 * 1) the scene stays dirty if repaint triggering is disabled, or
 * 2) a follow-up repaint is scheduled automatically if repaint triggering is enabled.
 */
public class Issue_41_SceneDirtyDuringPaintTests {

	/**
	 * Used to verify that the item added during the first paint is actually drawn
	 * in the follow-up repaint.
	 */
	private static final class RecordingItem extends GraphicsItem {
		private final AtomicInteger mDrawCount = new AtomicInteger();

		RecordingItem() {
			super(new Rectangle2D.Double(-5, -5, 10, 10));
		}

		@Override
		public void draw(final Graphics2D g, final IDrawContext ctx) {
			super.draw(g, ctx);
			mDrawCount.incrementAndGet();
		}

		public int getDrawCount() {
			return mDrawCount.get();
		}
	}

	/**
	 * Adds a new item to the scene during its first draw call. This simulates the
	 * problematic race from issue 41: the current paint has already collected the
	 * visible items, so the newly added item can only appear in a later repaint.
	 */
	private static final class MutatingItem extends GraphicsItem {
		private final GraphicsScene mScene;
		private final RecordingItem mAddedItem = new RecordingItem();
		private volatile boolean mAdded;

		MutatingItem(final GraphicsScene scene) {
			super(new Rectangle2D.Double(-10, -10, 20, 20));
			mScene = scene;
		}

		@Override
		public void draw(final Graphics2D g, final IDrawContext ctx) {
			super.draw(g, ctx);
			if (!mAdded) {
				mAdded = true;
				mScene.addItem(mAddedItem);
			}
		}

		public RecordingItem getAddedItem() {
			return mAddedItem;
		}
	}

	/**
	 * Test render target that immediately executes repaint requests by calling
	 * {@link GraphicsView#doPaint(Graphics2D)} again. This keeps the test
	 * deterministic and lets us assert that a follow-up repaint was actually requested.
	 */
	private static final class AutoPaintRenderTarget implements IRenderTarget {
		private final Rectangle mVisibleRect = new Rectangle(0, 0, 100, 100);
		private final AtomicInteger mRepaintRequests = new AtomicInteger();
		private final CountDownLatch mFollowUpPaint = new CountDownLatch(1);
		private GraphicsView mView;

		@Override
		public void setGraphicsView(final GraphicsView view) {
			mView = view;
		}

		@Override
		public void requestRepaint() {
			mRepaintRequests.incrementAndGet();
			final BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
			mView.doPaint((Graphics2D) img.getGraphics());
			mFollowUpPaint.countDown();
		}

		@Override
		public Rectangle getVisibleRect() {
			return mVisibleRect;
		}

		@Override
		public int getWidth() {
			return mVisibleRect.width;
		}

		@Override
		public int getHeight() {
			return mVisibleRect.height;
		}

		@Override
		public Component getComponent() {
			return null;
		}
	}

	/**
	 * Acceptance criterion:
	 * if the scene changes during an active paint and automatic repaint triggering
	 * is disabled, the scene must remain dirty afterwards so an external caller
	 * can still detect that another paint is required.
	 */
	@Test
	public void sceneStaysDirtyIfChangedDuringPaintWithoutAutoRepaint() {
		final GraphicsScene scene = new GraphicsScene();
		scene.addItem(new GraphicsItem(new Rectangle2D.Double(-20, -20, 40, 40)));
		final MutatingItem mutatingItem = new MutatingItem(scene);
		scene.addItem(mutatingItem);

		final BufferedImageRenderTarget rt = new BufferedImageRenderTarget(100, 100, false);
		final GraphicsView view = new GraphicsView(scene, rt);
		view.enableRepaintTrigger(false);

		final BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		view.doPaint((Graphics2D) img.getGraphics());

		assertTrue(scene.isDirty(), "Scene changes during paint must stay dirty for a follow-up repaint");
		assertEquals(3, scene.getItems().size());
	}

	/**
	 * Acceptance criterion:
	 * if the scene changes during an active paint and repaint triggering is enabled,
	 * GraphicsView must request a follow-up repaint and that repaint must actually
	 * render the item that was added during the first paint pass.
	 */
	@Test
	public void followUpRepaintIsScheduledWhenSceneChangesDuringPaint() throws Exception {
		final GraphicsScene scene = new GraphicsScene();
		scene.addItem(new GraphicsItem(new Rectangle2D.Double(-20, -20, 40, 40)));
		final MutatingItem mutatingItem = new MutatingItem(scene);
		scene.addItem(mutatingItem);

		final AutoPaintRenderTarget rt = new AutoPaintRenderTarget();
		final GraphicsView view = new GraphicsView(scene, rt);
		final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		view.setSchedulerService(scheduler);
		view.setMaximumFPS(1000);

		final BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		view.doPaint((Graphics2D) img.getGraphics());

		assertTrue(rt.mFollowUpPaint.await(1, TimeUnit.SECONDS), "A follow-up repaint must be scheduled automatically");
		assertTrue(rt.mRepaintRequests.get() >= 1, "At least one repaint request is expected");
		assertTrue(mutatingItem.getAddedItem().getDrawCount() >= 1, "The item added during paint must be rendered by the follow-up repaint");
		assertEquals(3, scene.getItems().size());

		scheduler.shutdownNow();
	}
}
