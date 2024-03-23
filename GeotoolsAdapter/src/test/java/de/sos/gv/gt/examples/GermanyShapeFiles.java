package de.sos.gv.gt.examples;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
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
		contentPane.add(mView.getComponent(), BorderLayout.CENTER);
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
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and initializes a
		 * standard TileFactory with 4 threads. For more informations on how to
		 * initialize the Tile Background, see OSMExample
		 */
		final ITileImageProvider cache = ITileFactory.buildCache(ITileImageProvider.OSM, 10, SizeUnit.MegaByte, new File("./.cache"), 100, SizeUnit.MegaByte);
		mView.addHandler(new TileHandler(new TileFactory(cache)));
	}

}
