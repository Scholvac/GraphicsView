package de.sos.gvc;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.sos.gvc.param.IParameter;
import de.sos.gvc.param.Parameter;
import de.sos.gvc.storage.ListStorage;
import de.sos.gvc.storage.QuadTreeStorage;

/**
 * 
 * @author scholvac
 *
 */
public class GraphicsScene {

	public interface IItemFilter {
		public boolean accept(GraphicsItem item);
	}
	
	public static class ComboundItemFilter implements IItemFilter {
		private IItemFilter[] mFilter;
		public ComboundItemFilter(IItemFilter ...filters) {
			mFilter = filters;
		}
		@Override
		public boolean accept(GraphicsItem item) {
			try {
				for (IItemFilter f : mFilter){
					if (!f.accept(item))
						return false;
				}
				return true;
			}catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	
	
	public static class RectangleSelectionFilter implements IItemFilter {
		private Rectangle2D 		mQuery;
		private boolean 			mSceneCoordinates;

		public RectangleSelectionFilter(Point2D point, double epsilon, boolean sceneCoordinates) {
			this(new Rectangle2D.Double(point.getX() - epsilon/2.0, point.getY()-epsilon/2.0, epsilon, epsilon), sceneCoordinates);
		}

		public RectangleSelectionFilter(Rectangle2D rect, boolean sceneCoordinates) {
			mQuery = rect;
			mSceneCoordinates = sceneCoordinates;
		}
		
		@Override
		public boolean accept(GraphicsItem item) {
			Shape s = item.getShape();
			if (s == null) return false;
			if (mSceneCoordinates) {
				//first check the bounding box, if that fit, we also check the shape, otherwise we can skip the expensive test
				Rectangle2D sb = item.getSceneBounds();
				if (sb.contains(mQuery) || sb.intersects(mQuery)) {
					return true;
				}
			}else {
				Rectangle2D sb = item.getLocalBounds();
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

		public ShapeSelectionFilter(Point2D point, double epsilon, boolean sceneCoordinates) {
			this(new Rectangle2D.Double(point.getX() - epsilon/2.0, point.getY()-epsilon/2.0, epsilon, epsilon), sceneCoordinates);
		}

		public ShapeSelectionFilter(Rectangle2D rect, boolean sceneCoordinates) {
			mQuery = rect;
			mSceneCoordinates = sceneCoordinates;
		}
		
		@Override
		public boolean accept(GraphicsItem item) {
			Shape s = item.getShape();
			if (s == null) return false;
			if (mSceneCoordinates) {
				//first check the bounding box, if that fit, we also check the shape, otherwise we can skip the expensive test
				Rectangle2D sb = item.getSceneBounds();
				if (sb.contains(mQuery) || sb.intersects(mQuery)) {
					Rectangle2D sceneLocal = Utils.inverseTransform(mQuery, item.getWorldTransform()); //its faster to convert the 4 points of the query into the coordinates of the shape as converting the complex shape
					if (s.contains(sceneLocal) || s.intersects(sceneLocal))
						return true;
				}
			}else {
				Rectangle2D sb = item.getLocalBounds();
				if (sb.contains(mQuery) || sb.intersects(mQuery)) {
					Rectangle2D localLocal = Utils.inverseTransform(mQuery, item.getLocalTransform()); //its faster to convert the 4 points of the query into the coordinates of the shape as converting the complex shape
					if (s.contains(localLocal) || s.intersects(localLocal))
						return true;
				}
			}
			return false;
		}
	}
	
	class ItemListener implements PropertyChangeListener {
		IParameter<Boolean> mDP = null;
		public ItemListener(IParameter<Boolean> dp) { mDP = dp;}
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			mDP.set(true);
		}
	}
	
	private IParameter<Boolean>			mDirty = new Parameter<>("Dirty", "", true, false);
	
	private IItemStorage				mItemStore = new QuadTreeStorage();
	private ItemListener				mItemListener;

	private List<GraphicsView> 			mViews = new ArrayList<>();
	
	
	public IParameter<Boolean> getDirtyProperty() { return mDirty; }
	
	
	public GraphicsScene() {
		 this(new ListStorage());
	}
	public GraphicsScene(IItemStorage itemStore) {
		mItemStore = itemStore;
		mItemListener = new ItemListener(mDirty);
	}
	
	/**
	 * Adds a view that displays the scene to its internal list
	 * @param view
	 */
	void _addView(GraphicsView view) {
		mViews.add(view);
	}
	void _removeView(GraphicsView view) {
		mViews.remove(view);
	}
	
	public boolean addItem(GraphicsItem... items) {
		boolean result = true;
		for (GraphicsItem item : items)
			result = result & addItem(item);
		return result;
	}
	public boolean addItem(GraphicsItem item) {
		if (item == null) return false;
		if (mItemStore.addItem(item)) {
			item._setScene(this);;
			item.addPropertyChangeListener(mItemListener);
			markDirty();
			return true;
		}
		return false;
	}
	public void markDirty() {
		mDirty.set(true);
	}


	public boolean removeItem(GraphicsItem item) {
		if (item == null) return false;
		if (mItemStore.removeItem(item)) {
			item.removePropertyChangeListener(mItemListener);
			item._setScene(null);
			markDirty();
			return true;
		}
		return false;				
	}

	public List<GraphicsItem> getItems(Rectangle2D rect){
		return mItemStore.getItems(rect, null);
	}
	public List<GraphicsItem> getItems(Rectangle2D rect, IItemFilter filter){
		return mItemStore.getItems(rect, filter);
	}
	public List<GraphicsItem> getAllItems(Rectangle2D rect, IItemFilter filter) {
		List<GraphicsItem> topLevel = getItems(rect, filter);
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
	public List<GraphicsItem> getAllItems(Rectangle2D rect, List<GraphicsItem> items, IItemFilter filter) {
		ArrayList<GraphicsItem> out = new ArrayList<>();
		//first find all top level features and iterate only their children. all other would not meet the 2) condition
		List<GraphicsItem> openList = getItems(rect, items, filter);
		while(openList.isEmpty() == false) {
			GraphicsItem first = openList.remove(0);
			out.add(first);
			if (first.hasChildren()) {
				for (GraphicsItem child : first.getChildren()) {
					//we do not now if the child is part of the box, may it another child that let the parent be inside the box
					if (child.isVisible() == false) 
						continue;
					Rectangle2D wb = child.getSceneBounds();
					if (rect.contains(wb) || rect.intersects(wb)) {
						if (filter == null || filter.accept(child))
							openList.add(child);
					}
				}
			}
		}
//		for (GraphicsItem item : items) {
////			if (!item.isVisible()) continue;
//			Rectangle2D wb = item.getSceneBounds();
//			if (rect.contains(wb) || rect.intersects(wb)) {
//				if (filter == null || filter.accept(item)) {
//					out.add(item);
//				}
//				if (item.hasChildren()) {
//					List<GraphicsItem> tmp = getAllItems(rect, item.getChildren(), filter);
//					if (tmp != null && tmp.isEmpty() == false)
//						out.addAll(tmp);
//				}
//			}
//		}
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
	public List<GraphicsItem> getItems(Rectangle2D rect, Collection<GraphicsItem> items, IItemFilter filter) {
		ArrayList<GraphicsItem> out = new ArrayList<>();
		for (GraphicsItem item : items) {
			if (!item.isVisible()) continue;
			Rectangle2D wb = item.getSceneBounds();
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

	
	public static String rect2WKT(Rectangle2D r) {
		Point2D[] v = getVertices(r);
		String out = "POLYGON((";
		for (Point2D p : v) {
			out += p.getX() + " " + p.getY() + ", ";
		}
		out += v[0].getX() + " " + v[0].getY();
		out += "))";
		return out;
	}
	public static Point2D[] getVertices(Rectangle2D r) {
		return new Point2D[] {
			new Point2D.Double(r.getMinX(), r.getMinY()),
			new Point2D.Double(r.getMinX(), r.getMaxY()),
			new Point2D.Double(r.getMaxX(), r.getMaxY()),
			new Point2D.Double(r.getMaxX(), r.getMinY()),
		};
	}

	private boolean intersect(Rectangle2D a, Rectangle2D b) {
		return b.intersects(a) || b.contains(a);
//		double minq = Math.min(b.getMinX(), b.getMaxX());
//		double maxq = Math.max(b.getMinX(), b.getMaxX());
//		double minp = Math.min(a.getMinX(), a.getMaxX());
//		double maxp = Math.max(a.getMinX(), a.getMaxX());
//		if (minp > maxq)
//			return false;
//		if (maxp < minq)
//			return false;
//		
//		double t = -b.getMinY() + b.getHeight();
//		double ay = a.getMinY();
//		
//		minq = Math.min(b.getMinY(), -b.getMaxY());
//		maxq = Math.max(b.getMinY(), -b.getMaxY());
//		minp = Math.min(a.getMinY(), -a.getMaxY());
//		maxp = Math.max(a.getMinY(), -a.getMaxY());
//		if (minp > maxq)
//			return false;
//		if (maxp < minq)
//			return false;
//		
//		return true;
	}


	public List<GraphicsItem> getItems() {
		return mItemStore.getAllItems();
	}

	public List<GraphicsView> getViews() {
		return mViews;
	}




	
}
