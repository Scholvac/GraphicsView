package de.sos.gv.geo.tiles.downloader;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;

import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gvc.log.GVLog;

public class WMSImageProvider extends AbstractTileDownloader {

	public enum WMSVersion {
		VERSION_1_1_1,
		VERSION_1_3_0
	}

	private static final Logger	logger = GVLog.getLogger(WMSImageProvider.class);

	private String 			mBaseURL;
	private String 			mFormat = "image/png";
	private int				mTileSize = 512;
	private String 			mStyles = "";
	private String 			mSrs = "EPSG:4326";
	private String			mLayer = "ENC";
	private WMSVersion		mVersion;



	public WMSImageProvider(final String url, final WMSVersion vers) {
		this(url, vers, "ENC");
	}
	public WMSImageProvider(final String url, final WMSVersion vers, final String layer) {
		mBaseURL = url;
		mLayer = layer;
		mVersion = vers;
		logger.debug("New WMSImageProvider");
	}



	@Override
	protected URL getURL(final TileInfo info) {
		try {
			final String format = "image/png";
			final String styles = "";
			final int ts = mTileSize;
			final LatLonBox bb = info.getLatLonBounds();
			final double ulx = bb.getWest();
			final double uly = bb.getSouth();
			final double lrx = bb.getEast();
			final double lry = bb.getNorth();
			//			ulx = 53.85; uly=8.45;lrx=54.05;lry=9.0;
			String bbox = ulx + "," + uly + "," + lrx + "," + lry;

			String version = "1.1.1";
			String srs = "&SRS=" + mSrs;
			if (mVersion == WMSVersion.VERSION_1_3_0) {
				bbox = uly + "," + ulx + "," + lry + "," + lrx;
				version = "1.3.0";
				srs = "&CRS="+srs;
			}
			String baseURL = mBaseURL;
			if (baseURL == null) {
				baseURL = "";
			}

			if (baseURL.contains("://") == false) {
				baseURL = "http://" + baseURL;
			}

			if (baseURL.contains("?") == false) {
				baseURL += "?";
			} else if (baseURL.endsWith("?") == false && baseURL.endsWith("&") == false) {
				baseURL += "&";
			}
			String layers = "";
			if (mLayer != null && mLayer.isEmpty()==false)
				layers = "&LAYERS=" + mLayer;
			final String url = baseURL + "VERSION="+version+"&REQUEST=" + "GetMap" + layers + srs + "&BBOX="+bbox + "&WIDTH=" + ts + "&HEIGHT=" + ts + "&FORMAT="+format;
			return new URL(url);
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

}
