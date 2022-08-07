package de.sos.gv.ge.tools;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.items.GeometryItem.GeometryItemMode;
import de.sos.gv.ge.items.Styles;
import de.sos.gv.ge.menu.MenuManager;
import de.sos.gv.ge.model.geom.Geometry;
import de.sos.gv.ge.model.geom.IGeometry;
import de.sos.gv.ge.model.geom.IGeometry.GeometryType;

public abstract class AbstractGeometryTool extends AbstractTool {

	private MenuManager		mMenuManager;

	private GeometryItem 	mItem = null;
	private GeometryItem	mFutureSegment = null;

	public AbstractGeometryTool(final MenuManager mm) {
		mMenuManager = mm;
	}


	@Override
	protected void deactivate() {
		if (mItem != null) {
			getScene().removeItem(mItem);//has not been finished, thus we remove it
			mItem = null;
		}
		if (mFutureSegment != null) {
			getScene().removeItem(mFutureSegment);
			mFutureSegment = null;
		}
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)){
			if (mItem != null) {
				mItem.setGeometryItemMode(GeometryItemMode.NORMAL);
				finishItem(mItem);
				mItem = null;
			}
			deactivate();
		}
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (mItem == null) {
				final Point2D loc = getSceneLocation(e);
				mItem = createNewItem(loc);
				mItem.setGeometryItemMode(GeometryItemMode.CREATE);
				mFutureSegment = createItem(loc, GeometryType.LineString);
				mFutureSegment.setStyle(Styles.FutureSegment);

				final Point2D loc2 = new Point2D.Double(loc.getX(), loc.getY()+1);//need to differ at least a little bit
				mFutureSegment.getGeometry().addPoint(1, loc2);
				mFutureSegment.setZOrder(mItem.getZOrder()+1);

				getScene().addItem(mItem);
				getScene().addItem(mFutureSegment);
			}else {
				final IGeometry geom = mItem.getGeometry();
				final int o = geom.isClosed() ? 1 : 0;
				geom.addPoint(geom.numPoints()-o, getSceneLocation(e));

				mFutureSegment.getGeometry().replacePoint(0, getSceneLocation(e));
			}
		}
	}

	protected void finishItem(final GeometryItem item) {
		// TODO: to be implemented by sub tools
	}


	protected abstract GeometryItem createNewItem(Point2D loc);


	@Override
	public void mouseMoved(final MouseEvent e) {
		if (mFutureSegment != null) {
			final IGeometry geom = mFutureSegment.getGeometry();
			geom.replacePoint(1, getSceneLocation(e));
		}
		super.mouseMoved(e);
	}

	public MenuManager getMenuManager() { return mMenuManager;}
	public GeometryItem	getItem() { return mItem;}
	protected GeometryItem getFutureSegment() { return mFutureSegment;}

	protected IGeometry createGeometry(final Point2D start, final GeometryType type){
		final Geometry geom = new Geometry(type);
		geom.addPoint(0, start);
		return geom;
	}
	protected GeometryItem createItem(final IGeometry geom) {
		return new GeometryItem(getMenuManager(), geom);
	}
	protected GeometryItem createItem(final Point2D location, final GeometryType type) {
		return createItem(createGeometry(location, type));
	}
}
