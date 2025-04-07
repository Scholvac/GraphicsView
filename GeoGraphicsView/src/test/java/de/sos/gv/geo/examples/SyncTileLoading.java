package de.sos.gv.geo.examples;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;

public class SyncTileLoading extends JFrame {

	/** Commonad line application to load a map and save it to a image file
	 * @throws IOException */
	public static void main(final String[] args) throws IOException {

		final BufferedImageRenderTarget rt = new BufferedImageRenderTarget(500, 500, false);
		final GraphicsScene scene = new GraphicsScene();
		final GraphicsView view = new GraphicsView(scene, rt);
		view.enableRepaintTrigger(false); //no need to paint on every change.

		final TileHandler tileHandler = new TileHandler(new TileFactory(ITileImageProvider.OSM, 1)); //note: We use no cache to slow down the receiving of image files from the web.
		//enable waiting until all tiles have been downloaded. This method waits without a timeout.
		//However it is also possible to specify a timeout value (tileHandler.waitForAllTiles(5, TimeUnit.SECOND);).
		//If not all tiles could be loaded, the flag: tileHandler.isComplete() will return false.
		tileHandler.waitForAllTiles(true);
		view.addHandler(tileHandler);


		final LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);
		GeoUtils.setViewCenter(view, llp_brhv);
		view.setScale(200);

		rt.requestRepaint();
		if (false == tileHandler.isComplete())
			System.err.println("Failed to download all tiles in time");

		ImageIO.write(rt.getImage(), "PNG", new File("SyncTileLoading.png"));

	}
}
