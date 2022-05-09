package de.sos.gv.gt.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gv.gta.GridCoverage2DItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;

public class GeoTiffExample extends JFrame {

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final GeoTiffExample frame = new GeoTiffExample("GeoTiff Example");
					frame.setVisible(true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private GraphicsScene mScene;
	private GraphicsView mView;
	private ColorModel 		mReplacementColorModel;

	/**
	 * Create the frame.
	 */
	public GeoTiffExample(final String title) {
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

		final GridCoverage2DItem item = configure();

		mReplacementColorModel = createCustomColorTable();
		final JToggleButton tglbtnNewToggleButton = new JToggleButton("Custom Color Table");
		tglbtnNewToggleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				mReplacementColorModel = item.setColorTable(mReplacementColorModel);//grayColorModel(255, 23, 255));
			}
		});
		contentPane.add(tglbtnNewToggleButton, BorderLayout.SOUTH);
	}

	protected ColorModel createCustomColorTable() {
		final Color backgroundColor = Color.green;
		final Color[] cols = new Color[104];
		cols[0] = new Color(255, 255, 255);
		cols[1] = new Color(100, 100, 100);
		cols[2] = new Color(255, 0, 0);
		cols[3] = new Color(0, 255, 0);
		cols[4] = new Color(0, 0, 255);
		cols[5] = new Color(0, 255, 255);
		cols[6] = new Color(255, 0, 255);
		cols[7] = new Color(224, 208, 0); // previously: new Color(245, 245, 0);
		cols[8] = new Color(152, 251, 152);
		cols[9] = new Color(135, 206, 250);
		cols[10] = new Color(255, 165, 0);
		cols[11] = new Color(200, 150, 100);
		cols[12] = new Color(255, 200, 200);
		cols[13] = new Color(170, 170, 170);
		cols[14] = new Color(0, 0, 0);
		cols[15] = new Color(255, 63, 63);
		cols[16] = new Color(255, 127, 127);
		cols[17] = new Color(255, 191, 191);
		cols[101] = new Color(102, 51, 153);
		cols[102] = new Color(153, 102, 204);
		cols[103] = new Color(255, 248, 220);
		final byte[] reds = new byte[256];
		final byte[] greens = new byte[256];
		final byte[] blues = new byte[256];
		final byte[] alphas = new byte[256];
		int numColors = 0;
		for (final Color col : cols)
			if (col != null)
				numColors++;
		int i;
		/*
		 * Note: we're assuming in both cases that an opaque black is already available
		 * from the cols array.
		 */
		if (backgroundColor == null) {
			for (i = 0; i < 256 - numColors; i++) {
				reds[i] = greens[i] = blues[i] = 0;
				alphas[i] = (byte) (i * 0xff / (256 - numColors));
			}
		} else {
			reds[0] = greens[0] = blues[0] = alphas[0] = 0;
			final int red = backgroundColor.getRed();
			final int green = backgroundColor.getGreen();
			final int blue = backgroundColor.getBlue();
			for (i = 1; i < 256 - numColors; i++) {
				final float ratio = (float) i / (255 - numColors);
				reds[i] = (byte) (red * ratio);
				greens[i] = (byte) (green * ratio);
				blues[i] = (byte) (blue * ratio);
				alphas[i] = (byte) 0xff;
			}
		}
		for (final Color col : cols) {
			if (col == null)
				continue;
			reds[i] = (byte) col.getRed();
			greens[i] = (byte) col.getGreen();
			blues[i] = (byte) col.getBlue();
			alphas[i] = (byte) col.getAlpha();
			i++;
		}
		return new IndexColorModel(8, 256, reds, greens, blues, alphas);
	}

	public IndexColorModel grayColorModel(final int window, final float level, final int maxval) {
		int length = window;
		if (maxval > window) {
			length = maxval;
		}

		final byte[] r = new byte[length];
		final byte[] g = new byte[length];
		final byte[] b = new byte[length];

		for (int i = 0; i < length; i++) {
			int val = Math.round(255 / (float) window * (i - level + window * 0.5f));
			if (val > 255) {
				val = 255;
			}
			if (val < 0) {
				val = 0;
			}
			r[ i] = (byte) 0;
			g[ i] = (byte) val;
			b[ i] = (byte) val;
		}
		return new IndexColorModel(8, length, r, g, b);
	}

	// TODO: do the example specific part
	public GridCoverage2DItem configure() {
		final LatLonPoint llp_brhv = new LatLonPoint(45.49321216438442, 12.31682336777468);
		GeoUtils.setViewCenter(mView, llp_brhv);
		mView.setScale(200);

		try {
			//	Image source: http://leoworks.terrasigna.com/sample-data
			final File tiffFile = new File("src/test/resources/Envisat_ASAR_2003-08-04.tif");
			final GridCoverage2DItem item = GridCoverage2DItem.createGeoTiffItem(tiffFile);
			mScene.addItem(item);
			return item;
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void createScene() {
		mScene = new GraphicsScene();
		mView = new GraphicsView(mScene);

		// Standard Handler
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
