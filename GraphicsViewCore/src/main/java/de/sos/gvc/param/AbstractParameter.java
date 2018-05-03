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
    public AbstractParameter(AbstractParameter _copy){
    	if (_copy.mChildProperties != null){
    		mChildProperties = new ArrayList<>();
    		for (Object _p : _copy.mChildProperties)
    			mChildProperties.add(((IParameter<?>)_p).copy());
    	}
    }
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(listener);
        Collection<IParameter> subProperties = getSubProperties();
        if (subProperties != null) {
        	for (IParameter sub : subProperties)
        		sub.addPropertyChangeListener(listener);
        }
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
        Collection<IParameter> subProperties = getSubProperties();
        if (subProperties != null) {
        	for (IParameter sub : subProperties)
                sub.removePropertyChangeListener(listener);
        }
    }

    public void firePropertyChange(Object oldValue, Object newValue) {
        listeners.firePropertyChange(getName(), oldValue, newValue);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        listeners = new PropertyChangeSupport(this);
    }

    @Override
    public IParameter getParentProperty() {
    	return mParentProperty;
    }
    public void setParentProperty(IParameter prop){
    	if (mParentProperty != null && mParentProperty != prop)
    		((AbstractParameter)mParentProperty)._removeChild(this);
    	mParentProperty = prop;
    	if (mParentProperty != null && mParentProperty instanceof AbstractParameter)
    		((AbstractParameter)mParentProperty)._addChild(this);
    }
    
    public void addChild(IParameter prop){
    	if (prop instanceof AbstractParameter){
    		((AbstractParameter) prop).setParentProperty(this);
    		_addChild((AbstractParameter)prop);
    	}else
    		mChildProperties.add(prop);
    }

	public void removeChild(IParameter obj) {
		if (mChildProperties != null)
			mChildProperties.remove(obj);
	}

    /**
     * removes a child, without notifing the child about the changed parent 
     * normally this method is called by the child when a new parent is set
     * @param child
     */
    private void _removeChild(AbstractParameter child) {
		if (mChildProperties != null)
			mChildProperties.remove(child);
	}

    /**
     * adds a new child to the internal list (creates the list if required) but only if the child is not yet part of the child list (e.g.
     * the child will be added only once). 
     * this method does not change the parent of the child, since it is normally called from the setParentProperty of the child 
     * @param child
     */
	private void _addChild(AbstractParameter child) {
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
