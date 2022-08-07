package de.sos.gvc;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.sos.gvc.storage.ListStorage;
import de.sos.gvc.storage.QuadTreeStorage;

/**
 *
 * @author scholvac
 *
 */
public class GraphicsScene {

	public interface DirtyListener {
		/** Called if the dirty state changes to clean */
		void notifyClean();
		/** called if the dirty state change from clean to dirty */
		void notifyDirty();
	}

	public interface IItemFilter {
		public boolean accept(GraphicsItem item);

		public static IItemFilter combound(final IItemFilter first, final IItemFilter[] filter) {
			return combound(first, Arrays.asList(filter));
		}

		public static IItemFilter combound(final IItemFilter first, final Collection<IItemFilter> additional) {
			if (additional == null || additional.isEmpty())
				return first;
			final LinkedList<IItemFilter> filter = new LinkedList<>();
			if (first != null)
				filter.add(first);
			for (final IItemFilter add : additional)
				if (add != null)
					filter.add(add);
			if(filter.size() == 1)
				return filter.get(0);
			return new ComboundItemFilter(filter);
		}
	}


	public static class ComboundItemFilter implements IItemFilter {
		private final Collection<IItemFilter> 	mFilter;
		public ComboundItemFilter(final IItemFilter ...filters) {
			this(Arrays.asList(filters));
		}
		public ComboundItemFilter(final Collection<IItemFilter> filters) {
			mFilter = filters;
		}
		@Override
		public boolean accept(final GraphicsItem item) {
			try {
				for (final IItemFilter f : mFilter){
					if (!f.accept(item))
						return false;
				}
				return true;
			}catch(final Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}


	public static class RectangleSelectionFilter implements IItemFilter {
		private Rectangle2D 		mQuery;
		private boolean 			mSceneCoordinates;

		public RectangleSelectionFilter(final Point2D point, final double epsilon, final boolean sceneCoordinates) {
			this(new Rectangle2D.Double(point.getX() - epsilon/2.0, point.getY()-epsilon/2.0, epsilon, epsilon), sceneCoordinates);
		}

		public RectangleSelectionFilter(final Rectangle2D rect, final boolean sceneCoordinates) {
			mQuery = rect;
			mSceneCoordinates = sceneCoordinates;
		}

		@Override
		public boolean accept(final GraphicsItem item) {
			final Shape s = item.getShape();
			if (s == null) return false;
			if (mSceneCoordinates) {
				//first check the bounding box, if that fit, we also check the shape, otherwise we can skip the expensive test
				final Rectangle2D sb = item.getSceneBounds();
				if (sb.contains(mQuery) || sb.intersects(mQuery)) {
					return true;
				}
			}else {
				final Rectangle2D sb = item.getLocalBounds();
				if (sb.contains(mQuery) || sb.intersects(mQuery)) {
					return true;
				}
			}
			return false;
		}
	}
	public static class ShapeSelectionFilter implements IItemFilter {
		private Rectangle2D 		mQuery;
		private boolean 			mSceneCoordinates;

		public ShapeSelectionFilter(final Point2D point, final double epsilon, final boolean sceneCoordinates) {
			this(new Rectangle2D.Double(point.getX() - epsilon/2.0, point.getY()-epsilon/2.0, epsilon, epsilon), sceneCoordinates);
		}

		public ShapeSelectionFilter(final Rectangle2D rect, final boolean sceneCoordinates) {
			mQuery = rect;
			mSceneCoordinates = sceneCoordinates;
		}

		@Override
		public boolean accept(final GraphicsItem item) {
			final Shape s = item.getShape();
			if (s == null) return false;
			if (mSceneCoordinates) {
				//first check the bounding box, if that fit, we also check the shape, otherwise we can skip the expensive test
				final Rectangle2D sb = item.getSceneBounds();
				if (sb.contains(mQuery) || sb.intersects(mQuery)) {
					final Rectangle2D sceneLocal = Utils.inverseTransform(mQuery, item.getWorldTransform()); //its faster to convert the 4 points of the query into the coordinates of the shape as converting the complex shape
					if (s.contains(sceneLocal) || s.intersects(sceneLocal))
						return true;
				}
			}else {
				final Rectangle2D sb = item.getLocalBounds();
				if (sb.contains(mQuery) || sb.intersects(mQuery)) {
					final Rectangle2D localLocal = Utils.inverseTransform(mQuery, item.getLocalTransform()); //its faster to convert the 4 points of the query into the coordinates of the shape as converting the complex shape
					if (s.contains(localLocal) || s.intersects(localLocal))
						return true;
				}
			}
			return false;
		}
	}

	class ItemListener implements PropertyChangeListener {
		//		IParameter<Boolean> mDP = null;
		//		public ItemListener(IParameter<Boolean> dp) { mDP = dp;}
		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			if (!mDirty) {
				markDirty();
			}
		}
	}

	/** Property - key to get notified (with PropertyChangeEvent) about adding and removing of new Items into the Root item list */
	public static final String						ITEM_LIST_PROPERTY 					= "ItemListProperty";

	private IItemStorage							mItemStore = new QuadTreeStorage();
	private ItemListener							mItemListener;

	private List<GraphicsView> 						mViews = new ArrayList<>();

	private boolean									mDirty = true;
	private ArrayList<DirtyListener>				mDirtyListener = new ArrayList<>();

	private transient PropertyChangeSupport 		mPropertySupport = new PropertyChangeSupport(this);

	public GraphicsScene() {
		this(new ListStorage());
	}
	public GraphicsScene(final IItemStorage itemStore) {
		mItemStore = itemStore;
		mItemListener = new ItemListener();
	}

	public void addPropertyListener(final PropertyChangeListener pcl) {
		if (pcl != null)
			mPropertySupport.addPropertyChangeListener(pcl);
	}
	public void addPropertyListener(final String propertyName, final PropertyChangeListener pcl) {
		if (pcl != null)
			mPropertySupport.addPropertyChangeListener(propertyName, pcl);
	}
	public void removePropertyListener(final PropertyChangeListener pcl) {
		mPropertySupport.removePropertyChangeListener(pcl);
	}
	public void removePropertyListener(final String propertyName, final PropertyChangeListener pcl) {
		mPropertySupport.removePropertyChangeListener(propertyName, pcl);
	}



	public boolean registerDirtyListener(final DirtyListener dl) {
		if (dl != null && !mDirtyListener.contains(dl))
			return mDirtyListener.add(dl);
		return false;
	}
	public boolean removeDirtyListener(final DirtyListener dl) {
		if (dl == null || !mDirtyListener.contains(dl))
			return true;
		return mDirtyListener.remove(dl);
	}




	/**
	 * Adds a view that displays the scene to its internal list
	 * @param view
	 */
	void _addView(final GraphicsView view) {
		mViews.add(view);
	}
	void _removeView(final GraphicsView view) {
		mViews.remove(view);
	}

	/**
	 * Adds a list of items
	 * @param items
	 * @return true if all items has been added, false otherwise.
	 * @deprecated(since = 1.6) use addItems instead.
	 */
	@Deprecated
	public boolean addItem(final GraphicsItem... items) {
		return addItems(items);
	}

	/**
	 * Adds a list of items
	 * @param items
	 * @return true if all items has been added, false otherwise.
	 */
	public boolean addItems(final GraphicsItem... items) {
		return addItems(Arrays.asList(items));
	}

	/**
	 * Adds a list of items
	 * @param items
	 * @return true if all items has been added, false otherwise.
	 */
	public boolean addItems(final Collection<GraphicsItem> items) {
		boolean result = true;
		for (final GraphicsItem item : items)
			result = result & addItem(item);
		return result;
	}
	public boolean addItem(final GraphicsItem item) {
		if (item == null)
			return false;
		if (item.getScene() == this)
			return true;//alread inside but no notification
		if (mItemStore.addItem(item)) {
			item._setScene(this);
			item.addPropertyChangeListener(mItemListener);
			markDirty();

			mPropertySupport.firePropertyChange(ITEM_LIST_PROPERTY, null, item);
			return true;
		}
		return false;
	}
	public void markDirty() {
		if (!mDirty) {
			mDirty = true;
			//notify listener
			for (final DirtyListener element : mDirtyListener)
				element.notifyDirty();
		}
	}

	/** Resets the dirty state.
	 * @note this method shall only be called by the GraphicsView after drawing the current scene
	 */
	void markClean() {
		if (mDirty) {
			mDirty = false;
			//notify listener
			for (final DirtyListener element : mDirtyListener)
				element.notifyClean();
		}
	}

	/**
	 * removes a list of items
	 * @param items
	 * @return true if all items has been removed, false otherwise.
	 * @deprecated(since = 1.6) use removeItems instead
	 */
	@Deprecated
	public boolean removeItem(final GraphicsItem ...items) {
		return removeItems(Arrays.asList(items));
	}

	/**
	 * removes a list of items
	 * @param items
	 * @return true if all items has been removed, false otherwise.
	 */
	public boolean removeItems(final GraphicsItem ...items) {
		return removeItems(Arrays.asList(items));
	}
	/**
	 * removes a list of items
	 * @param items
	 * @return true if all items has been removed, false otherwise.
	 */
	public boolean removeItems(final Collection<GraphicsItem> items) {
		if (items == null) return false;
		boolean res = true;
		for (final GraphicsItem item : items)
			res &= removeItem(item);
		return res;
	}
	public boolean removeItem(final GraphicsItem item) {
		if (item == null) return false;
		if (mItemStore.removeItem(item)) {
			item.removePropertyChangeListener(mItemListener);
			item._setScene(null);
			markDirty();

			mPropertySupport.firePropertyChange(ITEM_LIST_PROPERTY, item, null);
			return true;
		}
		return false;
	}

	public List<GraphicsItem> getItems(final Rectangle2D rect){
		return mItemStore.getItems(rect, null);
	}
	public List<GraphicsItem> getItems(final Rectangle2D rect, final IItemFilter filter){
		return mItemStore.getItems(rect, filter);
	}
	public List<GraphicsItem> getAllItems(final Rectangle2D rect, final IItemFilter filter) {
		final List<GraphicsItem> topLevel = getItems(rect, null);
		return getAllItems(rect, topLevel, filter);
	}

	/**
	 * Return all (top level features but also child items that match the following conditions
	 * 1) item is visible
	 * 2) item is within the rectangle
	 * 3) item is accepted by the filter
	 * @param rect
	 * @param items
	 * @param filter
	 * @return
	 */
	public List<GraphicsItem> getAllItems(final Rectangle2D rect, final List<GraphicsItem> items, final IItemFilter filter) {
		final ArrayList<GraphicsItem> out = new ArrayList<>();
		//first find all top level features and iterate only their children. all other would not meet the 2) condition
		final List<GraphicsItem> openList = getItems(rect, items, null); //@note do not use the filter here, to not exclude childen whose parent does not fit the fiilter but the child itself would pass the filter
		while(!openList.isEmpty()) {
			final GraphicsItem first = openList.remove(0);
			if (filter == null || filter.accept(first))
				out.add(first);
			if (first.hasChildren()) {
				for (final GraphicsItem child : first.getChildren()) {
					//we do not now if the child is part of the box, may it another child that let the parent be inside the box
					if (!child.isVisible())
						continue;
					final Rectangle2D wb = child.getSceneBounds();
					if (rect.contains(wb) || rect.intersects(wb)) {
						openList.add(child);
					}
				}
			}
		}
		Collections.reverse(out);
		return out;
	}

	/**
	 * return all top level (Items that have no parent) that are
	 * 1) visible
	 * 2) within the rectangle
	 * 3) match the filter (may be a combound filter)
	 * @param rect
	 * @param items
	 * @param filter
	 * @return
	 */
	public List<GraphicsItem> getItems(final Rectangle2D rect, final Collection<GraphicsItem> items, final IItemFilter filter) {
		final ArrayList<GraphicsItem> out = new ArrayList<>();
		for (final GraphicsItem item : items) {
			if (!item.isVisible()) continue;
			final Rectangle2D wb = item.getSceneBounds();
			if (intersect(wb, rect)) {
				if (filter == null || filter.accept(item))
					out.add(item);
			}else {
				//				System.out.println("Skip: " + rect + " AND: " + wb);
				//				System.out.println("Compare: \n\t" + rect2WKT(rect) + "\n\t" + rect2WKT(wb) + "\n");
			}
		}
		return out;
	}


	public static String rect2WKT(final Rectangle2D r) {
		final Point2D[] v = getVertices(r);
		final StringBuilder out = new StringBuilder("POLYGON((");
		for (final Point2D p : v) {
			out.append(p.getX()).append(" ").append(p.getY()).append(", ");
		}
		out.append(v[0].getX()).append(" ").append(v[0].getY());
		out.append("))");
		return out.toString();
	}
	public static Point2D[] getVertices(final Rectangle2D r) {
		return new Point2D[] {
				new Point2D.Double(r.getMinX(), r.getMinY()),
				new Point2D.Double(r.getMinX(), r.getMaxY()),
				new Point2D.Double(r.getMaxX(), r.getMaxY()),
				new Point2D.Double(r.getMaxX(), r.getMinY()),
		};
	}

	private boolean intersect(final Rectangle2D a, final Rectangle2D b) {
		return b.intersects(a) || b.contains(a);
	}


	public List<GraphicsItem> getItems() {
		return mItemStore.getAllItems();
	}

	public List<GraphicsView> getViews() {
		return mViews;
	}

	public void clear() {
		final List<GraphicsItem> itemsToRemove = mItemStore.getAllItems();
		for (final GraphicsItem item : itemsToRemove)
			removeItem(item);
		for (final GraphicsView view : mViews)
			view.notifySceneCleared(); //some view's may cache items (e.g. tiles) and become notified to update caches...
		markDirty();
	}


}
