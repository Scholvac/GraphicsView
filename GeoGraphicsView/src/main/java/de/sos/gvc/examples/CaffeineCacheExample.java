package de.sos.gvc.examples;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.gt.GeoUtils;
import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.tiles.TileHandler;
import de.sos.gvc.gt.tiles.cache.MultiCacheFactory;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileDownloader;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.param.ParameterContext;


/**
 * Shows another cache (compared to OSMExample) that makes uses of Caffeine caching library. 
 * @author scholvac
 *
 */
public class CaffeineCacheExample {
	
	
	public static void main(String[] args) throws IOException {
		
		//Create a new Scene and a new View 
		GraphicsScene scene = new GraphicsScene();
		GraphicsView view = new GraphicsView(scene, new ParameterContext());
		
		
		//Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());
	
	
		MultiCacheFactory<OSMTileDescription>	factory = new MultiCacheFactory<>(
					new OSMTileFactory(),										//tile factory, to calculate the required tile to fill the view area 
					new OSMTileDownloader(), 									//OSM loader that loads the tiles from OSM server - note this loader may be used by different threads
					new File(System.getProperty("user.home")+"/.OSMCache"),		//directory to store downloaded files into, may be null 
					100*1024*1024, 												//size of stored images on disk. if the folder above exceeds this size the cache will start to delete files from the folder, may be <= 0 to disable the writing on disk
					20*1024*1024,												//the MultiCacheFactory stores an amount of images as compressed (PNG - encoded) byte array. This factor defines the amount in bytes that shall be keept in memory 
					20*1024*1024												//besides the byte array, the cache also hold some images as BufferedImages in memory, thats the size in bytes
				);
		view.addHandler(new TileHandler(factory));
		
		LatLonPoint llp_brhv = new LatLonPoint.Double(53.523495, 8.641542);
		GeoUtils.setViewCenter(view, llp_brhv);
		view.setScale(0.6);
		
		JFrame frame = new JFrame("Caffeine Cache Example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);		
	}

}
