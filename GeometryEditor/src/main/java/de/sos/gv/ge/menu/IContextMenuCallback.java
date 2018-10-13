package de.sos.gv.ge.menu;

import java.awt.PopupMenu;

import javax.swing.JPopupMenu;

import de.sos.gv.ge.items.ContourPointItem;
import de.sos.gv.ge.model.geom.IGeometry;

public interface IContextMenuCallback {

	void onGeometryContext(final IGeometry geometry, JPopupMenu menu);
	void onContourPointContextMenu(final ContourPointItem item, final IGeometry geometry, JPopupMenu menu);
}
