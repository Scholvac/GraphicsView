package de.sos.gvc.gt.tiles.osm;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.apache.log4j.helpers.UtilLoggingLevel;
import org.slf4j.Logger;

import de.sos.gvc.gt.tiles.ITileLoader;
import de.sos.gvc.gt.tiles.cache.MultiCacheFactory.IByteTileLoader;
import de.sos.gvc.log.GVLog;


/**
 * 
 * @author scholvac
 *
 */
public class OSMTileDownloader implements ITileLoader<OSMTileDescription>, IByteTileLoader<OSMTileDescription>{
	
	private static final Logger	logger = GVLog.getLogger(OSMTileDownloader.class);
	
	private String			mBaseURL;
	private int				mAttemps = 3;
	private String 			mUserAgent = "GeoGraphView/1.0";
	private int 			mConnectTimeOutMillis = 2000; //2sec
	private int 			mReadTimeOutMillis = 2000; //2sec
	
	
	
	public OSMTileDownloader() {
		this("http://tile.openstreetmap.org/");
	}
	public OSMTileDownloader(String baseURL) {
		mBaseURL = baseURL;
	}
	
	public URL getURL(OSMTileDescription desc) {
		try {
			return new URL(mBaseURL + desc.getZoom() + "/" + desc.getTileX() + "/" + desc.getTileY() + ".png");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public BufferedImage getTileImage(OSMTileDescription tile) {
		byte[] data = getCompressedTileImage(tile);
		if (data != null) {
			try {
				BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(data));
				return bimg;
			} catch (IOException e) {
				e.printStackTrace();
			}finally {
				
			}
		}
		return null;
	}
	
	
	public byte[] getCompressedTileImage(OSMTileDescription tile) {
		int att = mAttemps;
		URL url = getURL(tile);
		if (url == null)
			return null;
		while(att >= 0) {
			try {
				byte[] data = getCompressedTileImage(url);
				if (data != null)
					return data;
				else
					throw new NullPointerException("Failed to get Compressed Tile Image for URL: " + url);
			}catch(SocketTimeoutException ste) {
				GVLog.warn("Socket Timedout: " + url);
				mReadTimeOutMillis += 2000;
				if (att == 1) try { Thread.sleep(250); } catch (InterruptedException e) { e.printStackTrace(); }
			}catch(Exception | Error e) {
				logger.error("Failed to download tile " + url, e);
				e.printStackTrace();
			}finally {
				att--;
			}
		}
		return null;
	}
	
	public byte[] getCompressedTileImage(URL url) throws IOException {
		if (logger.isTraceEnabled()) logger.trace("Downloading Tile: {}", url);
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(mConnectTimeOutMillis);
		connection.setReadTimeout(mReadTimeOutMillis);
        connection.setRequestProperty("User-Agent", mUserAgent);
        InputStream ins = connection.getInputStream();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[256];
        while (true)
        {
            int n = ins.read(buf);
            if (n == -1)
                break;
            bout.write(buf, 0, n);
        }
        ins.close();

        byte[] data = bout.toByteArray();
        bout.close();
        return data;
	}
	@Override
	public byte[] loadTile(OSMTileDescription description) {
		
		return new OSMTileDownloader().getCompressedTileImage(description);
//		return getCompressedTileImage(description);
	}


}
