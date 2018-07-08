package de.sos.gvc.examples;
import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Weigher;

import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.gt.GeoUtils;
import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.tiles.ITileDescription;
import de.sos.gvc.gt.tiles.ITileFactory;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;
import de.sos.gvc.gt.tiles.LazyTileItem;
import de.sos.gvc.gt.tiles.TileHandler;
import de.sos.gvc.gt.tiles.cache.MultiCacheFactory;
import de.sos.gvc.gt.tiles.cache.MultiCacheFactory.IByteTileLoader;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileDownloader;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.log.GVLog;
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
