package de.sos.gvc.handler;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene.IItemFilter;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IGraphicsViewHandler;

/**
 * 
 * @author scholvac
 *
 */
public class MouseDelegateHandler implements IGraphicsViewHandler, MouseListener, MouseMotionListener, MouseWheelListener
{
	private GraphicsView mView;
	
	private IItemFilter		mMouseWheelSupportFilter = new IItemFilter() {		
		@Override
		public boolean accept(GraphicsItem item) {
			if (item.getMouseWheelSupport() != null)
				return true;
			return false;
		}
	};
	private IItemFilter 	mMouseMotionSupportFilter = new IItemFilter() {		
		@Override
		public boolean accept(GraphicsItem item) {
			return item.getMouseMotionSupport() != null;
		}
	};
	private IItemFilter		mMouseSupportFilter = new IItemFilter() {		
		@Override
		public boolean accept(GraphicsItem item) {
			return item.getMouseSupport() != null;
		}
	};
	

	@Override
	public void install(GraphicsView view) {
		mView = view;
		mView.addMouseListener(this);
		mView.addMouseMotionListener(this);
		mView.addMouseWheelListener(this);
	}

	@Override
	public void uninstall(GraphicsView view) {
		mView.removeMouseListener(this);
		mView.removeMouseMotionListener(this);
		mView.removeMouseWheelListener(this);	
	}
		
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), 2, 2, mMouseWheelSupportFilter);
		if (items != null && !items.isEmpty()) {
			for (GraphicsItem item : items) {
				item.getMouseWheelSupport().mouseWheelMoved(e);
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		//handle the drag event
		List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), 2, 2, mMouseMotionSupportFilter);
		if (items != null && !items.isEmpty()) {
			for (GraphicsItem item : items) {
				item.getMouseMotionSupport().mouseDragged(e);
			}
		}
		
		handleMouseEnterAndExit(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		//delegate the mouse move event
		List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), 2, 2, mMouseMotionSupportFilter);
		if (items != null && !items.isEmpty()) {
			for (GraphicsItem item : items) {
				item.getMouseMotionSupport().mouseMoved(e);
			}
		}
		
		handleMouseEnterAndExit(e);
	}

	private HashSet<GraphicsItem> 	mEnteredItems = new HashSet<>();
	private void handleMouseEnterAndExit(MouseEvent e) {
		List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), 2, 2, mMouseSupportFilter);
		if (items != null && !items.isEmpty()) {
			//check all allready entered items, if they are still returned. if not we did leave them
			if (mEnteredItems.isEmpty() == false) {
				ArrayList<GraphicsItem> toRemove = new ArrayList<>();
				for (GraphicsItem item : mEnteredItems)
					if (items.contains(item) == false) {
						item.getMouseSupport().mouseExited(e);
						toRemove.add(item);
					}
				mEnteredItems.removeAll(toRemove);
			}
			//check if we have a new added item, that is, if we can add it to our set, we do enter the item
			for (GraphicsItem item : items) {
				if (mEnteredItems.add(item) && !e.isConsumed()) {
					item.getMouseSupport().mouseEntered(e);
				}
			}
		}else {
			//we have no item selelected, thus we have left all items
			if (mEnteredItems.isEmpty() == false) {
				for (GraphicsItem item : mEnteredItems)
					item.getMouseSupport().mouseExited(e);
				mEnteredItems.clear();
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), 2, 2, mMouseSupportFilter);
		if (items != null && !items.isEmpty()) {
			for (GraphicsItem item : items) {
				item.getMouseSupport().mouseClicked(e);
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), 2, 2, mMouseSupportFilter);
		if (items != null && !items.isEmpty()) {
			for (GraphicsItem item : items) {
				item.getMouseSupport().mousePressed(e);
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), 2, 2, mMouseSupportFilter);
		if (items != null && !items.isEmpty()) {
			for (GraphicsItem item : items) {
				item.getMouseSupport().mouseReleased(e);
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// This is handled in the mouseMoved event, since this method is triggered when we enter the views component
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// This is handled in the mouseMoved event, since this method is triggered when we enter the views component
	}



}
