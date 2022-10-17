package de.sos.gv.geo.tiles.downloader;

import java.awt.image.BufferedImage;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gvc.log.GVLog;

/** Provides Images from nextzen terrain (terrarium) images
 *  @see <a href="https://github.com/tilezen/joerd/blob/master/docs/formats.md">Terrarium description</a>
 **/
public class TerrariumImageProvider extends AbstractTileDownloader {

	private static final Logger					LOG = GVLog.getLogger(TerrariumImageProvider.class);

	public enum DownloadMode {
		RAW,
		DECODED
	}


	static class StretchColorTable extends LookupTable {
		private static final int[] BLACK = {0, 0, 0, 255};
		private static final int[] WHITE = {255, 255, 255, 255};
		private final float MAX_DIFF = 8840 - -10916; //mt. everest -> marianen graben
		private float mMin;
		private float mMax;
		private float mDiff;

		protected StretchColorTable(final float minHeight, final float maxHeight) {
			super(0, 4);
			mMin = minHeight;
			mMax = maxHeight;

			mDiff = maxHeight - minHeight;
		}
		public void setMinScale(final float scale) { mMin = scale; mDiff = mMax - mMin; }
		public void setMaxScale(final float scale) { mMax = scale; mDiff = mMax - mMin; }

		@Override
		public int[] lookupPixel(final int[] src, final int[] dest) {
			final float raw = src[0] * 256.f + src[1] + src[2] / 256.f - 32768.f;
			if (raw <= mMin) {
				System.arraycopy(BLACK, 0, dest, 0, 4);
				return dest;
			}
			if (raw >= mMax) {
				System.arraycopy(WHITE, 0, dest, 0, 4);
				return dest;
			}
			final int val = (int)(mDiff * (raw + mMin) / 256f);
			dest[0] = val;
			dest[1] = val;
			dest[2] = val;
			dest[3] = 255;//alpha
			return dest;
		}

	}


	private final String mAPIKey;
	private final String mBaseURL;
	private DownloadMode mMode = DownloadMode.DECODED;
	private StretchColorTable mColorTable;
	private LookupOp mLookupOperation;


	public TerrariumImageProvider(final String apiKey, final float minScale, final float maxScale) {
		this(apiKey, "https://tile.nextzen.org/tilezen/terrain/v1/256/terrarium/{z}/{x}/{y}.png", minScale, maxScale); //only tilesize = 256 is still online.
	}
	public TerrariumImageProvider(final String apiKey, final String baseURL, final float minScale, final float maxScale) {
		mBaseURL = baseURL;
		mAPIKey = apiKey;
		if (mAPIKey == null || mAPIKey.isEmpty()) {
			throw new NullPointerException("Please enter a valid Mapzen API-key. A key may be generated here https://developers.nextzen.org/ (for free)" );
		}
		mLookupOperation = new LookupOp(mColorTable = new StretchColorTable(minScale, maxScale), null);
	}

	public void setMinScale(final float scale) { mColorTable.setMinScale(scale); }
	public void setMaxScale(final float scale) { mColorTable.setMaxScale(scale);}
	public String getBaseURL() {return mBaseURL;}
	public String getAPIKey() { return mAPIKey;}

	@Override
	protected URL getURL(final TileInfo tile) {
		final String urlStr = mBaseURL
				.replace("{x}", ""+tile.tileX())
				.replace("{y}", ""+tile.tileY())
				.replace("{z}", ""+tile.tileZ())
				.concat("?api_key=")
				.concat(getAPIKey());
		try {
			return new URL(urlStr);
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public synchronized BufferedImage syncLoad(final TileInfo tile) {
		if (mMode == DownloadMode.RAW)
			return super.syncLoad(tile);

		if (LOG.isTraceEnabled()) LOG.trace("Start downloading of tile {}", tile.getHash());
		final byte[] data = getCompressedTileImage(tile);
		if (data != null) {
			try {
				final BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(data));
				return mLookupOperation.filter(bimg, null);
			} catch (final IOException e) {
				e.printStackTrace();
			}finally {
				if (LOG.isTraceEnabled()) LOG.trace("Finish downloading of tile {}", tile.getHash());
			}
		}
		return null;
	}

}
