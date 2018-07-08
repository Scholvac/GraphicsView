package de.sos.gv.ge.items;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import de.sos.gv.ge.model.geom.GeometryUtils;
import de.sos.gv.ge.model.geom.IGeometry;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.styles.DrawableStyle;
import de.sos.gvc.styles.ScaledStroke;

public class GeometryItem extends GraphicsItem {

	private static final DrawableStyle 					sNormalStyle = new DrawableStyle("DefaultStyle", Color.BLUE, new ScaledStroke(2), new Color(0, 148, 255));
	
	
	private IGeometry 									mGeometry;
	private List<ContourPointItem>						mContourPoints = new ArrayList<>();
		
	public GeometryItem(IGeometry geom) {
		super(GeometryUtils.createShape(geom));
		mGeometry = geom;
		setStyle(sNormalStyle);
		
		enableEditPoints(true);
	}

	public void enableEditPoints(boolean b) {
		if (b) {
			for (int i = 0; i < mGeometry.numPoints(); i++) {
				ContourPointItem cp = new ContourPointItem(mGeometry, i);
				addItem(cp); mContourPoints.add(cp);
				cp.setSceneLocation(mGeometry.getPoint(i));
			}
		}else {
			for (ContourPointItem cp : mContourPoints)
				removeItem(cp);
			mContourPoints.clear();
		}
	}
}
