package de.sos.gvc.gt.tiles;

import java.awt.image.BufferedImage;

/**
 * 
 * @author scholvac
 *
 */
public interface ITileLoader<DESC extends ITileDescription> {
	public BufferedImage getTileImage(DESC tile);
}
