package de.sos.gv.geo.examples;
import java.awt.BorderLayout;
import java.io.File;
import java.util.function.Supplier;

import javax.swing.JFrame;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gv.geo.tiles.cache.FileCache;
import de.sos.gv.geo.tiles.cache.ITileCache;
import de.sos.gv.geo.tiles.cache.ImageCache;
import de.sos.gv.geo.tiles.impl.WMSImageProvider;
import de.sos.gv.geo.tiles.impl.WMSImageProvider.WMSVersion;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.param.ParameterContext;


/**
 *
 * @author scholvac
 *
 */
public class WMSExample {

	public static void main(String[] args) {

		//Create a new Scene and a new View
		GraphicsScene scene = new GraphicsScene();
		GraphicsView view = new GraphicsView(scene, new ParameterContext());


		//Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());


		Supplier<ITileImageProvider> webCache =	() -> new WMSImageProvider("https://wms.sevencs.com", WMSVersion.VERSION_1_1_1);
		//		WMSOptions WMS =new WMSOptions("10.53.1.24:8484", WMSVersion.VERSION_1_3_0);
		/**
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and initializes a standard TileFactory with 4 threads.
		 * For more informations on how to initialize the Tile Background, see OSMExample
		 */
		ITileCache cache = ITileCache.build(webCache, new ImageCache(10*1024*1024), new FileCache(new File("./.wms_cache"), 100*1024*1024));
		TileFactory factory = new TileFactory(cache);
		view.addHandler(new TileHandler(factory));

		LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);
		GeoUtils.setViewCenter(view, llp_brhv);
		view.setScale(20);

		JFrame frame = new JFrame("WMSExample");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);
	}

}