package de.sos.gvc.handler;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IGraphicsView;
import de.sos.gvc.IGraphicsViewHandler;
import de.sos.gvc.Utils;
import de.sos.gvc.handler.selection.BoundingBoxSelectionItem;
import de.sos.gvc.param.IParameter;
import de.sos.gvc.param.Parameter;

/**
 *
 * @author scholvac
 *
 */
public class SelectionHandler implements IGraphicsViewHandler, MouseListener {

	public static class ItemMoveEvent {
		public final List<GraphicsItem>					items;
		public final List<Point2D>						oldSceneLocations;
		public final List<Point2D>						newSceneLocations;
		public final List<Point2D>						moveDistance;

		public ItemMoveEvent(final List<GraphicsItem> items, final List<Point2D> move) {
			this.items = items;
			oldSceneLocations = new ArrayList<>();
			newSceneLocations = new ArrayList<>();
			this.moveDistance = move;
			for (int i = 0; i < move.size(); i++) {
				final Point2D p = items.get(i).getSceneLocation();
				oldSceneLocations.add(new Point2D.Double(p.getX(), p.getY()));
				newSceneLocations.add(new Point2D.Double(p.getX() + move.get(i).getX(), p.getY() + move.get(i).getY()));
			}
		}
		public ItemMoveEvent(final List<GraphicsItem> items, final List<Point2D> oldLoc, final List<Point2D> newLoc) {
			this.items = items;
			this.oldSceneLocations = oldLoc;
			this.newSceneLocations = newLoc;
			this.moveDistance = new ArrayList<>();
			for (int i = 0; i < newLoc.size(); i++)
				moveDistance.add(new Point2D.Double(newLoc.get(i).getX() - oldLoc.get(i).getX(), newLoc.get(i).getY()-oldLoc.get(i).getY()));
		}
	}

	public interface ISelectionCallback {
		/** notifies that an item has been selected. */
		public void onItemSelected(final List<GraphicsItem> items);
	}

	public interface IMoveCallback {
		public void onItemMoved(ItemMoveEvent event);
	}

	public static class ItemScaleEvent {
		public final List<GraphicsItem> 			items;
		public final List<Point2D[]> 				oldSceneVertices;
		public final List<Point2D[]> 				newSceneVertices;

		private List<Rectangle2D> 					mOldSceneBounds;
		private List<Rectangle2D> 					mNewSceneBounds;

		public ItemScaleEvent(final List<GraphicsItem> items, final List<Point2D[]> oldVertices, final List<Point2D[]> newVertices) {
			this.items = items;
			this.oldSceneVertices = oldVertices;
			this.newSceneVertices = newVertices;
		}

		public List<Rectangle2D> getOldSceneBounds() {
			if (mOldSceneBounds == null)
				mOldSceneBounds = Utils.verticesToRectangle(oldSceneVertices);
			return mOldSceneBounds;
		}
		public List<Rectangle2D> getNewSceneBounds() {
			if (mNewSceneBounds == null)
				mNewSceneBounds = Utils.verticesToRectangle(newSceneVertices);
			return mNewSceneBounds;
		}


		public double[] getScaleFactors(final int i) {
			final Rectangle2D or = getOldSceneBounds().get(i), nr = getNewSceneBounds().get(i);
			final double ow = or.getWidth(), nw = nr.getWidth();
			final double oh = or.getHeight(), nh = nr.getHeight();
			return new double[] { nw / ow, nh / oh};
		}
	}
	public interface IScaleCallback {
		public void onItemScaled(ItemScaleEvent event);
	}

	public static class ItemRotateEvent {

		/**
		 * Angle of items (in degrees) before the rotate operation started
		 */
		public final  List<Double> startAngles;
		/**
		 * The items that has been manipulated throught the rotate event
		 */
		public final  List<GraphicsItem> items;
		/**
		 * angles of the items after the rotation took place (in degrees)
		 */
		public final  List<Double> endAngles;

		public ItemRotateEvent(final List<GraphicsItem> items, final List<Double> startAngles, final List<Double> endAngles) {
			this.items = items;
			this.startAngles = startAngles;
			this.endAngles = endAngles;
		}

	}
	public interface IRotateCallback {
		public void onItemRotated(ItemRotateEvent event);
	}

	private IGraphicsView 					mView;
	private GraphicsItem 					mLastSelectedItem 		= null;
	private GraphicsItem					mSelectionMarker		= null;

	private IParameter<Double>				mEpsilon 				= new Parameter<>("Epsilon", "Selection Tolerance", true, 5.0);

	private ArrayList<IMoveCallback>		mMoveCallbacks = new ArrayList<>();
	private ArrayList<IScaleCallback>		mScaleCallbacks = new ArrayList<>();
	private ArrayList<IRotateCallback>		mRotationCallbacks = new ArrayList<>();
	private ArrayList<ISelectionCallback>	mSelectionCallbacks = new ArrayList<>();


	public SelectionHandler() {
	}

	/** register a new selection callback. The callback will be notified, if a new item was selected.
	 * @note: this interface prevent the caller from register a listener on each GraphicsItem.
	 */
	public void addSelectionCallback(final ISelectionCallback sh) {
		if (sh != null && !mSelectionCallbacks.contains(sh))
			mSelectionCallbacks.add(sh);
	}
	public void removeSelectionCallback(final ISelectionCallback sh) {
		mSelectionCallbacks.remove(sh);
	}
	public boolean hasSelectionCallbacks() { return mSelectionCallbacks != null && mSelectionCallbacks.isEmpty() == false;}

	public void addMoveCallback(final IMoveCallback mh) {
		if (mh != null && !mMoveCallbacks.contains(mh))
			mMoveCallbacks.add(mh);
	}
	public void removeMoveCallback(final IMoveCallback mh) {
		mMoveCallbacks.remove(mh);
	}
	public boolean hasMoveCallbacks() { return mMoveCallbacks != null && !mMoveCallbacks.isEmpty(); }

	public void addScaleCallback(final IScaleCallback sc) {
		if (sc != null && !mScaleCallbacks.contains(sc))
			mScaleCallbacks.add(sc);
	}
	public void removeScaleCallback(final IScaleCallback sc) {
		mScaleCallbacks.remove(sc);
	}
	public boolean hasScaleCallbacks() { return mScaleCallbacks != null && !mScaleCallbacks.isEmpty(); }

	public void addRotationCallback(final IRotateCallback rc) {
		if (rc != null && !mRotationCallbacks.contains(rc))
			mRotationCallbacks.add(rc);
	}
	public void removeRotationCallback(final IRotateCallback rc) {
		mRotationCallbacks.remove(rc);
	}
	public boolean hasRotationCallbacks() { return mRotationCallbacks != null && !mRotationCallbacks.isEmpty(); }

	@Override
	public void install(final IGraphicsView view) {
		mView = view;
		mView.addMouseListener(this);
	}

	@Override
	public void uninstall(final IGraphicsView view) {
		mView.removeMouseListener(this);
		mView = null;
	}


	@Override
	public void mouseClicked(final MouseEvent e) {
		if (e.isConsumed())
			return ;
		final GraphicsItem item = getBestFit(e.getPoint());
		if (item == mLastSelectedItem) {
			return ; //do nothing also do not consume the event to give other the chance to work on the selected item
		}


		if (mLastSelectedItem != null)
			uninstallSelectionMarker(); //since its not the same, we do a cleanup and eventually reinitialize it in the next step
		if (item != null) {
			installSelectionMarker(item);
			fireSelectionEvent(item);
		}

		e.consume();
	}

	protected GraphicsItem getSelectionMarker(final GraphicsItem item) {
		final BoundingBoxSelectionItem sbi = new BoundingBoxSelectionItem(this);
		sbi.setSelectedItem(item);
		return sbi;
	}

	private void installSelectionMarker(final GraphicsItem item) {
		mLastSelectedItem = item; //remember for next time

		mSelectionMarker = getSelectionMarker(item);
		if (mSelectionMarker != null) {
			mSelectionMarker.setVisible(true);
			mView.getScene().addItem(mSelectionMarker);
		}

		item.setSelected(true);
		onItemSelected(item); //notify subclasses
	}

	/**
	 * 	This method may be overwritten by subclasses, to get notified if an item has been selected
	 * @param item
	 */
	protected void onItemSelected(final GraphicsItem item) {
		// This method may be overwritten by subclasses, to get notified if an item has been selected
	}
	private void uninstallSelectionMarker() {
		onItemDeselected(mLastSelectedItem);
		mLastSelectedItem.setSelected(false);
		mLastSelectedItem = null;

		if (mSelectionMarker != null) {
			mSelectionMarker.setVisible(false);
			mView.getScene().removeItem(mSelectionMarker);

		}
		mSelectionMarker = null;
	}

	/**
	 * This method may be overwritten by subclasses, to get notified if an item has been deselected
	 * @param item
	 */
	protected void onItemDeselected(final GraphicsItem item) {
		// This method may be overwritten by subclasses, to get notified if an item has been deselected
	}
	private GraphicsItem getBestFit(final Point point) {
		return Utils.getBestFit(mView, point, mEpsilon.get(), GraphicsItem::isSelectable);
	}

	@Override
	public void mousePressed(final MouseEvent e) { }
	@Override
	public void mouseReleased(final MouseEvent e) { }
	@Override
	public void mouseEntered(final MouseEvent e) { }
	@Override
	public void mouseExited(final MouseEvent e) { }

	public void fireMoveEvent(final ItemMoveEvent evt) {
		for (final IMoveCallback mc : mMoveCallbacks) {
			mc.onItemMoved(evt);
		}
	}

	public ItemMoveEvent createMoveEvent(final MouseEvent e) {
		final List<GraphicsItem> items = Arrays.asList(mLastSelectedItem);
		final Point2D ol = mLastSelectedItem.getSceneLocation();
		final List<Point2D> oldLoc = Arrays.asList(new Point2D.Double(ol.getX(), ol.getY()));
		final List<Point2D> newLoc = Arrays.asList(mSelectionMarker.getSceneLocation());
		final ItemMoveEvent evt = new ItemMoveEvent(items, oldLoc, newLoc);
		return evt;
	}

	public void fireSelectionEvent(final GraphicsItem item) {
		final List<GraphicsItem> listOfItems = Arrays.asList(item);
		for (final ISelectionCallback sc : mSelectionCallbacks) {
			sc.onItemSelected(listOfItems);
		}
	}

	public void fireScaleEvent(final ItemScaleEvent evt) {
		for (final IScaleCallback sc : mScaleCallbacks) {
			sc.onItemScaled(evt);
		}
	}

	public void fireScaleEvent(final Point2D[] oldVertices, final Point2D[] newVertices) {
		final ArrayList<Point2D[]> ov = new ArrayList<>();
		ov.add(oldVertices);
		final ArrayList<Point2D[]> nv = new ArrayList<>();
		nv.add(newVertices);
		fireScaleEvent(new ItemScaleEvent(Arrays.asList(mLastSelectedItem), ov, nv));
	}


	public void fireRotationEvent(final ItemRotateEvent evt) {
		for (final IRotateCallback sc : mRotationCallbacks) {
			sc.onItemRotated(evt);
		}
	}
	public void fireRotateEvent(final double startDegrees, final double endDegrees) {
		fireRotationEvent(new ItemRotateEvent(Arrays.asList(mLastSelectedItem), Arrays.asList(startDegrees), Arrays.asList(endDegrees)));
	}
}
