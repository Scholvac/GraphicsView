package de.sos.gvc.gl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.log.GVLog;
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;
import de.sos.gvc.styles.DrawableStyle;

/**
 * Performance benchmark comparing {@link GLRenderTarget} against the
 * software-based {@link BufferedImageRenderTarget}.
 *
 * <p>The GL sprite-based approach caches each item as a texture after the
 * first rasterization.  Subsequent frames only composite textured quads,
 * which is significantly cheaper than re-rasterizing every item in Java2D.
 * </p>
 *
 * <p>For offscreen rendering the GL readback ({@code glReadPixels}) is
 * expensive and eats into the compositing savings.  The benchmark therefore
 * uses {@link GLRenderTarget#renderOffscreen(boolean)} with readback
 * disabled for intermediate frames, matching the behaviour of headful mode
 * where readback never happens.</p>
 */
public class GLPerformanceBenchmarkTest {

	private static final Logger LOG = GVLog.getLogger(GLPerformanceBenchmarkTest.class);

	private static final int VIEW_WIDTH  = 800;
	private static final int VIEW_HEIGHT = 800;

	/** Number of items in the scene. */
	private static final int ITEM_COUNT = 3000;
	/** Number of frames to render for the benchmark. */
	private static final int FRAME_COUNT = 500;
	/** Number of warm-up frames (not counted). */
	private static final int WARMUP_FRAMES = 50;

	/** Random seed for reproducibility. */
	private static final long SEED = 42L;

	private GLRenderTarget mGLTarget;

	@BeforeEach
	void setUp() {
		try {
			mGLTarget = GLRenderTarget.createOffscreen(VIEW_WIDTH, VIEW_HEIGHT);
		} catch (final Exception e) {
			mGLTarget = null;
		}
	}

	@AfterEach
	void tearDown() {
		if (mGLTarget != null) {
			mGLTarget.dispose();
			mGLTarget = null;
		}
	}

	// -------------------------------------------------------------------
	//  Benchmarks
	// -------------------------------------------------------------------

	/**
	 * Compares pure rendering throughput (no GL readback on intermediate
	 * frames).  This mirrors headful mode where the GLJPanel presents
	 * directly without a readback step.
	 */
	@Test
	void glRenderFasterThanSoftware() {
		assumeTrue(mGLTarget != null, "OpenGL context not available");

		// ---- software benchmark ----
		final GraphicsScene swScene = createBenchmarkScene();
		final BufferedImageRenderTarget swRT =
				new BufferedImageRenderTarget(VIEW_WIDTH, VIEW_HEIGHT,
						BufferedImage.TYPE_INT_RGB, false);
		swRT.setClearColor(Color.WHITE);
		final GraphicsView swView = new GraphicsView(swScene, swRT);
		swView.enableRepaintTrigger(false);
		applyRenderHints(swView);
		swView.setScale(0.3, 0.3);

		// warm up
		for (int i = 0; i < WARMUP_FRAMES; i++) {
			swView.setCenter(swView.getCenterX() + 0.1, swView.getCenterY());
			swRT.requestRepaint();
		}

		final long swStart = System.nanoTime();
		for (int i = 0; i < FRAME_COUNT; i++) {
			swView.setCenter(swView.getCenterX() + 0.1, swView.getCenterY());
			swRT.requestRepaint();
		}
		final long swElapsed = System.nanoTime() - swStart;
		final double swMs = swElapsed / 1_000_000.0;

		// ---- GL benchmark (no readback on intermediate frames) ----
		final GraphicsScene glScene = createBenchmarkScene();
		mGLTarget.setClearColor(Color.WHITE);
		final GraphicsView glView = new GraphicsView(glScene, mGLTarget);
		glView.enableRepaintTrigger(false);
		applyRenderHints(glView);
		glView.setScale(0.3, 0.3);

		// warm up — builds sprite cache; first frame includes readback
		mGLTarget.requestRepaint();
		for (int i = 1; i < WARMUP_FRAMES; i++) {
			glView.setCenter(glView.getCenterX() + 0.1, glView.getCenterY());
			mGLTarget.renderOffscreen(false);
		}

		final long glStart = System.nanoTime();
		for (int i = 0; i < FRAME_COUNT; i++) {
			glView.setCenter(glView.getCenterX() + 0.1, glView.getCenterY());
			mGLTarget.renderOffscreen(false);
		}
		final long glElapsed = System.nanoTime() - glStart;
		final double glMs = glElapsed / 1_000_000.0;

		// verify GL actually works (one readback)
		mGLTarget.requestRepaint();
		assertNotNull(mGLTarget.getResultImage(), "GL target produced no image");

		// ---- report ----
		final double swFps = FRAME_COUNT / (swMs / 1000.0);
		final double glFps = FRAME_COUNT / (glMs / 1000.0);
		final double speedup = swMs / glMs;

		System.out.println();
		System.out.println("===== Render Performance (" + ITEM_COUNT + " items, " + FRAME_COUNT + " frames, no readback) =====");
		System.out.printf("Software: %.1f ms total, %.1f FPS%n", swMs, swFps);
		System.out.printf("GL:       %.1f ms total, %.1f FPS%n", glMs, glFps);
		System.out.printf("Speedup:  %.2fx%n", speedup);
		System.out.println();

		assertTrue(speedup > 1.0,
				"GL rendering (without readback) should be faster than software. "
						+ "Software: " + String.format("%.1f", swMs) + " ms, "
						+ "GL: " + String.format("%.1f", glMs) + " ms, "
						+ "Speedup: " + String.format("%.2f", speedup) + "x");
	}

	/**
	 * Verifies that subsequent GL frames (with cached sprites) are
	 * significantly faster than the first frame that must create all sprites.
	 */
	@Test
	void glCachedFramesFasterThanFirstFrame() {
		assumeTrue(mGLTarget != null, "OpenGL context not available");

		final GraphicsScene scene = createBenchmarkScene();
		mGLTarget.setClearColor(Color.WHITE);
		final GraphicsView view = new GraphicsView(scene, mGLTarget);
		view.enableRepaintTrigger(false);
		applyRenderHints(view);
		view.setScale(0.3, 0.3);

		// first frame: sprites need to be created (no readback for fair comparison)
		final long firstStart = System.nanoTime();
		mGLTarget.renderOffscreen(false);
		final long firstElapsed = System.nanoTime() - firstStart;
		final double firstMs = firstElapsed / 1_000_000.0;

		// subsequent frames: sprites cached, only compositing
		final int subsequentFrames = 30;
		final long subStart = System.nanoTime();
		for (int i = 0; i < subsequentFrames; i++) {
			view.setCenter(view.getCenterX() + 0.1, view.getCenterY());
			mGLTarget.renderOffscreen(false);
		}
		final long subElapsed = System.nanoTime() - subStart;
		final double subAvgMs = (subElapsed / 1_000_000.0) / subsequentFrames;

		// verify GL actually works
		mGLTarget.requestRepaint();
		assertNotNull(mGLTarget.getResultImage(), "GL target produced no image");

		System.out.println();
		System.out.println("===== GL Sprite Caching (" + ITEM_COUNT + " items) =====");
		System.out.printf("First frame (sprite creation): %.1f ms%n", firstMs);
		System.out.printf("Subsequent frames (avg of %d):  %.1f ms%n", subsequentFrames, subAvgMs);
		System.out.printf("Cached speedup: %.2fx%n", firstMs / subAvgMs);
		System.out.println();

		assertTrue(subAvgMs < firstMs,
				"Cached GL frames should be faster than the first (sprite-creation) frame. "
						+ "First: " + String.format("%.1f", firstMs) + " ms, "
						+ "Avg subsequent: " + String.format("%.1f", subAvgMs) + " ms");
	}

	// -------------------------------------------------------------------
	//  Scene setup
	// -------------------------------------------------------------------

	private static void applyRenderHints(final GraphicsView view) {
		view.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		view.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		view.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	private static GraphicsScene createBenchmarkScene() {
		final GraphicsScene scene = new GraphicsScene();
		final Random rng = new Random(SEED);

		// complex shapes with many vertices — expensive to rasterize in Java2D
		final String[] shapes = {
				"POLYGON ((0 50, 5 40, 15 30, 10 15, 25 10, 50 0, 25 -10, 10 -15, 15 -30, 5 -40, 0 -50, -5 -40, -15 -30, -10 -15, -25 -10, -50 0, -25 10, -10 15, -15 30, -5 40, 0 50))",
				"POLYGON ((-20 -20, -10 -25, 0 -20, 10 -25, 20 -20, 25 -10, 20 0, 25 10, 20 20, 10 25, 0 20, -10 25, -20 20, -25 10, -20 0, -25 -10, -20 -20))",
				"POLYGON ((0 30, 8 25, 14 18, 18 10, 20 0, 18 -10, 14 -18, 8 -25, 0 -30, -8 -25, -14 -18, -18 -10, -20 0, -18 10, -14 18, -8 25, 0 30))",
				"POLYGON ((-25 0, -20 -15, -10 -22, 0 -25, 10 -22, 20 -15, 25 0, 20 15, 10 22, 0 25, -10 22, -20 15, -25 0))",
				"POLYGON ((-15 -30, 0 -35, 15 -30, 25 -18, 30 0, 25 18, 15 30, 0 35, -15 30, -25 18, -30 0, -25 -18, -15 -30))"
		};

		final Point2D gpStart = new Point2D.Float(-20, -20);
		final Point2D gpEnd   = new Point2D.Float(20, 20);
		final float[] dist    = { 0.0f, 0.5f, 1.0f };

		// keep items inside the visible area at scale 0.3
		// visible scene rect ≈ ±(400/0.3) ≈ ±1333
		final double scatterRange = 1200;

		for (int i = 0; i < ITEM_COUNT; i++) {
			final String wkt = shapes[rng.nextInt(shapes.length)];
			final GraphicsItem item = GraphicsItem.createFromWKT(wkt);
			if (item == null) continue;

			// gradient fill — expensive for Java2D to rasterize per frame
			final Color c1 = randomColor(rng);
			final Color c2 = randomColor(rng);
			final Color c3 = randomColor(rng);
			final LinearGradientPaint gradient = new LinearGradientPaint(
					gpStart, gpEnd, dist, new Color[] { c1, c2, c3 });

			final DrawableStyle style = new DrawableStyle();
			style.setFillPaint(gradient);
			style.setLinePaint(Color.BLACK);
			item.setStyle(style);

			final double x = (rng.nextDouble() - 0.5) * 2 * scatterRange;
			final double y = (rng.nextDouble() - 0.5) * 2 * scatterRange;
			item.setCenter(x, y);
			item.setZOrder(rng.nextInt(1000));
			scene.addItem(item);
		}
		return scene;
	}

	private static Color randomColor(final Random rng) {
		return new Color(64 + rng.nextInt(192), 64 + rng.nextInt(192), 64 + rng.nextInt(192));
	}
}
