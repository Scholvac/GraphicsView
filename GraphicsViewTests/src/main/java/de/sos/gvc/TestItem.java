package de.sos.gvc;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

import de.sos.gvc.styles.DrawableStyle;

public class TestItem extends GraphicsItem {

	public enum FillColorStyles {
		BLUE("blue", Color.blue),
		RED("red", Color.red),
		GREEN("green", Color.green);

		private final DrawableStyle style;
		FillColorStyles(final String name, final Color color) {
			style = new DrawableStyle(name, null, null, color);
		}
	}

	public static GraphicsItem createStar(final double x, final double y, final double radius, final FillColorStyles style) {
		final double innerRadius = radius * 0.3; // Inner points ratio for sharp spikes
		final GeneralPath star = new GeneralPath();

		// Start at bottom spike
		star.moveTo(0, -radius);

		// Create points clockwise
		star.lineTo(innerRadius, -innerRadius);  // Right inner bottom
		star.lineTo(radius, 0);                 // Right outer
		star.lineTo(innerRadius, innerRadius);   // Right inner top
		star.lineTo(0, radius);                 // Top spike
		star.lineTo(-innerRadius, innerRadius); // Left inner top
		star.lineTo(-radius, 0);                // Left outer
		star.lineTo(-innerRadius, -innerRadius); // Left inner bottom
		star.closePath();                       // Return to start

		final TestItem ti = new TestItem(star);
		if (style != null)
			ti.setStyle(style.style);
		return ti;
	}




	public TestItem(final Shape shape) {
		super(shape);
	}
}
