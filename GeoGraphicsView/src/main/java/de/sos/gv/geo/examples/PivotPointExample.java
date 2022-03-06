package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.drawables.ImageDrawable;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.handler.SelectionHandler;
import de.sos.gvc.handler.SelectionHandler.IRotateCallback;
import de.sos.gvc.handler.SelectionHandler.ItemRotateEvent;
import de.sos.gvc.log.GVLog;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.storage.QuadTreeStorage;
import de.sos.gvc.styles.DrawableStyle;


/**
 *
 * @author scholvac
 *
 */
public class PivotPointExample {

	public static Random			mRandom = new Random(4242);

	public static void main(String[] args) throws IOException {
		GVLog.getInstance().initialize();

		//Create a new Scene and a new View
		GraphicsScene scene = new GraphicsScene(new QuadTreeStorage());
		GraphicsView view = new GraphicsView(scene, new ParameterContext());


		//Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());

		SelectionHandler selectionHandler = new SelectionHandler();
		view.addHandler(selectionHandler);

		selectionHandler.addRotationCallback(new IRotateCallback() {
			@Override
			public void onItemRotated(ItemRotateEvent event) {
				for (int i = 0; i < event.items.size(); i++)
					event.items.get(i).setRotation(event.endAngles.get(i));
			}
		});


		view.setScale(2);

		addItems(scene, view);

		JFrame frame = new JFrame("Pivot Point Example");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);

		frame.setLocation(2400, 200);
	}

	static class OffsetItem extends GraphicsItem{
		final Point2D.Double 			mOffset = new Point2D.Double();

		public OffsetItem(final Shape shape) {
			super(shape);
		}
		public void setOffset(final double x, double y) {
			mOffset.setLocation(x, y);
		}
		public void setOffset(final Point2D.Double p) {
			mOffset.setLocation(p);
		}

		@Override
		protected void updateLocalTransform() {
			synchronized (mLocalTransform) {
				mLocalTransform.setToIdentity();

				final double x = getCenterX(), y = getCenterY(), r = getRotationRadians();
				final double sx = getScaleX(), sy = getScaleY();
				final double ox = mOffset.x * sx, oy = mOffset.y * sy;

				mLocalTransform.translate(x+ox, y+oy);
				mLocalTransform.rotate(r, -ox, -oy);
				mLocalTransform.scale(sx, sy);

				mInvalidLocalTransform = false;
			}
		}
		@Override
		public void draw(Graphics2D g, IDrawContext ctx) {
			setScale(ctx.getScaleX(), ctx.getScaleY());
			super.draw(g, ctx);
		}
	}

	private static void addItems(GraphicsScene scene, GraphicsView view) throws IOException {
		Rectangle2D pinRect = new Rectangle2D.Double(-64, -64, 128, 128);

		GraphicsItem boundaryItem = new GraphicsItem(pinRect);
		boundaryItem.setZOrder(100);
		boundaryItem.setSelectable(false);
		boundaryItem.setStyle(new DrawableStyle("Border", Color.RED, null, null));
		boundaryItem.setCenter(0, 0);

		GraphicsItem needlePoint = new GraphicsItem(new Arc2D.Double(-2, -2, 4, 4, 0, 360, Arc2D.CHORD));
		needlePoint.setStyle(new DrawableStyle("Filled", Color.BLUE, null, Color.RED));
		needlePoint.setZOrder(101);
		needlePoint.setCenter(0, 0);

		OffsetItem pinItem = new OffsetItem(pinRect);
		pinItem.setDrawable(new ImageDrawable(pinRect, ImageIO.read(PivotPointExample.class.getClassLoader().getResource("example_pin.png"))));
		pinItem.setZOrder(99);
		pinItem.setOffset(0, 64);





		boundaryItem.addItem(needlePoint);
		scene.addItem(boundaryItem);
		scene.addItem(pinItem);

	}
}
