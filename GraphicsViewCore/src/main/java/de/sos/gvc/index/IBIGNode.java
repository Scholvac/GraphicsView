package de.sos.gvc.index;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

import de.sos.gvc.index.impl.BIGLeaf;
import de.sos.gvc.index.impl.BIGNode;

/**
 * 
 * @author scholvac
 *
 */
public interface IBIGNode<T extends IndexEntry<?>> {
	
	public enum BIGNodeType {
		NODE, LEAF
	}
	
	public interface IBIGNodeFactory<T extends IndexEntry<?>> {
		IBIGNode<T> 	createNode(Rectangle2D geometry, IBIGNode<T> parent);
		IBIGNode<T>		createLeaf(Rectangle2D geometry, IBIGNode<T> parent);
	}
	public static class DefaultBIGNodeFactroy<T extends IndexEntry<?>> implements IBIGNodeFactory<T>{
		@Override
		public IBIGNode<T> createLeaf(Rectangle2D geometry, IBIGNode<T> parent) { return new BIGLeaf<>(geometry, parent); }
		@Override
		public IBIGNode<T> createNode(Rectangle2D geometry, IBIGNode<T> parent) { return new BIGNode<>(geometry, parent); }
	}
	
	public static final int THIS	= -1;
	public static final int CHILD_NW = 0;
	public static final int CHILD_NO = 1;
	public static final int CHILD_SW = 2;
	public static final int CHILD_SO = 3;
	
	BIGNodeType type();
	
	/**
	 * returns the bounds, used to create the quad tree. However those bounds do not nessesarily be the same as the sum of all 
	 * children or values. This is due to the insertion technique that only uses the center of a box to decide which node the value shall be inserted. 
	 * The real Bounding Box of a Node may be much bigger
	 * @return
	 */
	Rectangle2D getQTBounds();
	
	/**
	 * return the real bounding box of this node, that is the sum of all children or all values
	 * @note this box may be much bigger as the bounding box used to associate the node for an value
	 * @return
	 */
	Rectangle2D getRealBounds();
	
	/** invalidates (optional) caching of real bounds, this method shall be invoked, if a node or a child has been changed */
	void invalidateRealBounds();
	
	IBIGNode<T> getChild(int child);
	
	Collection<T> getValues();

	int findNextIndex(Point2D center);

	IBIGNode<T> createChild(int idx, IBIGNodeFactory<T> mNodeFactory);

	IBIGNode<T> getParent();
	void replaceChild(IBIGNode<T> oldChild, IBIGNode<T> newChild);	
}
