package de.sos.gvc.index;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.sos.gvc.index.IBIGNode.BIGNodeType;
import de.sos.gvc.index.IBIGNode.IBIGNodeFactory;
import de.sos.gvc.index.impl.BIGLeaf;
import de.sos.gvc.log.GVLog;

/**
 * BIdirectionalGrowingQuadTree
 * @author scholvac
 *
 */
public class BIGQuadTree<T extends IndexEntry<?>> {
	
	public interface IBIGQuadTreeHandler<T extends IndexEntry<?>> {
		public void install(BIGQuadTree<T> qt);
		public void uninstall(BIGQuadTree<T> qt);
	}
	public interface ISplitHandler<T extends IndexEntry<?>> extends IBIGQuadTreeHandler<T> {
		public boolean canSplit(IBIGNode<T> node);
	}
	public interface IBIGQTListener<T extends IndexEntry<?>> extends IBIGQuadTreeHandler<T> {
		public void valueAdded(T entry, IBIGNode<T> node);
		public void valueRemoved(T value, IBIGNode<T> node);
		public void nodeCreated(IBIGNode<T> node);
		public void nodeRemoved(IBIGNode<T> node);
	}
	@SuppressWarnings("rawtypes")
	private final static class ComboundSplitHandler implements ISplitHandler {
		ArrayList<ISplitHandler> 		mDelegates = new ArrayList<>();		
		@Override
		public boolean canSplit(IBIGNode node) {
			for (ISplitHandler sh : mDelegates)
				if (sh.canSplit(node))
					return false;
			return true;
		}
		@Override
		public void install(BIGQuadTree qt) { /*not used*/ }
		@Override
		public void uninstall(BIGQuadTree qt) {/*not used*/ }
	}
	@SuppressWarnings("rawtypes")
	private final static class ComboundTreeListener implements IBIGQTListener {
		ArrayList<IBIGQTListener> 		mDelegates = new ArrayList<>();
		@Override
		public void valueAdded(IndexEntry entry, IBIGNode node) { mDelegates.forEach(l -> valueAdded(entry, node)); }
		@Override
		public void valueRemoved(IndexEntry value, IBIGNode node) { mDelegates.forEach(l -> valueRemoved(value, node));}
		@Override
		public void nodeCreated(IBIGNode node) { mDelegates.forEach(l->nodeCreated(node));}
		@Override
		public void nodeRemoved(IBIGNode node) { mDelegates.forEach(l->nodeRemoved(node));}
		@Override
		public void install(BIGQuadTree qt) { /*not used*/ }
		@Override
		public void uninstall(BIGQuadTree qt) {/*not used*/ }
		
	}
	
	
	private IBIGNode<T>								mRoot = null;
	private int 									mMaxEntryCount = 10;
	private IBIGNodeFactory<T> 						mNodeFactory = new IBIGNode.DefaultBIGNodeFactroy<T>();
	
	private ISplitHandler<T> 							mSplitChecker;
	private IBIGQTListener<T> 						mTreeListener;
	private ArrayList<IBIGQuadTreeHandler<T>> 		mHandler = new ArrayList<>();
	
	public BIGQuadTree() {
	}
	public BIGQuadTree(int maxEntryCount, IBIGNodeFactory<T> factory) {
		this();
		mMaxEntryCount = maxEntryCount;
		if (factory != null)
			mNodeFactory = factory;
	}
	public BIGQuadTree(Rectangle2D geometry, int maxEntryCount, IBIGNodeFactory<T> factory) {
		this(maxEntryCount, factory);
		mRoot = mNodeFactory.createLeaf(geometry, null);
	}
	
	
	public IBIGNode<T> getRoot() { return mRoot; }
	public void addSplitChecker(ISplitHandler<T> handler) { 
		if (mSplitChecker != null) {
			if (mSplitChecker instanceof ComboundSplitHandler)
				((ComboundSplitHandler)mSplitChecker).mDelegates.add(handler);
			else {
				ISplitHandler old = mSplitChecker;
				ComboundSplitHandler csh = new ComboundSplitHandler();
				csh.mDelegates.add(old); csh.mDelegates.add(handler);
				mSplitChecker = csh;
			}
		}else
			mSplitChecker = handler; 
	}
	public void removeSplitChecker(ISplitHandler<T> handler) {
		if (mSplitChecker == null) return ;
		if (mSplitChecker == handler) { mSplitChecker = null; return ;}
		if (mSplitChecker instanceof ComboundSplitHandler) {
			if (((ComboundSplitHandler)mSplitChecker).mDelegates.remove(handler)) {
				if (((ComboundSplitHandler)mSplitChecker).mDelegates.size() == 1)
					mSplitChecker = ((ComboundSplitHandler)mSplitChecker).mDelegates.get(0);
			}
		}
	}
	
	public void addTreeListener(IBIGQTListener<T> handler) { 
		if (mTreeListener != null) {
			if (mTreeListener instanceof ComboundTreeListener)
				((ComboundTreeListener)mTreeListener).mDelegates.add(handler);
			else {
				IBIGQTListener old = mTreeListener;
				ComboundTreeListener csh = new ComboundTreeListener();
				csh.mDelegates.add(old); csh.mDelegates.add(handler);
				mTreeListener = csh;
			}
		}else
			mTreeListener = handler; 
	}
	public void removeTreeListener(IBIGQTListener<T> handler) {
		if (mTreeListener == null) return ;
		if (mTreeListener == handler) { mTreeListener = null; return ;}
		if (mTreeListener instanceof ComboundTreeListener) {
			if (((ComboundTreeListener)mTreeListener).mDelegates.remove(handler)) {
				if (((ComboundTreeListener)mTreeListener).mDelegates.size() == 1)
					mTreeListener = ((ComboundTreeListener)mTreeListener).mDelegates.get(0);
			}
		}
	}
	
	
	public void addHandler(IBIGQuadTreeHandler<T> handler) {
		if (handler == null || mHandler.contains(handler))
			return ;
		handler.install(this);
		mHandler.add(handler);
	}
	public void removeHandler(IBIGQuadTreeHandler<T> handler) {
		if (handler == null || mHandler.contains(handler) == false)
			return ;
		if (mHandler.remove(handler))
			handler.uninstall(this);		
	}
	
	public List<T> query(Rectangle2D area) {
		return query(getRoot(), area, false);
	}
	public List<T> query(IBIGNode<T> root, Rectangle2D area, boolean checkEntries) {
		ArrayList<T> out = new ArrayList<>();
		Stack<IBIGNode<T>> openStack = new Stack<>();
		openStack.push(root);
		int sc = 1;
		while(!openStack.isEmpty()) {
			IBIGNode<T> node = openStack.pop();
			Rectangle2D rnbb = node.getRealBounds();
			if (area.intersects(rnbb) || area.contains(rnbb)) {//TODO: do we realy need both checks?
				if (node.type() == BIGNodeType.LEAF) {
					for (T e : node.getValues()) {
						if (checkEntries) {
							Rectangle2D ebb = e.getGeometry();
							if (area.intersects(ebb) || area.contains(ebb))
								out.add(e);
						}else
							out.add(e);
					}
				}else {
					for (int i = 0; i < 4; i++) {
						IBIGNode<T> c = node.getChild(i);
						if (c != null) openStack.push(c);
						sc = Math.max(sc, openStack.size());
					}
				}
			}
		}
		return out;
	}
	
	
	
	
	public IBIGNode<T> insert(T entry) {
		if (entry == null) return null;
		Rectangle2D geometry = entry.getGeometry();
		if (geometry == null) return null;
		
		if (mRoot == null) { //first item is null so we create a default item with the same size as the inserted entry
			mRoot = mNodeFactory.createLeaf(geometry, null);
			if (mTreeListener != null) mTreeListener.nodeCreated(mRoot);
			((BIGLeaf<T>)mRoot).addEntry(entry); //this cast is only valid at this point
			return mRoot;
		}
		
		return insert(mRoot, entry);
	}
	private synchronized IBIGNode<T> insert(final IBIGNode<T> parent, T entry){
		IBIGNode<T> node = parent;
		Rectangle2D geometry = entry.getGeometry();
		
		Point2D center = new Point2D.Double(geometry.getCenterX(), geometry.getCenterY());
		while(node != null && node.type() != BIGNodeType.LEAF) {
			int idx = node.findNextIndex(center);
			if (idx == IBIGNode.THIS)
				break;
			else {
				IBIGNode<T> c = node.getChild(idx);
				if (c == null) {
					c = node.createChild(idx, mNodeFactory);
					if (mTreeListener != null)
						mTreeListener.nodeCreated(c);
				}
				node = c;
			}
		}
		
		if (node != null) {
			if (node.type() == BIGNodeType.LEAF) {
				((BIGLeaf<T>)node).addEntry(entry);
				if (mTreeListener != null)
					mTreeListener.valueAdded(entry, node);
				if (node.getValues().size() > mMaxEntryCount)
					split(node);
			}
		}
		return node;//something went wrong		
	}

	private void split(IBIGNode<T> node) {
		Rectangle2D geom = node.getQTBounds();
		if (mSplitChecker != null) {
			if (mSplitChecker.canSplit(node) == false)
				return ;
		}
		
		IBIGNode<T> newNode = mNodeFactory.createNode(geom, node.getParent());

		for (T entry : node.getValues()) {
			insert(newNode, entry);
		}
		
		//check if we did insert all into the same child, thus just increase the depth but did not win anything
		int maxValueCount = 0;
		for (int i = 0; i < 4; i++) {
			IBIGNode<T> c = newNode.getChild(i);
			if (c != null && c.getValues() != null) {
				int vc = c.getValues().size();
				if (vc > maxValueCount) maxValueCount = vc;
			}
		}
		if (maxValueCount == node.getValues().size()) {
			//we did insert everything into the same child and thus gain nothing
			//TODO: how to handle this?
			if (!balanceNode(newNode))
				return ; //do NOT split
		}
		if (mTreeListener != null)
			mTreeListener.nodeCreated(newNode);
		if (node == mRoot) {
			IBIGNode<T> oldRoot = mRoot;
			replaceRoot(newNode);
			if (mTreeListener != null)
				mTreeListener.nodeRemoved(oldRoot);
		}else {
			node.getParent().replaceChild(node, newNode);
			if (mTreeListener != null) {
				mTreeListener.nodeRemoved(node);
			}
		}
		
	}
	protected void replaceRoot(IBIGNode<T> newNode) {
		mRoot = newNode;
	}
	private boolean balanceNode(IBIGNode<T> newNode) {
		GVLog.warn("Need to balance the Tree");
		return false;
	}
	
	
	public boolean remove(T entry) {
		return false;
	}
	public synchronized boolean remove(T entry, IBIGNode<T> node) {
		if (node.type() == BIGNodeType.LEAF) {
			if (((BIGLeaf<T>)node).remove(entry)) {
				if (mTreeListener != null) {
					mTreeListener.valueRemoved(entry, node);
					return true;
				}
			}
			return false;
		}else {
			Rectangle2D bb = entry.getGeometry();
			Point2D.Double center = new Point2D.Double(bb.getCenterX(), bb.getCenterY());
			int idx = node.findNextIndex(center);
			if (idx == IBIGNode.THIS)
				return false; //there went something realy wrong
			IBIGNode<T> sub = node.getChild(idx);
			if (sub != null) {
				if (remove(entry, sub))
					return true;
			}
		}
		//if we reached this point, we could not find the entry in the expected subtree. 
		//that may happen, if the geometry of the entry has changed between insertion and deletion, without notifing the tree
		//TODO: search the whole tree
		return false;
	}
	
	public void updateItem(T item, Rectangle2D oldGeom, IBIGNode<T> oldNode) {
		Rectangle2D newGeom = item.getGeometry();
		Point2D newCenter = new Point2D.Double(newGeom.getCenterX(), newGeom.getCenterY());
		//first check if we can remain in the old node
		if (oldNode.getQTBounds().contains(newCenter)) {
			//we can remain, but have to update the real bounds
			oldNode.invalidateRealBounds();
			return ;
		}else {
			//find the first parent that contains the new center and do a normal remove and insert from that points
			IBIGNode<T> parent = oldNode.getParent();
			while(parent != null && parent.getQTBounds().contains(newCenter) == false)
				parent = parent.getParent();
			if (parent == null)
				parent = getRoot();
			//first remove from old node
			remove(item, oldNode);
			//insert into the new node
			insert(parent, item);
		}
	}
	

	
	
}
