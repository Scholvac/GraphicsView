package de.sos.gv.ge.tools;

import java.awt.geom.Point2D;

import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.menu.MenuManager;
import de.sos.gv.ge.model.geom.IGeometry.GeometryType;

public class PolygonTool extends LinearRingTool {

	public PolygonTool(final MenuManager mm) {
		super(mm);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected GeometryItem createNewItem(final Point2D loc) {
		return createItem(loc, GeometryType.Polygon);
	}
}
