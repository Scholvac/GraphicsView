package de.sos.gvc.index;

import java.awt.geom.Rectangle2D;

/**
 * 
 * @author scholvac
 *
 */
public class Utils {
	
	public static double getArea(Rectangle2D rect) {
		return rect.getHeight() * rect.getWidth();
	}
}
