package de.sos.gvc.gt.tiles.google;

import java.net.MalformedURLException;
import java.net.URL;

import de.sos.gvc.gt.tiles.ITileLoader;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileDownloader;


/**
 * 
 * @author scholvac
 *
 */
public class GoogleTileDownloader extends OSMTileDownloader implements ITileLoader<OSMTileDescription>{

	private String			mBaseURL;
	
	public GoogleTileDownloader() {
		this("http://mt" + (int) (Math.random() * 3 + 0.5)+".google.com/vt/v=w2.106&hl=de");
	}
	
	public GoogleTileDownloader(String string) {
		mBaseURL = string;
	}

	@Override
	public URL getURL(OSMTileDescription desc) {
		try {
			int zoom = desc.getZoom();
			String str = mBaseURL + "&x=" + desc.getTileX() + "&y=" + desc.getTileY() + "&z=" + zoom;
			return new URL(str);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	

}
