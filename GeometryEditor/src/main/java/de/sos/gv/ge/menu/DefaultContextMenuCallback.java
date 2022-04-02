package de.sos.gv.ge.menu;

import javax.swing.JPopupMenu;

import de.sos.gv.ge.actions.RemoveGeometryAction;
import de.sos.gv.ge.actions.RemovePointAction;
import de.sos.gv.ge.items.ContourPointItem;
import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.model.geom.IGeometry;
import de.sos.gvc.GraphicsScene;

public class DefaultContextMenuCallback implements IContextMenuCallback {

	@Override
	public void onGeometryContext(final GeometryItem geometry, final GraphicsScene scene, final JPopupMenu menu) {
		menu.add(new RemoveGeometryAction(geometry, scene));
	}

	@Override
	public void onContourPointContextMenu(final ContourPointItem item, final IGeometry geometry, final JPopupMenu menu) {
		menu.add(new RemovePointAction(geometry, item));
	}

}
