package de.sos.gvc.gt.tiles.google;

import de.sos.gvc.gt.tiles.ITileFactory;
import de.sos.gvc.gt.tiles.ITileLoader;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;

/**
 * 
 * @author scholvac
 *
 */
public class GoogleTileFactory extends OSMTileFactory implements ITileFactory<OSMTileDescription> {
	
	public GoogleTileFactory(String baseURL) {
		super(baseURL);
	}
	
	@Override
	public ITileLoader<OSMTileDescription> createTileLoader() {
		return new GoogleTileDownloader(getBaseURL());
	}

}
