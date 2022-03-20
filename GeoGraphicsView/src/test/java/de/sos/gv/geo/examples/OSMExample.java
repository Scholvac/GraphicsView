package de.sos.gv.geo.examples;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
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
import de.sos.gv.geo.tiles.cache.impl.AbstractTileCacheCascade;
import de.sos.gv.geo.tiles.impl.TileImageProvider;
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
public class OSMExample {

	public static void main(String[] args) throws IOException {

		//Create a new Scene and a new View
		GraphicsScene scene = new GraphicsScene();
		GraphicsView view = new GraphicsView(scene, new ParameterContext());


		//Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());

		/**
		 * Create new TileFactory Hierarchie, using different caches that shall speedup the loading and reuse of downloaded tiles.
		 * The last cache entry is the actual downloader (web-cache). Since that may is the most time consuming part, the web cache
		 * is provided through a supplier and instanciiated for each TileFactory thread.
		 */
		Supplier<ITileImageProvider> webCache = TileImageProvider.OSM; //may be initiated multiple times through the TileFactory
		AbstractTileCacheCascade fileCache = new FileCache(new File("./.cache"), 100*1024*1024); // store up to 100MB into the local directory ./.cache
		AbstractTileCacheCascade imageCache = new ImageCache(10*1024*1024); //hold up to 10MB images in RAM
		ITileCache cacheCascade = ITileCache.build(webCache, imageCache, fileCache);
		/** A TileFactory is used to calculate the required tiles, to cover an area (defined through the GraphicsView)
		 * and to manage multiple threads to access / download the images. It is also part of the TileFactory's job to
		 * create the TileItem instances and assign the image to them.
		 */
		TileFactory factory = new TileFactory(cacheCascade, "TileFactoryWorker", 4); //create a tile factory with up to 4 threads.
		/**
		 * The TileHandler is a IGraphicsViewHandler and is asked on every repaint to check if new tiles are required.
		 * To increase the performance, the TileHandler ensures to not reload already visible tiles. However the TileHandler
		 * only holds the currently visible (or partial visible) TileItems.
		 */
		view.addHandler(new TileHandler(factory)); //add a TileHandler to the view, to manage the currently visible tiles on each repaint.

		LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);
		GeoUtils.setViewCenter(view, llp_brhv);
		view.setScale(20);

		JFrame frame = new JFrame("OSMExample");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);
	}

}
