package de.sos.gvc;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;

import de.sos.gvc.GraphicsScene.DirtyListener;
import de.sos.gvc.GraphicsScene.IItemFilter;
import de.sos.gvc.Utils.WindowStat;
import de.sos.gvc.log.GVLog;
import de.sos.gvc.param.IParameter;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.rt.IRenderTarget;
import de.sos.gvc.rt.JPanelRenderTarget;

/**
 *
 * @author scholvac
 *
 */
public class GraphicsView {

	public static final String PROP_VIEW_CENTER_X 	= "VIEW_CENTER_X";
	public static final String PROP_VIEW_CENTER_Y 	= "VIEW_CENTER_Y";
	public static final String PROP_VIEW_SCALE_X 	= "VIEW_SCALE_X";
	public static final String PROP_VIEW_SCALE_Y 	= "VIEW_SCALE_Y";
	public static final String PROP_VIEW_ROTATE 	= "VIEW_ROTATE";
	public static final String PROP_VIEW_WIDTH 		= "VIEW_WIDTH";
	public static final String PROP_VIEW_HEIGHT 	= "VIEW_HEIGHT";


	private static Logger LOG = GVLog.getLogger(GraphicsView.class);


	private GraphicsScene 					mScene;
	private ParameterContext				mPropertyContext = null;
	protected IParameter<Double> 			mCenterX;
	protected IParameter<Double> 			mCenterY;
	protected IParameter<Double> 			mScaleX;
	protected IParameter<Double> 			mScaleY;
	protected IParameter<Double> 			mRotation;
	protected IParameter<Integer>			mRTWidth;
	protected IParameter<Integer>			mRTHeight;

	private AffineTransform					mViewTransform = null;

	/** maximum repaints (triggered by dirty scene) per second */
	private int								mRepaintDelay = 1000/30; //default: maximum of 30 repaints per second, triggered by dirty scene

	private DoubleSummaryStatistics			mOverallStatitic = new DoubleSummaryStatistics();
	private WindowStat						mWindowStatistic = new WindowStat(20);

	private final IRenderTarget				mRenderTarget;
	/** Whether the GraphicsView shall trigger repaints, if a change in the scene or the view has been detected.
	 */
	private boolean							mTriggersRepaint = true;
	private AtomicInteger 					mUpdateCounter = new AtomicInteger(0);
	private AtomicInteger 					mRequestCounter = new AtomicInteger(0);
	private ScheduledExecutorService 		mScheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<Integer> 		mScheduledFuture;

	private RenderingHints					mRenderHints = null;
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
			markViewAsDirty();
		}
	};
	private List<IGraphicsViewHandler>		mHandler = new ArrayList<>();

	private DirtyListener					mDirtySceneListener = new DirtyListener() {
		@Override
		public void notifyDirty() {
			markViewAsDirty();
		}
		@Override
		public void notifyClean() {}
	};

	static class GVDrawContext implements IDrawContext {
		final GraphicsView 			view;
		final Rectangle2D.Double 	visibleScreenRect = new Rectangle2D.Double();
		final Rectangle2D.Double 	visibleSceneRect = new Rectangle2D.Double();

		public GVDrawContext(final GraphicsView v) {
			view = v;
		}

		@Override
		public GraphicsView getView() {
			return view;
		}
		@Override
		public AffineTransform getViewTransform() {
			return view.getViewTransform();
		}
		@Override
		public Rectangle2D.Double getVisibleSceneRect(){ return visibleSceneRect;}
		@Override
		public Rectangle2D.Double getVisibleScreenRect() { return visibleScreenRect;}
	}
	private final GVDrawContext				mDrawContext;


	public GraphicsView(final GraphicsScene scene) {
		this(scene, new JPanelRenderTarget());
	}
	public GraphicsView(final GraphicsScene scene, final IRenderTarget renderTarget) {
		this(scene, renderTarget, new ParameterContext());
	}

	public GraphicsView(final GraphicsScene scene, final IRenderTarget renderTarget, final ParameterContext propertyContext) {
		mScene = scene;
		mScene._addView(this);
		mRenderTarget = renderTarget;
		mRenderTarget.setGraphicsView(this);
		mDrawContext = new GVDrawContext(this);

		mPropertyContext = propertyContext;

		mCenterX 	= mPropertyContext.getProperty(PROP_VIEW_CENTER_X, 0.0);
		mCenterY 	= mPropertyContext.getProperty(PROP_VIEW_CENTER_Y, 0.0);
		mScaleX 	= mPropertyContext.getProperty(PROP_VIEW_SCALE_X, 1.0);
		mScaleY 	= mPropertyContext.getProperty(PROP_VIEW_SCALE_Y, 1.0);
		mRotation 	= mPropertyContext.getProperty(PROP_VIEW_ROTATE, 0.0);
		mRTWidth 	= mPropertyContext.getProperty(PROP_VIEW_WIDTH, 0);
		mRTHeight 	= mPropertyContext.getProperty(PROP_VIEW_HEIGHT, 0);

		mCenterX.addPropertyChangeListener(mTransformListener);
		mCenterY.addPropertyChangeListener(mTransformListener);
		mScaleX.addPropertyChangeListener(mTransformListener);
		mScaleY.addPropertyChangeListener(mTransformListener);
		mRotation.addPropertyChangeListener(mTransformListener);
		mRTWidth.addPropertyChangeListener(mTransformListener);
		mRTHeight.addPropertyChangeListener(mTransformListener);

		mPropertyContext.registerListener(mRepaintListener);
		mScene.registerDirtyListener(mDirtySceneListener);
	}


	/** Whether the GraphicsView shall trigger repaints, if a change in the scene or the view has been detected.
	 *
	 * Disable repaint trigger may be usefull if rendered within another render loop
	 *
	 * @param enabled true, if the GraphicsView shall trigger a repaint if a change was detected, false otherwise.
	 */
	public void enableRepaintTrigger(final boolean enabled) {
		mTriggersRepaint = enabled;
	}
	public boolean isRepaintTriggerEnabled() { return mTriggersRepaint;}

	public void setMaximumFPS(final int fps) {
		mRepaintDelay = 1000 / fps;
	}
	/** Sets the internal scheduler service to a custom instance.
	 * The scheduler-thread is most likely the thread that triggers the rendering
	 * and may also performs the rendering.
	 *
	 * @param scheduler
	 */
	public void setSchedulerService(final ScheduledExecutorService scheduler) {
		mScheduler = scheduler;
	}
	public void setRenderHints(final RenderingHints rh) { mRenderHints = rh; }
	public RenderingHints getREnderHints() { return mRenderHints;}

	public void addPaintListener(final IPaintListener listener) {
		if (listener != null && !mPaintListener.contains(listener)) {
			if (LOG.isTraceEnabled()) LOG.trace("Register Paint Listener");
			mPaintListener.add(listener);
		}
	}
	public boolean removePaintListener(final IPaintListener listener) {
		if (listener != null) {
			final boolean res = mPaintListener.remove(listener);
			if (LOG.isTraceEnabled()) LOG.trace("Remove PaintListener " + (res?"successfull":"failed"));
			return res;
		}
		return false;
	}

	public void addHandler(final IGraphicsViewHandler handler) {
		if (handler != null && !mHandler.contains(handler)) {
			mHandler.add(handler);
			handler.install(this);
		}
	}
	public void removeHandler(final IGraphicsViewHandler handler) {
		if (handler != null && mHandler.contains(handler)) {
			mHandler.remove(handler);
			handler.uninstall(this);
		}
	}

	public void setScale(final double scaleXY) {
		setScale(scaleXY, scaleXY);
	}
	public void setScale(final double scaleX, final double scaleY) {
		mScaleX.set(scaleX);
		mScaleY.set(scaleY);
	}

	private void triggerRTRepaint() {
		if (isRepaintTriggerEnabled())
			mRenderTarget.requestRepaint();
	}

	public void doPaint(final Graphics2D g2d) {
		doPaint(g2d, 0, 0, getImageWidth(), getImageHeight());
	}
	/**
	 * Paint the given area
	 *
	 * @param g2d
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void doPaint(final Graphics2D g2d, final int x, final int y, final int width, final int height) {
		synchronized (mUpdateCounter) {
			final long profile_time_start = System.currentTimeMillis();

			internalPaint(g2d, x, y, width, height);

			final long profile_time_end = System.currentTimeMillis();
			final double profile_time_sec = (profile_time_end - profile_time_start) / 1000.0;
			mOverallStatitic.accept(profile_time_sec);
			mWindowStatistic.accept(profile_time_sec);
		}
	}
	private void internalPaint(final Graphics2D g2d, int x, int y, int width, int height) {
		mUpdateCounter.set(mRequestCounter.get());
		//check for changes
		validateView();

		final int rtWidth = getImageWidth();
		final int rtHeight = getImageHeight();
		//clip to max area = render target area.
		// @note: this has no effect on the view matrix but only on the area that items that are repainted.
		if (x < 0) x = 0;
		if (y < 0) y = 0;
		if (x+width > rtWidth) width = rtWidth-x;
		if (y+height > rtHeight) height = rtHeight-y;

		mDrawContext.visibleScreenRect.setRect(x, y, width, height);
		getVisibleSceneRect(mDrawContext.visibleScreenRect, mDrawContext.visibleSceneRect);


		for (final IPaintListener pl : mPaintListener) {
			try {
				pl.prePaint(g2d, mDrawContext);
			}catch(Exception | Error e) {
				LOG.error("Failed to call PaintListener.prePaint on : " + pl + " Error: " + e.getMessage());
				e.printStackTrace();
			}
		}

		RenderingHints oldHints = null;
		if (mRenderHints != null) {
			oldHints = g2d.getRenderingHints();
			g2d.setRenderingHints(mRenderHints);
		}

		//transform the view but remember the old transform
		final AffineTransform oldTransform = g2d.getTransform();
		g2d.transform(getViewTransform());

		//get all visible items, depending on the visible rect
		final List<GraphicsItem> itemList = mScene.getItems(mDrawContext.visibleSceneRect);

		//sort to overdraw the correct items, for example background items
		itemList.sort(Comparator.comparing(GraphicsItem::getZOrder));

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

		//reset the old transform & hints
		g2d.setTransform(oldTransform);
		mScene.markClean(); //remember that the scene is no longer dirty, at least not in terms of visualisation
		if (mRenderHints != null && oldHints != null) //otherwise we did not change them...
			g2d.setRenderingHints(oldHints);

		for (final IPaintListener pl : mPaintListener) {
			try {
				pl.postPaint(g2d, mDrawContext);
			}catch(Exception | Error e) {
				LOG.error("Failed to call PaintListener.postPaint on : " + pl + " Error: " + e.getMessage());
				e.printStackTrace();
			}
		}

		//		System.out.println("Finish:"  + mUpdateCounter.get());
	}

	/** checks some properties and may invalidate the view matrix */
	private void validateView() {
		//just set the new value, if something has changed, the parameter will do the notification as well as the invalidation of the view matrix (e.g. TransformLIstener)
		mRTWidth.set(mRenderTarget.getWidth());
		mRTHeight.set(mRenderTarget.getHeight());
	}
	/**
	 * Requests the GraphicsView to repaint it's content.
	 *
	 * The request is forwarded to the underlying RenderTarget.
	 * The RenderTarget is in charge to decide whether a repaint is
	 * required or not.
	 */
	public void triggerRepaint() {
		markViewAsDirty();
	}
	public Component getComponent() {
		return mRenderTarget.getComponent();
	}

	protected AffineTransform getViewTransform() {
		if (mViewTransform == null) {
			final AffineTransform t = new AffineTransform();
			//add offset so 0.0, 0.0 would be in the center of the sceen
			final double w2 = getImageWidth() / 2.0;
			final double h2 = getImageHeight() / 2.0;
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
	public void resetViewTransform() {
		mViewTransform = null;
	}

	public void setCenter(final double x, final double y) {
		mCenterX.set(x);
		mCenterY.set(y);
	}
	public double getCenterX() { return mCenterX.get(); }
	public double getCenterY() { return mCenterY.get(); }

	/** Returns the width of the underlying render target */
	public int getImageWidth() { return mRenderTarget.getWidth(); }
	/** Returns the height of the underlying render target */
	public int getImageHeight() { return mRenderTarget.getHeight(); }
	public IRenderTarget getRenderTarget() {
		return mRenderTarget;
	}
	public Point2D getPositionInComponent(final Point2D sceneLocation) {
		try {
			final Point2D tmp = getViewTransform().transform(sceneLocation, null);
			return new Point2D.Double(tmp.getX(), tmp.getY());
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public Point2D getPositionOnScreen(final Point2D sceneLocation) {
		final Point2D inComp = getPositionInComponent(sceneLocation);
		final Point screenLoc = getComponent() != null ? getComponent().getLocationOnScreen() : new Point(0,0);
		inComp.setLocation(inComp.getX() + screenLoc.x, inComp.getY() + screenLoc.y);
		return inComp;
	}

	/**
	 * Returns the currently visible area as rectangle in scene coordinates.
	 *
	 * @param src the visible screen coordinates
	 * @param store the area to store the scene coordinates into (may be null)
	 * @return
	 */
	public Rectangle2D getVisibleSceneRect(final Rectangle2D src, final Rectangle2D.Double store) {
		//		final Rectangle vr = mRenderTarget.getVisibleRect();
		//		final Rectangle2D rect = Utils.inverseTransform(vr, getViewTransform());
		return Utils.inverseTransform(src, getViewTransform(), store);
		//		return rect;
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
	public Point2D getSceneLocation(final Point point) {
		return getSceneLocation(point, new Point2D.Double());
	}
	public double getScaleX() { return mScaleX.get(); }
	public double getScaleY() { return mScaleY.get(); }

	public GraphicsScene getScene() { return mScene; }
	public ParameterContext getPropertyContext() { return mPropertyContext; }

	public <T> IParameter<T> getProperty(final String string) {
		return mPropertyContext.getProperty(string);
	}

	public void setRotation(final double degrees) {
		mRotation.set(degrees);
	}
	public double getRotationDegrees() { return mRotation.get(); }


	public void setCenter(final Point2D center) {
		setCenter(center.getX(), center.getY());
	}

	public void setCenterAndZoom(final Point2D min, final Point2D max, final boolean scaleXandY) {
		final double x = Math.min(min.getX(), max.getX());
		final double y = Math.min(min.getY(), max.getY());
		final double w = Math.abs(min.getX() - max.getX());
		final double h = Math.abs(min.getY() - max.getY());
		setCenterAndZoom(new Rectangle2D.Double(x, y, w, h), scaleXandY);
	}
	public void setCenterAndZoom(final Rectangle2D bounds, final boolean scaleXandY) {
		final Rectangle visRect = mRenderTarget.getVisibleRect();

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
	public void notifySceneCleared() {
		for (final IGraphicsViewHandler handler : mHandler)
			handler.notifySceneCleared();
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//							Mouse Handling - Forward
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/** Forward to the component of the underlying render target, if available */
	public void addMouseListener(final MouseListener listener) {
		ifNotNull(getComponent(), c -> c.addMouseListener(listener));
	}
	/** Forward to the component of the underlying render target, if available */
	public void addMouseMotionListener(final MouseMotionListener listener) {
		ifNotNull(getComponent(), c -> c.addMouseMotionListener(listener));
	}
	/** Forward to the component of the underlying render target, if available */
	public void addMouseWheelListener(final MouseWheelListener listener) {
		ifNotNull(getComponent(), c -> c.addMouseWheelListener(listener));
	}
	/** Forward to the component of the underlying render target, if available */
	public void removeMouseListener(final MouseListener listener) {
		ifNotNull(getComponent(), c -> c.removeMouseListener(listener));
	}
	/** Forward to the component of the underlying render target, if available */
	public void removeMouseMotionListener(final MouseMotionListener listener) {
		ifNotNull(getComponent(), c -> c.removeMouseMotionListener(listener));
	}
	/** Forward to the component of the underlying render target, if available */
	public void removeMouseWheelListener(final MouseWheelListener listener) {
		ifNotNull(getComponent(), c -> c.removeMouseWheelListener(listener));
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//							Render Target forwards
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Sets the value of a single preference for the rendering algorithms.
	 * Hint categories include controls for rendering quality and overall
	 * time/quality trade-off in the rendering process.  Refer to the
	 * {@code RenderingHints} class for definitions of some common
	 * keys and values.
	 * @param hintKey the key of the hint to be set.
	 * @param hintValue the value indicating preferences for the specified
	 * hint category.
	 * @see #getRenderingHint(RenderingHints.Key)
	 * @see RenderingHints
	 */
	public void setRenderingHint(final Key hintKey, final Object hintValue) {
		if (mRenderHints == null) mRenderHints = new RenderingHints(hintKey, hintValue);
		else mRenderHints.put(hintKey, hintValue);
	}

	/**
	 * Returns the value of a single preference for the rendering algorithms.
	 * Hint categories include controls for rendering quality and overall
	 * time/quality trade-off in the rendering process.  Refer to the
	 * {@code RenderingHints} class for definitions of some common
	 * keys and values.
	 * @param hintKey the key corresponding to the hint to get.
	 * @return an object representing the value for the specified hint key.
	 * Some of the keys and their associated values are defined in the
	 * {@code RenderingHints} class.
	 * @see RenderingHints
	 * @see #setRenderingHint(RenderingHints.Key, Object)
	 */
	public Object getRenderingHint(final Key hintKey) {
		if (mRenderHints == null)
			return null;
		return mRenderHints.get(hintKey);
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//							Statistics
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public DoubleSummaryStatistics getPaintDurationStatistic() { return mOverallStatitic;}
	public WindowStat getMovingWindowDurationStatistic() { return mWindowStatistic;}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//							Utils
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private <T> void ifNotNull(final T obj, final Consumer<T> func){
		if (obj != null)
			func.accept(obj);
	}

	private void markViewAsDirty() {
		synchronized (mRequestCounter) {
			mRequestCounter.incrementAndGet();

			if (mScheduledFuture == null) {
				mScheduledFuture = mScheduler.schedule(() -> {
					mScheduledFuture = null;
					triggerRTRepaint();
					return 0;
				}, mRepaintDelay, TimeUnit.MILLISECONDS);
			}
		}
	}

}
