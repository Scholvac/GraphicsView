package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gv.geo.tiles.cache.ThreadedTileProvider;
import de.sos.gv.geo.tiles.downloader.DefaultTileDownloader;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;

public class MapOverlays extends JFrame {

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				final MapOverlays frame = new MapOverlays("Map Overlay");
				frame.setVisible(true);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		});
	}

	private GraphicsScene 		mScene;
	private GraphicsView		mView;


	/**
	 * Create the frame.
	 */
	public MapOverlays(final String title) {
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

	// TODO: do the example specific part
	public void configure() {
		final LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);
		GeoUtils.setViewCenter(mView, llp_brhv);
		mView.setScale(20);
	}



	private void createScene() {
		mScene = new GraphicsScene();
		mView = new GraphicsView(mScene);

		//Standard Handler
		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());

		//		setupMapSea();
		setupMapAir();
	}
	private void setupMapSea() {
		/**
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and initializes a
		 * standard TileFactory with 4 threads. For more informations on how to
		 * initialize the Tile Background, see OSMExample
		 */
		final Supplier<ITileImageProvider>		osmBaseProvider = () -> new DefaultTileDownloader("https://tile.openstreetmap.org/{z}/{x}/{y}.png");
		final ITileImageProvider baseProvider = ITileFactory.buildCache(new ThreadedTileProvider(osmBaseProvider), 10, SizeUnit.MegaByte, null, 100, SizeUnit.MegaByte);
		mView.addHandler(new TileHandler(new TileFactory(baseProvider)));

		final Supplier<ITileImageProvider>		seachartOverlayProvider = () -> new DefaultTileDownloader("https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png");
		final ITileImageProvider cache2 = ITileFactory.buildCache(new ThreadedTileProvider(seachartOverlayProvider), 10, SizeUnit.MegaByte, null, 100, SizeUnit.MegaByte);
		final TileFactory overlayFactory = new TileFactory(cache2);
		overlayFactory.setZOrder(11);
		final TileHandler overlayHandler = new TileHandler(overlayFactory);
		mView.addHandler(overlayHandler);
	}

	private void setupMapAir() {
		/**
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and initializes a
		 * standard TileFactory with 4 threads. For more informations on how to
		 * initialize the Tile Background, see OSMExample
		 */

		final Supplier<ITileImageProvider>		osmBaseProvider = () -> new DefaultTileDownloader("https://nwy-tiles-api.prod.newaydata.com/tiles/{z}/{x}/{y}.jpg?path=2403/base/latest");
		final ITileImageProvider baseProvider = ITileFactory.buildCache(new ThreadedTileProvider(osmBaseProvider), 10, SizeUnit.MegaByte, null, 100, SizeUnit.MegaByte);
		mView.addHandler(new TileHandler(new TileFactory(baseProvider)));

		final Supplier<ITileImageProvider>		seachartOverlayProvider = () -> new DefaultTileDownloader("https://nwy-tiles-api.prod.newaydata.com/tiles/{z}/{x}/{y}.png?path=2403/aero/latest");
		final ITileImageProvider cache2 = ITileFactory.buildCache(new ThreadedTileProvider(seachartOverlayProvider), 10, SizeUnit.MegaByte, null, 100, SizeUnit.MegaByte);
		final TileFactory overlayFactory = new TileFactory(cache2);
		overlayFactory.setZOrder(11);
		final TileHandler overlayHandler = new TileHandler(overlayFactory);
		mView.addHandler(overlayHandler);
	}

}
