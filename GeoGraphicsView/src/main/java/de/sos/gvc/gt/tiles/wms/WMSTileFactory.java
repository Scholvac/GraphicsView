package de.sos.gvc.gt.tiles.wms;

import java.awt.geom.Point2D;

import de.sos.gvc.gt.GeoUtils;
import de.sos.gvc.gt.tiles.ITileFactory;
import de.sos.gvc.gt.tiles.ITileLoader;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;
import de.sos.gvc.gt.tiles.wms.WMSOptions.WMSVersion;

/**
 * 
 * @author scholvac
 *
 */
public class WMSTileFactory extends OSMTileFactory implements ITileFactory<OSMTileDescription> {
	
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
