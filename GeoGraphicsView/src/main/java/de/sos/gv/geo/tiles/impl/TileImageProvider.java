package de.sos.gv.geo.tiles.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gvc.log.GVLog;

public class TileImageProvider implements ITileImageProvider {



	private static final Logger	logger = GVLog.getLogger(TileImageProvider.class);

	protected final String			mURL;
	private String 					mUserAgent = "GeoGraphView/1.0";
	private int 					mConnectTimeOutMillis = 2000; //2sec
	private int 					mReadTimeOutMillis = 2000; //2sec

	public TileImageProvider(final String url) {
		mURL = url;
		logger.info("New TileImageProvider");
	}


	@Override
	public BufferedImage provide(TileInfo info) {
		final URL url = createURL(info);
		if (url == null)
			return null;
		try {
			if (logger.isTraceEnabled()) logger.trace("Downloading Tile: {}", url);
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(mConnectTimeOutMillis);
			connection.setReadTimeout(mReadTimeOutMillis);
			connection.setRequestProperty("User-Agent", mUserAgent);
			InputStream ins = connection.getInputStream();
			return ImageIO.read(ins);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	protected URL createURL(TileInfo info) {
		final String urlStr = mURL.replace("{x}", ""+info.tileX()).replace("{y}", ""+info.tileY()).replace("{z}", info.tileZ()+"");
		try {
			return new URL(urlStr);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

}
