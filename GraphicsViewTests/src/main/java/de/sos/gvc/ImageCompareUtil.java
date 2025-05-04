package de.sos.gvc;

import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.UUID;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;

/**
 * Utility class providing assertion methods for image comparison testing.
 *
 * This class offers various methods to compare images and verify their equality,
 * with options to configure allowed differences and output directories.
 * It uses the image-comparison library to perform pixel-by-pixel comparisons
 * and generates visual comparison results when tests fail.
 *
 * The class provides several overloaded methods to accommodate different testing
 * scenarios:
 * <ul>
 *     <li>Simple comparison with default settings (0% allowed difference)</li>
 *     <li>Custom allowed difference percentage for fuzzy comparisons</li>
 *     <li>Custom output directories for comparison results</li>
 *     <li>Custom failure messages for better test reporting</li>
 * </ul>
 *
 * When a comparison fails, the class automatically generates three images:
 * <ul>
 *     <li>_expected.png - The expected image</li>
 *     <li>_actual.png - The actual image that was compared</li>
 *     <li>_result.png - The comparison result showing differences</li>
 * </ul>
 */
public class ImageCompareUtil {
	/**
	 * Default allowed difference percentage for image comparisons
	 */
	private static final double DEFAULT_ALLOWED_DIFFERENCE = 0.0; // 0% difference allowed by default

	/**
	 * Default target directory for comparison results
	 */
	private static final String DEFAULT_TARGET_DIRECTORY = "./target/test-results";

	/**
	 * Compares two images with default settings (0% allowed difference)
	 *
	 * @param testName Name of the test for result files
	 * @param expected Expected image
	 * @param actual Actual image
	 */
	public static void assertEquals(final String testName, final BufferedImage expected, final BufferedImage actual) {
		assertEquals(testName, expected, actual, DEFAULT_ALLOWED_DIFFERENCE, "Images do not match", DEFAULT_TARGET_DIRECTORY);
	}


	/**
	 * Compares two images with custom allowed difference percentage
	 *
	 * @param testName Name of the test for result files
	 * @param expected Expected image
	 * @param actual Actual image
	 * @param allowedDifferenceInPercentage Allowed percentage of different pixels (0.0 to 100.0)
	 */
	public static void assertEquals(final String testName, final BufferedImage expected, final BufferedImage actual, final double allowedDifferenceInPercentage) {
		assertEquals(testName, expected, actual, allowedDifferenceInPercentage, "Images do not match (allowed difference: " + allowedDifferenceInPercentage + "%)", DEFAULT_TARGET_DIRECTORY);
	}

	/**
	 * Compares two images with custom target directory
	 *
	 * @param testName Name of the test for result files
	 * @param expected Expected image
	 * @param actual Actual image
	 * @param targetDirectory Directory to save comparison results
	 */
	public static void assertEquals(final String testName, final BufferedImage expected, final BufferedImage actual, final String targetDirectory) {
		assertEquals(testName, expected, actual, DEFAULT_ALLOWED_DIFFERENCE, "Images do not match", targetDirectory);
	}

	/**
	 * Compares two images with all custom settings
	 *
	 * @param testName Name of the test for result files
	 * @param expected Expected image
	 * @param actual Actual image
	 * @param allowedDifferenceInPercentage Allowed percentage of different pixels (0.0 to 100.0)
	 * @param message Custom failure message
	 * @param targetDirectory Directory to save comparison results
	 */
	public static void assertEquals(final String testName, final BufferedImage expected, final BufferedImage actual, final double allowedDifferenceInPercentage, final String message, final String targetDirectory) {
		final boolean equals = compareAndWriteResult(expected, actual, allowedDifferenceInPercentage, testName, targetDirectory);
		if (!equals)
			fail("Image comparison failed for: " + testName + ": " + message);
	}

	/**
	 * Compares two images and writes the result files
	 *
	 * @param expected Expected image
	 * @param actual Actual image
	 * @param allowedDifferenceInPercentage Allowed percentage of different pixels (0.0 to 100.0)
	 * @param testName Name of the test for result files
	 * @param targetDirectory Directory to save comparison results
	 * @return true if images match within allowed difference, false otherwise
	 */
	public static boolean compareAndWriteResult(final BufferedImage expected, final BufferedImage actual, final double allowedDifferenceInPercentage, final String testName, final String targetDirectory) {
		final ImageComparison comp = new ImageComparison(expected, actual);
		comp.setAllowingPercentOfDifferentPixels(allowedDifferenceInPercentage);

		final ImageComparisonResult result = comp.compareImages();
		if (result.getImageComparisonState() != ImageComparisonState.MATCH) {
			writeResult(result, testName, targetDirectory);
			return false;
		}
		return true;
	}

	/**
	 * Writes comparison result images to disk
	 *
	 * @param result Comparison result containing expected, actual and diff images
	 * @param testName Name of the test for file naming
	 * @param targetDirectory Directory to save files
	 */
	private static void writeResult(final ImageComparisonResult result, final String testName, final String targetDirectory) {
		final File directory = targetDirectory != null ? new File(targetDirectory) : new File("./");
		final String name = ((testName == null) || testName.isEmpty()) ? UUID.randomUUID().toString() : testName;

		// Create directory if it doesn't exist
		directory.mkdirs();

		ImageComparisonUtil.saveImage(new File(directory, name + "_expected.png"), result.getExpected());
		ImageComparisonUtil.saveImage(new File(directory, name + "_actual.png"), result.getActual());
		ImageComparisonUtil.saveImage(new File(directory, name + "_result.png"), result.getResult());
	}
}
