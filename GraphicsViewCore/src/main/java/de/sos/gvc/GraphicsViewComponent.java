package de.sos.gvc;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JPanel;

import org.slf4j.Logger;

import de.sos.gvc.GraphicsScene.DirtyListener;
import de.sos.gvc.GraphicsScene.IItemFilter;
import de.sos.gvc.log.GVLog;
import de.sos.gvc.param.IParameter;
import de.sos.gvc.param.ParameterContext;

/**
 *
 * @author scholvac
 *
 */
public class GraphicsViewComponent extends JPanel implements IGraphicsView {

	private static Logger LOG = GVLog.getLogger(GraphicsViewComponent.class);


	private GraphicsScene 					mScene;
	private ParameterContext				mPropertyContext = null;
	protected IParameter<Double> 			mCenterX;
	protected IParameter<Double> 			mCenterY;
	protected IParameter<Double> 			mScaleX;
	protected IParameter<Double> 			mScaleY;
	protected IParameter<Double> 			mRotation;

	private AffineTransform					mViewTransform = null;

	/** maximum repaints (triggered by dirty scene) per second */
	private int								mRepaintDelay = 1000/30; //default: maximum of 30 repaints per second, triggered by dirty scene
	private Timer							mRepaintTimer;
	private TimerTask						mRepaintTimerTask = null;

	/**
	 * List of listener that will be notified before and after the painting has been done, for example to prepare a paint or clean up after painting
	 */
	private ArrayList<IPaintListener>		mPaintListener = new ArrayList<>();
	/**
	 * Invalidates the view transform and will be registered to all properties that have an effect to the view transform
	 */
	private PropertyChangeListener			mTransformListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			mViewTransform = null;
		}
	};

	/**
	 * Listen to all properties that require a repaint (which are basically all :) )
	 */
	private PropertyChangeListener			mRepaintListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			repaint();
		}
	};
	private List<IGraphicsViewHandler>		mHandler = new ArrayList<>();

	private DirtyListener					mDirtySceneListener = new DirtyListener() {
		@Override
		public void notifyDirty() {
			if (mRepaintTimerTask != null) {
				return ; //repaint is already scheduled
			}
			mRepaintTimer.schedule(mRepaintTimerTask = new TimerTask() {
				@Override
				public void run() {
					mRepaintTimerTask = null;
					repaint();
				}
			}, mRepaintDelay);
		}
		@Override
		public void notifyClean() {}
	};

	IDrawContext							mDrawContext = new IDrawContext() {
		@Override
		public IGraphicsView getView() {
			return GraphicsViewComponent.this;
		}
		@Override
		public AffineTransform getViewTransform() {
			return GraphicsViewComponent.this.getViewTransform();
		}
	};


	public GraphicsViewComponent(final GraphicsScene scene) {
		this(scene, new ParameterContext());
	}


	public GraphicsViewComponent(final GraphicsScene scene, final ParameterContext propertyContext) {
		mScene = scene;
		mScene._addView(this);

		mPropertyContext = propertyContext;

		mCenterX = mPropertyContext.getProperty(PROP_VIEW_CENTER_X, 0.0);
		mCenterY = mPropertyContext.getProperty(PROP_VIEW_CENTER_Y, 0.0);
		mScaleX = mPropertyContext.getProperty(PROP_VIEW_SCALE_X, 1.0);
		mScaleY = mPropertyContext.getProperty(PROP_VIEW_SCALE_Y, 1.0);
		mRotation = mPropertyContext.getProperty(PROP_VIEW_ROTATE, 0.0);
		mCenterX.addPropertyChangeListener(mTransformListener);
		mCenterY.addPropertyChangeListener(mTransformListener);
		mScaleX.addPropertyChangeListener(mTransformListener);
		mScaleY.addPropertyChangeListener(mTransformListener);
		mRotation.addPropertyChangeListener(mTransformListener);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				mViewTransform = null; //new offset to center 0.0, 0.0
			}
		});
		mPropertyContext.registerListener(mRepaintListener);

		mScene.registerDirtyListener(mDirtySceneListener);

		mRepaintTimer = new Timer("GraphicsViewRepaintTimer", true);
	}


	@Override
	public void setMaximumFPS(final int fps) {
		mRepaintDelay = 1000 / fps;
	}

	@Override
	public void addPaintListener(final IPaintListener listener) {
		if (listener != null && !mPaintListener.contains(listener)) {
			if (LOG.isTraceEnabled()) LOG.trace("Register Paint Listener");
			mPaintListener.add(listener);
		}
	}
	@Override
	public boolean removePaintListener(final IPaintListener listener) {
		if (listener != null) {
			final boolean res = mPaintListener.remove(listener);
			if (LOG.isTraceEnabled()) LOG.trace("Remove PaintListener " + (res?"successfull":"failed"));
			return res;
		}
		return false;
	}

	@Override
	public void addHandler(final IGraphicsViewHandler handler) {
		if (handler != null && !mHandler.contains(handler)) {
			mHandler.add(handler);
			handler.install(this);
		}
	}
	@Override
	public void removeHandler(final IGraphicsViewHandler handler) {
		if (handler != null && mHandler.contains(handler)) {
			mHandler.remove(handler);
			handler.uninstall(this);
		}
	}

	@Override
	public void setScale(final double scaleXY) {
		setScale(scaleXY, scaleXY);
	}
	@Override
	public void setScale(final double scaleX, final double scaleY) {
		mScaleX.set(scaleX);
		mScaleY.set(scaleY);
	}

	int pcounter = 0;
	@Override
	protected void paintComponent(final Graphics g) {
		//		System.out.println("Paint " + pcounter++ + " Scale: " + getScaleX());
		super.paintComponent(g);
		final Graphics2D g2d = (Graphics2D)g;

		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); //TODO: as property
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		synchronized (mPaintListener) {
			for (final IPaintListener pl : mPaintListener)
				try {
					pl.prePaint(g2d, mDrawContext);
				}catch(Exception | Error e) {
					LOG.error("Failed to call PaintListener.prePaint on : " + pl + " Error: " + e.getMessage());
					e.printStackTrace();
				}
		}

		//transform the view but remember the old transform
		final AffineTransform oldTransform = g2d.getTransform();
		g2d.transform(getViewTransform());

		//get all visible items, depending on the visible rect
		final Rectangle2D rect = getVisibleSceneRect();

		final List<GraphicsItem> itemList = mScene.getItems(rect);

		//sort to overdraw the correct items, for example background items
		Collections.sort(itemList, Comparator.comparing(GraphicsItem::getZOrder));

		for (final GraphicsItem item : itemList) {
			try {
				item.draw(g2d, mDrawContext);
			}catch(Exception | Error e) {
				//catch all exceptions (latest) here. Even if we do not handle them,
				//it crashes the whole (drawing-) system if it is not catched
				LOG.error("Failed to paint an Item with error {}", e);
				e.printStackTrace();
			}
		}

		//		g2d.setColor(new Color(255, 0, 0, 180));
		//		g2d.fill(new Rectangle2D.Double(rect.getX() + 5, rect.getY() + 5, rect.getWidth() - 10, rect.getHeight()-10));
		//reset the old transform
		g2d.setTransform(oldTransform);
		mScene.markClean(); //remember that the scene is no longer dirty, at least not in terms of visualisation

		synchronized (mPaintListener) {
			for (final IPaintListener pl : mPaintListener)
				try {
					pl.postPaint(g2d, mDrawContext);
				}catch(Exception | Error e) {
					LOG.error("Failed to call PaintListener.postPaint on : " + pl + " Error: " + e.getMessage());
					e.printStackTrace();
				}
		}
	}

	protected AffineTransform getViewTransform() {
		if (mViewTransform == null) {
			final AffineTransform t = new AffineTransform();
			//add offset so 0.0, 0.0 would be in the center of the sceen
			final double w2 = getWidth() / 2.0;
			final double h2 = getHeight() / 2.0;
			final double rdeg = mRotation.get();
			final double r = Math.toRadians(-rdeg);

			t.translate(-w2 * mScaleX.get(), h2 * mScaleY.get());
			//now the view transform relative to the scene
			t.translate(mCenterX.get(), -mCenterY.get());
			t.scale(mScaleX.get(), -mScaleY.get());
			t.translate(w2, h2);
			t.rotate(r);
			t.translate(-w2, -h2);
			try {
				mViewTransform = t.createInverse();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return mViewTransform;
	}

	@Override
	public void setCenter(final double x, final double y) {
		mCenterX.set(x);
		mCenterY.set(y);
	}
	@Override
	public double getCenterX() { return mCenterX.get(); }
	@Override
	public double getCenterY() { return mCenterY.get(); }

	@Override
	public Point2D getPositionInComponent(final Point2D sceneLocation) {
		try {
			final Point2D tmp = getViewTransform().transform(sceneLocation, null);
			return new Point2D.Double(tmp.getX(), tmp.getY());
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	@Override
	public Point2D getPositionOnScreen(final Point2D sceneLocation) {
		final Point2D inComp = getPositionInComponent(sceneLocation);
		inComp.setLocation(inComp.getX() + getLocationOnScreen().x, inComp.getY() + getLocationOnScreen().y);
		return inComp;
	}

	/**
	 * Returns the currently visible area as rectangle in scene coordinates.
	 * @return
	 */
	@Override
	public Rectangle2D getVisibleSceneRect() {
		final Rectangle vr = getVisibleRect();
		final Rectangle2D rect = Utils.inverseTransform(vr, getViewTransform());
		return rect;
	}

	/**
	 * returns a list of (top-level) items that are within the rectangle defined through
	 * (point.getX() - epsilonX/2, point.getY() - epsilonY/2, epsilonX, epsilonY)
	 * @note toplevel items are those items, added directly to the scene
	 *
	 * @param point
	 * @param epsilonX
	 * @param epsilonY
	 * @return
	 */
	@Override
	public List<GraphicsItem> getItemsAt(final Point point, final double epsilonX, final double epsilonY) {
		final Point2D scene = getSceneLocation(point, null);
		final Rectangle2D r = new Rectangle2D.Double(scene.getX() - epsilonX/2, scene.getY() - epsilonY/2, epsilonX, epsilonY);
		return mScene.getItems(r);
	}

	/**
	 * returns a list of all items that intersect the rectangle defined by point and +/- epsilon
	 * this method also returns child items, if the parent and the child do pass the filter (if not null)
	 * @param point
	 * @param epsilonX
	 * @param epsilonX
	 * @param filter (may null)
	 * @return
	 */
	@Override
	public List<GraphicsItem> getAllItemsAt(final Point point, final double epsilonX, final double epsilonY, final IItemFilter filter) {
		final Point2D scene = getSceneLocation(point, null);
		final Rectangle2D r = new Rectangle2D.Double(scene.getX() - epsilonX/2, scene.getY() - epsilonY/2, epsilonX, epsilonY);
		return mScene.getAllItems(r, filter);
	}

	/**
	 * transforms the screen position input (screen) into scene location and stores the result in scene point
	 * @param screen
	 * @param scene (may null)
	 * @return scene point or new point if scene is null
	 */
	@Override
	public Point2D getSceneLocation(final Point2D screen, Point2D scene) {
		if (scene == null)
			scene = new Point2D.Double();
		try {
			return getViewTransform().inverseTransform(new Point2D.Double(screen.getX(), /* getHeight() -*/ screen.getY()), scene);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * @see GraphicsView.getSceneLocation(Point2D, Point2D)
	 * @param point
	 * @return
	 */
	@Override
	public Point2D getSceneLocation(final Point point) {
		return getSceneLocation(point, new Point2D.Double());
	}
	@Override
	public double getScaleX() { return mScaleX.get(); }
	@Override
	public double getScaleY() { return mScaleY.get(); }

	@Override
	public GraphicsScene getScene() { return mScene; }
	@Override
	public ParameterContext getPropertyContext() { return mPropertyContext; }

	@Override
	public <T> IParameter<T> getProperty(final String string) {
		return mPropertyContext.getProperty(string);
	}

	@Override
	public void setRotation(final double degrees) {
		mRotation.set(degrees);
	}
	@Override
	public double getRotationDegrees() { return mRotation.get(); }


	@Override
	public void setCenter(final Point2D center) {
		setCenter(center.getX(), center.getY());
	}

	@Override
	public void setCenterAndZoom(final Point2D min, final Point2D max, final boolean scaleXandY) {
		final double x = Math.min(min.getX(), max.getX());
		final double y = Math.min(min.getY(), max.getY());
		final double w = Math.abs(min.getX() - max.getX());
		final double h = Math.abs(min.getY() - max.getY());
		setCenterAndZoom(new Rectangle2D.Double(x, y, w, h), scaleXandY);
	}
	@Override
	public void setCenterAndZoom(final Rectangle2D bounds, final boolean scaleXandY) {
		final Rectangle visRect = getVisibleRect();

		//bounds.width * scaleX = visRect.getWidth()
		double scaleX = 1.1 * bounds.getWidth() / visRect.getWidth();
		double scaleY = 1.1 * bounds.getHeight() / visRect.getHeight();
		final double cx = bounds.getCenterX(), cy = bounds.getCenterY();


		if (scaleX <= 0 || !Double.isFinite(scaleX) ||
				scaleY <= 0 || !Double.isFinite(scaleY) ||
				!Double.isFinite(cx) || !Double.isFinite(cy))
		{
			LOG.debug("Detected invalid scale parameter: X = {}, Y = {}", scaleX, scaleY);
			return ;
		}
		if (!scaleXandY) {
			if (scaleX > scaleY)
				scaleY = scaleX;
			else
				scaleX = scaleY;
		}
		setCenter(cx, -cy);
		setScale(scaleX, scaleY);
	}


	/** Forward the cleared notification to all IGraphicsViewHandler that may relay on Scene content */
	@Override
	public void notifySceneCleared() {
		for (final IGraphicsViewHandler handler : mHandler)
			handler.notifySceneCleared();
	}

}
