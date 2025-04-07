package de.sos.gv.svg;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Random;
import java.util.function.Function;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.kitfox.svg.Group;
import com.kitfox.svg.RenderableElement;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.ShapeElement;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.handler.SelectionHandler;
import de.sos.gvc.storage.QuadTreeStorage;
import de.sos.gvc.styles.DrawableStyle;


/**
 *
 * @author scholvac
 *
 */
public class SelectionExample {

	public static Random			mRandom = new Random(4242);

	public static void main(final String[] args) throws MalformedURLException {
		//Create a new Scene and a new View
		final GraphicsScene scene = new GraphicsScene(new QuadTreeStorage());
		final GraphicsView view = new GraphicsView(scene);


		//Standard Handler
		view.addHandler(new MouseDelegateHandler() {
			@Override
			public void mouseMoved(final MouseEvent e) {
				//				System.out.println("Scene " + view.getSceneLocation(e.getPoint()));
				super.mouseMoved(e);
			}
		});
		view.addHandler(new DefaultViewDragHandler());
		view.addHandler(new SelectionHandler());

		view.setScale(2);


		final SVGUniverse universe = new SVGUniverse();
		final URI svgURI = new File("De-facto-territory-control-map-of-the-world-borderless-14-05-2019.svg").toURI();
		final SVGDiagram diagram = universe.getDiagram(universe.loadSVG(svgURI.toURL()));
		System.out.println("Loaded");

		final GraphicsItem parentItem = GraphicsItem.createFromShape(new SVGShape(diagram));
		parentItem.setDrawable((g, style, ctx) -> {
			try {
				g.setColor(Color.black);
				g.fill(parentItem.getBoundingBox());
				g.scale(1, -1);
				diagram.render(g);
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		parentItem.setStyle(new DrawableStyle(null, null, null, Color.blue));
		parentItem.setSelectable(true);
		parentItem.setMouseSupport(new MouseAdapter() {
			@Override
			public void mouseEntered(final MouseEvent e) {
				System.out.println("Enter");

			}
		});
		scene.addItem(parentItem);
		final GraphicsItem sceneCenter = new GraphicsItem(new Arc2D.Double(-10, -10, 20, 20, 0, 360, Arc2D.CHORD));
		scene.addItem(sceneCenter);
		sceneCenter.setZOrder(500);
		sceneCenter.setStyle(new DrawableStyle(null, Color.red, null, Color.red));
		view.addViewTransformListener(c -> sceneCenter.setScale(c.getScale()));

		final GraphicsItem itemCenter = new GraphicsItem(new Arc2D.Double(-20, -20, 38, 38, 0, 360, Arc2D.CHORD)) {
			@Override
			public void draw(final java.awt.Graphics2D g, final de.sos.gvc.IDrawContext ctx) {
				super.draw(g, ctx);
				System.out.println("Draw: " + getSceneLocation());
			}
			;
		};
		parentItem.addItem(itemCenter);
		view.addViewTransformListener(c -> itemCenter.setSceneScale(c.getScale()));
		itemCenter.setStyle(new DrawableStyle(null, Color.red, null, Color.green));
		sceneCenter.setZOrder(502);

		final JFrame frame = new JFrame("OSMExample");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view.getComponent(), BorderLayout.CENTER);
		frame.setVisible(true);

		frame.setLocation(2400, 200);

	}

	static class SVGShape implements Shape {

		private SVGDiagram mDiagram;
		private Rectangle2D mGeom;
		private Rectangle mBounds;

		public SVGShape(final SVGDiagram diagram) {
			mDiagram = diagram;
		}

		@Override
		public boolean contains(final Point2D p) {
			return recursiveCheck(mDiagram.getRoot(), s -> s.contains(p));
		}

		@Override
		public boolean contains(final Rectangle2D r) {
			return recursiveCheck(mDiagram.getRoot(), s -> s.contains(r));
		}

		@Override
		public boolean contains(final double x, final double y) {
			return recursiveCheck(mDiagram.getRoot(), s -> s.contains(x, y));
		}

		@Override
		public boolean contains(final double x, final double y, final double w, final double h) {
			return recursiveCheck(mDiagram.getRoot(), s -> s.contains(x, y, w, h));
		}

		@Override
		public Rectangle getBounds() {
			if (mBounds == null) {
				final Rectangle2D r = getBounds2D();
				mBounds = new Rectangle((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());
			}
			return mBounds;
		}

		@Override
		public Rectangle2D getBounds2D() {
			if (mGeom == null)
				try {
					mGeom = mDiagram.getRoot().getBoundingBox();
					final double w2 = mGeom.getWidth()*0.5, h2 = mGeom.getHeight()*0.5;
					mGeom = new Rectangle2D.Double(mGeom.getX()-w2, mGeom.getY()-h2, mGeom.getWidth(), mGeom.getHeight());
				} catch (final SVGException e) {
					e.printStackTrace();
				}
			return mGeom;
		}

		@Override
		public PathIterator getPathIterator(final AffineTransform at) {
			return getBounds2D().getPathIterator(at);
		}

		@Override
		public PathIterator getPathIterator(final AffineTransform at, final double flatness) {
			return getPathIterator(at, flatness);
		}

		@Override
		public boolean intersects(final Rectangle2D r) {
			return recursiveCheck(mDiagram.getRoot(), s -> s.intersects(r));
		}

		@Override
		public boolean intersects(final double x, final double y, final double w, final double h) {
			return recursiveCheck(mDiagram.getRoot(), s -> s.intersects(x, y, w, h));
		}

		public boolean recursiveCheck(final SVGElement el, final Function<Shape, Boolean> func) {
			if (el instanceof RenderableElement)
				if (el instanceof Group) {
					final Group g = (Group)el;
					try {
						if (func.apply(g.getBoundingBox()))
							for (int i = 0; i<  g.getNumChildren(); i++)
								if (recursiveCheck(g.getChild(i), func))
									return true;
					} catch (final SVGException e) {
						e.printStackTrace();
					}
				}else if (el instanceof ShapeElement){
					final ShapeElement se = (ShapeElement)el;
					return func.apply(se.getShape());
				}
			return false;
		}
	}


}
