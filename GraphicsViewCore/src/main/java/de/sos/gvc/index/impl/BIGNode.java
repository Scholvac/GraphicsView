package de.sos.gvc.index.impl;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

import de.sos.gvc.index.IBIGNode;
import de.sos.gvc.index.IndexEntry;


/**
 *
 * @author scholvac
 *
 */
public class BIGNode<T extends IndexEntry<?>> implements IBIGNode<T> {

	private Rectangle2D				mBounds;
	private Rectangle2D				mRealGeometry;
	private Rectangle2D[]			mQuadrants;
	private IBIGNode<T>[]			mChildren;
	private IBIGNode<T>				mParent;

	public BIGNode(Rectangle2D geom, IBIGNode<T> parent) {
		mBounds = geom;
		mParent = parent;
		double x = geom.getMinX(), y = geom.getMinY();
		double w2 = geom.getWidth() / 2.0, h2 = geom.getHeight() / 2.0;
		mQuadrants = new Rectangle2D[] {
				new Rectangle2D.Double(x, y, w2, h2),
				new Rectangle2D.Double(x+w2, y, w2, h2),
				new Rectangle2D.Double(x, y + h2, w2, h2),
				new Rectangle2D.Double(x+w2, y + h2, w2, h2)
		};
	}

	@Override
	public BIGNodeType type() {
		return BIGNodeType.NODE;
	}

	@Override
	public Rectangle2D getQTBounds() {
		return mBounds;
	}

	@Override
	public IBIGNode<T> getChild(int child) {
		if (mChildren == null) return null;
		return mChildren[child];
	}

	@Override
	public Collection<T> getValues() {
		return null;
	}

	@Override
	public int findNextIndex(Point2D center) {
		for (int i = 0; i < 4; i++) {
			if (mQuadrants[i].contains(center))
				return i;
		}
		return THIS;
	}

	@Override
	public IBIGNode<T> createChild(int idx, IBIGNodeFactory<T> factory) {
		if (mChildren == null) mChildren = new IBIGNode[4];
		return mChildren[idx] = factory.createLeaf(mQuadrants[idx], this);
	}

	@Override
	public IBIGNode<T> getParent() {
		return mParent;
	}

	@Override
	public void replaceChild(IBIGNode<T> oldChild, IBIGNode<T> newChild) {
		for (int i = 0; i < 4; i++) {
			if (mChildren[i] == oldChild) {
				mChildren[i] = newChild;
				return ;
			}
		}
		throw new ArrayIndexOutOfBoundsException();
	}

	@Override
	public Rectangle2D getRealBounds() {
		if (mRealGeometry == null) {
			Rectangle2D qb = getQTBounds();
			Rectangle2D rb = new Rectangle2D.Double(qb.getX(), qb.getY(), qb.getWidth(), qb.getHeight());
			for (int i = 0; i < 4; i++) {
				IBIGNode<T> c = mChildren[i];
				if (c != null) {
					rb = rb.createUnion(c.getRealBounds());
				}
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
