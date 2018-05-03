package de.sos.gvc.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.Random;

import javax.swing.JFrame;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.drawables.DrawableStyle;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.handler.SelectionHandler;
import de.sos.gvc.log.GVLog;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.storage.QuadTreeStorage;


/**
 * 
 * @author scholvac
 *
 */
public class SelectionExample {
	
	public static Random			mRandom = new Random(4242);	
	
	public static void main(String[] args) {
		GVLog.getInstance().initialize();
		
		//Create a new Scene and a new View 
		GraphicsScene scene = new GraphicsScene(new QuadTreeStorage());
		GraphicsView view = new GraphicsView(scene, new ParameterContext());
		
		
		//Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());
		//adding a simple selection handler (with no arguments) will enable selection only. That is: 
		//if the mouse is clicked upon an item the SelectionHandler will add (and bind) an additional item to the scene
		//that encloses the selected item. In addition the selection state of the item is changed (to be observed by the GraphicsItem.PROP_SELECTED property)
		//@note this will only work on items that are 1) visible and 2) selectable (see properties GraphicsItem.PROP_VISIBLE and GraphicsItem.PROP_SELECTABLE)
		//for additional options of the SelectionHandler see the other examples like: 
		// - MoveItemExample
		// - RotateItemExample
		// - ScaleItemExample
		view.addHandler(new SelectionHandler()); 
		
		view.setScale(2);
		
		//create a number of items and simulations that shall be drawn to the view
		String[] wkts = new String[] {
				"POLYGON ((0 150, 25 75, 100 50, 25 25, 0 -50, -25 25, -100 50, -25 75, 0 150))",
				"POLYGON ((-100 200, -89.47368421052632 200, -89.47368421052632 -89.47368421052632, -78.94736842105263 -89.47368421052632, -78.94736842105263 200, -68.42105263157895 200, -68.42105263157895 -89.47368421052632, -57.89473684210526 -89.47368421052632, -57.89473684210526 200, -47.368421052631575 200, -47.368421052631575 -89.47368421052632, -36.84210526315789 -89.47368421052632, -36.84210526315789 200, -26.315789473684205 200, -26.315789473684205 -89.47368421052632, -15.78947368421052 -89.47368421052632, -15.78947368421052 200, -5.263157894736835 200, -5.263157894736835 -89.47368421052632, 5.26315789473685 -89.47368421052632, 5.26315789473685 200, 15.789473684210535 200, 15.789473684210535 -89.47368421052632, 26.31578947368422 -89.47368421052632, 26.31578947368422 200, 36.842105263157904 200, 36.842105263157904 -89.47368421052632, 47.36842105263159 -89.47368421052632, 47.36842105263159 200, 57.894736842105274 200, 57.894736842105274 -89.47368421052632, 68.42105263157896 -89.47368421052632, 68.42105263157896 200, 78.94736842105264 200, 78.94736842105264 -89.47368421052632, 89.47368421052633 -89.47368421052632, 89.47368421052633 200, 100.00000000000001 200, 100 -100, -100 -100, -100 200))",
				"POLYGON ((-100 43.74769457764663, -61.96975285872371 62.30173367760975, -58.17041682036148 62.30173367760975, -58.17041682036148 -69.67908520841019, -57.89376613795646 -80.33474732571007, -57.06381409074142 -86.03836222796016, -55.37163408336407 -88.89247510143858, -52.50829952047215 -90.99963113242346, -47.26576908889709 -92.35521947620803, -38.436001475470306 -92.95462928808557, -38.436001475470306 -97.23349317594983, -97.23349317594983 -97.23349317594983, -97.23349317594983 -92.95462928808557, -88.12707488011803 -92.36905201032829, -82.93987458502397 -91.05496126890446, -80.20103282921431 -89.09074142382885, -78.43969015123571 -86.55477683511619, -77.48524529693839 -80.9249354481741, -77.16709701217263 -69.67908520841019, -77.16709701217263 14.680929546292887, -77.45296938399113 28.697897454813727, -78.31058649944669 36.5916635927702, -79.45407598672077 39.79158981925488, -81.26152711176687 42.01401696790853, -83.61305791220951 43.31427517521209, -86.38878642567317 43.74769457764663, -91.49760236075248 42.85319070453707, -98.26632239026189 40.1696790852084, -100 43.74769457764663))",
			};
		
		for (int i = 0; i < wkts.length; i++) {
			Shape shape = ExampleUtils.wkt2Shape(wkts[i]);
			GraphicsItem item = new GraphicsItem(shape) {
				@Override
				public void draw(Graphics2D g, IDrawContext ctx) {
					super.draw(g, ctx);
					g.setColor(Color.RED);
					g.draw(getSceneBounds());
				}
			};
			double x = (mRandom.nextFloat() * 1000) - 500;
			double y = (mRandom.nextFloat() * 1000) - 500;
			item.setCenter(x, y);
			
			DrawableStyle style = new DrawableStyle();
			style.setName("default");
			Point2D start = new Point2D.Float(-100, -100);
		    Point2D end = new Point2D.Float(100, 100);
		    float[] dist = {0.0f, 0.5f, 1.0f};
		    Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
		    LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
			style.setFillPaint(p);
			style.setLinePaint(Color.BLACK);
			item.setStyle(style);
			
			scene.addItem(item);
		}
		
		JFrame frame = new JFrame("OSMExample");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);
		
		frame.setLocation(2400, 200);
		
	}
}
