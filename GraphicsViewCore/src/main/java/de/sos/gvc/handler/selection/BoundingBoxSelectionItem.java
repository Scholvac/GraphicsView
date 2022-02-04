package de.sos.gvc.handler.selection;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.Utils;
import de.sos.gvc.handler.SelectionHandler;
import de.sos.gvc.styles.DrawableStyle;

public class BoundingBoxSelectionItem extends AbstractSelectionItem { //TODO: change the name

	protected class ScalePointItem extends AbstractSelectionWorkerItem {

		private Point2D[] 		mInitialVertices;
		private Point2D[]		mNewVertices;
		private int				mScalePointID;

		public ScalePointItem(Shape shape, int scalePointID, CallbackMode cm, MouseMode mm, Cursor cursor, boolean useMotionListener) {
			super(true, cm, mm, cursor, useMotionListener);
			setShape(shape);
			mScalePointID = scalePointID;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);
			mInitialVertices = getVertices();
		}


		@Override
		public void mouseDragged(MouseEvent e) {
			if (getMouseMode() != MouseMode.SCALE)
				return ;

			Point2D[] vertices = getVertices();
			Point2D loc = getView().getSceneLocation(e.getPoint());
			try {
				getParent().getWorldTransform().inverseTransform(loc, loc);
			} catch (Exception e1) {
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
			switch(mScalePointID) {
			case SP_UL : mix = loc.getX(); may = loc.getY(); break;
			case SP_UR : max = loc.getX(); may = loc.getY(); break;
			case SP_LR : max = loc.getX(); miy = loc.getY(); break;
			case SP_LL : mix = loc.getX(); miy = loc.getY(); break;
			}
			mNewVertices = new Point2D[] {
					new Point2D.Double(mix, may),
					new Point2D.Double(max, may),
					new Point2D.Double(max, miy),
					new Point2D.Double(mix, miy)
			};
			setVertices(new Rectangle2D.Double(mix, miy, max-mix, may-miy), mNewVertices);
			e.consume();
		}

		@Override
		protected void fireEvent() {
			GraphicsItem item = getSelectedItem();
			fireScaleEvent(item, mInitialVertices, mNewVertices);
		}

		Point2D[] getVertices() {
			Rectangle2D localBounds = mSelectedItem.getBoundingBox(); //getLocalBounds();
			return Utils.getVertices(localBounds);
		}

	}
	protected class RotatePointItem extends AbstractSelectionWorkerItem {

		private double 	mInitialRotation;
		private double 	mLastXPosition;

		public RotatePointItem(Shape shape, CallbackMode cm, MouseMode mm, Cursor cursor, boolean useMotionListener) {
			super(true, cm, mm, cursor, useMotionListener);
			setShape(shape);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);

			mInitialRotation = BoundingBoxSelectionItem.this.getSceneRotation();
			mLastXPosition = e.getLocationOnScreen().getX();
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			if (getMouseMode() != MouseMode.ROTATE)
				return ;
			Point los = e.getLocationOnScreen();
			double cx = los.getX();

			double dx = cx - mLastXPosition;
			double currentRotation = BoundingBoxSelectionItem.this.getSceneRotation();
			BoundingBoxSelectionItem.this.setSceneRotation(currentRotation + dx * 0.4);

			mLastXPosition = cx;
			e.consume();
		}
		@Override
		protected void fireEvent() {
			GraphicsItem item = getSelectedItem();
			double newRotation = BoundingBoxSelectionItem.this.getSceneRotation();
			fireRotateEvent(item, mInitialRotation, newRotation);
		}
	}

	protected class MovePointItem extends AbstractSelectionWorkerItem {

		private double 				mOffsetX = 0;
		private double 				mOffsetY = 0;
		private final Point2D		mMouseSceneLocation = new Point2D.Double(0, 0);
		private final Point2D		mInitialItemSceneLocation = new Point2D.Double();

		public MovePointItem(CallbackMode cm, MouseMode mm, Cursor cursor, boolean useMotionListener) {
			super(false, cm, mm, cursor, useMotionListener);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);

			BoundingBoxSelectionItem.this.getSceneLocation(mInitialItemSceneLocation);

			getView().getSceneLocation(e.getPoint(), mMouseSceneLocation);
			mOffsetX = mInitialItemSceneLocation.getX() - mMouseSceneLocation.getX();
			mOffsetY = mInitialItemSceneLocation.getY() - mMouseSceneLocation.getY();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (getMouseMode() != MouseMode.MOVE)
				return ;

			getView().getSceneLocation(e.getPoint(), mMouseSceneLocation);
			double x = mMouseSceneLocation.getX() + mOffsetX;
			double y = mMouseSceneLocation.getY() + mOffsetY;

			BoundingBoxSelectionItem.this.setSceneLocation(x, y);

			e.consume();
		}
		@Override
		protected void fireEvent() {
			GraphicsItem item = getSelectedItem();
			Point2D startLoc = new Point2D.Double(mInitialItemSceneLocation.getX(), mInitialItemSceneLocation.getY());
			Point2D endLoc = BoundingBoxSelectionItem.this.getSceneLocation();
			fireMoveEvent(item, startLoc, endLoc);
		}
	}

	/** inactive item (red dot) at the center of the bounding volume, marks the center point of rotation and scale operations */
	class CenterPointItem extends GraphicsItem {
		public CenterPointItem() {
			super(new Arc2D.Double(-2,-2,4,4, 0, 360, Arc2D.CHORD));
		}
		@Override
		public void draw(Graphics2D g, IDrawContext ctx) {
			setSceneScale(ctx.getScale());
			super.draw(g, ctx);
		}
	}

	class RebuildListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			rebuild();
		}
	}


	private static DrawableStyle		sBoundingBoxStyle = null;
	private static DrawableStyle		sScalePointStyle = null;
	private static DrawableStyle		sCenterItemSyle = null;

	private final ScalePointItem[]		mScalePointItems = new ScalePointItem[4];
	private final RotatePointItem		mRotatePointItem;
	private final MovePointItem			mMovePointItem;
	private final CenterPointItem		mCenterItem;
	private final RebuildListener		mRebuildListener = new RebuildListener();

	private GraphicsItem 				mSelectedItem;


	public BoundingBoxSelectionItem(SelectionHandler callbackManager) {
		super(callbackManager);

		mMovePointItem = new MovePointItem(CallbackMode.MOVE, MouseMode.MOVE, new Cursor(Cursor.MOVE_CURSOR), true);
		mMovePointItem.setStyle(getBoundingBoxStyle());
		addItem(mMovePointItem);

		Rectangle2D controlPointShape = new Rectangle2D.Double(-5, -5, 10, 10);
		for (int i = 0; i < 4; i++) {
			mScalePointItems[i] = new ScalePointItem(controlPointShape, i, CallbackMode.SCALE, MouseMode.SCALE, new Cursor(SCALE_POINT_CURSOR_TYPES[i]), true);
			mScalePointItems[i].setStyle(getScalePointStyle());
			addItem(mScalePointItems[i]);
		}

		Cursor rotateCursor = null;
		try {
			Image rotateCursorImage = ImageIO.read(getClass().getClassLoader().getResource("Rotate-Icon.png"));
			rotateCursor = Toolkit.getDefaultToolkit().createCustomCursor(rotateCursorImage, new Point(16, 16), "Rotate");
		} catch (IOException e) {
			rotateCursor = new  Cursor(Cursor.WAIT_CURSOR); //not the best solution but somehow it also works....
		}
		mRotatePointItem = new RotatePointItem(controlPointShape, CallbackMode.ROTATE, MouseMode.ROTATE, rotateCursor, true);
		mRotatePointItem.setStyle(getScalePointStyle());
		addItem(mRotatePointItem);

		mCenterItem = new CenterPointItem();
		mCenterItem.setCenter(0,0);
		mCenterItem.setStyle(getCenterItemStyle());
		addItem(mCenterItem);

		setStyle(getBoundingBoxStyle());
	}


	@Override
	protected void onRemovedFromScene(GraphicsScene scene) {
		super.onRemovedFromScene(scene);
		cleanUpListener();
	}

	private void cleanUpListener() {
		mSelectedItem.removePropertyChangeListener(GraphicsItem.PROP_CENTER_X, mRebuildListener);
		mSelectedItem.removePropertyChangeListener(GraphicsItem.PROP_CENTER_Y, mRebuildListener);
		mSelectedItem.removePropertyChangeListener(GraphicsItem.PROP_ROTATION, mRebuildListener);
		mSelectedItem.removePropertyChangeListener(GraphicsItem.PROP_SCALE_X, mRebuildListener);
		mSelectedItem.removePropertyChangeListener(GraphicsItem.PROP_SCALE_Y, mRebuildListener);
	}

	@Override
	public void setSelectedItem(GraphicsItem item) {
		if (item == null) {
			if (mSelectedItem != null) { //clean up listener
				cleanUpListener();
			}
		}
		mSelectedItem = item;
		if (mSelectedItem == null) {
			setVisible(false);
			return ;
		}

		mSelectedItem.addPropertyChangeListener(GraphicsItem.PROP_CENTER_X, mRebuildListener);
		mSelectedItem.addPropertyChangeListener(GraphicsItem.PROP_CENTER_Y, mRebuildListener);
		mSelectedItem.addPropertyChangeListener(GraphicsItem.PROP_ROTATION, mRebuildListener);
		mSelectedItem.addPropertyChangeListener(GraphicsItem.PROP_SCALE_X, mRebuildListener);
		mSelectedItem.addPropertyChangeListener(GraphicsItem.PROP_SCALE_Y, mRebuildListener);

		rebuild();
	}


	private void rebuild() {

		setSceneLocation(mSelectedItem.getSceneLocation());
		setSceneRotation(mSelectedItem.getSceneRotation());
		//@note: Some of the items may have a scale correction to handle the scale derivation of Web-Mercator
		//see GeoGraphicsItem. However at this point we have no access to this correction factor thus we have
		//to read it from the transformation matrix
		AffineTransform at = mSelectedItem.getWorldTransform();
		//@note: The method at.getScaleX() does only works fine, if the matrix does not contain any rotation
		//if there is a rotation we have to calculate the scale manually using the m00 and m01 components (for x)
		//those values can be accessed by at.getScale()==m00 and at.getShearX()==m01
		double sx1 = at.getScaleX();
		double sx2 = at.getShearX();
		double sx = Math.sqrt(sx1*sx1+sx2*sx2);
		double sy1 = at.getScaleY(), sy2 = at.getShearY();
		double sy = Math.sqrt(sy1*sy1 + sy2*sy2);

		setSceneScale(sx, sy);

		Rectangle2D localBounds = mSelectedItem.getBoundingBox(); //getLocalBounds();
		Point2D[] localVertices = Utils.getVertices(localBounds);

		setVertices(localBounds, localVertices);
	}

	public void setVertices(Rectangle2D localBounds, Point2D[] localVertices) {
		for (int i = 0; i < 4; i++)
			mScalePointItems[i].setCenter(localVertices[i]);

		mMovePointItem.setCenter(0, 0);
		mMovePointItem.setShape(getShape(localVertices));

		double h = localBounds.getHeight() / 2.0 + localBounds.getHeight() / 5.0;
		mRotatePointItem.setCenter(localBounds.getCenterX(), localBounds.getCenterY() + h);

		//mark each item to be dirty => bounds and matrices will be re-calculated
		mRotatePointItem.markDirty();
		mMovePointItem.markDirty();
		mCenterItem.markDirty();
		mCenterItem.setCenter(localBounds.getCenterX(), localBounds.getCenterY());
		for (int i = 0; i < 4; i++) mScalePointItems[i].markDirty();
	}


	public GraphicsItem getSelectedItem() {
		return mSelectedItem;
	}


	protected DrawableStyle getBoundingBoxStyle() {
		if (sBoundingBoxStyle == null) {
			sBoundingBoxStyle = new DrawableStyle();
			sBoundingBoxStyle.setName("BoundingBoxStyle");
			Color bb = Color.BLUE.brighter();
			Color tbb = new Color(bb.getRed(), bb.getGreen(), bb.getBlue(), 55);
			sBoundingBoxStyle.setLinePaint(bb);
			sBoundingBoxStyle.setFillPaint(tbb);
//			Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
//			sBoundingBoxStyle.setLineStroke(dashed);
		}
		return sBoundingBoxStyle;
	}

	protected DrawableStyle getScalePointStyle() {
		if (sScalePointStyle == null) {
			sScalePointStyle = new DrawableStyle("ControlPointStyle", null, null, Color.BLUE);
		}
		return sScalePointStyle;
	}
	protected DrawableStyle getCenterItemStyle() {
		if (sCenterItemSyle == null) {
			sCenterItemSyle = new DrawableStyle("CenterItemStyle", null, null, Color.RED);
		}
		return sCenterItemSyle;
	}


	private Path2D getShape(Point2D[] vertices) {
		Path2D 	p = new Path2D.Double();
		p.moveTo(vertices[0].getX(), vertices[0].getY());
		for (int i = 1; i < 4; i++)
			p.lineTo(vertices[i].getX(), vertices[i].getY());
		p.lineTo(vertices[0].getX(), vertices[0].getY());
		p.closePath();
		return p;
	}

}
