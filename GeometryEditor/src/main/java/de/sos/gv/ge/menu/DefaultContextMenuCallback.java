package de.sos.gv.ge.menu;

import javax.swing.JPopupMenu;

import de.sos.gv.ge.actions.RemovePointAction;
import de.sos.gv.ge.items.ContourPointItem;
import de.sos.gv.ge.model.geom.IGeometry;

public class DefaultContextMenuCallback implements IContextMenuCallback {

	@Override
	public void onGeometryContext(IGeometry geometry, JPopupMenu menu) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onContourPointContextMenu(ContourPointItem item, IGeometry geometry, JPopupMenu menu) {
		menu.add(new RemovePointAction(geometry, item));
	}

	

}
