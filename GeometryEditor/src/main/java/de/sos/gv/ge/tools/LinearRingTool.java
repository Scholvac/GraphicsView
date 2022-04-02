package de.sos.gv.ge.tools;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.menu.MenuManager;
import de.sos.gv.ge.model.geom.IGeometry.GeometryType;

public class LinearRingTool extends AbstractGeometryTool {

	public LinearRingTool(final MenuManager mm) {
		super(mm);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected GeometryItem createNewItem(final Point2D loc) {
		return createItem(loc, GeometryType.LinearRing);
	}
	@Override
	public void mouseClicked(final MouseEvent e) {

		if (SwingUtilities.isLeftMouseButton(e) && getItem() != null) {
			if (getItem().getGeometry().numPoints() > 3)
				getItem().getGeometry().removePoint(getItem().getGeometry().numPoints()-1);
		}
		super.mouseClicked(e);

		if (SwingUtilities.isLeftMouseButton(e) && getItem() != null) {
			final Point2D loc = getSceneLocation(e);
			loc.setLocation(loc.getX(), loc.getY()+1);
			getItem().getGeometry().addPoint(getItem().getGeometry().numPoints()-1, loc);
		}

	}
	@Override
	public void mouseMoved(final MouseEvent e) {
		super.mouseMoved(e);
		if (getItem() != null) {
			final Point2D loc = getSceneLocation(e);
			final int c = getItem().getGeometry().numPoints()-1;
			getItem().getGeometry().replacePoint(c, loc);
		}
	}
}
