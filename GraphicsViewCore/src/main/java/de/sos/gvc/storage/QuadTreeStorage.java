package de.sos.gvc.storage;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene.IItemFilter;
import de.sos.gvc.IItemStorage;
import de.sos.gvc.index.BIGQuadTree;
import de.sos.gvc.index.BIGQuadTree.IBIGQTListener;
import de.sos.gvc.index.BIGQuadTree.ISplitHandler;
import de.sos.gvc.index.IBIGNode;
import de.sos.gvc.index.IBIGNode.BIGNodeType;
import de.sos.gvc.index.impl.DefaultEntry;


/**
 * 
 * @author scholvac
 *
 */
public class QuadTreeStorage implements IItemStorage {

	private final class MyGraphicsItemIndex extends DefaultEntry<GraphicsItem>{
		IBIGNode<MyGraphicsItemIndex> 	node;
		PropertyChangeListener			pcl = new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				mDirtySet.add(MyGraphicsItemIndex.this);
			}
		};

		public MyGraphicsItemIndex(GraphicsItem value) {
			super(value, value.getSceneBounds());
			value.addPropertyChangeListener(GraphicsItem.PROP_CENTER_X, pcl);
			value.addPropertyChangeListener(GraphicsItem.PROP_CENTER_Y, pcl);
			value.addPropertyChangeListener(GraphicsItem.PROP_ROTATION, pcl); //in this case we may not need to relocate the item (as the center should not change) but the real bounding box of the node changes
		}
		
		public void cleanUp() {
			mValue.removePropertyChangeListener(GraphicsItem.PROP_CENTER_X, pcl);
			mValue.removePropertyChangeListener(GraphicsItem.PROP_CENTER_Y, pcl);
			mValue.removePropertyChangeListener(GraphicsItem.PROP_ROTATION, pcl);
		}
		
		public void updateGeometry() {
			mBound = mValue.getSceneBounds();
		}	
	}
	
	class MyTreeHandler implements IBIGQTListener<MyGraphicsItemIndex>, ISplitHandler<MyGraphicsItemIndex> {

		@Override
		public void install(BIGQuadTree<MyGraphicsItemIndex> qt) {
			qt.addSplitChecker(this);
			qt.addTreeListener(this);
		}

		@Override
		public void uninstall(BIGQuadTree<MyGraphicsItemIndex> qt) {
			qt.removeSplitChecker(this);
			qt.removeTreeListener(this);
		}

		@Override
		public boolean canSplit(IBIGNode<MyGraphicsItemIndex> node) {
			double a = node.getQTBounds().getWidth() * node.getQTBounds().getHeight();
			if (a < mMinimumQuadrantArea)
				return false;
			return true;
		}

		@Override
		public void valueAdded(MyGraphicsItemIndex entry, IBIGNode<MyGraphicsItemIndex> node) {
			entry.node = node; //used for local updates
		}
		@Override
		public void valueRemoved(MyGraphicsItemIndex value, IBIGNode<MyGraphicsItemIndex> node) { }
		@Override
		public void nodeCreated(IBIGNode<MyGraphicsItemIndex> node) { }
		@Override
		public void nodeRemoved(IBIGNode<MyGraphicsItemIndex> node) { }		
	}
	
	
	private Set<MyGraphicsItemIndex>					mDirtySet = ConcurrentHashMap.newKeySet();
	BIGQuadTree<MyGraphicsItemIndex>					mTree = new BIGQuadTree<>(40, null);
	private HashMap<GraphicsItem, MyGraphicsItemIndex>	mItemDataMap = new HashMap<>();
	
	private double										mMinimumQuadrantArea = Double.NaN; 
	private MyTreeHandler								mMyTreeHandler = new MyTreeHandler();
	
	
	public QuadTreeStorage() {
		mTree = new BIGQuadTree<>(new Rectangle2D.Double(-1e8, -1e8, 2e8, 2e8), 40, null);
		mMinimumQuadrantArea = Double.NaN;
		mMyTreeHandler.install(mTree);
	}
	
	@Override
	public boolean contains(GraphicsItem item) {
		return mItemDataMap.containsKey(item);
	}

	@Override
	public boolean addItem(GraphicsItem item) {
		if (item == null) 
			return false;
		if (contains(item))
			return false;
		
		if (mMinimumQuadrantArea != mMinimumQuadrantArea) { //has not been defined yet, so we use the doubled area of the first added item
			Rectangle2D b = item.getSceneBounds();
			mMinimumQuadrantArea = 2.0 * b.getWidth() * b.getHeight();
		}
		
		MyGraphicsItemIndex entry = new MyGraphicsItemIndex(item);
		mItemDataMap.put(item, entry);
		mTree.insert(entry);
		
		return true;
	}


	@Override
	public boolean removeItem(GraphicsItem item) {
		MyGraphicsItemIndex entry = mItemDataMap.get(item);
		if (entry == null) return false;
		assert(entry.node != null && entry.node.type() == BIGNodeType.LEAF);
		if (mTree.remove(entry, entry.node)) { //use the specializied method, to speed up the deletion
			entry.cleanUp();
			mItemDataMap.remove(item);
			return true;
		}
		return false;
	}

	@Override
	public List<GraphicsItem> getAllItems() {
		return Collections.unmodifiableList(new ArrayList<>(mItemDataMap.keySet()));
	}

	@Override
	public List<GraphicsItem> getItems(Rectangle2D rect, IItemFilter filter) {
		applyChanges();
		List<MyGraphicsItemIndex> list = mTree.query(rect);
		return list.parallelStream().map(e->e.getValue()).collect(Collectors.toList());
	}

	private void applyChanges() {
		Set<MyGraphicsItemIndex> copy = mDirtySet;
		mDirtySet = ConcurrentHashMap.newKeySet();
		copy.parallelStream().forEach( item -> {
			Rectangle2D oldGeom = item.getGeometry();
			item.updateGeometry();
			mTree.updateItem(item, oldGeom, item.node);
		});
	}

}
