package de.sos.gvc.index.handler;

import java.awt.geom.Rectangle2D;

import de.sos.gvc.index.BIGQuadTree;
import de.sos.gvc.index.BIGQuadTree.ISplitHandler;
import de.sos.gvc.index.IBIGNode;


/**
 * 
 * @author scholvac
 *
 */
public class MinimumLeafSizeSplitHandler implements ISplitHandler {

	private double mMinArea;

	public MinimumLeafSizeSplitHandler(double area) {
		mMinArea = area;
	}
	@Override
	public void install(BIGQuadTree qt) {
		qt.addSplitChecker(this);
	}

	@Override
	public void uninstall(BIGQuadTree qt) {
		qt.addSplitChecker(null);
	}

	@Override
	public boolean canSplit(IBIGNode node) {
		Rectangle2D r = node.getQTBounds();
		if (r.getWidth() * r.getHeight() < mMinArea)
			return false;
		return true;
	}

}
