package de.sos.gv.geo.tiles.downloader;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gvc.log.GVLog;

public abstract class AbstractTileDownloader implements ITileImageProvider {

	private static final Logger					LOG = GVLog.getLogger(AbstractTileDownloader.class);

	protected int			mAttemps = 3;
	protected String 		mUserAgent = "GeoGraphView/1.0";
	protected int 			mConnectTimeOutMillis = 2000; //2sec
	protected int 			mReadTimeOutMillis = 2000; //2sec


	@Override
	public CompletableFuture<BufferedImage> load(final TileInfo info) {
		final CompletableFuture<BufferedImage> res = new CompletableFuture<>();
		res.complete(syncLoad(info));
		return res;
	}

	public synchronized BufferedImage syncLoad(final TileInfo tile) {
		if (LOG.isTraceEnabled()) LOG.trace("Start downloading of tile {}", tile.getHash());
		final byte[] data = getCompressedTileImage(tile);
		if (data != null) {
			try {
				final BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(data));
				return bimg;
			} catch (final IOException e) {
				e.printStackTrace();
			}finally {
				if (LOG.isTraceEnabled()) LOG.trace("Finish downloading of tile {}", tile.getHash());
			}
		}
		return null;
	}

	public byte[] getCompressedTileImage(final TileInfo tile) {
		int att = mAttemps;
		final URL url = getURL(tile);
		if (url == null)
			return null;
		while(att >= 0) {
			try {
				final byte[] data = getCompressedTileImage(url);
				if (data != null)
					return data;
				throw new NullPointerException("Failed to get Compressed Tile Image for URL: " + url);
			}catch(final SocketTimeoutException ste) {
				LOG.warn("Socket Timedout: " + url);
				mReadTimeOutMillis += 2000;
				if (att == 1) try { Thread.sleep(250); } catch (final InterruptedException e) { e.printStackTrace(); }
			}catch(Exception | Error e) {
				LOG.error("Failed to download tile " + url, e);
				e.printStackTrace();
			}finally {
				att--;
			}
		}
		return null;
	}

	protected abstract URL getURL(final TileInfo tile);

	public byte[] getCompressedTileImage(final URL url) throws IOException {
		if (LOG.isTraceEnabled())
			LOG.trace("Downloading Tile: {}", url);

		final URLConnection connection = url.openConnection();
		connection.setConnectTimeout(mConnectTimeOutMillis);
		connection.setReadTimeout(mReadTimeOutMillis);
		connection.setRequestProperty("User-Agent", mUserAgent);
		final InputStream ins = connection.getInputStream();

		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		final byte[] buf = new byte[256];
		while (true)
		{
			final int n = ins.read(buf);
			if (n == -1)
				break;
			bout.write(buf, 0, n);
		}
		ins.close();

		final byte[] data = bout.toByteArray();
		bout.close();
		return data;
	}


	@Override
	public void free(final TileInfo info, final BufferedImage img) {
		//do nothing - we do not want to delete the internet ... :)
	}

}
