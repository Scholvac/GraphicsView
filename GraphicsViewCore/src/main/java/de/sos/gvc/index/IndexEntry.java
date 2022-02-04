package de.sos.gvc.index;

import java.awt.geom.Rectangle2D;

/**
 *
 * @author scholvac
 *
 */
public interface IndexEntry<VALUE> {
	VALUE getValue();
	Rectangle2D getGeometry();
}
