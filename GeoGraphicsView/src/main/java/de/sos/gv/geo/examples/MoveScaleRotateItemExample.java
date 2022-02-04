package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import javax.swing.JFrame;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.handler.SelectionHandler;
import de.sos.gvc.handler.SelectionHandler.IMoveCallback;
import de.sos.gvc.handler.SelectionHandler.IRotateCallback;
import de.sos.gvc.handler.SelectionHandler.IScaleCallback;
import de.sos.gvc.handler.SelectionHandler.ItemMoveEvent;
import de.sos.gvc.handler.SelectionHandler.ItemRotateEvent;
import de.sos.gvc.handler.SelectionHandler.ItemScaleEvent;
import de.sos.gvc.log.GVLog;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.storage.ListStorage;
import de.sos.gvc.styles.DrawableStyle;


/**
 * 
 * @author scholvac
 *
 */
public class MoveScaleRotateItemExample {
	
	public static Random			mRandom = new Random(4242);
	
	public static void main(String[] args) {
		GVLog.getInstance().initialize();
		
		//Create a new Scene and a new View 
		GraphicsScene scene = new GraphicsScene(new ListStorage());
		GraphicsView view = new GraphicsView(scene, new ParameterContext());
		
		
		//Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());
		//create a and configure a new selection handler. The selection handler will take care about the selection and will add a new item to the scene, 
		//that encloses the selected item. This new item will response to mouse drag events, as soon as we register an instance of a 
		//SelectionHandler.IMoveCallback. This callback will be notified when the mouse is dragged, while the object is selected (and the mouse is within
		//the bounds of the item). 
		SelectionHandler selectionHandler = new SelectionHandler();
		selectionHandler.addMoveCallback(new IMoveCallback() {			
			@Override
			public void onItemMoved(ItemMoveEvent event) {
				//This method is called whenever the item is dragged
				//for this example we simply change the (selected)items positions
				for (int i = 0; i < event.items.size(); i++) {
					GraphicsItem item = event.items.get(i);
					Point2D newLocation = event.newSceneLocations.get(i); //nice thing: we get the new center position of the item, among other usefull information about the movement
					item.setSceneLocation(newLocation);//attention: take care if you change the scene location or the location within the parent. In this example its simple, since there is no item hierarchy.
				}
			}
		});
		//The new created item will have 4 "scaling" points, one on each corner of the bounding rectangle. If at least one SelectionHandler.IScaleCallback
		//is registered within the selection handler, the mouse icon will change to an scaling icon and the callbacks are fired
		selectionHandler.addScaleCallback(new IScaleCallback() {			
			@Override
			public void onItemScaled(ItemScaleEvent event) {
				//as Example, we do calculate the scale factor and apply this to the shape, e.g. we do not realy change the geometry of the item
				//but that would be also possible 
				for (int i = 0; i < event.items.size(); i++) {
					GraphicsItem item = event.items.get(i);
					double[]  factors = event.getScaleFactors(i);
					System.out.println("Factors: " + factors[0] + ";" + factors[1]);
					item.setScale(factors[0] * item.getScaleX(), factors[1] * item.getScaleY());
					Rectangle2D newBounds = event.getNewSceneBounds().get(i);
					item.setSceneLocation(newBounds.getCenterX(), newBounds.getCenterY());
				}
			}
		});
		
		selectionHandler.addRotationCallback(new IRotateCallback() {			
			@Override
			public void onItemRotated(ItemRotateEvent event) {
				for (int i = 0; i < event.items.size(); i++)
					event.items.get(i).setRotation(event.endAngles.get(i));
			}
		});
		
		view.addHandler(selectionHandler); 
		
		view.setScale(2);
		
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
		
		String arrowWKT = "POLYGON ((-100 0, 0 100, 100 0, 50 0, 50 -100, -50 -100, -50 0, -100 0))";
		String sinStarWKT = "POLYGON ((100 0, 61.193502094049606 22.993546861223116, 47.37629522998975 42.01130058032534, 37.5 86.02387002944835, 3.6399700797813024 56.22209406247465, -18.71674025585594 48.9579585314524, -63.62712429686843 53.16567552200251, -45.74727574170163 11.753618188079914, -45.74727574170165 -11.75361818807988, -63.627124296868445 -53.16567552200249, -18.71674025585592 -48.957958531452284, 3.6399700797812438 -56.222094062474724, 37.49999999999998 -86.02387002944836, 47.376295229989815 -42.01130058032546, 61.19350209404953 -22.993546861223134, 100 0))";
		
		GraphicsItem arrowItem = new GraphicsItem(ExampleUtils.wkt2Shape(arrowWKT));
		arrowItem.setCenter(0, 0);
		arrowItem.setScale(1, 1);
		arrowItem.setRotation(0.0);
		arrowItem.setStyle(style);
		scene.addItem(arrowItem);
		
		GraphicsItem starItem = new GraphicsItem(ExampleUtils.wkt2Shape(sinStarWKT));
		starItem.setCenter(-300, 200);
		starItem.setScale(.5, .5);
		starItem.setRotation(0.0);
		starItem.setStyle(style);
		scene.addItem(starItem);
		
		//build also a mixed item to show how it works on item hierarchies
		GraphicsItem mixedItem = new GraphicsItem(ExampleUtils.wkt2Shape(arrowWKT));
		mixedItem.setCenter(300, -200);
		mixedItem.setScale(1, 1);
		mixedItem.setRotation(45.0);
		mixedItem.setStyle(style);
		GraphicsItem subStar = new GraphicsItem(ExampleUtils.wkt2Shape(sinStarWKT));
		subStar.setCenter(0, 100);
		subStar.setStyle(style);
		subStar.setScale(0.25, 0.25);
		mixedItem.addItem(subStar);
		scene.addItem(mixedItem);
		
		JFrame frame = new JFrame("Move - Scale - Rotate - Example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);
		
		frame.setLocation(200, 200);
		
	}
}
