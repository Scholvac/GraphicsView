package de.sos.gvc.gl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.ImageCompareUtil;
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;
import de.sos.gvc.styles.DrawableStyle;

/**
 * Tests that {@link GLRenderTarget} produces output matching
 * {@link BufferedImageRenderTarget} for identical scenes.
 *
 * <p>Each test renders the same scene with both render targets and compares
 * the resulting images.  A tolerance of 5 % is allowed to account for
 * differences between Java2D direct rendering and GL sprite compositing
 * (anti-aliased sprite edges, texture filtering, etc.).</p>
 */
public class GLRenderTargetTest {

	private static final int VIEW_WIDTH  = 640;
	private static final int VIEW_HEIGHT = 480;
	/** Allowed percentage of differing pixels between GL and software render. */
	private static final double MAX_DIFF_PERCENT = 5.0;

	private static final Path TEST_RESULT_DIR = Paths.get(
			System.getProperty("basedir", ".")).toAbsolutePath().normalize()
			.resolve(Paths.get("target", "test-results", "gl-render-target"));

	private static final String[] SAMPLE_WKTS = {
		"POLYGON ((0 150, 25 75, 100 50, 25 25, 0 -50, -25 25, -100 50, -25 75, 0 150))",
		"POLYGON ((-100 200, -89.47368421052632 200, -89.47368421052632 -89.47368421052632, "
			+ "-78.94736842105263 -89.47368421052632, -78.94736842105263 200, "
			+ "-68.42105263157895 200, -68.42105263157895 -89.47368421052632, "
			+ "-57.89473684210526 -89.47368421052632, -57.89473684210526 200, "
			+ "-47.368421052631575 200, -47.368421052631575 -89.47368421052632, "
			+ "-36.84210526315789 -89.47368421052632, -36.84210526315789 200, "
			+ "-26.315789473684205 200, -26.315789473684205 -89.47368421052632, "
			+ "-15.78947368421052 -89.47368421052632, -15.78947368421052 200, "
			+ "-5.263157894736835 200, -5.263157894736835 -89.47368421052632, "
			+ "5.26315789473685 -89.47368421052632, 5.26315789473685 200, "
			+ "15.789473684210535 200, 15.789473684210535 -89.47368421052632, "
			+ "26.31578947368422 -89.47368421052632, 26.31578947368422 200, "
			+ "36.842105263157904 200, 36.842105263157904 -89.47368421052632, "
			+ "47.36842105263159 -89.47368421052632, 47.36842105263159 200, "
			+ "57.894736842105274 200, 57.894736842105274 -89.47368421052632, "
			+ "68.42105263157896 -89.47368421052632, 68.42105263157896 200, "
			+ "78.94736842105264 200, 78.94736842105264 -89.47368421052632, "
			+ "89.47368421052633 -89.47368421052632, 89.47368421052633 200, "
			+ "100.00000000000001 200, 100 -100, -100 -100, -100 200))",
		"POLYGON ((-100 43.74769457764663, -61.96975285872371 62.30173367760975, "
			+ "-58.17041682036148 62.30173367760975, -58.17041682036148 -69.67908520841019, "
			+ "-57.89376613795646 -80.33474732571007, -57.06381409074142 -86.03836222796016, "
			+ "-55.37163408336407 -88.89247510143858, -52.50829952047215 -90.99963113242346, "
			+ "-47.26576908889709 -92.35521947620803, -38.436001475470306 -92.95462928808557, "
			+ "-38.436001475470306 -97.23349317594983, -97.23349317594983 -97.23349317594983, "
			+ "-97.23349317594983 -92.95462928808557, -88.12707488011803 -92.36905201032829, "
			+ "-82.93987458502397 -91.05496126890446, -80.20103282921431 -89.09074142382885, "
			+ "-78.43969015123571 -86.55477683511619, -77.48524529693839 -80.9249354481741, "
			+ "-77.16709701217263 -69.67908520841019, -77.16709701217263 14.680929546292887, "
			+ "-77.45296938399113 28.697897454813727, -78.31058649944669 36.5916635927702, "
			+ "-79.45407598672077 39.79158981925488, -81.26152711176687 42.01401696790853, "
			+ "-83.61305791220951 43.31427517521209, -86.38878642567317 43.74769457764663, "
			+ "-91.49760236075248 42.85319070453707, -98.26632239026189 40.1696790852084, "
			+ "-100 43.74769457764663))"
	};

	private GLRenderTarget mGLTarget;

	@BeforeEach
	void setUp() {
		try {
			mGLTarget = GLRenderTarget.createOffscreen(VIEW_WIDTH, VIEW_HEIGHT);
		} catch (final Exception e) {
			// GL not available (e.g. headless CI)
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

	// -----------------------------------------------------------------------
	//  Tests
	// -----------------------------------------------------------------------

	@Test
	void glRenderTarget_matchesSoftwareRender() throws IOException {
		assumeTrue(mGLTarget != null, "OpenGL context not available — skipping");

		final GraphicsScene scene = createTestScene();

		// --- software reference ---
		final BufferedImageRenderTarget swRT =
				new BufferedImageRenderTarget(VIEW_WIDTH, VIEW_HEIGHT,
						BufferedImage.TYPE_INT_RGB, false);
		swRT.setClearColor(Color.RED);
		final GraphicsView swView = new GraphicsView(scene, swRT);
		swRT.requestRepaint();
		final BufferedImage expected = swRT.getImage();

		// --- GL render ---
		mGLTarget.setClearColor(Color.RED);
		final GraphicsView glView = new GraphicsView(scene, mGLTarget);
		mGLTarget.requestRepaint();
		final BufferedImage actual = mGLTarget.getResultImage();

		assertNotNull(actual, "GL render target did not produce an image");
		ImageCompareUtil.assertEquals(
				"gl_vs_software", expected, actual, MAX_DIFF_PERCENT,
				"GL output differs from software render by more than " + MAX_DIFF_PERCENT + "%",
				TEST_RESULT_DIR.toString());
	}

	@Test
	void glRenderTarget_matchesSoftwareRender_withOffset() throws IOException {
		assumeTrue(mGLTarget != null, "OpenGL context not available — skipping");

		final GraphicsScene scene = createTestScene();

		// --- software reference with offset ---
		final BufferedImageRenderTarget swRT =
				new BufferedImageRenderTarget(VIEW_WIDTH, VIEW_HEIGHT,
						BufferedImage.TYPE_INT_RGB, false);
		swRT.setClearColor(Color.WHITE);
		final GraphicsView swView = new GraphicsView(scene, swRT);
		swView.setCenter(50, -30);
		swRT.requestRepaint();
		final BufferedImage expected = swRT.getImage();

		// --- GL render with same offset ---
		mGLTarget.setClearColor(Color.WHITE);
		final GraphicsView glView = new GraphicsView(scene, mGLTarget);
		glView.setCenter(50, -30);
		mGLTarget.requestRepaint();
		final BufferedImage actual = mGLTarget.getResultImage();

		assertNotNull(actual, "GL render target did not produce an image");
		ImageCompareUtil.assertEquals(
				"gl_vs_software_offset", expected, actual, MAX_DIFF_PERCENT,
				"GL output with offset differs from software render",
				TEST_RESULT_DIR.toString());
	}

	@Test
	void glRenderTarget_matchesSoftwareRender_scaled() throws IOException {
		assumeTrue(mGLTarget != null, "OpenGL context not available — skipping");

		final GraphicsScene scene = createTestScene();

		// --- software reference with scale ---
		final BufferedImageRenderTarget swRT =
				new BufferedImageRenderTarget(VIEW_WIDTH, VIEW_HEIGHT,
						BufferedImage.TYPE_INT_RGB, false);
		swRT.setClearColor(Color.DARK_GRAY);
		final GraphicsView swView = new GraphicsView(scene, swRT);
		swView.setScale(2.0, 2.0);
		swRT.requestRepaint();
		final BufferedImage expected = swRT.getImage();

		// --- GL render with same scale ---
		mGLTarget.setClearColor(Color.DARK_GRAY);
		final GraphicsView glView = new GraphicsView(scene, mGLTarget);
		glView.setScale(2.0, 2.0);
		mGLTarget.requestRepaint();
		final BufferedImage actual = mGLTarget.getResultImage();

		assertNotNull(actual, "GL render target did not produce an image");
		ImageCompareUtil.assertEquals(
				"gl_vs_software_scaled", expected, actual, MAX_DIFF_PERCENT,
				"GL output with scale differs from software render",
				TEST_RESULT_DIR.toString());
	}

	// -----------------------------------------------------------------------
	//  Scene setup (mirrors ImageRenderTargetTest)
	// -----------------------------------------------------------------------

	private static GraphicsScene createTestScene() {
		final GraphicsScene scene = new GraphicsScene();
		for (final String wkt : SAMPLE_WKTS) {
			final GraphicsItem item = GraphicsItem.createFromWKT(wkt);
			final DrawableStyle style = new DrawableStyle();
			style.setFillPaint(Color.green);
			style.setLinePaint(Color.blue);
			item.setStyle(style);
			scene.addItem(item);
		}
		return scene;
	}
}
