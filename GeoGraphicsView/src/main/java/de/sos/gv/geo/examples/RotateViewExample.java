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

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gv.geo.tiles.cache.FileCache;
import de.sos.gv.geo.tiles.cache.ITileCache;
import de.sos.gv.geo.tiles.cache.ImageCache;
import de.sos.gv.geo.tiles.impl.TileImageProvider;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.styles.DrawableStyle;

public class RotateViewExample {

	public static void main(String[] args) throws IOException {

		// Create a new Scene and a new View
		GraphicsScene scene = new GraphicsScene();
		GraphicsView view = new GraphicsView(scene, new ParameterContext());

		// Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());

		/**
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and initializes a standard TileFactory with 4 threads.
		 * For more informations on how to initialize the Tile Background, see OSMExample
		 */
		ITileCache cache = ITileCache.build(TileImageProvider.OSM, new ImageCache(10*1024*1024), new FileCache(new File("./.cache"), 100*1024*1024));
		view.addHandler(new TileHandler(new TileFactory(cache)));

		LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);
		GeoUtils.setViewCenter(view, llp_brhv);
		view.setScale(3);


		String arrowWKT = "POLYGON ((-100 0, 0 100, 100 0, 50 0, 50 -100, -50 -100, -50 0, -100 0))";
		String sinStarWKT = "POLYGON ((100 0, 61.193502094049606 22.993546861223116, 47.37629522998975 42.01130058032534, 37.5 86.02387002944835, 3.6399700797813024 56.22209406247465, -18.71674025585594 48.9579585314524, -63.62712429686843 53.16567552200251, -45.74727574170163 11.753618188079914, -45.74727574170165 -11.75361818807988, -63.627124296868445 -53.16567552200249, -18.71674025585592 -48.957958531452284, 3.6399700797812438 -56.222094062474724, 37.49999999999998 -86.02387002944836, 47.376295229989815 -42.01130058032546, 61.19350209404953 -22.993546861223134, 100 0))";

		//create a number of items and simulations that shall be drawn to the view
		DrawableStyle style = new DrawableStyle();
		style.setName("default");
		Point2D start = new Point2D.Float(-100, -100);
		Point2D end = new Point2D.Float(100, 100);
		float[] dist = {0.0f, 0.5f, 1.0f};
		Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
		LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
		style.setFillPaint(p);
		style.setLinePaint(Color.BLACK);

		//build also a mixed item to show how it works on item hierarchies
		GraphicsItem mixedItem = new GraphicsItem(ExampleUtils.wkt2Shape(arrowWKT));
		mixedItem.setScale(1, 1);
		mixedItem.setStyle(style);
		GraphicsItem subStar = new GraphicsItem(ExampleUtils.wkt2Shape(sinStarWKT));
		subStar.setCenter(0, 100);
		subStar.setStyle(style);
		subStar.setScale(0.25, 0.25);
		mixedItem.addItem(subStar);
		scene.addItem(mixedItem);

		GeoUtils.setGeoPosition(mixedItem, llp_brhv);


		JFrame frame = new JFrame("Rotate View Example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);

		JPanel rotPanel = new JPanel();
		rotPanel.setLayout(new BorderLayout());
		JButton rLeft = new JButton("<<");
		JButton rRight = new JButton(">>");
		rotPanel.add(rLeft, BorderLayout.WEST);
		rotPanel.add(rRight, BorderLayout.EAST);
		frame.add(rotPanel, BorderLayout.SOUTH);

		rLeft.addActionListener( al -> rotate(view, -5));
		rRight.addActionListener( al -> rotate(view, +5));

		frame.setVisible(true);
	}

	private static void rotate(GraphicsView view, double delta) {
		double newRot = view.getRotationDegrees() + delta;
		view.setRotation(newRot);
	}



}
