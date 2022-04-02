package de.sos.gvc.handler.selection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
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
import de.sos.gvc.handler.SelectionHandler.ItemRotateEvent;
import de.sos.gvc.handler.SelectionHandler.ItemScaleEvent;
import de.sos.gvc.styles.DrawableStyle;

public class BoundingBoxSelectionItem extends AbstractSelectionItem { //TODO: change the name

	protected class ScalePointItem extends AbstractSelectionWorkerItem {

		private Point2D[] 		mInitialVertices;
		private Point2D[]		mNewVertices;
		private int				mScalePointID;

		public ScalePointItem(final Shape shape, final int scalePointID, final CallbackMode cm, final MouseMode mm, final Cursor cursor, final boolean useMotionListener) {
			super(true, cm, mm, cursor, useMotionListener);
			setShape(shape);
			mScalePointID = scalePointID;
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			super.mousePressed(e);
			mInitialVertices = getVertices();
		}


		@Override
		public void mouseDragged(final MouseEvent e) {
			if (getMouseMode() != MouseMode.SCALE)
				return ;

			final Point2D[] vertices = getVertices();
			final Point2D loc = getView().getSceneLocation(e.getPoint());
			try {
				getParent().getWorldTransform().inverseTransform(loc, loc);
			} catch (final Exception e1) {
				e1.printStackTrace();
			}
			double mix = Double.MAX_VALUE, miy = Double.MAX_VALUE;
			double max = -Double.MAX_VALUE, may = -Double.MAX_VALUE;
			for (int i = 0; i < 4; i++) {
				final double x = vertices[i].getX(), y = vertices[i].getY();
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

			final double nw = max-mix, nh = may-miy;
			final double ow = mSelectedItem.getBoundingBox().getWidth(), oh = mSelectedItem.getBoundingBox().getHeight();

			final double sx = nw / ow, sy = nh / oh;
			BoundingBoxSelectionItem.this.setScale(sx, sy);

			e.consume();
		}

		@Override
		protected void fireEvent() {
			final GraphicsItem item = getSelectedItem();
			fireScaleEvent(item, mInitialVertices, mNewVertices);
		}

		Point2D[] getVertices() {
			final Rectangle2D localBounds = mSelectedItem.getBoundingBox(); //getLocalBounds();
			return Utils.getVertices(localBounds);
		}

	}
	protected class RotatePointItem extends AbstractSelectionWorkerItem {

		private double 	mInitialRotation;
		private double 	mLastXPosition;

		public RotatePointItem(final Shape shape, final CallbackMode cm, final MouseMode mm, final Cursor cursor, final boolean useMotionListener) {
			super(true, cm, mm, cursor, useMotionListener);
			setShape(shape);
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			super.mousePressed(e);

			mInitialRotation = BoundingBoxSelectionItem.this.getRotation();
			mLastXPosition = e.getLocationOnScreen().getX();
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			if (getMouseMode() != MouseMode.ROTATE)
				return ;
			final Point los = e.getLocationOnScreen();
			final double cx = los.getX();

			final double dx = cx - mLastXPosition;
			final double currentRotation = BoundingBoxSelectionItem.this.getRotation();
			final double newRotationDeg = currentRotation + dx * 0.4;

			BoundingBoxSelectionItem.this.setRotation(newRotationDeg);

			mLastXPosition = cx;
			e.consume();
		}
		@Override
		protected void fireEvent() {
			final GraphicsItem item = getSelectedItem();
			final double newRotation = BoundingBoxSelectionItem.this.getSceneRotation();
			fireRotateEvent(item, mInitialRotation, newRotation);
		}
	}
	protected class MovePointItem extends AbstractSelectionWorkerItem {

		private double 				mOffsetX = 0;
		private double 				mOffsetY = 0;
		private final Point2D		mMouseSceneLocation = new Point2D.Double(0, 0);
		private final Point2D		mInitialItemSceneLocation = new Point2D.Double();

		public MovePointItem(final CallbackMode cm, final MouseMode mm, final Cursor cursor, final boolean useMotionListener) {
			super(false, cm, mm, cursor, useMotionListener);
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			super.mousePressed(e);

			BoundingBoxSelectionItem.this.getSceneLocation(mInitialItemSceneLocation);

			getView().getSceneLocation(e.getPoint(), mMouseSceneLocation);
			mOffsetX = mInitialItemSceneLocation.getX() - mMouseSceneLocation.getX();
			mOffsetY = mInitialItemSceneLocation.getY() - mMouseSceneLocation.getY();
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			if (getMouseMode() != MouseMode.MOVE)
				return ;

			getView().getSceneLocation(e.getPoint(), mMouseSceneLocation);
			final double x = mMouseSceneLocation.getX() + mOffsetX;
			final double y = mMouseSceneLocation.getY() + mOffsetY;

			BoundingBoxSelectionItem.this.setSceneLocation(x, y);

			e.consume();
		}
		@Override
		protected void fireEvent() {
			final GraphicsItem item = getSelectedItem();
			final Point2D startLoc = new Point2D.Double(mInitialItemSceneLocation.getX(), mInitialItemSceneLocation.getY());
			final Point2D endLoc = BoundingBoxSelectionItem.this.getSceneLocation();
			fireMoveEvent(item, startLoc, endLoc);
		}
	}

	/** inactive item (red dot) at the center of the bounding volume, marks the center point of rotation and scale operations */
	class CenterPointItem extends GraphicsItem {
		public CenterPointItem() {
			super(new Arc2D.Double(-3,-3, 6, 6, 0, 360, Arc2D.CHORD));
		}
		@Override
		public void draw(final Graphics2D g, final IDrawContext ctx) {
			setSceneScale(ctx.getScale());
			super.draw(g, ctx);
		}
	}

	class RebuildListener implements PropertyChangeListener {
		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			rebuild();
		}
	}

	private static final Cursor sRotationCursor;
	static {
		Cursor c = null;
		try {
			final Image rotateCursorImage = ImageIO.read(BoundingBoxSelectionItem.class.getClassLoader().getResource("Rotate-Icon.png"));
			c = Toolkit.getDefaultToolkit().createCustomCursor(rotateCursorImage, new Point(16, 16), "Rotate");
		} catch (final IOException e) {
			e.printStackTrace();
			c = new  Cursor(Cursor.WAIT_CURSOR); //not the best solution but somehow it also works....
		}
		sRotationCursor = c;
	}

	private static DrawableStyle		sBoundingBoxStyle = null;
	private static DrawableStyle		sScalePointStyle = null;
	private static DrawableStyle		sCenterItemSyle = null;

	private final ScalePointItem[]		mScalePointItems = new ScalePointItem[4];
	private final RotatePointItem		mRotatePointItem;
	private final MovePointItem			mMovePointItem;
	private final CenterPointItem		mCenterItem;
	private final GraphicsItem			mShapeItem;
	private final RebuildListener		mRebuildListener = new RebuildListener();

	private GraphicsItem 				mSelectedItem;


	public BoundingBoxSelectionItem(final SelectionHandler callbackManager) {
		super(callbackManager);

		mMovePointItem = new MovePointItem(CallbackMode.MOVE, MouseMode.MOVE, new Cursor(Cursor.MOVE_CURSOR), true);
		mMovePointItem.setStyle(getBoundingBoxStyle());
		addItem(mMovePointItem);

		final Shape controlPointShape = new Rectangle2D.Double(-4, -4, 8, 8);
		for (int i = 0; i < 4; i++) {
			mScalePointItems[i] = new ScalePointItem(controlPointShape, i, CallbackMode.SCALE, MouseMode.SCALE, new Cursor(SCALE_POINT_CURSOR_TYPES[i]), true);
			mScalePointItems[i].setStyle(getScalePointStyle());
			addItem(mScalePointItems[i]);
		}

		mRotatePointItem = new RotatePointItem(controlPointShape, CallbackMode.ROTATE, MouseMode.ROTATE, sRotationCursor, true);
		mRotatePointItem.setStyle(getScalePointStyle());
		addItem(mRotatePointItem);

		mCenterItem = new CenterPointItem();
		mCenterItem.setStyle(getCenterItemStyle());
		addItem(mCenterItem);


		setStyle(getBoundingBoxStyle());

		mShapeItem = new GraphicsItem();
		mShapeItem.setStyle(getBoundingBoxStyle());
		addItem(mShapeItem);
	}


	@Override
	protected void onRemovedFromScene(final GraphicsScene scene) {
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
	public void setSelectedItem(final GraphicsItem item) {
		if (item == null && mSelectedItem != null) { //clean up listener
			cleanUpListener();
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
		mShapeItem.setShape(mSelectedItem.getShape());


		setSceneLocation(mSelectedItem.getSceneLocation());
		setSceneRotation(mSelectedItem.getSceneRotation());
		//@note: Some of the items may have a scale correction to handle the scale derivation of Web-Mercator
		//see GeoGraphicsItem. However at this point we have no access to this correction factor thus we have
		//to read it from the transformation matrix
		final AffineTransform at = mSelectedItem.getWorldTransform();
		//@note: The method at.getScaleX() does only works fine, if the matrix does not contain any rotation
		//if there is a rotation we have to calculate the scale manually using the m00 and m01 components (for x)
		//those values can be accessed by at.getScale()==m00 and at.getShearX()==m01
		final double sx1 = at.getScaleX();
		final double sx2 = at.getShearX();
		final double sx = Math.sqrt(sx1*sx1+sx2*sx2);
		final double sy1 = at.getScaleY(), sy2 = at.getShearY();
		final double sy = Math.sqrt(sy1*sy1 + sy2*sy2);

		setSceneScale(sx, sy);

		final Rectangle2D localBounds = mSelectedItem.getBoundingBox(); //getLocalBounds();
		final Point2D[] localVertices = Utils.getVertices(localBounds);

		setVertices(localBounds, localVertices);
		mCenterItem.setSceneLocation(mSelectedItem.getSceneLocation());

	}

	public void setVertices(final Rectangle2D localBounds, final Point2D[] localVertices) {
		for (int i = 0; i < 4; i++)
			mScalePointItems[i].setCenter(localVertices[i]);

		mMovePointItem.setCenter(0, 0);
		mMovePointItem.setShape(getShape(localVertices));

		final double h = localBounds.getHeight() / 2.0 + localBounds.getHeight() / 5.0;
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
			final Color bb = Color.BLUE.brighter();
			final Color tbb = new Color(bb.getRed(), bb.getGreen(), bb.getBlue(), 55);
			sBoundingBoxStyle.setLinePaint(bb);
			sBoundingBoxStyle.setFillPaint(tbb);
			final Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
			sBoundingBoxStyle.setLineStroke(dashed);
		}
		return sBoundingBoxStyle;
	}

	protected DrawableStyle getScalePointStyle() {
		if (sScalePointStyle == null) {
			sScalePointStyle = new DrawableStyle("ControlPointStyle", Color.RED, null, Color.BLUE);
		}
		return sScalePointStyle;
	}
	protected DrawableStyle getCenterItemStyle() {
		if (sCenterItemSyle == null) {
			sCenterItemSyle = new DrawableStyle("CenterItemStyle", Color.BLUE, null, Color.RED);
		}
		return sCenterItemSyle;
	}


	private Path2D getShape(final Point2D[] vertices) {
		final Path2D 	p = new Path2D.Double();
		p.moveTo(vertices[0].getX(), vertices[0].getY());
		for (int i = 1; i < 4; i++)
			p.lineTo(vertices[i].getX(), vertices[i].getY());
		p.lineTo(vertices[0].getX(), vertices[0].getY());
		p.closePath();
		return p;
	}


	@Override
	protected void fireScaleEvent(final ItemScaleEvent event) {
		super.fireScaleEvent(event);
		final double[] factors = event.getScaleFactors(0);
		mShapeItem.setScale(factors[0], factors[1]);
	}
	@Override
	protected void fireRotateEvent(final ItemRotateEvent event) {
		// TODO Auto-generated method stub
		super.fireRotateEvent(event);
	}


}
