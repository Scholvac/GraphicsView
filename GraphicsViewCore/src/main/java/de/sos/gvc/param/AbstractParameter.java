package de.sos.gvc.param;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * AbstractProperty. <br>
 * @author scholvac
 */
public abstract class AbstractParameter<T> implements IParameter<T> {

	// PropertyChangeListeners are not serialized.
	protected transient PropertyChangeSupport 	listeners = new PropertyChangeSupport(this);

	private IParameter 							mParentProperty = null;
	private List<IParameter>						mChildProperties = null;

	public AbstractParameter() {
	}
	public AbstractParameter(final AbstractParameter _copy){
		if (_copy.mChildProperties != null){
			mChildProperties = new ArrayList<>();
			for (final Object _p : _copy.mChildProperties)
				mChildProperties.add(((IParameter<?>)_p).copy());
		}
	}
	@Override
	public IDisposeable addPropertyChangeListener(final PropertyChangeListener listener) {
		listeners.addPropertyChangeListener(listener);
		final Collection<IParameter> subProperties = getSubProperties();
		if (subProperties != null) {
			for (final IParameter sub : subProperties)
				sub.addPropertyChangeListener(listener);
		}
		return () -> removePropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(final PropertyChangeListener listener) {
		listeners.removePropertyChangeListener(listener);
		final Collection<IParameter> subProperties = getSubProperties();
		if (subProperties != null) {
			for (final IParameter sub : subProperties)
				sub.removePropertyChangeListener(listener);
		}
	}

	public void firePropertyChange(final Object oldValue, final Object newValue) {
		listeners.firePropertyChange(getName(), oldValue, newValue);
	}

	private void readObject(final java.io.ObjectInputStream in) throws IOException,
	ClassNotFoundException {
		in.defaultReadObject();
		listeners = new PropertyChangeSupport(this);
	}

	@Override
	public IParameter getParentProperty() {
		return mParentProperty;
	}
	public void setParentProperty(final IParameter prop){
		if (mParentProperty != null && mParentProperty != prop)
			((AbstractParameter)mParentProperty)._removeChild(this);
		mParentProperty = prop;
		if (mParentProperty instanceof AbstractParameter)
			((AbstractParameter)mParentProperty)._addChild(this);
	}

	public void addChild(final IParameter prop){
		if (prop instanceof AbstractParameter){
			((AbstractParameter) prop).setParentProperty(this);
			_addChild((AbstractParameter)prop);
		}else
			mChildProperties.add(prop);
	}

	public void removeChild(final IParameter obj) {
		if (mChildProperties != null)
			mChildProperties.remove(obj);
	}

	/**
	 * removes a child, without notifing the child about the changed parent
	 * normally this method is called by the child when a new parent is set
	 * @param child
	 */
	private void _removeChild(final AbstractParameter child) {
		if (mChildProperties != null)
			mChildProperties.remove(child);
	}

	/**
	 * adds a new child to the internal list (creates the list if required) but only if the child is not yet part of the child list (e.g.
	 * the child will be added only once).
	 * this method does not change the parent of the child, since it is normally called from the setParentProperty of the child
	 * @param child
	 */
	private void _addChild(final AbstractParameter child) {
		if (mChildProperties == null)
			mChildProperties = new ArrayList<>();
		if (!mChildProperties.contains(child))
			mChildProperties.add(child);
	}


	@Override
	public List<IParameter> getSubProperties() {
		return mChildProperties;
	}

	@Override
	public String toString() {
		return "Parameter ["+getName()+" = " + get() + "]";
	}
}
