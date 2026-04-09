package de.sos.gvc.gl;

import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.ImageCompareUtil;

public class AssertJSwingGeoGraphicsScreenshotTest {

	private static final String REFERENCE_PROPERTY = "graphicsviewgl.updateWindowReferences";
	private static final String REFERENCE_RESOURCE = "de/sos/gvc/gl/reference/window/window_zoom_3_0_east_200m.png";
	private static final Path MODULE_BASEDIR = Paths.get(System.getProperty("basedir", ".")).toAbsolutePath().normalize();
	private static final Path REFERENCE_SOURCE = MODULE_BASEDIR.resolve(Paths.get("src", "test", "resources", "de", "sos", "gvc", "gl", "reference", "window", "window_zoom_3_0_east_200m.png"));
	private static final Path TEST_RESULT_DIR = MODULE_BASEDIR.resolve(Paths.get("target", "test-results", "assertj-swing"));
	private static final boolean UPDATE_REFERENCES = Boolean.getBoolean(REFERENCE_PROPERTY);

	private FrameFixture mWindow;

	@BeforeAll
	static void installRepaintManager() {
		FailOnThreadViolationRepaintManager.install();
	}

	@AfterEach
	void cleanup() {
		if (mWindow != null)
			mWindow.cleanUp();
	}

	@Test
	public void opensWindowAndMatchesOnScreenScreenshot() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "AssertJ-Swing screenshot test requires a headful graphics environment");

		final Robot robot = BasicRobot.robotWithNewAwtHierarchy();
		final JFrame frame = GuiActionRunner.execute((Callable<JFrame>) AssertJSwingGeoGraphicsScreenshotTest::createFrame);
		mWindow = new FrameFixture(robot, frame);
		mWindow.show();
		mWindow.requireVisible();
		mWindow.focus();
		robot.waitForIdle();
		Pause.pause(750);

		final Component component = GuiActionRunner.execute(() -> frame.getContentPane().getComponent(0));
		final BufferedImage actual = captureOnScreen(component);

		if (UPDATE_REFERENCES) {
			writeReference(actual);
			return;
		}

		final BufferedImage expected = loadReference();
		if (expected == null) {
			writeCandidate(actual);
			fail("Missing AssertJ-Swing reference image. Run with -D" + REFERENCE_PROPERTY + "=true to generate it.");
		}
		ImageCompareUtil.assertEquals("assertj_swing_window_zoom_3_0_east_200m", expected, actual, 0.5, "Failed", TEST_RESULT_DIR.toString());
	}

	private static JFrame createFrame() {
		final GraphicsView view = new GraphicsView(new GraphicsScene());
		view.getComponent().setPreferredSize(new Dimension(800, 800));
		ManualGeoGraphicsTest.configureGeoView(view, 3.0, 200, 0);

		final JFrame frame = new JFrame("AssertJ Swing Geo Screenshot");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setResizable(false);
		frame.getContentPane().add(view.getComponent());
		frame.pack();
		frame.setLocation(50, 50);
		return frame;
	}

	private static BufferedImage captureOnScreen(final Component component) throws Exception {
		final Rectangle bounds = GuiActionRunner.execute(() -> {
			final Point screen = component.getLocationOnScreen();
			return new Rectangle(screen.x, screen.y, component.getWidth(), component.getHeight());
		});
		return new java.awt.Robot().createScreenCapture(bounds);
	}

	private static BufferedImage loadReference() throws IOException {
		try (InputStream stream = AssertJSwingGeoGraphicsScreenshotTest.class.getClassLoader().getResourceAsStream(REFERENCE_RESOURCE)) {
			if (stream != null)
				return ImageIO.read(stream);
		}
		if (Files.isRegularFile(REFERENCE_SOURCE))
			return ImageIO.read(REFERENCE_SOURCE.toFile());
		return null;
	}

	private static void writeReference(final BufferedImage image) throws IOException {
		Files.createDirectories(REFERENCE_SOURCE.getParent());
		ImageIO.write(image, "png", REFERENCE_SOURCE.toFile());
		System.out.println("Updated AssertJ-Swing reference image: " + REFERENCE_SOURCE);
	}

	private static void writeCandidate(final BufferedImage image) throws IOException {
		Files.createDirectories(TEST_RESULT_DIR);
		ImageIO.write(image, "png", TEST_RESULT_DIR.resolve("assertj_swing_window_zoom_3_0_east_200m_candidate.png").toFile());
	}
}
