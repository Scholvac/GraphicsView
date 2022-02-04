package de.sos.gvc.param;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author scholvac
 *
 */
public class ParameterContext {
	private Map<String, IParameter> 					mProperties = new HashMap<>();
	private ArrayList<PropertyChangeListener> 			mListener = new ArrayList<>();

	public IParameter getProperty(String name) {
		return getProperty(name, null);
	}

	public <T> IParameter<T> getProperty(String name, T defaultValue) {
		IParameter property = mProperties.get(name);

		if(property == null) {

			property = new Parameter<>(name, null);
			for (PropertyChangeListener pcl : mListener) {
				property.addPropertyChangeListener(pcl);
			}
			property.set(defaultValue);
			mProperties.put(name, property);
		}

		return property;

	}

	@SuppressWarnings("unchecked")
	public <T> T getValue(String name, T defaultValue) {

		IParameter property = getProperty(name, defaultValue);
		return property.get() == null ? defaultValue : (T) property.get();
	}

	public boolean hasProperty(String name) {
		return mProperties.get(name) != null;
	}

	@SuppressWarnings("unchecked")
	public <T> T getValue(String name) {
		return (T)getProperty(name, null).get();
	}

	public void setValue(String name, Object value) {
		getProperty(name, null).set(value);
	}

	public Collection<IParameter> getAllProperties() {
		return Collections.unmodifiableCollection(mProperties.values());
	}

	public void registerListener(PropertyChangeListener listener) {
		if ((listener == null) || mListener.contains(listener))
			return ;
		for (IParameter p : mProperties.values()){
			p.addPropertyChangeListener(listener);
		}
		mListener.add(listener);
	}

	public void removeListener(PropertyChangeListener listener) {
		if (listener == null) return ;
		mListener.remove(listener);
		for (IParameter p : mProperties.values())
			p.removePropertyChangeListener(listener);
	}


}
