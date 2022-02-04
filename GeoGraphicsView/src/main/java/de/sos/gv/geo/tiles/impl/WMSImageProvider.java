package de.sos.gv.geo.tiles.impl;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;

import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gvc.log.GVLog;

public class WMSImageProvider extends TileImageProvider {

	public enum WMSVersion {
		VERSION_1_1_1,
		VERSION_1_3_0
	}

	private static final Logger	logger = GVLog.getLogger(WMSImageProvider.class);

	private String 			mFormat = "image/png";
	private int				mTileSize = 512;
	private String 			mStyles = "";
	private String 			mSrs = "EPSG:4326";
	private String			mLayer = "ENC";
	private WMSVersion		mVersion;

	public WMSImageProvider(final String url, WMSVersion vers) {
		this(url, vers, "ENC");
	}
	public WMSImageProvider(final String url, WMSVersion vers, final String layer) {
		super(url);
		mLayer = layer;
		mVersion = vers;
		logger.debug("New WMSImageProvider");
	}



	@Override
	protected URL createURL(TileInfo info) {
		try {
			String format = "image/png";
			String styles = "";
			int ts = mTileSize;
			LatLonBox bb = info.getLatLonBounds();
			double ulx = bb.getWest();
			double uly = bb.getSouth();
			double lrx = bb.getEast();
			double lry = bb.getNorth();
			//			ulx = 53.85; uly=8.45;lrx=54.05;lry=9.0;
			String bbox = ulx + "," + uly + "," + lrx + "," + lry;

			String version = "1.1.1";
			String srs = "&SRS=" + mSrs;
			if (mVersion == WMSVersion.VERSION_1_3_0) {
				bbox = uly + "," + ulx + "," + lry + "," + lrx;
				version = "1.3.0";
				srs = "&CRS="+srs;
			}
			String baseURL = mURL;
			if (baseURL == null) {
				baseURL = "";
			}

			if (baseURL.contains("://") == false) {
				baseURL = "http://" + baseURL;
			}

			if (baseURL.contains("?") == false) {
				baseURL += "?";
			} else {
				if (baseURL.endsWith("?") == false && baseURL.endsWith("&") == false) {
					baseURL += "&";
				}
			}
			String layers = "";
			if (mLayer != null && mLayer.isEmpty()==false)
				layers = "&LAYERS=" + mLayer;
			String url = baseURL + "VERSION="+version+"&REQUEST=" + "GetMap" + layers + srs + "&BBOX="+bbox + "&WIDTH=" + ts + "&HEIGHT=" + ts + "&FORMAT="+format;
			return new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

}
