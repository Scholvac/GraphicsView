package de.sos.gvc.handler.selection;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.handler.MouseDelegateHandler.DelegateMouseEvent;
import de.sos.gvc.handler.SelectionHandler;
import de.sos.gvc.handler.SelectionHandler.ItemMoveEvent;
import de.sos.gvc.handler.SelectionHandler.ItemRotateEvent;
import de.sos.gvc.handler.SelectionHandler.ItemScaleEvent;


/**
 *
 * @author scholvac
 *
 */
public abstract class AbstractSelectionItem extends GraphicsItem {

	public static enum MouseMode {
		NONE, MOVE, ROTATE, SCALE
	}

	public static enum CallbackMode {
		MOVE,
		ROTATE,
		SCALE
	}

	public static final int SP_UL = 0; //ScalePoint_UpperLeft
	public static final int SP_UR = 1;
	public static final int SP_LR = 2;
	public static final int SP_LL = 3;

	public final static int[] SCALE_POINT_CURSOR_TYPES = new int[] {
			Cursor.NW_RESIZE_CURSOR, //UL
			Cursor.SW_RESIZE_CURSOR,
			Cursor.NW_RESIZE_CURSOR,
			Cursor.SW_RESIZE_CURSOR
	};



	abstract protected class AbstractSelectionWorkerItem extends GraphicsItem implements MouseListener, MouseMotionListener {
		protected final boolean				mFixSize;
		protected final Cursor				mActiveCursor;
		protected final MouseMode			mTargetMode;
		protected final CallbackMode		mCallbackMode;
		protected boolean					mUsePermanentMotionListener = true;

		protected Point2D					mInitialPosition;

		private Cursor 						mOldCursor; //remmeber previous cursor to resotore ist
		private MouseMode 					mOldMouseMode;

		public AbstractSelectionWorkerItem(boolean fixSize, CallbackMode cm, MouseMode mm, Cursor cursor, boolean useMotionListener) {
			mActiveCursor = cursor;
			mCallbackMode = cm;
			mTargetMode = mm;
			mFixSize = fixSize;
			mUsePermanentMotionListener = useMotionListener;
			setSelectable(false);
			setSelected(false);

			setMouseSupport(this);
			setMouseMotionSupport(this);
		}

		@Override
		public void draw(Graphics2D g, IDrawContext ctx) {
			if (mFixSize)
				setSceneScale(ctx.getScale());
			super.draw(g, ctx);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			if (hasCallback(mCallbackMode)) {
				if (mActiveCursor != null) {
					mOldCursor = e.getComponent().getCursor();
					e.getComponent().setCursor(mActiveCursor);
				}
				e.consume();
			}
		}
		@Override
		public void mouseExited(MouseEvent e) {
			if (e.getComponent().getCursor() == mActiveCursor) {
				if (mActiveCursor != null) {
					e.getComponent().setCursor(mOldCursor);
				}
				e.consume();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (hasCallback(mCallbackMode) && mMouseMode == MouseMode.NONE) {
				mInitialPosition = e.getLocationOnScreen();
				if (mUsePermanentMotionListener && e instanceof DelegateMouseEvent) {
					((DelegateMouseEvent)e).addPermanentMouseMotionListener(this);
					((DelegateMouseEvent)e).addPermanentMouseListener(this);
				}
				mOldMouseMode = mMouseMode;
				mMouseMode = mTargetMode;

				e.consume();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (hasCallback(mCallbackMode) && mMouseMode == mTargetMode) {
				if (mUsePermanentMotionListener && e instanceof DelegateMouseEvent) {
					((DelegateMouseEvent)e).removePermanentMouseMotionListener(this);
					((DelegateMouseEvent)e).removePermanentMouseListener(this);
				}
				mMouseMode = mOldMouseMode;
				e.consume();
				fireEvent();
			}
		}

		protected MouseMode getMouseMode() {
			return mMouseMode;
		}

		@Override
		public void mouseClicked(MouseEvent e) {}


		@Override
		public void mouseMoved(MouseEvent e) {}

		abstract protected void fireEvent();
	}





	private final SelectionHandler 			mCallbackManager;

	private MouseMode						mMouseMode = MouseMode.NONE;


	public AbstractSelectionItem(SelectionHandler callbackManager) {
		super();
		mCallbackManager = callbackManager;
		setSelectable(false);
	}


	public abstract void setSelectedItem(GraphicsItem item);


	protected boolean hasCallback(CallbackMode cm) {
		switch(cm) {
		case MOVE : return mCallbackManager.hasMoveCallbacks();
		case ROTATE: return mCallbackManager.hasRotationCallbacks();
		case SCALE: return mCallbackManager.hasScaleCallbacks();
		}
		return false;
	}

	protected void fireMoveEvent(ItemMoveEvent event) {
		mCallbackManager.fireMoveEvent(event);
	}
	protected void fireMoveEvent(GraphicsItem item, Point2D startLoc, Point2D endLoc) {
		ItemMoveEvent ime = new ItemMoveEvent(Arrays.asList(item), Arrays.asList(startLoc), Arrays.asList(endLoc));
		fireMoveEvent(ime);
	}

	protected void fireRotateEvent(GraphicsItem item, double startRotation, double endRotation) {
		ItemRotateEvent ire = new ItemRotateEvent(Arrays.asList(item), Arrays.asList(startRotation), Arrays.asList(endRotation));
		fireRotateEvent(ire);
	}
	protected void fireRotateEvent(ItemRotateEvent event) {
		mCallbackManager.fireRotationEvent(event);
	}


	protected void fireScaleEvent(GraphicsItem item, Point2D[] oldVertices, Point2D[] newVertices) {
		ArrayList<Point2D[]> ov = new ArrayList<>();
		ov.add(oldVertices);
		ArrayList<Point2D[]> nv = new ArrayList<>();
		nv.add(newVertices);
		ItemScaleEvent ise = new ItemScaleEvent(Arrays.asList(item), ov, nv);
		fireScaleEvent(ise);
	}
	protected void fireScaleEvent(ItemScaleEvent event) {
		mCallbackManager.fireScaleEvent(event);
	}



}
