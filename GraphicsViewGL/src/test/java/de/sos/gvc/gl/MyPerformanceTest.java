package de.sos.gvc.gl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Path2D;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.junit.jupiter.api.Test;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.rt.IRenderTarget;
import de.sos.gvc.rt.JPanelRenderTarget;
import de.sos.gvc.storage.ListStorage;
import de.sos.gvc.styles.DrawableStyle;

/**
 * Headful JUnit performance comparison for software and GL render targets.
 */
public class MyPerformanceTest {

	private static final int VIEW_SIZE = 800;
	private static final int BENCHMARK_DURATION_MS = 10000;
	private static final int SETTLE_DURATION_MS = 1200;
	private static final int FINAL_FLUSH_MS = 400;
	private static final int SIMULATION_INTERVAL_MS = 200;
	private static final long SCENE_SEED = 42L;
	//
	//	@Test
	//	void compareJPanelAndGLHeadfulPerformance_10x10() throws Exception {
	//		compareJPanelAndGLHeadfulPerformance(new BenchmarkConfig(10, 10));
	//	}

	@Test
	void compareJPanelAndGLHeadfulPerformance_100x100() throws Exception {
		compareJPanelAndGLHeadfulPerformance(new BenchmarkConfig(200, 200));
	}

	private void compareJPanelAndGLHeadfulPerformance(final BenchmarkConfig config) throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(),
				"Headful benchmark requires a display");

		final BenchmarkResult swResult = runBenchmark(config, "JPanelRenderTarget", JPanelRenderTarget::new);
		final BenchmarkResult glResult;
		try {
			glResult = runBenchmark(config, "GLRenderTarget",
					() -> new GLRenderTarget(VIEW_SIZE, VIEW_SIZE));
		} catch (final Exception e) {
			assumeTrue(false, "GLCanvas not available in this environment: " + e.getMessage());
			return;
		}

		System.out.println();
		System.out.printf("===== Headful Renderer Benchmark (%s, %d items) =====%n",
				config.label(), config.itemCount());
		System.out.printf("JPanelRenderTarget: paints=%d, avg=%.3f ms, window=%.3f ms, cpu=%s%n",
				swResult.paintCount, swResult.averagePaintDurationMs, swResult.movingWindowAverageMs,
				formatCpuLoad(swResult.averageProcessCpuLoadPercent));
		System.out.printf("GLRenderTarget:     paints=%d, avg=%.3f ms, window=%.3f ms, cpu=%s%n",
				glResult.paintCount, glResult.averagePaintDurationMs, glResult.movingWindowAverageMs,
				formatCpuLoad(glResult.averageProcessCpuLoadPercent));
		if (swResult.averagePaintDurationMs > 0.0)
			System.out.printf("GL/JPanel avg ratio: %.2fx%n",
					glResult.averagePaintDurationMs / swResult.averagePaintDurationMs);
		System.out.println();

		assertTrue(swResult.paintCount > 0,
				"JPanelRenderTarget should produce measured paint samples");
		assertTrue(glResult.paintCount > 0,
				"GLRenderTarget should produce measured paint samples");
		assertTrue(swResult.averagePaintDurationMs > 0.0,
				"JPanelRenderTarget should report positive average paint durations");
		assertTrue(glResult.averagePaintDurationMs > 0.0,
				"GLRenderTarget should report positive average paint durations");
		assertTrue(swResult.movingWindowAverageMs > 0.0,
				"JPanelRenderTarget should report positive moving-window durations");
		assertTrue(glResult.movingWindowAverageMs > 0.0,
				"GLRenderTarget should report positive moving-window durations");
		assertFalse(swResult.hadRenderExceptions,
				"JPanelRenderTarget run should not report render exceptions");
		assertFalse(glResult.hadRenderExceptions,
				"GLRenderTarget run should not report render exceptions");
	}

	private BenchmarkResult runBenchmark(
			final BenchmarkConfig config,
			final String label,
			final RenderTargetFactory targetFactory) throws Exception {

		final Random sceneRandom = new Random(SCENE_SEED);
		final GraphicsScene scene = new GraphicsScene(new ListStorage());
		final List<RandomItemSimulator> simulators = addItems(scene, config, sceneRandom);

		final AtomicReference<GraphicsView> viewRef = new AtomicReference<>();
		final AtomicReference<JFrame> frameRef = new AtomicReference<>();
		final AtomicReference<IRenderTarget> targetRef = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> {
			final IRenderTarget renderTarget = targetFactory.create();
			final GraphicsView view = new GraphicsView(scene, renderTarget);
			view.setMaximumFPS(32);
			view.setScale(200);

			final ITileImageProvider cache = ITileFactory.buildCache(ITileImageProvider.OSM, 10, SizeUnit.MegaByte, new File("./.cache"), 100, SizeUnit.MegaByte);
			view.addHandler(new TileHandler(new TileFactory(cache)));

			final JFrame frame = new JFrame(label + " " + config.label());
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.setLayout(new BorderLayout());
			frame.setSize(VIEW_SIZE, VIEW_SIZE);
			frame.add(view.getComponent(), BorderLayout.CENTER);
			frame.setVisible(true);

			targetRef.set(renderTarget);
			viewRef.set(view);
			frameRef.set(frame);
			renderTarget.requestRepaint();
		});

		try {
			Thread.sleep(SETTLE_DURATION_MS);

			final GraphicsView view = viewRef.get();
			final StatsSnapshot startSnapshot = snapshot(view);
			final CpuMeasurementStart cpuMeasurementStart = CpuMeasurementStart.capture();
			final AtomicBoolean running = new AtomicBoolean(true);
			final Thread simulationThread = createSimulationThread(simulators, running);
			simulationThread.start();

			Thread.sleep(BENCHMARK_DURATION_MS);
			running.set(false);
			simulationThread.join(BENCHMARK_DURATION_MS);

			SwingUtilities.invokeAndWait(() -> view.getRenderTarget().requestRepaint());
			Thread.sleep(FINAL_FLUSH_MS);

			final StatsSnapshot endSnapshot = snapshot(view);
			final double averageProcessCpuLoadPercent = cpuMeasurementStart.measureAverageCpuLoadPercent();
			return BenchmarkResult.of(startSnapshot, endSnapshot, averageProcessCpuLoadPercent);
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				final JFrame frame = frameRef.get();
				if (frame != null) {
					frame.setVisible(false);
					frame.dispose();
				}
				final IRenderTarget renderTarget = targetRef.get();
				if (renderTarget instanceof GLRenderTarget)
					((GLRenderTarget) renderTarget).dispose();
			});
		}
	}

	private static Thread createSimulationThread(
			final List<RandomItemSimulator> simulators,
			final AtomicBoolean running) {
		final Thread thread = new Thread(() -> {
			while (running.get()) {
				for (final RandomItemSimulator simulator : simulators)
					simulator.update();
				try {
					Thread.sleep(SIMULATION_INTERVAL_MS);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}, "performance-simulation");
		thread.setDaemon(true);
		return thread;
	}

	private static StatsSnapshot snapshot(final GraphicsView view) throws Exception {
		final AtomicReference<StatsSnapshot> snapshotRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			final DoubleSummaryStatistics stats = view.getPaintDurationStatistic();
			snapshotRef.set(new StatsSnapshot(
					stats.getCount(),
					stats.getSum(),
					view.getMovingWindowDurationStatistic().count(),
					view.getMovingWindowDurationStatistic().avg(),
					view.hadRenderExceptions()));
		});
		return snapshotRef.get();
	}

	private static List<RandomItemSimulator> addItems(
			final GraphicsScene scene,
			final BenchmarkConfig config,
			final Random random) {
		final List<RandomItemSimulator> simulators = new ArrayList<>(config.itemCount());
		final double itemWidth = 60;
		final double itemHeight = 100;
		final double xMargin = 100;
		final double yMargin = 100;

		final double xStep = itemWidth + xMargin / 2.0;
		final double xStart = -0.5 * config.numX * xStep;
		final double yStep = itemHeight + yMargin / 2.0;
		final double yStart = -0.5 * config.numY * yStep;

		for (int ix = 0; ix < config.numX; ix++) {
			final double x = xStart + xStep * ix;
			for (int iy = 0; iy < config.numY; iy++) {
				final double y = yStart + yStep * iy;
				final GraphicsItem item = createItem(itemWidth, itemHeight, random);
				item.setCenter(x, y);
				scene.addItem(item);
				simulators.add(new RandomItemSimulator(item, random.nextLong()));
			}
		}
		return simulators;
	}

	private static GraphicsItem createItem(final double width, final double height, final Random random) {
		final GraphicsItem item = new GraphicsItem();
		final double w2 = width / 2.0;
		final double h2 = height / 2.0;
		final Path2D path = new Path2D.Double();
		path.moveTo(-w2, -h2);
		path.lineTo(0, h2);
		path.lineTo(w2, -h2);
		path.closePath();
		item.setShape(path);

		final DrawableStyle style = new DrawableStyle();
		style.setFillPaint(new Color(random.nextFloat(), random.nextFloat(), random.nextFloat()));
		style.setLinePaint(Color.BLACK);
		item.setStyle(style);
		return item;
	}

	private interface RenderTargetFactory {
		IRenderTarget create();
	}

	private static String formatCpuLoad(final double averageProcessCpuLoadPercent) {
		if (Double.isNaN(averageProcessCpuLoadPercent) || averageProcessCpuLoadPercent < 0.0)
			return "n/a";
		return String.format("%.1f %%", averageProcessCpuLoadPercent);
	}

	private static final class BenchmarkConfig {
		final int numX;
		final int numY;

		BenchmarkConfig(final int numX, final int numY) {
			this.numX = numX;
			this.numY = numY;
		}

		int itemCount() {
			return numX * numY;
		}

		String label() {
			return numX + "x" + numY;
		}

		@Override
		public String toString() {
			return label();
		}
	}

	private static final class BenchmarkResult {
		final long paintCount;
		final double averagePaintDurationMs;
		final double movingWindowAverageMs;
		final double averageProcessCpuLoadPercent;
		final boolean hadRenderExceptions;

		static BenchmarkResult of(
				final StatsSnapshot start,
				final StatsSnapshot end,
				final double averageProcessCpuLoadPercent) {
			final long paintCount = Math.max(0L, end.totalCount - start.totalCount);
			final double durationSumSeconds = Math.max(0.0, end.totalDurationSeconds - start.totalDurationSeconds);
			final double averagePaintDurationMs =
					paintCount > 0 ? (durationSumSeconds / paintCount) * 1000.0 : 0.0;
					return new BenchmarkResult(
							paintCount,
							averagePaintDurationMs,
							end.movingWindowAverageSeconds * 1000.0,
							averageProcessCpuLoadPercent,
							end.hadRenderExceptions);
		}

		BenchmarkResult(
				final long paintCount,
				final double averagePaintDurationMs,
				final double movingWindowAverageMs,
				final double averageProcessCpuLoadPercent,
				final boolean hadRenderExceptions) {
			this.paintCount = paintCount;
			this.averagePaintDurationMs = averagePaintDurationMs;
			this.movingWindowAverageMs = movingWindowAverageMs;
			this.averageProcessCpuLoadPercent = averageProcessCpuLoadPercent;
			this.hadRenderExceptions = hadRenderExceptions;
		}
	}

	private static final class CpuMeasurementStart {
		private final com.sun.management.OperatingSystemMXBean operatingSystemMXBean;
		private final long processCpuTimeNanos;
		private final long wallClockNanos;
		private final int availableProcessors;

		static CpuMeasurementStart capture() {
			final java.lang.management.OperatingSystemMXBean platformBean =
					ManagementFactory.getOperatingSystemMXBean();
			if (!(platformBean instanceof com.sun.management.OperatingSystemMXBean))
				return new CpuMeasurementStart(null, -1L, -1L, Runtime.getRuntime().availableProcessors());
			return new CpuMeasurementStart(
					(com.sun.management.OperatingSystemMXBean) platformBean,
					((com.sun.management.OperatingSystemMXBean) platformBean).getProcessCpuTime(),
					System.nanoTime(),
					Math.max(1, Runtime.getRuntime().availableProcessors()));
		}

		CpuMeasurementStart(
				final com.sun.management.OperatingSystemMXBean operatingSystemMXBean,
				final long processCpuTimeNanos,
				final long wallClockNanos,
				final int availableProcessors) {
			this.operatingSystemMXBean = operatingSystemMXBean;
			this.processCpuTimeNanos = processCpuTimeNanos;
			this.wallClockNanos = wallClockNanos;
			this.availableProcessors = availableProcessors;
		}

		double measureAverageCpuLoadPercent() {
			if (operatingSystemMXBean == null || processCpuTimeNanos < 0L || wallClockNanos < 0L)
				return Double.NaN;

			final long endProcessCpuTimeNanos = operatingSystemMXBean.getProcessCpuTime();
			final long endWallClockNanos = System.nanoTime();
			final long cpuDeltaNanos = endProcessCpuTimeNanos - processCpuTimeNanos;
			final long wallDeltaNanos = endWallClockNanos - wallClockNanos;
			if (cpuDeltaNanos < 0L || wallDeltaNanos <= 0L)
				return Double.NaN;

			return cpuDeltaNanos * 100.0 / (wallDeltaNanos * availableProcessors);
		}
	}

	private static final class StatsSnapshot {
		final long totalCount;
		final double totalDurationSeconds;
		final double movingWindowAverageSeconds;
		final boolean hadRenderExceptions;

		StatsSnapshot(
				final long totalCount,
				final double totalDurationSeconds,
				final int movingWindowCount,
				final double movingWindowAverageSeconds,
				final boolean hadRenderExceptions) {
			this.totalCount = totalCount;
			this.totalDurationSeconds = totalDurationSeconds;
			this.movingWindowAverageSeconds = movingWindowCount > 0 ? movingWindowAverageSeconds : 0.0;
			this.hadRenderExceptions = hadRenderExceptions;
		}
	}

	static final class RandomItemSimulator {
		private final GraphicsItem item;
		private final Random random;
		private double sog;

		RandomItemSimulator(final GraphicsItem item, final long seed) {
			this.item = item;
			this.random = new Random(seed);
			this.sog = random.nextDouble() * 0.1;
		}

		void update() {
			if (random.nextFloat() > 0.63f)
				return;

			float probability = random.nextFloat();
			if (probability < 0.4f) {
				final float deltaCog = random.nextFloat() * 100f - 50f;
				item.setRotation(item.getRotationDegrees() + deltaCog);
			}

			probability = random.nextFloat();
			if (probability < 0.05f) {
				final float deltaSog = random.nextFloat() * 200f - 100f;
				sog = Math.max(-1000, Math.min(1000, sog + deltaSog));
			}

			final double fx = 0;
			final double fy = -sog;
			final double angle = -item.getRotationRadians();
			final double cos = Math.cos(angle);
			final double sin = Math.sin(angle);
			final double dx = fx * cos + fy * sin;
			final double dy = fx * -sin + fy * cos;

			double x = item.getCenterX() + dx;
			double y = item.getCenterY() + dy;
			final LatLonPoint llp = GeoUtils.getLatLon(x, y);
			if (Math.abs(llp.getLatitude()) > 80)
				y = 0;
			if (Math.abs(llp.getLongitude()) > 170)
				x = 0;
			item.setCenter(x, y);
		}
	}
}
