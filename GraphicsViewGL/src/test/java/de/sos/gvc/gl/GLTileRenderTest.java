package de.sos.gvc.gl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.ImageCompareUtil;
import de.sos.gvc.TestGraphicsView;

/**
 * Verifies that the {@link GLRenderTarget} renders tile-based geo scenes
 * correctly by comparing its output against the software
 * {@link de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget}.
 *
 * <p>Uses a {@link DeterministicTileImageProvider} to produce reproducible
 * tile imagery without network access.</p>
 */
public class GLTileRenderTest {

	private static final LatLonPoint BREMERHAVEN = new LatLonPoint(53.523495, 8.641542);
	private static final int VIEW_WIDTH  = 800;
	private static final int VIEW_HEIGHT = 800;
	/** Allowed pixel difference between GL and software output. */
	private static final double GL_TOLERANCE = 5.0;

	private static final Path TEST_RESULT_DIR = Paths.get(
			System.getProperty("basedir", ".")).toAbsolutePath().normalize()
			.resolve(Paths.get("target", "test-results", "gl-tile-render"));

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
	//  Tests
	// -------------------------------------------------------------------

	@Test
	void tilesOnly_defaultZoom() throws IOException {
		assumeTrue(mGLTarget != null, "OpenGL context not available");

		final BufferedImage expected = renderSoftware(3.0, 0, 0);
		final BufferedImage actual   = renderGL(3.0, 0, 0);

		assertNotNull(actual, "GL target produced no image");
		ImageCompareUtil.assertEquals("gl_tiles_default", expected, actual,
				GL_TOLERANCE, "GL tile output differs at default zoom",
				TEST_RESULT_DIR.toString());
	}

	@Test
	void tilesOnly_zoomedIn() throws IOException {
		assumeTrue(mGLTarget != null, "OpenGL context not available");

		final BufferedImage expected = renderSoftware(4.5, 0, 0);
		final BufferedImage actual   = renderGL(4.5, 0, 0);

		assertNotNull(actual, "GL target produced no image");
		ImageCompareUtil.assertEquals("gl_tiles_zoomed", expected, actual,
				GL_TOLERANCE, "GL tile output differs when zoomed in",
				TEST_RESULT_DIR.toString());
	}

	@Test
	void tilesOnly_withPan() throws IOException {
		assumeTrue(mGLTarget != null, "OpenGL context not available");

		final BufferedImage expected = renderSoftware(3.0, 300, -200);
		final BufferedImage actual   = renderGL(3.0, 300, -200);

		assertNotNull(actual, "GL target produced no image");
		ImageCompareUtil.assertEquals("gl_tiles_panned", expected, actual,
				GL_TOLERANCE, "GL tile output differs with pan offset",
				TEST_RESULT_DIR.toString());
	}

	@Test
	void tilesAndShapes_mixed() throws IOException {
		assumeTrue(mGLTarget != null, "OpenGL context not available");

		// Software reference
		final TestGraphicsView swView = TestGraphicsView.create(VIEW_WIDTH, VIEW_HEIGHT);
		applyRenderHints(swView);
		addTiles(swView.getScene(), swView);
		ManualGeoGraphicsTest.addItems(swView.getScene());
		swView.setCenter(swView.getCenterX() + 200, swView.getCenterY());
		swView.setScale(3.0);
		final BufferedImage expected = swView.getBufferedImage(true);

		// GL render
		mGLTarget.setClearColor(Color.BLACK);
		final GraphicsScene glScene = new GraphicsScene();
		final GraphicsView glView = new GraphicsView(glScene, mGLTarget);
		glView.enableRepaintTrigger(false);
		applyRenderHints(glView);
		addTiles(glScene, glView);
		ManualGeoGraphicsTest.addItems(glScene);
		glView.setCenter(glView.getCenterX() + 200, glView.getCenterY());
		glView.setScale(3.0);
		mGLTarget.requestRepaint();
		final BufferedImage actual = mGLTarget.getResultImage();

		assertNotNull(actual, "GL target produced no image");
		ImageCompareUtil.assertEquals("gl_tiles_mixed", expected, actual,
				GL_TOLERANCE, "GL mixed tile+shape output differs",
				TEST_RESULT_DIR.toString());
	}

	// -------------------------------------------------------------------
	//  Helpers
	// -------------------------------------------------------------------

	private BufferedImage renderSoftware(final double scale, final double panX, final double panY) {
		final TestGraphicsView view = TestGraphicsView.create(VIEW_WIDTH, VIEW_HEIGHT);
		applyRenderHints(view);
		addTiles(view.getScene(), view);
		view.setCenter(view.getCenterX() + panX, view.getCenterY() + panY);
		view.setScale(scale);
		return view.getBufferedImage(true);
	}

	private BufferedImage renderGL(final double scale, final double panX, final double panY) {
		mGLTarget.setClearColor(Color.BLACK);
		final GraphicsScene scene = new GraphicsScene();
		final GraphicsView view = new GraphicsView(scene, mGLTarget);
		view.enableRepaintTrigger(false);
		applyRenderHints(view);
		addTiles(scene, view);
		view.setCenter(view.getCenterX() + panX, view.getCenterY() + panY);
		view.setScale(scale);
		mGLTarget.requestRepaint();
		return mGLTarget.getResultImage();
	}

	private static void applyRenderHints(final GraphicsView view) {
		view.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		view.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		view.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	private static void addTiles(final GraphicsScene scene, final GraphicsView view) {
		final TileHandler handler = new TileHandler(new TileFactory(new DeterministicTileImageProvider(), 1));
		handler.waitForAllTiles(true);
		view.addHandler(handler);
		GeoUtils.setViewCenter(view, BREMERHAVEN);
		view.setScale(3);
	}

	// -------------------------------------------------------------------
	//  Deterministic tile provider (identical to ManualGeoGraphicsTest)
	// -------------------------------------------------------------------

	static final class DeterministicTileImageProvider implements ITileImageProvider {

		private static final int TILE_SIZE = 256;

		@Override
		public BufferedImage load(final TileInfo info) {
			final BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
			final java.awt.Graphics2D g = img.createGraphics();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

				final int seed = seed(info);
				final Color base   = color(seed);
				final Color accent = color(Integer.rotateLeft(seed, 11));

				g.setPaint(new GradientPaint(0, 0, base, TILE_SIZE, TILE_SIZE, accent));
				g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

				g.setStroke(new BasicStroke(2f));
				g.setColor(new Color(255, 255, 255, 150));
				for (int step = 0; step <= TILE_SIZE; step += 64) {
					g.drawLine(step, 0, step, TILE_SIZE);
					g.drawLine(0, step, TILE_SIZE, step);
				}

				g.setColor(new Color(0, 0, 0, 110));
				g.drawRect(1, 1, TILE_SIZE - 3, TILE_SIZE - 3);
				g.drawLine(0, 0, TILE_SIZE, TILE_SIZE);
				g.drawLine(0, TILE_SIZE, TILE_SIZE, 0);

				final int circleSize = 36 + Math.abs(seed % 48);
				final int circleX = 24 + Math.abs((seed / 7) % (TILE_SIZE - circleSize - 48));
				final int circleY = 24 + Math.abs((seed / 13) % (TILE_SIZE - circleSize - 48));
				g.setColor(new Color(255 - accent.getRed(), 255 - accent.getGreen(), 255 - accent.getBlue(), 180));
				g.fillOval(circleX, circleY, circleSize, circleSize);

				g.setColor(new Color(15, 15, 15, 160));
				g.setStroke(new BasicStroke(4f));
				g.drawRoundRect(18, 18, TILE_SIZE - 36, TILE_SIZE - 36, 28, 28);
			} finally {
				g.dispose();
			}
			return img;
		}

		@Override
		public void free(final TileInfo info, final BufferedImage img) { }

		private static int seed(final TileInfo info) {
			int seed = 17;
			seed = 31 * seed + info.tileX();
			seed = 31 * seed + info.tileY();
			seed = 31 * seed + info.tileZ();
			return seed;
		}

		private static Color color(final int seed) {
			final int red   = 48 + (seed & 0x7F);
			final int green = 48 + ((seed >>> 7) & 0x7F);
			final int blue  = 48 + ((seed >>> 14) & 0x7F);
			return new Color(red, green, blue);
		}
	}
}
