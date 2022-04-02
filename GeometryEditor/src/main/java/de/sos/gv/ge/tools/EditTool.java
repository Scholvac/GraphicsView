package de.sos.gv.ge.tools;

import java.awt.event.MouseEvent;

import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.items.GeometryItem.GeometryItemMode;
import de.sos.gvc.GraphicsItem;

public class EditTool extends AbstractTool {

	private GeometryItem		mSelectedItem;

	public EditTool() {
	}

	@Override
	protected void activate() {
		mSelectedItem = null;
	}
	@Override
	protected void deactivate() {
		if (mSelectedItem != null) {
			mSelectedItem.setGeometryItemMode(GeometryItemMode.NORMAL);
		}
		mSelectedItem = null;
	}


	@Override
	public void mouseClicked(final MouseEvent e) {
		final GraphicsItem selectedItem = de.sos.gvc.Utils.getBestFit(getView(), e.getPoint(), 5, GraphicsItem::isSelectable, item -> item instanceof GeometryItem);
		if (selectedItem == null) {
			if (mSelectedItem != null) {
				mSelectedItem.setGeometryItemMode(GeometryItemMode.NORMAL);
				mSelectedItem = null;
			}
		} else if (mSelectedItem == null) {
			mSelectedItem = (GeometryItem) selectedItem;
			mSelectedItem.setGeometryItemMode(GeometryItemMode.EDIT);
		}
	}
}
