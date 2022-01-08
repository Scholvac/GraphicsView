package de.sos.gvc.param;


/**
 * 
 * @author scholvac
 *
 */
public class Parameter<T> extends AbstractParameter<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8405591448562089600L;
	
	
	private final String mName;
	private final String mDescription;
	private final boolean mEditable;

	protected T mValue;

	
	public Parameter(final String name) {
		this(name, name);
	}
	public Parameter(final String name, final String description) {
		this(name, description, true);
	}

	public Parameter(String name, String description, boolean editable) {
		this(name, description, editable, null);
	}

	public Parameter(final String name, final String description, final boolean editable, final T value) {
		super();
		mName = name;
		mDescription = description;
		mEditable = editable;

		mValue = value;
	}

	public Parameter(Parameter<T> _copy) {
		super(_copy);
		mName = _copy.mName;
		mDescription = _copy.mDescription;
		mEditable = _copy.mEditable;
		mValue = _copy.mValue;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public String getDescription() {
		return mDescription;
	}

	@Override
	public Class<?> getType() {
		return mValue != null ? mValue.getClass() : null;
	}

	@Override
	public T get() {
		return mValue;
	}

	@Override
	public void set(T value) {
		if (value != mValue) {
			if (value == null) {
				Object oldValue = mValue;
				mValue = value;
				firePropertyChange(oldValue, mValue);
			}
			else if (value.equals(mValue) == false) { //also need to check for equals, otherwise primitive values (double, float, ...) will always be encapsuled into other Wrapper instances
				Object oldValue = mValue;
				mValue = value;
				firePropertyChange(oldValue, mValue);
			}
		}	
	}

	@Override
	public boolean isEditable() {
		return mEditable;
	}

	@Override
	public String getCategory() {
		return null;
	}

	@Override
	public IParameter copy() {
		return new Parameter(this);
	}

	 @Override
	public void firePropertyChange(Object oldValue, Object newValue) {
        listeners.firePropertyChange(mName, oldValue, newValue);
    }

}
