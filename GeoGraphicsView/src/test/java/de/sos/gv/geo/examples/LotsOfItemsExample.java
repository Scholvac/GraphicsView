package de.sos.gv.geo.examples;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.Path2D;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.storage.QuadTreeStorage;
import de.sos.gvc.styles.DrawableStyle;


/**
 *
 * @author scholvac
 *
 */
public class LotsOfItemsExample {

	public static Random			mRandom = new Random(42);

	/**
	 * Create a simple item, that draws its shape (triangle) and has a random color
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

		//Create a new Scene and a new View
		final GraphicsScene scene = new GraphicsScene(new QuadTreeStorage());
		//		GraphicsScene scene = new GraphicsScene(new ListStorage());
		final GraphicsView view = new GraphicsView(scene, new ParameterContext());


		//Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());



		view.setScale(50);

		//create a number of items and simulations that shall be drawn to the view
		final double itemWith = 100;
		final double itemHeight = 100;
		final double xMargin = 100, yMargin = 100;
		final int numX = 200;
		final int numY = 200;

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
				item.setCenter(x,y);
				scene.addItem(item);
				System.out.println(ic++);
			}
		}


		final JFrame frame = new JFrame("Huge Amount of Items (" + ic + ")");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);

	}

}
