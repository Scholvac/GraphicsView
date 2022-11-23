package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.geom.Path2D;
import java.io.File;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsViewComponent;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.styles.DrawableStyle;

public class MultiThreadUpdates extends JFrame {

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final MultiThreadUpdates frame = new MultiThreadUpdates("Example Template");
					frame.setVisible(true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private GraphicsScene			mScene;
	private GraphicsViewComponent	mView;

	public static final int			sBound_TTS			= 25;
	public static final double		sItemWidth			= 10;
	public static final double		sItemHeight			= 15;
	public static final int			sMaxItemCount		= 500;
	public static final int			sMaxSubItemCount	= 1000;
	private static int				sCounter			= 1;

	/**
	 * Create the frame.
	 */
	public MultiThreadUpdates(final String title) {
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

	private static ExecutorService es = Executors.newFixedThreadPool(30);

	// TODO: do the example specific part
	public void configure() {
		final LatLonPoint llp_brhv = new LatLonPoint(0, 0);
		GeoUtils.setViewCenter(mView, llp_brhv);
		mView.setScale(20);

		for (int i = 0; i < sMaxItemCount; i++) {
			es.execute(new ItemThread());
		}
	}

	public class ItemThread implements Runnable {

		final int		id	= sCounter++;
		final Random	rng	= new Random(id);
		GraphicsItem	item;
		private double	dx, dy, v, x, y;

		@Override
		public void run() {
			if (item == null) {
				item = createItem();
				mScene.addItem(item);
				dx = 20. * rng.nextDouble() - 10;
				dy = 20. * rng.nextDouble() - 10;
				v = rng.nextDouble() * 0.15;

				x = rng.nextDouble();
				y = rng.nextDouble();
				item.setCenter(x, y);
			}

			final int tts = rng.nextInt(sBound_TTS);
			try {
				Thread.sleep(tts);
			} catch (final Exception e) {
			}

			x += dx * tts * v;
			y += dy * tts * v;
			final GraphicsItem child = createItem();
			item.addItem(child);
			child.setSceneLocation(x, y);
			if (item.getChildren().size() < sMaxSubItemCount)
				es.execute(this);
		}

		private GraphicsItem createItem() {
			final GraphicsItem	item	= new GraphicsItem();
			final double		w2		= sItemWidth / 2, h2 = sItemHeight / 2;
			final Path2D		p		= new Path2D.Double();
			p.moveTo(-w2, -h2);
			p.lineTo(0, h2);
			p.lineTo(w2, -h2);
			p.lineTo(-w2, -h2);
			p.closePath();
			item.setShape(p);

			final DrawableStyle style = new DrawableStyle();
			style.setFillPaint(new Color(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()));
			style.setLinePaint(Color.BLACK);
			item.setStyle(style);

			return item;
		}
	}

	private void createScene() {
		mScene = new GraphicsScene();
		mView = new GraphicsViewComponent(mScene);

		// Standard Handler
		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());

		setupMap();
	}
	private void setupMap() {
		/**
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and
		 * initializes a standard TileFactory with 4 threads. For more
		 * informations on how to initialize the Tile Background, see OSMExample
		 */
		final ITileImageProvider cache = ITileFactory.buildCache(ITileImageProvider.OSM, 10, SizeUnit.MegaByte, new File("./.cache"), 100, SizeUnit.MegaByte);
		mView.addHandler(new TileHandler(new TileFactory(cache)));
	}

}
