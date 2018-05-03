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
		mItems.add(item);
		return true;
	}

	@Override
	public boolean removeItem(GraphicsItem item) {
		return mItems.remove(item);
	}

	@Override
	public List<GraphicsItem> getAllItems() {
		return new ArrayList<GraphicsItem>(mItems); 
	}

	@Override
	public List<GraphicsItem> getItems(Rectangle2D rect, IItemFilter filter) {
		Stream<GraphicsItem> res1 = mParallel ? mItems.parallelStream() : mItems.stream();		
		res1 = res1.filter(f-> f.isVisible()).filter(f->{
			Rectangle2D wb = f.getSceneBounds();
			if (rect.contains(wb) || rect.intersects(wb))
				return true;
			return false;
		});
		if (filter != null)
			res1 = res1.filter(f->filter.accept(f));
		List<GraphicsItem> list = res1.collect(Collectors.toList());
		return list;
	}
	

}
