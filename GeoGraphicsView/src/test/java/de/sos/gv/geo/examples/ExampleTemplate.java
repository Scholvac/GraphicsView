package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.File;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gv.geo.tiles.cache.FileCache;
import de.sos.gv.geo.tiles.cache.ITileCache;
import de.sos.gv.geo.tiles.cache.ImageCache;
import de.sos.gv.geo.tiles.cache.impl.AbstractTileCacheCascade;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;

public class ExampleTemplate extends JFrame {

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final ExampleTemplate frame = new ExampleTemplate("Example Template");
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
	 */
	public ExampleTemplate(final String title) {
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
