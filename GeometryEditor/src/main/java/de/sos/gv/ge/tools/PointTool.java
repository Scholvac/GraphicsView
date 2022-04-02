package de.sos.gv.ge.tools;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.menu.MenuManager;
import de.sos.gv.ge.model.geom.IGeometry.GeometryType;
import de.sos.gvc.GraphicsScene;

public class PointTool extends AbstractGeometryTool {

	public PointTool(final MenuManager mm) {
		super(mm);
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		final GraphicsScene scene = getScene();

		scene.addItem(createItem(getSceneLocation(e), GeometryType.Point));
	}

	@Override
	protected GeometryItem createNewItem(final Point2D loc) {
		//not used / called, but required to fullfill the interface
		throw new UnsupportedOperationException("Not used");
	}




}
