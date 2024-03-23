package test.de.sos.gvc;

import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;

import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget.VolatileImageRenderTarget;

public class TestUtils {

	public static void assertEquals(final String expectedURL, final BufferedImageRenderTarget rt, final float maxDiff) {
		final BufferedImage actualImg = rt.getImage();
		assertEquals(expectedURL, actualImg, maxDiff, true);
	}
	public static void assertNotEquals(final String expectedURL, final BufferedImageRenderTarget rt, final float maxDiff) {
		final BufferedImage actualImg = rt.getImage();
		assertEquals(expectedURL, actualImg, maxDiff, false);
	}
	public static void assertEquals(final String expectedURL, final VolatileImageRenderTarget rt, final float maxDiff) {
		final VolatileImage actualImg = rt.getImage();
		final BufferedImage bimg = new BufferedImage(actualImg.getWidth(), actualImg.getHeight(), actualImg.getTransparency());
		bimg.getGraphics().drawImage(actualImg, 0, 0, null);

		assertEquals(expectedURL, bimg, maxDiff, true);//note: this may be a little bigger to account for the operation above.
	}

	public static void assertEquals(final String expectedURL, final BufferedImage actualImg, final float maxDifference, final boolean smaller) {
		final BufferedImage expectedImg = ImageComparisonUtil.readImageFromResources(expectedURL);

		final ImageComparison comparision = new ImageComparison(expectedImg, actualImg);
		final ImageComparisonResult res = comparision.compareImages();

		//Check the result
		final float differenceInPercent = res.getDifferencePercent();
		final boolean boolRes = smaller ? differenceInPercent > maxDifference : differenceInPercent < maxDifference;
		if (boolRes) {
			ImageComparisonUtil.saveImage(new File("target/expected.png"), res.getExpected());
			ImageComparisonUtil.saveImage(new File("target/actual.png"), res.getActual());
			ImageComparisonUtil.saveImage(new File("target/result.png"), res.getResult());
			fail("Imagecomparision failed for : " + expectedURL + " Difference in Percent: " + differenceInPercent + " (max allowed difference = " + maxDifference + ")");
		}
	}



}
