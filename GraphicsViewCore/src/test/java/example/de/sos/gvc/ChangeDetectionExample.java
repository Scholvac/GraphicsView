package example.de.sos.gvc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.Utils;
import de.sos.gvc.handler.ChangeDetectionHandler;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.handler.SelectionHandler;
import de.sos.gvc.handler.SelectionHandler.IMoveCallback;
import de.sos.gvc.handler.SelectionHandler.ItemMoveEvent;
import de.sos.gvc.rt.JPanelRenderTarget;
import de.sos.gvc.styles.DrawableStyle;

public class ChangeDetectionExample {

	private JFrame frame;
	private GraphicsScene mScene;
	private GraphicsView mView;
	private GraphicsView mChangeView;
	private GraphicsScene mChangeScene;
	private ChangeDetectionHandler mChangeDetector;
	private GraphicsView mManagedView;

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final ChangeDetectionExample window = new ChangeDetectionExample();
					window.frame.setVisible(true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ChangeDetectionExample() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 1099, 646);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		setupScene();
		setupChangeDisplay();
		setupManaged();

		final JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.33);
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);

		splitPane.setLeftComponent(mView.getComponent());

		final JSplitPane split2 = new JSplitPane();
		split2.setResizeWeight(0.5);
		splitPane.setRightComponent(split2);
		split2.setLeftComponent(mChangeView.getComponent());
		split2.setRightComponent(mManagedView.getComponent());


		final JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.NORTH);

		final JButton btnNewButton = new JButton("ShowChanges");
		panel.add(btnNewButton);

		final JButton btnNewButton_1 = new JButton("ClearChanges");
		panel.add(btnNewButton_1);

		btnNewButton.addActionListener(al -> paintRegions());
		btnNewButton_1.addActionListener(al -> clear());
	}

	public void clear() {
		mChangeDetector.clear();
		mChangeScene.clear();
	}
	private void paintRegions() {
		final List<Rectangle2D.Double> regionsToRepaint = mChangeDetector.getRepaintRegions();
		final DrawableStyle style =new DrawableStyle(null, Color.red, null, null);
		for (final Rectangle2D rect : regionsToRepaint) {
			final GraphicsItem gi = new GraphicsItem(rect);
			gi.setStyle(style);
			mChangeScene.addItem(gi);
		}
	}






	private void setupChangeDisplay() {
		mChangeScene = new GraphicsScene();
		mChangeView= new GraphicsView(mChangeScene, new JPanelRenderTarget(), mView.getPropertyContext());
	}
	private void setupManaged() {
		mManagedView = new GraphicsView(mScene, new JPanelRenderTarget(), mView.getPropertyContext());
	}


	private void setupScene() {
		mScene = new GraphicsScene();
		mView = new GraphicsView(mScene);
		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());
		final SelectionHandler selectionHandler = new SelectionHandler();
		selectionHandler.addMoveCallback(new IMoveCallback() {
			@Override
			public void onItemMoved(final ItemMoveEvent event) {
				//This method is called whenever the item is dragged
				//for this example we simply change the (selected)items positions
				for (int i = 0; i < event.items.size(); i++) {
					final GraphicsItem item = event.items.get(i);
					final Point2D newLocation = event.newSceneLocations.get(i); //nice thing: we get the new center position of the item, among other usefull information about the movement
					item.setSceneLocation(newLocation);//attention: take care if you change the scene location or the location within the parent. In this example its simple, since there is no item hierarchy.
				}
			}
		});
		mView.addHandler(selectionHandler);
		mView.setScale(2);
		/** Need to be added before! the first Item (to consider) is added. */
		mView.addHandler(mChangeDetector = new ChangeDetectionHandler());

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

		final String arrowWKT = "POLYGON ((-100 0, 0 100, 100 0, 50 0, 50 -100, -50 -100, -50 0, -100 0))";
		final String sinStarWKT = "POLYGON ((100 0, 61.193502094049606 22.993546861223116, 47.37629522998975 42.01130058032534, 37.5 86.02387002944835, 3.6399700797813024 56.22209406247465, -18.71674025585594 48.9579585314524, -63.62712429686843 53.16567552200251, -45.74727574170163 11.753618188079914, -45.74727574170165 -11.75361818807988, -63.627124296868445 -53.16567552200249, -18.71674025585592 -48.957958531452284, 3.6399700797812438 -56.222094062474724, 37.49999999999998 -86.02387002944836, 47.376295229989815 -42.01130058032546, 61.19350209404953 -22.993546861223134, 100 0))";

		final GraphicsItem arrowItem = new GraphicsItem(Utils.wkt2Shape(arrowWKT));
		arrowItem.setCenter(0, 0);
		arrowItem.setScale(1, 1);
		arrowItem.setRotation(0.0);
		arrowItem.setStyle(style);
		mScene.addItem(arrowItem);

		final GraphicsItem starItem = new GraphicsItem(Utils.wkt2Shape(sinStarWKT));
		starItem.setCenter(-300, 200);
		starItem.setScale(.5, .5);
		starItem.setRotation(0.0);
		starItem.setStyle(style);
		mScene.addItem(starItem);

		//build also a mixed item to show how it works on item hierarchies
		final GraphicsItem mixedItem = new GraphicsItem(Utils.wkt2Shape(arrowWKT));
		mixedItem.setCenter(300, -200);
		mixedItem.setScale(1, 1);
		mixedItem.setRotation(45.0);
		mixedItem.setStyle(style);
		final GraphicsItem subStar = new GraphicsItem(Utils.wkt2Shape(sinStarWKT));
		subStar.setCenter(0, 100);
		subStar.setStyle(style);
		subStar.setScale(0.25, 0.25);
		mixedItem.addItem(subStar);
		mScene.addItem(mixedItem);


		try {
			final GraphicsItem backgroundImg = GraphicsItem.createFromImage(ImageIO.read(getClass().getClassLoader().getResource("texture/pexels-anni-roenkae-2832432.jpg")));
			backgroundImg.setZOrder(20); //default is 100
			backgroundImg.setScale(0.5);
			mScene.addItem(backgroundImg);

		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
