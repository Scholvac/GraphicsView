package de.sos.gvc.gt.tiles.cache;

import de.sos.gvc.gt.tiles.ITileDescription;

/**
 * 
 * @author scholvac
 *
 */
public class CacheData<DESC extends ITileDescription, VALUE> {
	public final DESC 			tile;
	public final VALUE		imageData;
	
	public CacheData(DESC t, VALUE d) {
		tile = t; imageData = d;
	}
}