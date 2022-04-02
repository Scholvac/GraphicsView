package de.sos.gv.ge.tools;

import java.awt.event.MouseEvent;

import de.sos.gv.ge.callbacks.GeometryInteractions;
import de.sos.gvc.handler.SelectionHandler;

public class SelectionTool extends AbstractTool {

	private SelectionHandler mSelectionHandler;
	private GeometryInteractions mCallback;

	public SelectionTool() {
		mSelectionHandler = new SelectionHandler();
		mCallback = new GeometryInteractions();
		mSelectionHandler.addMoveCallback(mCallback);
		mSelectionHandler.addScaleCallback(mCallback);
		mSelectionHandler.addRotationCallback(mCallback);
	}

	@Override
	protected void activate() {
		getView().addHandler(mSelectionHandler);
	}
	@Override
	protected void deactivate() {
		getView().removeHandler(mSelectionHandler);
	}
	@Override
	public void mouseClicked(final MouseEvent e) {
		// TODO Auto-generated method stub
		super.mouseClicked(e);
	}
}
