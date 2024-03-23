package de.sos.gv.ge.items;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import de.sos.gv.ge.menu.MenuManager;
import de.sos.gv.ge.model.geom.GeometryUtils;
import de.sos.gv.ge.model.geom.IGeometry;
import de.sos.gv.ge.model.geom.IGeometry.GeometryType;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.styles.DrawableStyle;

public class GeometryItem extends GraphicsItem implements PropertyChangeListener {

	public static enum GeometryItemMode {
		NORMAL, EDIT, CREATE
	}


	private IGeometry 									mGeometry;
	private MenuManager 								mMenuManager;
	private	GeometryItemMode							mMode = GeometryItemMode.NORMAL;


	private List<ContourPointItem>						mContourPoints = new ArrayList<>();
	private List<IntermediatePointItem>					mIntermediatePoints = new ArrayList<>();




	public GeometryItem(final MenuManager mm, final IGeometry geom) {
		this(mm, geom, null);
	}
	public GeometryItem(final MenuManager mm, final IGeometry geom, final ParameterContext context) {
		super(GeometryUtils.createShape(geom), context);
		mGeometry = geom;
		mMenuManager = mm;
		setStyles(mMode);

		enableEditPoints(mMode != GeometryItemMode.NORMAL);

		setMouseSupport(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				onMouseClicked(e);
			}


		});
	}

	public void enableEditPoints(final boolean b) {
		if (b) {
			for (int i = 0; i < mGeometry.numPoints(); i++) {
				final ContourPointItem cp = new ContourPointItem(mMenuManager, this, i);
				addItem(cp); mContourPoints.add(cp);
				cp.setLocalLocation(mGeometry.getPoint(i));
			}
			for (int i = 0; i < mGeometry.numPoints(); i++) {
				final int idx0 = i, idx1 = mGeometry.getNextIndex(i);
				if (idx1 < 0)
					continue; //can happen on point and linestring
				final IntermediatePointItem ipi = new IntermediatePointItem(this, idx0, idx1);
				addItem(ipi); mIntermediatePoints.add(ipi);
				ipi.update();
			}
			mGeometry.addListener(this);
		}else {
			mGeometry.removeListener(this);
			for (final ContourPointItem cp : mContourPoints) removeItem(cp);
			for (final IntermediatePointItem ipi : mIntermediatePoints) removeItem(ipi);

			mContourPoints.clear();
			mIntermediatePoints.clear();
		}
	}


	public void updateIntermediatePoints(final int idx) {
		if (mIntermediatePoints.isEmpty()) return ;
		final int lower = mGeometry.getPreviousIndex(idx);
		final IntermediatePointItem ipi_lower = mIntermediatePoints.get(lower);
		assert ipi_lower.getIndex0() == lower;
		ipi_lower.update();
		final IntermediatePointItem ipi_idx = mIntermediatePoints.get(idx);
		assert ipi_idx.getIndex0() == idx;
		ipi_idx.update();
	}


	private boolean needRepaintOnPropertyChange(final PropertyChangeEvent pce) {
		final String pn = pce.getPropertyName();
		if (IGeometry.POINT_CHANGE_EVENT.equals(pn) || IGeometry.POINTS_CHANGE_EVENT.equals(pn))
			return true;
		return false;
	}
	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (needRepaintOnPropertyChange(evt)) {
			setShape(GeometryUtils.createShape(mGeometry));
			if (mContourPoints.isEmpty() == false) {
				//not the fine way but works :)
				enableEditPoints(false);
				enableEditPoints(true);
			}
		}
	}

	public IGeometry getGeometry() { return mGeometry; }

	/**
	 * Transform's the Geometry in a way that the new Geometry's center is at the center of the
	 * current position.
	 */
	public void applyTransform() {
		System.out.println();
	}

	public GeometryItemMode getGeometryItemMode() { return mMode; }
	public void setGeometryItemMode(final GeometryItemMode mode) {
		if (mMode != mode) {
			mMode = mode;
			setStyles(mode);
			if (mMode == GeometryItemMode.NORMAL)
				enableEditPoints(false);
			else
				enableEditPoints(true);
		}
	}
	private void setStyles(final GeometryItemMode mode) {
		DrawableStyle geomStyle = null;
		DrawableStyle pointStyle = null;
		final GeometryType type = getGeometry().getType();
		switch(mode) {
			case NORMAL:
				geomStyle = type == GeometryType.Polygon ? Styles.GeometryNormalFilled : Styles.GeometryNormalLine;
				pointStyle = Styles.NormalStyle;
				break;
			case EDIT:
				geomStyle = type == GeometryType.Polygon ? Styles.GeometryEditFilled : Styles.GeometryEditLine;
				pointStyle = Styles.ActiveStyle;
				break;
			case CREATE:
				geomStyle = type == GeometryType.Polygon ? Styles.GeometryCreateFilled : Styles.GeometryCreateLine;
				pointStyle = Styles.ActiveStyle;
				break;
		}
		setStyle(geomStyle);
	}
	public boolean allowsManipulation() {
		return getGeometryItemMode() == GeometryItemMode.EDIT;
	}

	private void onMouseClicked(final MouseEvent e) {
		if (allowsManipulation() && SwingUtilities.isRightMouseButton(e)) {
			final JPopupMenu pm = new JPopupMenu();
			mMenuManager.fillGeometryItemMenu(this, getScene(), pm);
			pm.show(getView().getComponent(), e.getX(), e.getY());
			e.consume();
		}
	}

}
