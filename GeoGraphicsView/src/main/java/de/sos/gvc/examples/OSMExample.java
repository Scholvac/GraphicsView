package de.sos.gvc.examples;
import java.awt.BorderLayout;

import javax.swing.JFrame;

import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.gt.GeoUtils;
import de.sos.gvc.gt.TileHandler;
import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.tiles.ITileFactory;
import de.sos.gvc.gt.tiles.TileFactory;
import de.sos.gvc.gt.tiles.cache.MemoryCache;
import de.sos.gvc.gt.tiles.cache.factories.BufferedImageFactory;
import de.sos.gvc.gt.tiles.cache.factories.ByteDataFactory;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.param.ParameterContext;


/**
 * 
 * @author scholvac
 *
 */
public class OSMExample {
	
	public static void main(String[] args) {
		
		//Create a new Scene and a new View 
		GraphicsScene scene = new GraphicsScene();
		GraphicsView view = new GraphicsView(scene, new ParameterContext());
		
		
		//Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());
		
		/**
		 * Create new TileFactory Hierarchie, using different caches that shall speedup the loading and reuse of downloaded tiles. 
		 * The most deepest backup solution will be the OSMTileFactory, that downloads tiles from the default OSM server (https://tile.openstreetmap.org/). 
		 * The first real cache - byteCache - stores the downloaded image as compressed byte[], that is much smaller as the uncompressed BufferedImage. However
		 * to use the data, it first has to be uncompressed into an image. 
		 * The last cache - memImgCache - stores an uncompressed image that can directly be reused and thus is the fastest cache. On the other side it consumes the 
		 * most amount of memory. 
		 * 
		 * @see MemoryCache and OSMTileFactory
		 */
		ITileFactory<OSMTileDescription> webCache = new OSMTileFactory();
		ITileFactory<OSMTileDescription> byteCache = new MemoryCache<>(new ByteDataFactory<>(), webCache, 20*1024*1024, "Byte Cache");
		ITileFactory<OSMTileDescription> memImgcache = new MemoryCache<>(new BufferedImageFactory<>(), byteCache, 2*1024*1024, "Image Cache");
		TileFactory<OSMTileDescription> factory = new TileFactory<>(memImgcache, 8);
		
		view.addHandler(new TileHandler(factory));
		
		LatLonPoint llp_brhv = new LatLonPoint.Double(53.523495, 8.641542);
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
