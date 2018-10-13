package de.sos.gv.ge.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import de.sos.gv.ge.items.ContourPointItem;
import de.sos.gv.ge.model.geom.IGeometry;

public class RemovePointAction extends AbstractAction {

	
	private IGeometry mGeometry;
	private ContourPointItem mItem;

	public RemovePointAction(IGeometry geometry, ContourPointItem item) {
		super("Remove", new ImageIcon(RemovePointAction.class.getClassLoader().getResource("icons/Delete.png")));
		mItem = item;
		mGeometry = geometry;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		mGeometry.removePoint(mItem.getIndex());
	}

}
