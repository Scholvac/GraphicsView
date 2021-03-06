package de.sos.gvc.handler;

import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene.IItemFilter;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IGraphicsViewHandler;
import de.sos.gvc.Utils;
import de.sos.gvc.handler.selection.BoundingBoxSelectionItem;
import de.sos.gvc.handler.selection.SelectionBorderItem;
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
		
		public ItemMoveEvent(List<GraphicsItem> items, List<Point2D> move) {
			this.items = items;
			oldSceneLocations = new ArrayList<>();
			newSceneLocations = new ArrayList<>();
			this.moveDistance = move;
			for (int i = 0; i < move.size(); i++) {
				Point2D p = items.get(i).getSceneLocation();
				oldSceneLocations.add(new Point2D.Double(p.getX(), p.getY()));
				newSceneLocations.add(new Point2D.Double(p.getX() + move.get(i).getX(), p.getY() + move.get(i).getY()));
			}
		}
		public ItemMoveEvent(List<GraphicsItem> items, List<Point2D> oldLoc, List<Point2D> newLoc) {
			this.items = items;
			this.oldSceneLocations = oldLoc;
			this.newSceneLocations = newLoc;
			this.moveDistance = new ArrayList<>();
			for (int i = 0; i < newLoc.size(); i++)
				moveDistance.add(new Point2D.Double(newLoc.get(i).getX() - oldLoc.get(i).getX(), newLoc.get(i).getY()-oldLoc.get(i).getY()));
		}
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
	
		public ItemScaleEvent(List<GraphicsItem> items, List<Point2D[]> oldVertices, List<Point2D[]> newVertices) {
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


		public double[] getScaleFactors(int i) {
			Rectangle2D or = getOldSceneBounds().get(i), nr = getNewSceneBounds().get(i);
			double ow = or.getWidth(), nw = nr.getWidth();
			double oh = or.getHeight(), nh = nr.getHeight();
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

		public ItemRotateEvent(List<GraphicsItem> items, List<Double> startAngles, List<Double> endAngles) {
			this.items = items;
			this.startAngles = startAngles;
			this.endAngles = endAngles;
		}
		
	}
	public interface IRotateCallback {
		public void onItemRotated(ItemRotateEvent event);
	}
	
	private GraphicsView 				mView;
	private GraphicsItem 				mLastSelectedItem 		= null;
//	private SelectionBorderItem			mSelectionMarker 		= null;
	private GraphicsItem				mSelectionMarker		= null;
	
	private IParameter<Double>			mEpsilon 				= new Parameter<>("Epsilon", "Selection Tolerance", true, 5.0);
	private IItemFilter 				mSelectableFilter 		= new IItemFilter() {			
		@Override
		public boolean accept(GraphicsItem item) {
			return item.isSelectable();
		}
	};
	private ArrayList<IMoveCallback>	mMoveCallbacks = new ArrayList<>();
	private ArrayList<IScaleCallback>	mScaleCallbacks = new ArrayList<>();
	private ArrayList<IRotateCallback>	mRotationCallbacks = new ArrayList<>();
	
	
	public SelectionHandler() {
//		mSelectionMarker = new SelectionBorderItem(this);
	}
		
	public void addMoveCallback(IMoveCallback mh) {
		if (mh != null && mMoveCallbacks.contains(mh) == false)
		mMoveCallbacks.add(mh);
	}
	public void removeMoveCallback(IMoveCallback mh) {
		mMoveCallbacks.remove(mh);
	}
	public boolean hasMoveCallbacks() { return mMoveCallbacks != null && mMoveCallbacks.isEmpty() == false; }
	
	public void addScaleCallback(IScaleCallback sc) {
		if (sc != null && mScaleCallbacks.contains(sc) == false)
		mScaleCallbacks.add(sc);
	}
	public void removeScaleCallback(IScaleCallback sc) {
		mScaleCallbacks.remove(sc);
	}
	public boolean hasScaleCallbacks() { return mScaleCallbacks != null && mScaleCallbacks.isEmpty() == false; }
	
	public void addRotationCallback(IRotateCallback rc) {
		if (rc != null && mRotationCallbacks.contains(rc) == false)
		mRotationCallbacks.add(rc);
	}
	public void removeRotationCallback(IRotateCallback rc) {
		mRotationCallbacks.remove(rc);
	}
	public boolean hasRotationCallbacks() { return mRotationCallbacks != null && mRotationCallbacks.isEmpty() == false; }
	
	@Override
	public void install(GraphicsView view) {
		mView = view;
		mView.addMouseListener(this);
	}

	@Override
	public void uninstall(GraphicsView view) {
		mView.removeMouseListener(this);
		mView = null;
	}
	
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.isConsumed())
			return ;
		GraphicsItem item = getBestFit(e.getPoint());
		if (item == mLastSelectedItem) {
			return ; //do nothing also do not consume the event to give other the chance to work on the selected item
		}
		
		
		if (mLastSelectedItem != null)
			uninstallSelectionMarker(); //since its not the same, we do a cleanup and eventually reinitialize it in the next step
		if (item != null)
			installSelectionMarker(item);
		
		e.consume();
	}

	protected GraphicsItem getSelectionMarker(GraphicsItem item) {
		//return new ShapeSelectionMarker(this, item);
//		SelectionBorderItem sbi = new SelectionBorderItem(this);
		BoundingBoxSelectionItem sbi = new BoundingBoxSelectionItem(this);
		sbi.setSelectedItem(item);
		return sbi;
	}
	
	private void installSelectionMarker(GraphicsItem item) {
		mLastSelectedItem = item; //remember for next time
		
		mSelectionMarker = getSelectionMarker(item);
		if (mSelectionMarker != null) {
			mSelectionMarker.setVisible(true);
			mView.getScene().addItem(mSelectionMarker);
		}
		
//		mSelectionMarker.setSelectedItem(item);
//		mSelectionMarker.setVisible(true);
//		mView.getScene().addItem(mSelectionMarker);
		item.setSelected(true);		
		onItemSelected(item); //notify subclasses
	}
	/**
	 * 	This method may be overwritten by subclasses, to get notified if an item has been selected
	 * @param item
	 */
	protected void onItemSelected(GraphicsItem item) {
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
//		mSelectionMarker.setSelectedItem(null);
//		mSelectionMarker.setVisible(false);
//		mView.getScene().removeItem(mSelectionMarker);
	}
	
	/**
	 * This method may be overwritten by subclasses, to get notified if an item has been deselected
	 * @param item
	 */
	protected void onItemDeselected(GraphicsItem item) {
		// This method may be overwritten by subclasses, to get notified if an item has been deselected
	}
	private GraphicsItem getBestFit(Point point) {
		Point2D scene = mView.getSceneLocation(point, null);
		double eps = mEpsilon.get() * mView.getScaleX();
		
		List<GraphicsItem> items = mView.getAllItemsAt(point, eps, eps, null);
		if (items == null || items.isEmpty())
			return null;
		
		Optional<GraphicsItem> optItem = items.stream().filter(p -> mSelectableFilter.accept(p)).sorted(new Comparator<GraphicsItem>() {
			public int compare(GraphicsItem o1, GraphicsItem o2) {
				return -Float.compare(o1.getZOrder(), o2.getZOrder());
			}
		}).filter(predicate -> {
			Shape s = predicate.getShape();
			if (s != null) {
				AffineTransform wt = predicate.getWorldTransform();
				try {
					Point2D localScene = wt.inverseTransform(scene, null);
					Rectangle2D r = new Rectangle2D.Double(localScene.getX(), localScene.getY(), 3*eps, 3*eps);
					boolean res = s.contains(r);
					if (!res)
						res = s.intersects(r);
					return res;
				} catch (NoninvertibleTransformException e) {
					e.printStackTrace();
				}
			}
			return false;
		}).findFirst();
		if (optItem != null && optItem.isPresent())
			return optItem.get();
		return null;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void fireMoveEvent(ItemMoveEvent evt) {
		for (IMoveCallback mc : mMoveCallbacks) {
			mc.onItemMoved(evt);
		}
	}

	public ItemMoveEvent createMoveEvent(MouseEvent e) {
		List<GraphicsItem> items = Arrays.asList(mLastSelectedItem);
		Point2D ol = mLastSelectedItem.getSceneLocation();
		List<Point2D> oldLoc = Arrays.asList(new Point2D.Double(ol.getX(), ol.getY()));
		List<Point2D> newLoc = Arrays.asList(mSelectionMarker.getSceneLocation());
		ItemMoveEvent evt = new ItemMoveEvent(items, oldLoc, newLoc);
		return evt;
	}

	
	
	public void fireScaleEvent(ItemScaleEvent evt) {
		for (IScaleCallback sc : mScaleCallbacks) {
			sc.onItemScaled(evt);
		}
	}

	public void fireScaleEvent(Point2D[] oldVertices, Point2D[] newVertices) {
		ArrayList<Point2D[]> ov = new ArrayList<>();
		ov.add(oldVertices);
		ArrayList<Point2D[]> nv = new ArrayList<>();
		nv.add(newVertices);
		fireScaleEvent(new ItemScaleEvent(Arrays.asList(mLastSelectedItem), ov, nv));
	}

	
	public void fireRotationEvent(ItemRotateEvent evt) {
		for (IRotateCallback sc : mRotationCallbacks) {
			sc.onItemRotated(evt);
		}
	}
	public void fireRotateEvent(double startDegrees, double endDegrees) {
		fireRotationEvent(new ItemRotateEvent(Arrays.asList(mLastSelectedItem), Arrays.asList(startDegrees), Arrays.asList(endDegrees)));
	}






}
