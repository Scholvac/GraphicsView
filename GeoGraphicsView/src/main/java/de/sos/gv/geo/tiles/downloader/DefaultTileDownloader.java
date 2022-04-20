package de.sos.gv.geo.tiles.downloader;

import java.net.MalformedURLException;
import java.net.URL;

import de.sos.gv.geo.tiles.TileInfo;

public class DefaultTileDownloader extends AbstractTileDownloader {

	private String		mBaseURL;

	public DefaultTileDownloader(final String baseURL) {
		mBaseURL = baseURL;
	}
	public String getBaseURL() {return mBaseURL;}
	public void setBaseURL(final String base) {mBaseURL = base;}

	@Override
	protected URL getURL(final TileInfo info) {
		final String urlStr = mBaseURL
				.replace("{x}", ""+info.tileX())
				.replace("{y}", ""+info.tileY())
				.replace("{z}", ""+info.tileZ());
		try {
			return new URL(urlStr);
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

}
