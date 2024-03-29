package de.sos.gvc.storage;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene.IItemFilter;
import de.sos.gvc.IItemStorage;

/**
 *
 * @author scholvac
 *
 */
public class ListStorage implements IItemStorage {

	private ArrayList<GraphicsItem>			mItems = new ArrayList<>();
	private boolean 						mParallel;

	public ListStorage() {
		this(true);
	}
	public ListStorage(boolean parallel) {
		mParallel = parallel;
	}

	public boolean isParallelProcessing() { return mParallel;}
	public void enableParallelProcessing(boolean b) { mParallel = b; }

	@Override
	public boolean contains(GraphicsItem item) {
		if (item == null) return false;
		return mItems.contains(item);
	}

	@Override
	public boolean addItem(GraphicsItem item) {
		if (contains(item))
			return false;
		synchronized (mItems) {
			mItems.add(item);
		}
		return true;
	}

	@Override
	public boolean removeItem(GraphicsItem item) {
		synchronized (mItems) {
			return mItems.remove(item);
		}
	}

	@Override
	public List<GraphicsItem> getAllItems() {
		return new ArrayList<>(mItems);
	}

	@Override
	public List<GraphicsItem> getItems(Rectangle2D rect, IItemFilter filter) {
		synchronized (mItems) {
			boolean parallel = mParallel && mItems.size() > 100; //do not go through the trouble of creating all those worker threads if we have only a few items....
			Stream<GraphicsItem> res1 = parallel ? mItems.parallelStream() : mItems.stream();
			res1 = res1.filter(f-> f.isVisible()).filter(f->{
				Rectangle2D wb = f.getSceneBounds();
				if (rect.contains(wb) || intersects(rect, wb))
					return true;
				return false;
			});
			if (filter != null)
				res1 = res1.filter(f->filter.accept(f));
			List<GraphicsItem> list = res1.collect(Collectors.toList());
			return list;
		}
	}


	/** Actually the same implementation as in java.awt.RectangularShape but omits the empty check, as Lines could have a bounding box with w | h == 0 */
	 public boolean intersects(Rectangle2D r0, Rectangle2D r1) {
	     double x0 = r0.getX();
	     double y0 = r0.getY();
	     double x = r1.getX(), y = r1.getY();
	     return (x + r1.getWidth() > x0 &&
	    		 y + r1.getHeight() > y0 &&
	             x < x0 + r0.getWidth() &&
	             y < y0 + r0.getHeight());
    }

}
