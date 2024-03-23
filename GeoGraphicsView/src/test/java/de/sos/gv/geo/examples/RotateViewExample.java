package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
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
import de.sos.gvc.Utils;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.styles.DrawableStyle;

public class RotateViewExample {

	public static void main(final String[] args) throws IOException {

		// Create a new Scene and a new View
		final GraphicsScene scene = new GraphicsScene();
		final GraphicsView view = new GraphicsView(scene);

		// Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());

		/**
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and initializes a standard TileFactory with 4 threads.
		 * For more informations on how to initialize the Tile Background, see OSMExample
		 */
		final ITileImageProvider cache = ITileFactory.buildCache(ITileImageProvider.OSM, 10, SizeUnit.MegaByte, new File("./.cache"), 100, SizeUnit.MegaByte);
		view.addHandler(new TileHandler(new TileFactory(cache)));

		final LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);
		GeoUtils.setViewCenter(view, llp_brhv);
		view.setScale(3);


		final String arrowWKT = "POLYGON ((-100 0, 0 100, 100 0, 50 0, 50 -100, -50 -100, -50 0, -100 0))";
		final String sinStarWKT = "POLYGON ((100 0, 61.193502094049606 22.993546861223116, 47.37629522998975 42.01130058032534, 37.5 86.02387002944835, 3.6399700797813024 56.22209406247465, -18.71674025585594 48.9579585314524, -63.62712429686843 53.16567552200251, -45.74727574170163 11.753618188079914, -45.74727574170165 -11.75361818807988, -63.627124296868445 -53.16567552200249, -18.71674025585592 -48.957958531452284, 3.6399700797812438 -56.222094062474724, 37.49999999999998 -86.02387002944836, 47.376295229989815 -42.01130058032546, 61.19350209404953 -22.993546861223134, 100 0))";

		//create a number of items and simulations that shall be drawn to the view
		final DrawableStyle style = new DrawableStyle();
		style.setName("default");
		final Point2D start = new Point2D.Float(-100, -100);
		final Point2D end = new Point2D.Float(100, 100);
		final float[] dist = {0.0f, 0.5f, 1.0f};
		final Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
		final LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
		style.setFillPaint(p);
		style.setLinePaint(Color.BLACK);

		//build also a mixed item to show how it works on item hierarchies
		final GraphicsItem mixedItem = new GraphicsItem(Utils.wkt2Shape(arrowWKT));
		mixedItem.setScale(1, 1);
		mixedItem.setStyle(style);
		final GraphicsItem subStar = new GraphicsItem(Utils.wkt2Shape(sinStarWKT));
		subStar.setCenter(0, 100);
		subStar.setStyle(style);
		subStar.setScale(0.25, 0.25);
		mixedItem.addItem(subStar);
		scene.addItem(mixedItem);

		GeoUtils.setGeoPosition(mixedItem, llp_brhv);


		final JFrame frame = new JFrame("Rotate View Example");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view.getComponent(), BorderLayout.CENTER);

		final JPanel rotPanel = new JPanel();
		rotPanel.setLayout(new BorderLayout());
		final JButton rLeft = new JButton("<<");
		final JButton rRight = new JButton(">>");
		rotPanel.add(rLeft, BorderLayout.WEST);
		rotPanel.add(rRight, BorderLayout.EAST);
		frame.add(rotPanel, BorderLayout.SOUTH);

		rLeft.addActionListener( al -> rotate(view, -5));
		rRight.addActionListener( al -> rotate(view, +5));

		frame.setVisible(true);
	}

	private static void rotate(final GraphicsView view, final double delta) {
		final double newRot = view.getRotationDegrees() + delta;
		view.setRotation(newRot);
	}



}
