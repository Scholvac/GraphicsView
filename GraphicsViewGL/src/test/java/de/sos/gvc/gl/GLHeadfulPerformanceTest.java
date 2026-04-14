package de.sos.gvc.gl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.log.GVLog;
import de.sos.gvc.storage.ListStorage;
import de.sos.gvc.styles.DrawableStyle;

/**
 * Headful performance benchmark that compares {@link GLRenderTarget} against
 * the default software renderer ({@link de.sos.gvc.rt.JPanelRenderTarget})
 * in a scenario with many moving items — mirroring the MovingExampleGL and
 * MovingItemsExampleWithOSMBackground examples.
 *
 * <p>The test creates a visible JFrame with items that are moved on a
 * background thread (like the examples), counts actual rendered frames
 * during a measurement window, and compares FPS between GL and software.</p>
 *
 * <p>Key design decision: {@code GLRenderTarget.requestRepaint()} uses
 * {@code GLJPanel.repaint()} (Swing-coalesced, asynchronous) instead of
 * {@code GLJPanel.display()} (synchronous, blocking).  This matches the
 * software renderer's behaviour and prevents excessive CPU usage from
 * thousands of dirty-notifications triggering immediate renders.</p>
 */
public class GLHeadfulPerformanceTest {

	private static final Logger LOG = GVLog.getLogger(GLHeadfulPerformanceTest.class);

	private static final int VIEW_SIZE = 800;
	/** Number of moving items in the scene. */
	private static final int ITEM_COUNT = 2000;
	/** Duration of each benchmark run in milliseconds. */
	private static final int BENCHMARK_DURATION_MS = 5000;
	/** Interval between simulation updates (ms) — matches MovingExample. */
	private static final int SIM_INTERVAL_MS = 200;

	private static final long SEED = 42L;

	// -------------------------------------------------------------------
	//  Headful benchmark: GL vs Software with moving items + tiles
	// -------------------------------------------------------------------

	/**
	 * Opens a visible JFrame with moving items for both renderers and
	 * compares frames-per-second.  This replicates the MovingExampleGL vs
	 * MovingItemsExampleWithOSMBackground scenario at smaller scale.
	 *
	 * <p>The GL render target uses a {@code GLCanvas} (heavyweight) which
	 * renders directly into the native framebuffer without readback, giving
	 * lower CPU usage than the software renderer.</p>
	 */
	@Test
	void headfulMovingItems_glNotSlowerThanSoftware() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(),
				"Headful benchmark requires a display");

		// ---- Software benchmark first (no GL dependency) ----
		final BenchmarkResult swResult = runHeadfulBenchmark("Software", null);

		// ---- GL benchmark ----
		final BenchmarkResult glResult;
		try {
			glResult = runHeadfulBenchmark("GL", new GLRenderTarget(VIEW_SIZE, VIEW_SIZE));
		} catch (final Exception e) {
			assumeTrue(false, "GLCanvas not available in this environment: " + e.getMessage());
			return;
		}

		// ---- Report ----
		System.out.println();
		System.out.println("===== Headful Moving Items Benchmark (" + ITEM_COUNT + " items, tiles) =====");
		System.out.printf("Software: %d frames in %.1f s  ->  %.1f FPS%n",
				swResult.frameCount, swResult.elapsedMs / 1000.0, swResult.fps());
		System.out.printf("GL:       %d frames in %.1f s  ->  %.1f FPS%n",
				glResult.frameCount, glResult.elapsedMs / 1000.0, glResult.fps());
		System.out.printf("GL/SW ratio: %.2fx%n", glResult.fps() / swResult.fps());
		System.out.println();

		// GL should not be significantly slower than software in headful mode.
		// With repaint()-based coalescing both should be in the same ballpark.
		final double ratio = glResult.fps() / swResult.fps();
		assertTrue(ratio > 0.5,
				"GL headful FPS should be at least 50% of software FPS. "
						+ "GL: " + String.format("%.1f", glResult.fps())
						+ " FPS, SW: " + String.format("%.1f", swResult.fps())
						+ " FPS, ratio: " + String.format("%.2f", ratio));
	}

	// -------------------------------------------------------------------
	//  Headful benchmark infrastructure
	// -------------------------------------------------------------------

	private BenchmarkResult runHeadfulBenchmark(
			final String label,
			final GLRenderTarget glRT) throws Exception {

		final AtomicInteger frameCounter = new AtomicInteger(0);
		final GraphicsScene scene = new GraphicsScene(new ListStorage());
		final GraphicsView[] viewHolder = new GraphicsView[1];
		final JFrame[] frameHolder = new JFrame[1];

		SwingUtilities.invokeAndWait(() -> {
			// Create frame first so heavyweight GLCanvas can resolve
			// GraphicsConfiguration from its parent container.
			final JFrame frame = new JFrame(label + " Benchmark");
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.setLayout(new BorderLayout());
			frame.setSize(VIEW_SIZE, VIEW_SIZE);
			frame.setLocation(10, 10);

			if (glRT != null) {
				viewHolder[0] = new GraphicsView(scene, glRT);
			} else {
				viewHolder[0] = new GraphicsView(scene);
			}
			final GraphicsView view = viewHolder[0];
			view.setScale(0.5, 0.5);
			view.setMaximumFPS(120);

			// tile handler with deterministic tiles (no network)
			final TileHandler tileHandler = new TileHandler(
					new TileFactory(new GLTileRenderTest.DeterministicTileImageProvider(), 1));
			tileHandler.waitForAllTiles(true);
			view.addHandler(tileHandler);
			GeoUtils.setViewCenter(view, new LatLonPoint(53.5, 8.6));

			frame.add(view.getComponent(), BorderLayout.CENTER);
			frame.setVisible(true);
			frameHolder[0] = frame;
		});

		// add items
		final List<GraphicsItem> items = addItems(scene);

		// let the first paint + tile load settle
		Thread.sleep(500);
		frameCounter.set(0);

		final GraphicsView view = viewHolder[0];

		// simulation thread — moves items periodically
		final CountDownLatch startLatch = new CountDownLatch(1);
		final Thread simThread = new Thread(() -> {
			startLatch.countDown();
			final long deadline = System.currentTimeMillis() + BENCHMARK_DURATION_MS;
			int tick = 0;
			while (System.currentTimeMillis() < deadline) {
				moveItemsOnce(items, tick++);
				try {
					Thread.sleep(SIM_INTERVAL_MS);
				} catch (final InterruptedException e) {
					break;
				}
			}
		}, label + "-sim");
		simThread.setDaemon(true);

		// render thread — forces repaints at max rate and counts them
		final AtomicLong startNanos = new AtomicLong(0);
		final AtomicLong endNanos   = new AtomicLong(0);
		final Thread renderThread = new Thread(() -> {
			try {
				startLatch.await(2, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				return;
			}
			startNanos.set(System.nanoTime());
			final long deadline = System.currentTimeMillis() + BENCHMARK_DURATION_MS;
			while (System.currentTimeMillis() < deadline) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						view.getRenderTarget().requestRepaint();
						frameCounter.incrementAndGet();
					});
				} catch (final Exception e) {
					break;
				}
				// yield to prevent starvation
				Thread.yield();
			}
			endNanos.set(System.nanoTime());
		}, label + "-render");
		renderThread.setDaemon(true);

		simThread.start();
		renderThread.start();
		renderThread.join(BENCHMARK_DURATION_MS + 5000);
		simThread.interrupt();
		simThread.join(1000);

		final long elapsed = endNanos.get() - startNanos.get();
		final int frames = frameCounter.get();

		// cleanup
		SwingUtilities.invokeAndWait(() -> {
			frameHolder[0].setVisible(false);
			frameHolder[0].dispose();
		});

		return new BenchmarkResult(frames, elapsed / 1_000_000.0);
	}

	// -------------------------------------------------------------------
	//  Scene setup and item movement
	// -------------------------------------------------------------------

	private static List<GraphicsItem> addItems(final GraphicsScene scene) {
		final Random rng = new Random(SEED);
		final List<GraphicsItem> items = new ArrayList<>(ITEM_COUNT);

		for (int i = 0; i < ITEM_COUNT; i++) {
			final GraphicsItem item = new GraphicsItem();
			final double w = 30, h = 50;
			final Path2D p = new Path2D.Double();
			p.moveTo(-w / 2, -h / 2);
			p.lineTo(0, h / 2);
			p.lineTo(w / 2, -h / 2);
			p.closePath();
			item.setShape(p);

			final DrawableStyle style = new DrawableStyle();
			style.setFillPaint(new Color(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()));
			style.setLinePaint(Color.BLACK);
			item.setStyle(style);

			final double x = (rng.nextDouble() - 0.5) * VIEW_SIZE * 4;
			final double y = (rng.nextDouble() - 0.5) * VIEW_SIZE * 4;
			item.setCenter(x, y);
			item.setZOrder(rng.nextInt(1000));
			scene.addItem(item);
			items.add(item);
		}
		return items;
	}

	/**
	 * Moves items deterministically — similar to RandomItemSimulator but
	 * without shared Random state issues across threads.
	 */
	private static void moveItemsOnce(final List<GraphicsItem> items, final int tick) {
		final Random rng = new Random(SEED + tick);
		for (final GraphicsItem item : items) {
			if (rng.nextFloat() > 0.63f) continue;
			final double dx = (rng.nextDouble() - 0.5) * 20;
			final double dy = (rng.nextDouble() - 0.5) * 20;
			item.setCenter(item.getCenterX() + dx, item.getCenterY() + dy);
			if (rng.nextFloat() < 0.4f) {
				item.setRotation(item.getRotationDegrees() + (rng.nextFloat() * 100 - 50));
			}
		}
	}

	// -------------------------------------------------------------------

	private static final class BenchmarkResult {
		final int frameCount;
		final double elapsedMs;

		BenchmarkResult(final int frameCount, final double elapsedMs) {
			this.frameCount = frameCount;
			this.elapsedMs = elapsedMs;
		}

		double fps() {
			return elapsedMs > 0 ? frameCount / (elapsedMs / 1000.0) : 0;
		}
	}
}
