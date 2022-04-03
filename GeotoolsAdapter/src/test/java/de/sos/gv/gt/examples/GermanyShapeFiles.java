package de.sos.gv.gt.examples;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gv.geo.tiles.cache.FileCache;
import de.sos.gv.geo.tiles.cache.ITileCache;
import de.sos.gv.geo.tiles.cache.ImageCache;
import de.sos.gv.geo.tiles.cache.impl.AbstractTileCacheCascade;
import de.sos.gv.gta.FeatureReader;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.styles.DrawableStyle;

public class GermanyShapeFiles extends JFrame {

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final GermanyShapeFiles frame = new GermanyShapeFiles("ShapeFiles");
					frame.setVisible(true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private GraphicsScene 		mScene;
	private GraphicsView		mView;


	/**
	 * Create the frame.
	 * @throws IOException
	 */
	public GermanyShapeFiles(final String title) throws IOException {
		super(title);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setBounds(100, 100, 800, 800);
		final JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		contentPane.setLayout(new BorderLayout());

		createScene();
		contentPane.add(mView, BorderLayout.CENTER);
		configure();
	}

	public void configure() throws IOException {
		final LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);
		GeoUtils.setViewCenter(mView, llp_brhv);
		mView.setScale(5000);

		final File file = new File("src/test/resources/Germany/gadm40_DEU_1.shp");
		final Map<String, Object> map = new HashMap<>();
		map.put("url", file.toURI().toURL());

		final DataStore dataStore = DataStoreFinder.getDataStore(map);
		final String typeName = dataStore.getTypeNames()[0];

		final FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
		final Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")

		final FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
		try (FeatureIterator<SimpleFeature> features = collection.features()) {
			while (features.hasNext()) {
				final SimpleFeature feature = features.next();
				final GraphicsItem item = FeatureReader.createSimpleItem(feature);
				item.setStyle(new DrawableStyle("", Color.BLUE, new BasicStroke(2), new Color(128, 50, 50, 50)));
				mScene.addItem(item);
				//				break;
			}
		}catch(final Exception e) {
			e.printStackTrace();
		}
	}



	private void createScene() {
		mScene = new GraphicsScene();
		mView = new GraphicsView(mScene);

		//Standard Handler
		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());

		setupMap();
	}
	private void setupMap() {
		/**
		 * Create new TileFactory Hierarchie, using different caches that shall speedup the loading and reuse of downloaded tiles.
		 * The last cache entry is the actual downloader (web-cache). Since that may is the most time consuming part, the web cache
		 * is provided through a supplier and instanciiated for each TileFactory thread.
		 */
		final Supplier<ITileImageProvider> webCache = ITileImageProvider.OSM; //may be initiated multiple times through the TileFactory
		final AbstractTileCacheCascade fileCache = new FileCache(new File("./.cache"), 100*1024*1024); // store up to 100MB into the local directory ./.cache
		final AbstractTileCacheCascade imageCache = new ImageCache(10*1024*1024); //hold up to 10MB images in RAM
		final ITileCache cacheCascade = ITileCache.build(webCache, imageCache, fileCache);
		/** A TileFactory is used to calculate the required tiles, to cover an area (defined through the GraphicsView)
		 * and to manage multiple threads to access / download the images. It is also part of the TileFactory's job to
		 * create the TileItem instances and assign the image to them.
		 */
		final TileFactory factory = new TileFactory(cacheCascade, "TileFactoryWorker", 4); //create a tile factory with up to 4 threads.
		/**
		 * The TileHandler is a IGraphicsViewHandler and is asked on every repaint to check if new tiles are required.
		 * To increase the performance, the TileHandler ensures to not reload already visible tiles. However the TileHandler
		 * only holds the currently visible (or partial visible) TileItems.
		 */
		mView.addHandler(new TileHandler(factory)); //add a TileHandler to the view, to manage the currently visible tiles on each repaint.

	}

}
