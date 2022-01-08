package de.sos.gv.ge.menu;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;

import de.sos.gv.ge.items.ContourPointItem;
import de.sos.gv.ge.model.geom.IGeometry;

public class MenuManager {
	
	private List<IContextMenuCallback> 	mCallbacks = new ArrayList<>();
	
	public boolean registerCallback(IContextMenuCallback callback) {
		if (mCallbacks == null) mCallbacks = new ArrayList<IContextMenuCallback>();
		return mCallbacks.add(callback);
	}
	public boolean removeCallback(IContextMenuCallback callback) {
		if (mCallbacks == null) return true;
		return mCallbacks.remove(callback);
	}
	
	
	public void fillContourItemMenu(ContourPointItem item, IGeometry geometry, JPopupMenu menu) {
		if (mCallbacks == null) return ;
		for (IContextMenuCallback cb : mCallbacks) {
			cb.onContourPointContextMenu(item, geometry, menu);
		}
	}
	
	
	
}
