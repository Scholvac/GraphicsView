package de.sos.gvc.gt.tiles.wms;

import de.sos.gvc.gt.tiles.ITileLoader;
import de.sos.gvc.gt.tiles.ITileProvider;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;
import de.sos.gvc.gt.tiles.wms.WMSOptions.WMSVersion;

/**
 * 
 * @author scholvac
 *
 */
public class WMSTileFactory extends OSMTileFactory implements ITileProvider<OSMTileDescription> {
	
	private WMSOptions mOptions;

	public WMSTileFactory(String baseURL) {
		this(new WMSOptions(baseURL, WMSVersion.VERSION_1_1_1));
	}
	
	public WMSTileFactory(WMSOptions options) {
		mOptions = options;
	}

	@Override
	public ITileLoader<OSMTileDescription> createTileLoader() {
		return new WMSTileDownloader(mOptions);
	}
	
	public int calculateZoom(LatLonBoundingBox area, double imgWidth) {
		return super.calculateZoom(area, imgWidth);
	}

}
