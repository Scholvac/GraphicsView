package de.sos.gv.ge.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import de.sos.gv.ge.items.GeometryItem;
import de.sos.gvc.GraphicsScene;

public class RemoveGeometryAction extends AbstractAction {


	private GeometryItem mGeometry;
	private GraphicsScene mScene;

	public RemoveGeometryAction(final GeometryItem geometry, final GraphicsScene scene) {
		super("Remove Geometry", new ImageIcon(RemoveGeometryAction.class.getClassLoader().getResource("icons/Delete.png")));
		mScene = scene;
		mGeometry = geometry;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		mScene.removeItem(mGeometry);
	}

}
