package de.sos.gv.ge.menu;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;

import de.sos.gv.ge.items.ContourPointItem;
import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.model.geom.IGeometry;
import de.sos.gvc.GraphicsScene;

public class MenuManager {

	private List<IContextMenuCallback> 	mCallbacks = new ArrayList<>();

	public boolean registerCallback(final IContextMenuCallback callback) {
		if (mCallbacks == null) mCallbacks = new ArrayList<>();
		return mCallbacks.add(callback);
	}
	public boolean removeCallback(final IContextMenuCallback callback) {
		if (mCallbacks == null) return true;
		return mCallbacks.remove(callback);
	}


	public void fillContourItemMenu(final ContourPointItem item, final IGeometry geometry, final JPopupMenu menu) {
		if (mCallbacks == null) return ;
		for (final IContextMenuCallback cb : mCallbacks) {
			cb.onContourPointContextMenu(item, geometry, menu);
		}
	}

	public void fillGeometryItemMenu(final GeometryItem geometry, final GraphicsScene scene, final JPopupMenu menu) {
		if (mCallbacks == null) return ;
		for (final IContextMenuCallback cb : mCallbacks) {
			cb.onGeometryContext(geometry, scene, menu);
		}
	}



}
