package de.sos.gv.geo.examples;
import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gv.geo.tiles.cache.ThreadedTileProvider;
import de.sos.gv.geo.tiles.downloader.WMSImageProvider;
import de.sos.gv.geo.tiles.downloader.WMSImageProvider.WMSVersion;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;


/**
 *
 * @author scholvac
 *
 */
public class WMSExample {

	public static void main(final String[] args) {

		//Create a new Scene and a new View
		final GraphicsScene scene = new GraphicsScene();
		final GraphicsView view = new GraphicsView(scene);


		//Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());


		final ITileImageProvider webCache =	new ThreadedTileProvider(() -> new WMSImageProvider("https://wms.sevencs.com", WMSVersion.VERSION_1_1_1));
		//		WMSOptions WMS =new WMSOptions("10.53.1.24:8484", WMSVersion.VERSION_1_3_0);
		/**
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and initializes a standard TileFactory with 4 threads.
		 * For more informations on how to initialize the Tile Background, see OSMExample
		 */
		final ITileImageProvider cache = ITileFactory.buildCache(webCache, 10, SizeUnit.MegaByte, new File("./.cache"), 100, SizeUnit.MegaByte);
		final TileFactory factory = new TileFactory(cache);
		view.addHandler(new TileHandler(factory));

		final LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);
		GeoUtils.setViewCenter(view, llp_brhv);
		view.setScale(20);

		final JFrame frame = new JFrame("WMSExample");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view.getComponent(), BorderLayout.CENTER);
		frame.setVisible(true);
	}

}
