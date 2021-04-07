package de.sos.gv.ge.items;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import de.sos.gv.ge.menu.MenuManager;
import de.sos.gv.ge.model.geom.GeometryUtils;
import de.sos.gv.ge.model.geom.IGeometry;
import de.sos.gv.ge.model.geom.IGeometry.GeometryType;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.styles.DrawableStyle;
import de.sos.gvc.styles.ScaledStroke;

public class GeometryItem extends GraphicsItem implements PropertyChangeListener {

	private static final DrawableStyle 					sFilledStyle = new DrawableStyle("DefaultStyle", Color.BLUE, new ScaledStroke(2), new Color(0, 148, 255));
	private static final DrawableStyle 					sLineStyle = new DrawableStyle("DefaultStyle", Color.BLUE, new ScaledStroke(2), null);
	
	
	private IGeometry 									mGeometry;
	private MenuManager 								mMenuManager;
	
	
	private List<ContourPointItem>						mContourPoints = new ArrayList<>();
	private List<IntermediatePointItem>					mIntermediatePoints = new ArrayList<>();


	
		
	public GeometryItem(MenuManager mm, IGeometry geom) {
		super(GeometryUtils.createShape(geom));
		mGeometry = geom;
		mMenuManager = mm;
		if (geom.getType() == GeometryType.LineString)
			setStyle(sLineStyle);
		else
			setStyle(sFilledStyle);
		
		enableEditPoints(true);
//		addPropertyChangeListener(PROP_CENTER_X, this);
//		addPropertyChangeListener(PROP_CENTER_Y, this);
//		addPropertyChangeListener(PROP_ROTATION, this);
//		addPropertyChangeListener(PROP_SCALE_X, this);
//		addPropertyChangeListener(PROP_SCALE_Y, this);
	}

	public void enableEditPoints(boolean b) {
		if (b) {
			for (int i = 0; i < mGeometry.numPoints(); i++) {
				ContourPointItem cp = new ContourPointItem(mMenuManager, mGeometry, i);
				addItem(cp); mContourPoints.add(cp);
				cp.setLocalLocation(mGeometry.getPoint(i));
			}
			for (int i = 0; i < mGeometry.numPoints(); i++) {
				int idx0 = i, idx1 = mGeometry.getNextIndex(i);
				if (idx1 < 0)
					continue; //can happen on point and linestring
				IntermediatePointItem ipi = new IntermediatePointItem(mGeometry, idx0, idx1);
				addItem(ipi); mIntermediatePoints.add(ipi);
				ipi.update();
			}
			mGeometry.addListener(this);
		}else {
			mGeometry.removeListener(this);
			for (ContourPointItem cp : mContourPoints) removeItem(cp);
			for (IntermediatePointItem ipi : mIntermediatePoints) removeItem(ipi);
				
			mContourPoints.clear();
			mIntermediatePoints.clear();
		}
	}
	
	
	public void updateIntermediatePoints(int idx) {
		if (mIntermediatePoints.isEmpty()) return ;
		int lower = mGeometry.getPreviousIndex(idx);
		IntermediatePointItem ipi_lower = mIntermediatePoints.get(lower);
		assert(ipi_lower.getIndex0() == lower);
		ipi_lower.update();
		IntermediatePointItem ipi_idx = mIntermediatePoints.get(idx);
		assert(ipi_idx.getIndex0() == idx);
		ipi_idx.update();
	}
	
	
	private boolean needRepaintOnPropertyChange(PropertyChangeEvent pce) {
		String pn = pce.getPropertyName();
		if (pn.equals("Points")) 
			return true;
//		if (pn.equals(GraphicsItem.PROP_CENTER_X) || pn.equals(GraphicsItem.PROP_CENTER_Y)) 
//			return true;
//		if (pn.equals(GraphicsItem.PROP_ROTATION)) 
//			return true;
//		if (pn.equals(GraphicsItem.PROP_SCALE_X) || pn.equals(GraphicsItem.PROP_SCALE_Y)) 
//			return true;
		return false;
	}
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
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
	
	
	
//	@Override
//	protected void onAddedToScene(GraphicsScene scene) {
//		super.onAddedToScene(scene);
//		mGeometry.addListener(this);
//	}
//	@Override
//	protected void onRemovedFromScene(GraphicsScene scene) {
//		super.onRemovedFromScene(scene);
//		mGeometry.removeListener(this);
//	}
}
