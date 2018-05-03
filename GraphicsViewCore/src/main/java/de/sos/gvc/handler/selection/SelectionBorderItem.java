package de.sos.gvc.handler.selection;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Robot;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.Utils;
import de.sos.gvc.drawables.DrawableStyle;
import de.sos.gvc.handler.SelectionHandler;
import de.sos.gvc.handler.SelectionHandler.ItemMoveEvent;


/**
 * 
 * @author scholvac
 *
 */
public class SelectionBorderItem extends GraphicsItem {

	enum MouseMode {
		NONE, MOVE, ROTATE, SCALE
	}
	public static final int SP_UL = 0; //ScalePoint_UpperLeft
	public static final int SP_UR = 1;
	public static final int SP_LR = 2;
	public static final int SP_LL = 3;
	
	
	private static Robot 		sRobot;
	
	public final static int[] SCALE_POINT_CURSOR_TYPES = new int[] {
			Cursor.NW_RESIZE_CURSOR, //UL
			Cursor.SW_RESIZE_CURSOR,
			Cursor.NW_RESIZE_CURSOR,
			Cursor.SW_RESIZE_CURSOR
	};
	
	private class ScalePoint extends GraphicsItem implements MouseListener, MouseMotionListener {
		int 		mID;
		
		Point2D[]	mOldVertices = null;
		public ScalePoint(int id, Point2D vertex) {
			super(new Rectangle2D.Double(-5, -5, 10, 10));
			setSelectable(false);
			setSceneLocation(vertex);
			mID = id;
			setMouseSupport(this);
			setMouseMotionSupport(this);
		}
		@Override public void mouseMoved(MouseEvent e) {}
		@Override public void mouseClicked(MouseEvent e) { }
		@Override public void mousePressed(MouseEvent e) { 
			if (!mCallbackManager.hasScaleCallbacks()) return ;
			mOldVertices = getSceneVertices();
			mMouseMode = MouseMode.SCALE;
		}
		@Override public void mouseReleased(MouseEvent e) {
			if (!mCallbackManager.hasScaleCallbacks()) return ;
			Point2D[] newVertices = getSceneVertices();
			//build the scale event after the resizing of the shape has been done and the user releases the mouse
			mCallbackManager.fireScaleEvent(mOldVertices, newVertices);
			setRectangle(mSelectedItem.getSceneBounds());
			mOldVertices = null;
			mMouseMode = MouseMode.NONE;
		}

		@Override
		public void mouseEntered(MouseEvent e) { 
			if (!mCallbackManager.hasScaleCallbacks()) return ;
			e.getComponent().setCursor(new Cursor(SCALE_POINT_CURSOR_TYPES[mID]));
			e.consume();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (!mCallbackManager.hasScaleCallbacks()) return ;
			e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			e.consume();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (!mCallbackManager.hasScaleCallbacks() || mMouseMode != MouseMode.SCALE) return ;
			Point2D[] vertices = getLocalVertices();
			Point2D loc = getView().getSceneLocation(e.getPoint());
			try {
				getParent().getWorldTransform().inverseTransform(loc, loc);
//				getLocalTransform().inverseTransform(loc, loc);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			double mix = Double.MAX_VALUE, miy = Double.MAX_VALUE;
			double max = -Double.MAX_VALUE, may = -Double.MAX_VALUE;
			for (int i = 0; i < 4; i++) {
				double x = vertices[i].getX(), y = vertices[i].getY();
				mix = Math.min(x, mix);
				max = Math.max(x, max);
				miy = Math.min(y, miy);
				may = Math.max(y, may);
			}
			switch(mID) {
			case SP_UL : mix = loc.getX(); may = loc.getY(); break;
			case SP_UR : max = loc.getX(); may = loc.getY(); break;
			case SP_LR : max = loc.getX(); miy = loc.getY(); break;
			case SP_LL : mix = loc.getX(); miy = loc.getY(); break;
			}
			vertices = new Point2D[] {
					new Point2D.Double(mix, may),
					new Point2D.Double(max, may),
					new Point2D.Double(max, miy),
					new Point2D.Double(mix, miy)
			};
			setVertices(vertices);
			e.consume();
		}

		
		@Override
		public void draw(Graphics2D g, IDrawContext ctx) {
			setScale(ctx.getScale());
			super.draw(g, ctx);
		}
		
	}
	
	private class RotationPoint extends GraphicsItem implements MouseListener, MouseMotionListener {

		private Point mLastPosition;
		private double mStartDegrees;
		public RotationPoint() {
			super(new Rectangle2D.Double(-5, -5, 10, 10));
			setSelectable(false);
			setMouseSupport(this);
			setMouseMotionSupport(this);
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			if (!mCallbackManager.hasRotationCallbacks()) return ;
			Point p = e.getLocationOnScreen();
			int dx = p.x - mLastPosition.x;
			int dy = p.y - mLastPosition.y;
			SelectionBorderItem.this.setRotation(SelectionBorderItem.this.getRotationDegrees() + dx*0.4);		
			e.consume();
			try {
				Point2D sl = getSceneLocation();
				Point2D scrrenLoc = getView().getPositionOnScreen(sl);
				p.x = (int)scrrenLoc.getX();
				p.y = (int)scrrenLoc.getY();
				sRobot.mouseMove(p.x, p.y);
				mLastPosition = p;				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
		}


		@Override
		public void mousePressed(MouseEvent e) {
			if (!mCallbackManager.hasRotationCallbacks()) return ;
			mLastPosition = e.getLocationOnScreen();
			mMouseMode = MouseMode.ROTATE;
			e.consume();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (!mCallbackManager.hasRotationCallbacks() || mMouseMode != MouseMode.ROTATE) 
				return ;
			mLastPosition = null;
			double endDegrees = SelectionBorderItem.this.getRotationDegrees();
			mCallbackManager.fireRotateEvent(mStartDegrees, endDegrees);
			mMouseMode = MouseMode.NONE;
		}
				

		@Override
		public void mouseEntered(MouseEvent e) {
			if (!mCallbackManager.hasRotationCallbacks() || mMouseMode != MouseMode.NONE) return ;
			e.getComponent().setCursor(new Cursor(Cursor.WAIT_CURSOR));
			mMouseMode = MouseMode.ROTATE;
			mStartDegrees = SelectionBorderItem.this.getRotationDegrees();
			e.consume();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (!mCallbackManager.hasRotationCallbacks()) return ;
			e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			mMouseMode = MouseMode.NONE;
			e.consume();
		}

		
		@Override public void mouseMoved(MouseEvent e) {}
		@Override public void mouseClicked(MouseEvent e) {}
		
		@Override
		public void draw(Graphics2D g, IDrawContext ctx) {
			setScale(ctx.getScale());
			super.draw(g, ctx);
		}
	}
	
	private static final DrawableStyle 	sBorderStyle;
	private static final DrawableStyle	sControlPointStyle;
	
	
	static {
		sBorderStyle = new DrawableStyle();
		sBorderStyle.setName("SelectionBorder");
		sBorderStyle.setFillPaint(null);
		sBorderStyle.setLinePaint(Color.BLUE.brighter());
		Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
		sBorderStyle.setLineStroke(dashed);
		
		sControlPointStyle = new DrawableStyle("SelectionControlPoint");
		sControlPointStyle.setLinePaint(null);
		Point2D center = new Point2D.Float(0, 0);
	    float radius = 1;
	    float[] dist = {0.0f, 0.5f, 1.0f};
	    Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
	    RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
		sControlPointStyle.setFillPaint(p);
		
		try {
			sRobot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<GraphicsItem>			mScalePoints = new ArrayList<>();
	private PropertyChangeListener			mTargetPositionListener = new PropertyChangeListener() {		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (mSelectedItem != null && isVisible()) {
				Rectangle2D rect = mSelectedItem.getSceneBounds();
				double cx = rect.getCenterX();
				double cy = rect.getCenterY(); 
				setSceneLocation(new Point2D.Double(cx, cy));
			}
		}
	};
	private GraphicsItem 					mSelectedItem;
	private SelectionHandler 				mCallbackManager;
	
	
	private double mMarginX = 0;
	private double mMarginY = 0;
	
	private ScalePoint[]					mVertices = null;
	private RotationPoint					mRotationPoint = null;
	private MouseMode						mMouseMode = MouseMode.NONE;
	
	public SelectionBorderItem(SelectionHandler callbackManager) {
		super();
		mCallbackManager = callbackManager;
		setStyle(sBorderStyle);
		setSelectable(false);
				
		setMouseMotionSupport(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (mCallbackManager.hasMoveCallbacks() && mMouseMode == MouseMode.MOVE) {
					if (e.isConsumed() == false) {
						ItemMoveEvent evt = mCallbackManager.createMoveEvent(e);
						mCallbackManager.fireMoveEvent(evt);
						e.consume();
					}
				}
			}
		});
		setMouseSupport(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (mCallbackManager.hasMoveCallbacks() == false || mMouseMode != MouseMode.NONE)
					return ;
				mMouseMode = MouseMode.MOVE;
				super.mousePressed(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				mMouseMode = MouseMode.NONE;
			}
			@Override
			public void mouseEntered(MouseEvent e) {
				e.getComponent().setCursor(new Cursor(Cursor.MOVE_CURSOR));
				e.consume();
			}
			@Override
			public void mouseExited(MouseEvent e) {
				e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				mMouseMode = MouseMode.NONE;
				e.consume();
			}
		});
	}


	public void setSelectedItem(GraphicsItem item) {
		if (item != mSelectedItem) {
			if (mSelectedItem != null) {
				uninstallListener(mSelectedItem);
				
			}
		}
		if (item != null) {
			mSelectedItem = item;
			installListener(mSelectedItem);
			
			Rectangle2D rect = item.getSceneBounds();
			setRectangle(rect);
		}		
	}
	
	private void setRectangle(Rectangle2D rect) {
		Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
		//move the rectangle to 0,0
		Rectangle2D newRect = new Rectangle2D.Double(rect.getX() - center.getX(), rect.getY() - center.getY(), rect.getWidth(), rect.getHeight());
		setVertices(Utils.getVertices(newRect));
		setSceneLocation(center);
	}


	


	private void setVertices(Point2D[] vertices) {
		if (mVertices == null) {
			mRotationPoint = new RotationPoint();
			mRotationPoint.setStyle(sControlPointStyle);
			mRotationPoint.setCenter(0, vertices[0].getY() + (vertices[0].getY() - vertices[2].getY())/5.0);
			addItem(mRotationPoint);
			
			mVertices = new ScalePoint[4];
			for (int i = 0; i < mVertices.length; i++) {
				mVertices[i] = new ScalePoint(i, vertices[i]);
				mVertices[i].setStyle(sControlPointStyle);
				addItem(mVertices[i]);
			}
		}else {
			for (int i = 0; i < mVertices.length; i++)
				mVertices[i].setLocalLocation(vertices[i]);
			mRotationPoint.setCenter(0, vertices[0].getY() + (vertices[0].getY() - vertices[2].getY())/5.0);
		}
		//update the shape
		updateShape();
	}
	
	
	private void updateShape() {
		Point2D[] vertices = getLocalVertices();
		Path2D 	p = new Path2D.Double();
		p.moveTo(vertices[0].getX(), vertices[0].getY());
		for (int i = 1; i < 4; i++)
			p.lineTo(vertices[i].getX(), vertices[i].getY());
		p.lineTo(vertices[0].getX(), vertices[0].getY());
		p.closePath();
//		p.moveTo(0, vertices[0].getY());
//		p.lineTo(0, vertices[0].getY() + (vertices[0].getY() - vertices[2].getY())/5.0);
		
		setShape(p);
	}


	private Point2D[] getLocalVertices() {
		return new Point2D[] {
				mVertices[0].getLocalLocation(),
				mVertices[1].getLocalLocation(),
				mVertices[2].getLocalLocation(),
				mVertices[3].getLocalLocation()
		};
	}
	private Point2D[] getSceneVertices() {
		Point2D[] vertices = getLocalVertices();
		getWorldTransform().transform(vertices, 0, vertices, 0, 4);
		return vertices;
	}


	private void installListener(GraphicsItem mSelectedItem2) {
		mSelectedItem.addPropertyChangeListener(GraphicsItem.PROP_CENTER_X, mTargetPositionListener);
		mSelectedItem.addPropertyChangeListener(GraphicsItem.PROP_CENTER_Y, mTargetPositionListener);
		mSelectedItem.addPropertyChangeListener(GraphicsItem.PROP_SCALE_X, mTargetPositionListener);
		mSelectedItem.addPropertyChangeListener(GraphicsItem.PROP_SCALE_Y, mTargetPositionListener);
		mSelectedItem.addPropertyChangeListener(GraphicsItem.PROP_ROTATION, mTargetPositionListener);
	}

	private void uninstallListener(GraphicsItem mSelectedItem2) {
		mSelectedItem.removePropertyChangeListener(GraphicsItem.PROP_CENTER_X, mTargetPositionListener);
		mSelectedItem.removePropertyChangeListener(GraphicsItem.PROP_CENTER_Y, mTargetPositionListener);
		mSelectedItem.removePropertyChangeListener(GraphicsItem.PROP_SCALE_X, mTargetPositionListener);
		mSelectedItem.removePropertyChangeListener(GraphicsItem.PROP_SCALE_Y, mTargetPositionListener);
		mSelectedItem.removePropertyChangeListener(GraphicsItem.PROP_ROTATION, mTargetPositionListener);
	}

	@Override
	public void draw(Graphics2D g, IDrawContext ctx) {
		Stroke dashed = new BasicStroke((float)(ctx.getScale()*3.0f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
		getStyle().setLineStroke(dashed);
		super.draw(g, ctx);
		g.setColor(Color.red);
		g.fill(new Arc2D.Double(getCenterX()-5, getCenterY()-5, 10, 10, 0, 360, Arc2D.CHORD));
	}
	
}
