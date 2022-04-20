package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.Path2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.storage.ListStorage;
import de.sos.gvc.styles.DrawableStyle;

/**
 *
 * @author scholvac
 *
 */
public class MovingItemsExampleWithOSMBackground {

	public static Random mRandom = new Random(42);

	static class RandomItemSimulator {
		GraphicsItem mItem;

		double mSOG = mRandom.nextDouble() * 0.1;// [m/s]

		public RandomItemSimulator(final GraphicsItem item) {
			mItem = item;
		}

		public void update() {
			final float shallMove = mRandom.nextFloat();
			if (shallMove > 0.63f)
				return;
			// prop of 0.1 to change the course
			float p = mRandom.nextFloat();
			if (p < 0.4f) {
				final float dcog = mRandom.nextFloat() * 100 - 50; // +-5°
				mItem.setRotation(mItem.getRotationDegrees() + dcog);
			}
			// 0.05 to change the speed
			p = mRandom.nextFloat();
			if (p < 0.05f) {
				final float dsog = (mRandom.nextFloat() * 200 - 100) * 100f;
				mSOG += dsog;
				if (mSOG > 1000)
					mSOG = 1000;
				if (mSOG < -1000)
					mSOG = -1000;
			}
			// integrate new position
			final double fx = 0, fy = -mSOG, angle_radian = -mItem.getRotationRadians();
			final double cos = Math.cos(angle_radian);
			final double sin = Math.sin(angle_radian);
			final double xx = fx * cos + fy * sin;
			final double yy = fx * -sin + fy * cos;

			double x = mItem.getCenterX() + xx;
			double y = mItem.getCenterY() + yy;
			// check if we are outside of the world (with some margin)
			final LatLonPoint llp = GeoUtils.getLatLon(x, y);
			if (Math.abs(llp.getLatitude()) > 80)
				y = 0;
			if (Math.abs(llp.getLongitude()) > 170)
				x = 0;
			mItem.setCenter(x, y);
		}
	}

	/**
	 * Create a simple item, that draws its shape (triangle) and has a random color
	 *
	 * @param width
	 * @param height
	 * @return
	 */
	public static GraphicsItem createItem(final double width, final double height) {
		final GraphicsItem item = new GraphicsItem();
		final double w2 = width / 2, h2 = height / 2;
		final Path2D p = new Path2D.Double();
		p.moveTo(-w2, -h2);
		p.lineTo(0, h2);
		p.lineTo(w2, -h2);
		p.lineTo(-w2, -h2);
		p.closePath();
		item.setShape(p);

		final DrawableStyle style = new DrawableStyle();
		style.setFillPaint(new Color(mRandom.nextFloat(), mRandom.nextFloat(), mRandom.nextFloat()));
		style.setLinePaint(Color.BLACK);
		item.setStyle(style);

		return item;
	}

	public static void main(final String[] args) {
		// Create a new Scene and a new View
		// Advanced: Try out different Storage strategies (QuadTree or List Storage)
		// GraphicsScene scene = new GraphicsScene(new QuadTreeStorage());
		final GraphicsScene scene = new GraphicsScene(new ListStorage());
		final GraphicsView view = new GraphicsView(scene, new ParameterContext());

		// Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());

		/**
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and initializes a
		 * standard TileFactory with 4 threads. For more informations on how to
		 * initialize the Tile Background, see OSMExample
		 */
		final ITileImageProvider cache = ITileFactory.buildCache(ITileImageProvider.OSM, 10, SizeUnit.MegaByte, new File("./.cache"), 100, SizeUnit.MegaByte);
		view.addHandler(new TileHandler(new TileFactory(cache)));

		view.setScale(20);

		// create a number of items and simulations that shall be drawn to the view
		final double itemWith = 60;
		final double itemHeight = 100;
		final double xMargin = 100, yMargin = 100;
		final int numX = 200;
		final int numY = 200;

		final ArrayList<RandomItemSimulator> simulators = new ArrayList<>();
		final double xStep = itemWith + xMargin / 2.0;
		final double xStart = -0.5 * numX * xStep;
		final double yStep = itemHeight + yMargin / 2.0;
		final double yStart = -0.5 * numY * yStep;
		int ic = 0;
		for (int ix = 0; ix < numX; ix++) {
			final double x = xStart + xStep * ix;
			for (int iy = 0; iy < numY; iy++) {
				final double y = yStart + yStep * iy;

				final GraphicsItem item = createItem(itemWith, itemHeight);
				item.setCenter(x, y);
				scene.addItem(item);
				simulators.add(new RandomItemSimulator(item)); // create a simulator that moves the item randomly
				System.out.println(ic++);
			}
		}

		final Thread t = new Thread() {
			@Override
			public void run() {
				while (true) {
					simulators.parallelStream().forEach(RandomItemSimulator::update);
					try {
						Thread.sleep(200);
					} catch (final InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();

		final JFrame frame = new JFrame("Moving Items with OSM Background");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);

		// Used for profiling
		// new Thread() {
		// @Override
		// public void run() {
		// try {
		// Thread.sleep(60*1000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// System.exit(1);
		// }
		// }.start();

	}

}
