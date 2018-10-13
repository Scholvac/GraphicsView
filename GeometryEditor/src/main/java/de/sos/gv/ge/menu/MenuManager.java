package de.sos.gv.ge.menu;

import java.util.ArrayList;

import javax.swing.JPopupMenu;

import de.sos.gv.ge.items.ContourPointItem;
import de.sos.gv.ge.model.geom.IGeometry;

public class MenuManager {
	
	private ArrayList<IContextMenuCallback> 	mCallbacks = new ArrayList<>();
	
	public boolean registerCallback(IContextMenuCallback callback) {
		return mCallbacks.add(callback);
	}
	public boolean removeCallback(IContextMenuCallback callback) {
		return mCallbacks.remove(callback);
	}
	
	
	public void fillContourItemMenu(ContourPointItem item, IGeometry geometry, JPopupMenu menu) {
		for (IContextMenuCallback cb : mCallbacks) {
			cb.onContourPointContextMenu(item, geometry, menu);
		}
	}
	
	
	
}
