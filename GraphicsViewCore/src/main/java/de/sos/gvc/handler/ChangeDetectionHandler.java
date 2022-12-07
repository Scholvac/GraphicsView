package de.sos.gvc.handler;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IGraphicsViewHandler;
import de.sos.gvc.param.IParameter.IDisposeable;

public class ChangeDetectionHandler implements IGraphicsViewHandler {
	private GraphicsScene 				mScene;
	private List<IDisposeable> 			mDisposeables = new ArrayList<>();

	private Set<GraphicsItem> 			mItemsToRevalidate = new HashSet();
	private List<Rectangle2D.Double> 	mRegionsToRepaint = new ArrayList();

	private final Rectangle2D zeroRect = new Rectangle2D.Double();
	@Override
	public void install(final GraphicsView view) {
		mScene = view.getScene();
		mDisposeables.add(mScene.addPropertyListener(GraphicsScene.ITEM_LIST_PROPERTY, pcl -> registerTopLevelItem((GraphicsItem)pcl.getNewValue())));
	}

	public void clear() {
		mItemsToRevalidate.clear();
		mRegionsToRepaint.clear();
	}
	/**
	 * Find overlapping rectangles and merge them.
	 */
	private List<Rectangle2D.Double> mergeRectangles(final List<Rectangle2D.Double> rectangles) {
		int position = 0;
		int maxIter = rectangles.size() * 10;
		while (position < rectangles.size() && maxIter-- > 0) {
			if (rectangles.get(position).equals(zeroRect)) {
				position++;
				continue;
			}
			for (int i = 1 + position; i < rectangles.size(); i++) {
				final Rectangle2D.Double r1 = rectangles.get(position);
				final Rectangle2D.Double r2 = rectangles.get(i);
				if (r2.equals(zeroRect)) {
					continue;
				}
				if (isOverlapping(r1, r2)) {
					rectangles.set(position, merge(r1, r2));
					r2.setRect(0, 0, 0, 0);//invalidate
					if (position != 0) {
						position--;
					}
				}
			}
			position++;
		}

		return rectangles.stream().filter(it -> !it.equals(zeroRect)).collect(Collectors.toList());
	}
	private Rectangle2D.Double merge(final Rectangle2D.Double r1, final Rectangle2D.Double r2) {
		final double mix = Math.min(r1.x, r2.x);
		final double miy = Math.min(r1.y, r2.y);
		final double max = Math.max(r1.getMaxX(), r2.getMaxX());
		final double may = Math.max(r1.getMaxY(), r2.getMaxY());
		r1.setRect(mix, miy, max-mix, may-miy);
		return r1;
	}

	private boolean isOverlapping(final Rectangle2D.Double r1, final Rectangle2D.Double r2) {
		if (r1.getMaxY() < r2.getMinY() || r2.getMaxY() < r1.getMinY())
			return false;

		return r1.getMaxX() >= r2.getMinX() && r2.getMaxX() >= r1.getMinX();
	}

	public final List<Rectangle2D.Double> getRepaintRegions() {
		mItemsToRevalidate.forEach(it -> rememberRectangle(it.getSceneBounds()));
		final List<Rectangle2D.Double> regions = new ArrayList();
		regions.addAll(mRegionsToRepaint);
		return mergeRectangles(regions);
	}


	private void registerTopLevelItem(final GraphicsItem topLevelItem) {
		if (topLevelItem == null)
			return ;
		rememberItem(topLevelItem);
		mDisposeables.add(topLevelItem.addPropertyChangeListener(GraphicsItem.PROP_SCENE_BOUNDS, pcl -> onWorldBoundsChanged((GraphicsItem)pcl.getSource(), (Rectangle2D)pcl.getOldValue())));
		mDisposeables.add(topLevelItem.addPropertyChangeListener(GraphicsItem.PROP_DRAWABLE, pcl -> onDrawableChanged((GraphicsItem)pcl.getSource(), (Rectangle2D)pcl.getOldValue())));
	}

	private void onDrawableChanged(final GraphicsItem source, final Rectangle2D oldValue) {
		//just remember the current rectangle to be redrawn
		if (oldValue != null) //added the first time
			rememberRectangle(oldValue);
		else
			rememberItem(source);
	}

	public void onWorldBoundsChanged(final GraphicsItem source, final Rectangle2D oldValue) {
		//rember the current rectangle to be redrawn (cleared old image) and the item which will have a new world transform and thus a new rectangle on next render
		if (oldValue != null)
			rememberRectangle(oldValue);
		rememberItem(source);
	}
	private void rememberRectangle(final Rectangle2D ov) {
		mRegionsToRepaint.add(new Rectangle2D.Double(ov.getX(), ov.getY(), ov.getWidth(), ov.getHeight()));
	}

	private void rememberItem(final GraphicsItem source) {
		mItemsToRevalidate.add(source);
	}

	@Override
	public void uninstall(final GraphicsView view) {
		mDisposeables.forEach(IDisposeable::dispose);
	}
}