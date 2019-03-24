package de.sos.gvc.index.impl;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;

import de.sos.gvc.index.IBIGNode;
import de.sos.gvc.index.IndexEntry;

/**
 * 
 * @author scholvac
 *
 */
public class BIGLeaf<T extends IndexEntry<?>> implements IBIGNode<T> {

	private Rectangle2D 				mGeometry;
	private ArrayList<T>				mEntries = new ArrayList<>();
	private IBIGNode<T> 				mParent;
	private Rectangle2D					mRealGeometry;

	public BIGLeaf(Rectangle2D geometry, IBIGNode<T> parent) {
		mGeometry = geometry;
		mParent = parent;
	}

	public void addEntry(T entry) {
		mEntries.add(entry);
		mRealGeometry = null;
	}
	public boolean remove(T entry) {
		if (mEntries.remove(entry)) {
			mRealGeometry = null;
			//TODO: the leaf is empty now, so we can delete it to free the memory (issue: we should do this using the BIGQuadTree interface, to get the listener informed)
			return true;
		}
		return false;
	}
	
	@Override
	public BIGNodeType type() {
		return BIGNodeType.LEAF;
	}

	@Override
	public Rectangle2D getQTBounds() {
		return mGeometry;
	}

	@Override
	public IBIGNode<T> getChild(int child) {
		return null; //leafes do not have children
	}

	@Override
	public Collection<T> getValues() {
		return new ArrayList<>(mEntries);
	}


	@Override
	public int findNextIndex(Point2D center) {
		return THIS;
	}

	@Override
	public IBIGNode<T> createChild(int idx, IBIGNodeFactory<T> mNodeFactory) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBIGNode<T> getParent() {
		return mParent;
	}

	@Override
	public void replaceChild(IBIGNode<T> oldChild, IBIGNode<T> newChild) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Rectangle2D getRealBounds() {
		if (mRealGeometry == null) {
			Rectangle2D qb = getQTBounds();
			Rectangle2D rb = new Rectangle2D.Double(qb.getX(), qb.getY(), qb.getWidth(), qb.getHeight());
			for (T e : mEntries) {
				rb = rb.createUnion(e.getGeometry());
			}
			mRealGeometry = rb;
		}
		return mRealGeometry;
	}

	@Override
	public void invalidateRealBounds() {
		mRealGeometry = null;
		if (getParent() != null) getParent().invalidateRealBounds();
	}
}
