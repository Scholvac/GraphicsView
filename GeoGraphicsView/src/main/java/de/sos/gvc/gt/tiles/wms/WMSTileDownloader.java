package de.sos.gvc.gt.tiles.wms;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import de.sos.gvc.gt.tiles.ITileLoader;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileDownloader;
import de.sos.gvc.gt.tiles.wms.WMSOptions.WMSVersion;


/**
 * 
 * @author scholvac
 *
 */
public class WMSTileDownloader extends OSMTileDownloader implements ITileLoader<OSMTileDescription>{

	private WMSOptions mOptions;

	public WMSTileDownloader(WMSOptions options) {
		mOptions = options;
	}

	@Override
	public URL getURL(OSMTileDescription desc) {
		try {
			String format = "image/png";
			String styles = "";
			int ts = mOptions.tileSize;
			LatLonBoundingBox bb = desc.getBounds();
			double ulx = bb.getWest();
			double uly = bb.getSouth();
			double lrx = bb.getEast();
			double lry = bb.getNorth();
//			ulx = 53.85; uly=8.45;lrx=54.05;lry=9.0;
			String bbox = ulx + "," + uly + "," + lrx + "," + lry; 
			
			String version = "1.1.1";
			String srs = "&SRS=" + mOptions.srs;
			if (mOptions.version == WMSVersion.VERSION_1_3_0) {
				bbox = uly + "," + ulx + "," + lry + "," + lrx;
				
				version = "1.3.0";
				srs = "&CRS="+mOptions.srs;
			}
			String baseURL = mOptions.baseURL;
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
			if (mOptions.layer != null && mOptions.layer.isEmpty()==false)
				layers = "&LAYERS=" + mOptions.layer;
			
			String url = baseURL + "VERSION="+version+"&REQUEST=" + "GetMap" + layers + srs + "&BBOX="+bbox + "&WIDTH=" + ts + "&HEIGHT=" + ts + "&FORMAT="+format;

//			if (baseURL.toLowerCase().contains("styles") == false) {
//				url += "&Styles=" + styles;
//			}
			return new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public BufferedImage getTileImage(OSMTileDescription tile) {
		BufferedImage bimg = super.getTileImage(tile);
		
		return bimg;// scaleNearest(bimg, 1, -1);
	}
	
	public static BufferedImage scaleNearest(BufferedImage before, double scaleX, double scaleY) {
	    final int interpolation = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
	    return scale(before, scaleX, scaleY, interpolation);
	}

	private static BufferedImage scale(final BufferedImage before, final double scaleX, final double scaleY, final int type) {
	    int w = before.getWidth();
	    int h = before.getHeight();
	    int w2 = (int) (w * scaleX);
	    int h2 = (int) (h * -scaleY);
	    BufferedImage after = new BufferedImage(w2, h2, before.getType());
	    AffineTransform scaleInstance = AffineTransform.getScaleInstance(scaleX, scaleY);
	    AffineTransformOp scaleOp = new AffineTransformOp(scaleInstance, type);
	    scaleOp.filter(before, after);
	    return after;
	}

}
