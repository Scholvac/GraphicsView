package de.sos.gvc;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import de.sos.gvc.GraphicsScene.IItemFilter;
import de.sos.gvc.param.IParameter;
import de.sos.gvc.param.ParameterContext;

public interface IGraphicsView {

	String PROP_VIEW_CENTER_X = "VIEW_CENTER_X";
	String PROP_VIEW_CENTER_Y = "VIEW_CENTER_Y";
	String PROP_VIEW_SCALE_X = "VIEW_SCALE_X";
	String PROP_VIEW_SCALE_Y = "VIEW_SCALE_Y";
	String PROP_VIEW_ROTATE = "VIEW_ROTATE";

	void setMaximumFPS(int fps);

	void addPaintListener(IPaintListener listener);

	boolean removePaintListener(IPaintListener listener);

	void addHandler(IGraphicsViewHandler handler);

	void removeHandler(IGraphicsViewHandler handler);

	void setScale(double scaleXY);

	void setScale(double scaleX, double scaleY);

	void setCenter(double x, double y);

	double getCenterX();

	double getCenterY();

	Point2D getPositionInComponent(Point2D sceneLocation);

	Point2D getPositionOnScreen(Point2D sceneLocation);

	/**
	 * Returns the currently visible area as rectangle in scene coordinates.
	 * @return
	 */
	Rectangle2D getVisibleSceneRect();

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
	List<GraphicsItem> getItemsAt(Point point, double epsilonX, double epsilonY);

	/**
	 * returns a list of all items that intersect the rectangle defined by point and +/- epsilon
	 * this method also returns child items, if the parent and the child do pass the filter (if not null)
	 * @param point
	 * @param epsilonX
	 * @param epsilonX
	 * @param filter (may null)
	 * @return
	 */
	List<GraphicsItem> getAllItemsAt(Point point, double epsilonX, double epsilonY, IItemFilter filter);

	/**
	 * transforms the screen position input (screen) into scene location and stores the result in scene point
	 * @param screen
	 * @param scene (may null)
	 * @return scene point or new point if scene is null
	 */
	Point2D getSceneLocation(Point2D screen, Point2D scene);

	/**
	 * @see GraphicsView.getSceneLocation(Point2D, Point2D)
	 * @param point
	 * @return
	 */
	Point2D getSceneLocation(Point point);

	double getScaleX();

	double getScaleY();

	GraphicsScene getScene();

	ParameterContext getPropertyContext();

	<T> IParameter<T> getProperty(String string);

	void setRotation(double degrees);

	double getRotationDegrees();

	void setCenter(Point2D center);

	void setCenterAndZoom(Point2D min, Point2D max, boolean scaleXandY);

	void setCenterAndZoom(Rectangle2D bounds, boolean scaleXandY);

	/** Forward the cleared notification to all IGraphicsViewHandler that may relay on Scene content */
	void notifySceneCleared();

	void addMouseListener(MouseListener selectionHandler);
	void removeMouseListener(MouseListener selectionHandler);

	void addMouseMotionListener(MouseMotionListener handler);
	void removeMouseMotionListener(MouseMotionListener handler);

	void addMouseWheelListener(MouseWheelListener handler);
	void removeMouseWheelListener(MouseWheelListener handler);

	public Rectangle getBounds(Rectangle rv);
}