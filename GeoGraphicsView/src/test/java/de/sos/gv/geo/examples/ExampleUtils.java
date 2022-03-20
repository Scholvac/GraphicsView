package de.sos.gv.geo.examples;

import java.awt.Shape;
import java.awt.geom.GeneralPath;

/**
 * 
 * @author scholvac
 *
 */
public class ExampleUtils {

	public static Shape wkt2Shape(String wkt) {
		int idx1 = wkt.lastIndexOf("(")+1;
		int idx2 = wkt.indexOf(")");
		String coords1 = wkt.substring(idx1, idx2);
		String coordArr[] = coords1.split(",");
		GeneralPath path = new GeneralPath();
		String fc[] = coordArr[0].split(" ");
		double fx = Float.parseFloat(fc[0]);
		double fy = Float.parseFloat(fc[1]);
		path.moveTo(fx, fy);
		
		for (int i = 1; i < coordArr.length; i++) {
			String c[] = coordArr[i].trim().split(" ");
			float cx = Float.parseFloat(c[0]);
			float cy = Float.parseFloat(c[1]);
			path.lineTo(cx, cy);
		}
		if (coordArr[0].trim().equals(coordArr[coordArr.length-1].trim()))
			path.closePath();
		return path;
	}
}
