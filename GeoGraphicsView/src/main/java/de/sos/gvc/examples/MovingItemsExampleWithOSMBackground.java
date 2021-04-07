package de.sos.gvc.examples;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;

import javax.swing.JFrame;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.slf4j.Logger;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.gt.GeoUtils;
import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.tiles.ITileFactory;
import de.sos.gvc.gt.tiles.ITileProvider;
import de.sos.gvc.gt.tiles.TileHandler;
import de.sos.gvc.gt.tiles.cache.CacheTileFactory;
import de.sos.gvc.gt.tiles.cache.MemoryCache;
import de.sos.gvc.gt.tiles.cache.factories.BufferedImageFactory;
import de.sos.gvc.gt.tiles.cache.factories.ByteDataFactory;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.log.GVLog;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.storage.ListStorage;
import de.sos.gvc.storage.QuadTreeStorage;
import de.sos.gvc.styles.DrawableStyle;

/**
 * 
 * @author scholvac
 *
 */
public class MovingItemsExampleWithOSMBackground {
	
	public static Random			mRandom = new Random(42);
	
	static class RandomItemSimulator {
		GraphicsItem		mItem;
		
		double 				mSOG = mRandom.nextDouble() * 0.1;//[m/s]
		
		
		public RandomItemSimulator(GraphicsItem item) {
			mItem = item;
		}
		
		public void update() {
			float shallMove = mRandom.nextFloat();
			if (shallMove > 0.63f)
				return ;
			//prop of 0.1 to change the course
			float p = mRandom.nextFloat();
			if (p < 0.4f) {
				float dcog = (mRandom.nextFloat() * 100) - 50; // +-5°
				mItem.setRotation(mItem.getRotationDegrees() + dcog);
			}
			// 0.05 to change the speed
			p = mRandom.nextFloat();
			if (p < 0.05f) {
				float dsog = ((mRandom.nextFloat() * 200) - 100) * 100f;
				mSOG += dsog;
				if (mSOG > 1000) mSOG = 1000; if (mSOG < -1000) mSOG = -1000;
			}
			//integrate new position
			double fx = 0, fy = -mSOG, angle_radian = -mItem.getRotationRadians();
			double cos = Math.cos(angle_radian);
			double sin = Math.sin(angle_radian);
			double xx = fx * cos + fy * sin;
			double yy = fx * -sin + fy * cos;
			
			double x = mItem.getCenterX() + xx;
			double y = mItem.getCenterY() + yy;
			//check if we are outside of the world (with some margin)
			LatLonPoint llp = GeoUtils.getLatLon(x, y);
			if (Math.abs(llp.getLatitude()) > 80) 
				y = 0;
			if (Math.abs(llp.getLongitude()) > 170)
				x = 0;
			mItem.setCenter(x, y);
		} 
	}
	
	
	/**
	 * Create a simple item, that draws its shape (triangle) and has a random color 
	 * @param width
	 * @param height
	 * @return
	 */
	public static GraphicsItem createItem(double width, double height) {
		GraphicsItem item = new GraphicsItem();
		double w2 = width / 2, h2 = height / 2;
		Path2D p = new Path2D.Double();
		p.moveTo(-w2, -h2);
		p.lineTo(0, h2);
		p.lineTo(w2, -h2);
		p.lineTo(-w2, -h2);
		p.closePath();
		item.setShape(p);
		
		DrawableStyle style = new DrawableStyle();
		style.setFillPaint(new Color(mRandom.nextFloat(), mRandom.nextFloat(), mRandom.nextFloat()));
		style.setLinePaint(Color.BLACK);
		item.setStyle(style);
		
		return item;
	}
	
	
	
	public static void main(String[] args) {
		GVLog.getInstance().initialize();
		Logger l = GVLog.getLogger(Logger.ROOT_LOGGER_NAME);
		Enumeration app = org.apache.log4j.Logger.getRootLogger().getAllAppenders();
		while(app.hasMoreElements()){
			Object obj = app.nextElement();
			if (obj instanceof Appender){
				Appender a = (Appender)obj;
				GVLog.getInstance().changeLogLevel(a, Level.INFO);
			}
		}
		//Create a new Scene and a new View 
		//Advanced: Try out different Storage strategies (QuadTree or List Storage) 
//		GraphicsScene scene = new GraphicsScene(new QuadTreeStorage());
		GraphicsScene scene = new GraphicsScene(new ListStorage());
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
		ITileProvider<OSMTileDescription> webCache = new OSMTileFactory();
		ITileProvider<OSMTileDescription> byteCache = new MemoryCache<>(new ByteDataFactory<>(), webCache);
		ITileProvider<OSMTileDescription> memImgcache = new MemoryCache<>(new BufferedImageFactory<>(), byteCache);
		ITileFactory<OSMTileDescription> factory = new CacheTileFactory<>(memImgcache, 8);
		
		view.addHandler(new TileHandler(factory));
		
		view.setScale(20);
		
		//create a number of items and simulations that shall be drawn to the view
		double itemWith = 60;
		double itemHeight = 100;
		double xMargin = 100, yMargin = 100;
		int numX = 200;
		int numY = 200;
		
		ArrayList<RandomItemSimulator>	simulators = new ArrayList<>();
		double xStep = itemWith + xMargin / 2.0;
		double xStart = (-0.5 * numX) * (xStep);
		double yStep = itemHeight + yMargin / 2.0;
		double yStart = (-0.5 * numY) * (yStep);
		int ic = 0;
		for (int ix = 0; ix < numX; ix++) {
			double x = xStart + xStep * ix;
			for (int iy = 0; iy < numY; iy++) {
				double y = yStart + yStep * iy;;
				GraphicsItem item = createItem(itemWith, itemHeight);
				item.setCenter(x,y);
				scene.addItem(item);
				simulators.add(new RandomItemSimulator(item)); //create a simulator that moves the item randomly
				System.out.println(ic++);
			}
		}
		
		Thread t = new Thread() {
			@Override
			public void run() {
				while(true) {
					simulators.parallelStream().forEach(ris -> ris.update());
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
		
		
		
		JFrame frame = new JFrame("Moving Items with OSM Background");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);
		
//		Used for profiling
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					Thread.sleep(60*1000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.exit(1);
//			}
//		}.start();
		
	}

}
