package de.sos.gv.ge.model.geom;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;

public abstract class AbstractGeometry implements IGeometry {

	private PropertyChangeSupport	mPCS = null;

	@Override
	public void addListener(final PropertyChangeListener listener) {
		if (mPCS == null) mPCS = new PropertyChangeSupport(this);
		mPCS.addPropertyChangeListener(listener);
	}

	@Override
	public void removeListener(final PropertyChangeListener listener) {
		if (mPCS != null) {
			mPCS.removePropertyChangeListener(listener);
			if (!mPCS.hasListeners(null))
				mPCS = null;
		}
	}

	protected void firePointChanged(final Point2D oldPoint, final Point2D newPoint) {
		if (mPCS != null)
			mPCS.firePropertyChange(POINT_CHANGE_EVENT, oldPoint, newPoint);
	}
	protected void firePointsChanged(final Collection<Point2D> oldPoints, final Collection<Point2D> newPoints) {
		if (mPCS != null)
			mPCS.firePropertyChange(POINTS_CHANGE_EVENT, oldPoints, newPoints);
	}

}
