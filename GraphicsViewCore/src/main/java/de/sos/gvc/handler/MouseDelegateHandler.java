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
import de.sos.gvc.GraphicsScene.ShapeSelectionFilter;
import de.sos.gvc.IGraphicsView;
import de.sos.gvc.IGraphicsViewHandler;

/**
 *
 * @author scholvac
 *
 */
public class MouseDelegateHandler implements IGraphicsViewHandler, MouseListener, MouseMotionListener, MouseWheelListener
{
	private IGraphicsView mView;

	private HashSet<MouseListener>				mPermMouseListener = new HashSet<>();
	private HashSet<MouseMotionListener>		mPermMouseMotionListener = new HashSet<>();
	private HashSet<MouseWheelListener>			mPermMouseWheelListener = new HashSet<>();

	/**
	 * Specialized MouseEvent that is used if the MouseDelegateHandler notifies one of the MouseSupports.
	 * The DelegateMouseEvent provides the possibility to add permanent listener (mouse, motion and wheel) to the delegate handler.
	 * Those listener will be notified even if their Item is not "under" the mouse
	 * @author sschweigert
	 *
	 */
	public class DelegateMouseEvent extends MouseEvent {
		public DelegateMouseEvent(final MouseEvent e) {
			super(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers(), e.getX(), e.getY(), e.getClickCount(), e.isPopupTrigger(), e.getButton());
		}

		public boolean addPermanentMouseListener(final MouseListener ml){ return mPermMouseListener.add(ml); }
		public boolean addPermanentMouseMotionListener(final MouseMotionListener mml) { return mPermMouseMotionListener.add(mml);}
		public boolean addPermanentMouseWheelListener(final MouseWheelListener mwl) { return mPermMouseWheelListener.add(mwl); }

		public boolean removePermanentMouseListener(final MouseListener ml){ return mPermMouseListener.remove(ml); }
		public boolean removePermanentMouseMotionListener(final MouseMotionListener mml) { return mPermMouseMotionListener.remove(mml);}
		public boolean removePermanentMouseWheelListener(final MouseWheelListener mwl) { return mPermMouseWheelListener.remove(mwl); }
	}


	private IItemFilter		mMouseWheelSupportFilter = new IItemFilter() {
		@Override
		public boolean accept(final GraphicsItem item) {
			if (item.getMouseWheelSupport() != null)
				return true;
			return false;
		}
	};
	private IItemFilter 	mMouseMotionSupportFilter = new IItemFilter() {
		@Override
		public boolean accept(final GraphicsItem item) {
			return item.getMouseMotionSupport() != null;
		}
	};
	private IItemFilter		mMouseSupportFilter = new IItemFilter() {
		@Override
		public boolean accept(final GraphicsItem item) {
			return item.getMouseSupport() != null;
		}
	};


	@Override
	public void install(final IGraphicsView view) {
		mView = view;
		mView.addMouseListener(this);
		mView.addMouseMotionListener(this);
		mView.addMouseWheelListener(this);
	}

	@Override
	public void uninstall(final IGraphicsView view) {
		mView.removeMouseListener(this);
		mView.removeMouseMotionListener(this);
		mView.removeMouseWheelListener(this);
	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {
		final double eps = getSelectionEpsilon();
		final List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), eps, eps, mMouseWheelSupportFilter);
		if (items != null && !items.isEmpty()) {
			for (final GraphicsItem item : items) {
				item.getMouseWheelSupport().mouseWheelMoved(e);
			}
		}
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		//handle the drag event
		final double eps = getSelectionEpsilon();
		final List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), eps, eps, mMouseMotionSupportFilter);
		final HashSet<MouseMotionListener> mmhs = new HashSet<>(mPermMouseMotionListener);
		if (items != null && !items.isEmpty()) {
			for (final GraphicsItem item : items) {
				final MouseMotionListener support = item.getMouseMotionSupport();
				support.mouseDragged(e);
				mmhs.remove(support); //do not trigger again
			}
		}
		if (!mmhs.isEmpty()) {
			for (final MouseMotionListener ml : mmhs)
				try{
					ml.mouseDragged(e);
				}catch(final Exception ex) {ex.printStackTrace();}
		}
		handleMouseEnterAndExit(e);
	}

	@Override
	public void mouseMoved(final MouseEvent e) {
		//delegate the mouse move event
		final double eps = getSelectionEpsilon();
		final List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), eps, eps, mMouseMotionSupportFilter);
		final HashSet<MouseMotionListener> mmhs = new HashSet<>(mPermMouseMotionListener);
		if (items != null && !items.isEmpty()) {
			for (final GraphicsItem item : items) {
				final MouseMotionListener support = item.getMouseMotionSupport();
				support.mouseMoved(e);
				mmhs.remove(support); //do not trigger again
			}
		}
		if (!mmhs.isEmpty()) {
			for (final MouseMotionListener ml : mmhs)
				ml.mouseMoved(e);
		}
		handleMouseEnterAndExit(e);
	}

	private HashSet<GraphicsItem> 	mEnteredItems = new HashSet<>();

	private void handleMouseEnterAndExit(final MouseEvent e) {
		final double epsilon = getSelectionEpsilon();
		final List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), epsilon, epsilon, mMouseSupportFilter);
		final DelegateMouseEvent dme = new DelegateMouseEvent(e);
		if (items != null && !items.isEmpty()) {
			//check all allready entered items, if they are still returned. if not we did leave them
			if (!mEnteredItems.isEmpty()) {
				final ArrayList<GraphicsItem> toRemove = new ArrayList<>();
				for (final GraphicsItem item : mEnteredItems)
					if (!items.contains(item)) {
						item.getMouseSupport().mouseExited(dme);
						toRemove.add(item);
					}
				mEnteredItems.removeAll(toRemove);
			}
			//check if we have a new added item, that is, if we can add it to our set, we do enter the item
			for (final GraphicsItem item : items) {
				if (mEnteredItems.add(item)) {
					item.getMouseSupport().mouseEntered(dme);
				}
			}
		}else {
			//we have no item selelected, thus we have left all items
			if (!mEnteredItems.isEmpty()) {
				for (final GraphicsItem item : mEnteredItems)
					item.getMouseSupport().mouseExited(dme);
				mEnteredItems.clear();
			}
		}

	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		final double eps = getSelectionEpsilon();
		final List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), eps, eps, null);
		final HashSet<MouseListener> mmhs = new HashSet<>(mPermMouseListener);
		final DelegateMouseEvent dme = new DelegateMouseEvent(e);
		if (items != null && !items.isEmpty()) {
			final ShapeSelectionFilter ssf = new ShapeSelectionFilter(mView.getSceneLocation(e.getPoint()), eps, true);
			items.stream()
			.filter(pred -> mMouseSupportFilter.accept(pred))
			.filter(pred -> ssf.accept(pred))
			.forEach(cons -> {
				final MouseListener support = cons.getMouseSupport();
				support.mouseClicked(dme);
				mmhs.remove(support); //do not trigger again
			});
		}
		if (!mmhs.isEmpty())
			for (final MouseListener ml : mmhs)
				ml.mouseClicked(dme);
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		final double eps = getSelectionEpsilon();
		final List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), eps, eps, mMouseSupportFilter);
		final HashSet<MouseListener> mmhs = new HashSet<>(mPermMouseListener);
		final DelegateMouseEvent dme = new DelegateMouseEvent(e);
		if (items != null && !items.isEmpty()) {
			for (final GraphicsItem item : items) {
				final MouseListener support = item.getMouseSupport();
				support.mousePressed(dme);
				mmhs.remove(support); //do not trigger again
			}
		}
		if (!mmhs.isEmpty())
			for (final MouseListener ml : mmhs)
				ml.mouseClicked(dme);
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		final double eps = getSelectionEpsilon();
		final List<GraphicsItem> items = mView.getAllItemsAt(e.getPoint(), eps, eps, mMouseSupportFilter);
		final DelegateMouseEvent dme = new DelegateMouseEvent(e);
		final HashSet<MouseListener> mmhs = new HashSet<>(mPermMouseListener);
		if (items != null && !items.isEmpty()) {
			for (final GraphicsItem item : items) {
				final MouseListener support = item.getMouseSupport();
				support.mouseReleased(dme);
				mmhs.remove(support); //do not trigger again
			}
		}
		if (!mmhs.isEmpty())
			for (final MouseListener ml : mmhs)
				ml.mouseReleased(dme);
	}

	private double getSelectionEpsilon() {
		return 2 * mView.getScaleX(); //TODO: expose 2 as an variable or property
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
		// This is handled in the mouseMoved event, since this method is triggered when we enter the views component
	}

	@Override
	public void mouseExited(final MouseEvent e) {
		// This is handled in the mouseMoved event, since this method is triggered when we enter the views component
	}



}
