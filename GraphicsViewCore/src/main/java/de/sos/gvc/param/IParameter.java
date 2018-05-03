package de.sos.gvc.param;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.List;

/**
 * 
 * @author scholvac
 *
 */
public interface IParameter<T> extends Serializable, Cloneable {

    public String getName();

    public String getDescription();

    public Class<?> getType();

    public T get();

    public void set(T value);

    public boolean isEditable();

    public String getCategory();
    
    public void addPropertyChangeListener(PropertyChangeListener listener);

    public void removePropertyChangeListener(PropertyChangeListener listener);

    public IParameter getParentProperty();

    public List<IParameter> getSubProperties();

    /**
     * creates a deep copy of the property
     * @return
     */
	public IParameter copy();
}