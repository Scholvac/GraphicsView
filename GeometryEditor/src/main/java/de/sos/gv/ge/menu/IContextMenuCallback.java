package de.sos.gv.ge.menu;

import javax.swing.JPopupMenu;

import de.sos.gv.ge.items.ContourPointItem;
import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.model.geom.IGeometry;
import de.sos.gvc.GraphicsScene;

public interface IContextMenuCallback {

	void onGeometryContext(final GeometryItem geometry, final GraphicsScene scene, JPopupMenu menu);
	void onContourPointContextMenu(final ContourPointItem item, final IGeometry geometry, JPopupMenu menu);
}
