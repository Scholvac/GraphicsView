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
import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gv.geo.tiles.cache.FileCache;
import de.sos.gv.geo.tiles.cache.MemoryCache;
import de.sos.gv.geo.tiles.cache.ThreadedTileProvider;
import de.sos.gv.geo.tiles.downloader.DefaultTileDownloader;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsViewComponent;
import de.sos.gvc.IGraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.storage.ListStorage;

public class TileCache2Example extends JFrame {

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final TileCache2Example frame = new TileCache2Example("Tile Cache");
					frame.setVisible(true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private GraphicsScene			mScene;
	private GraphicsViewComponent	mView;

	/**
	 * Create the frame.
	 */
	public TileCache2Example(final String title) {
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
		mView.getProperty(IGraphicsView.PROP_VIEW_SCALE_X).addPropertyChangeListener(pcl -> System.out.println(mView.getScaleX()));

		new Thread() {
			@Override
			public void run() {
				double s = 0.1;
				while (s < 41400) {
					s *= 1.1;
					mView.setScale(s);
					try {
						Thread.sleep(250);
					} catch (final Exception e) {
					}
				}
				while (s > 0.1) {
					s *= 0.9;
					mView.setScale(s);
					try {
						Thread.sleep(250);
					} catch (final Exception e) {
					}
				}
			}
		}.start();
	}

	private void createScene() {
		mScene = new GraphicsScene(new ListStorage(false));
		mView = new GraphicsViewComponent(mScene);

		// Standard Handler
		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());

		setupMap();
	}

	private void setupMap() {
		final Supplier<ITileImageProvider>	webCache	= () -> new DefaultTileDownloader("https://tile.openstreetmap.org/{z}/{x}/{y}.png");
		final ITileImageProvider			cached		= buildCache(webCache);

		final ITileFactory					factory		= new TileFactory(cached, "TileFactoryWorker", 8);
		mView.addHandler(new TileHandler(factory));

	}

	private ITileImageProvider buildCache(final Supplier<ITileImageProvider> webCache) {
		final ThreadedTileProvider	osm	= new ThreadedTileProvider(webCache);
		final FileCache				fc	= new FileCache(osm, new File("cache/"), 8, SizeUnit.MegaByte);
		final MemoryCache			mc	= new MemoryCache(fc, 1, SizeUnit.MegaByte);
		return mc;
	}

}
