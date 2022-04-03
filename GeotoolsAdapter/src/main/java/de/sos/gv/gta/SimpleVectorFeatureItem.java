package de.sos.gv.gta;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.param.Parameter;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.styles.DrawableStyle;

public class SimpleVectorFeatureItem extends GraphicsItem
{
	private List<Parameter<?>>			mParameters;

	public SimpleVectorFeatureItem() {
	}

	public SimpleVectorFeatureItem(final Shape shape, final ParameterContext propertyContext) {
		super(shape, propertyContext);
	}

	public SimpleVectorFeatureItem(final Shape shape) {
		super(shape);
	}

	/** forward styles to children */
	@Override
	public void setStyle(final DrawableStyle style) {
		super.setStyle(style);
		getChildren().forEach(child -> child.setStyle(style));
	}

	public boolean addParameter(final Parameter<?> param) {
		if (param == null)
			return false;
		if (getParameter(param.getName()) != null)
			return false; //do not add twice
		if (mParameters == null)
			mParameters = new ArrayList<>();
		return mParameters.add(param);
	}

	public boolean removeParameter(final Parameter<?> param) {
		if (param == null)
			return false;
		if (mParameters == null)
			return false;
		if (mParameters.remove(param)) {
			if (mParameters.isEmpty())
				mParameters = null;
			return true;
		}
		return false;
	}

	public <T> Parameter<T> getParameter(final String name){
		if (mParameters == null)
			return null;
		final Parameter<?> param = mParameters.stream()
				.filter(p -> p.getName().equals(name))
				.findFirst()
				.orElse(null);
		return (Parameter<T>)param;
	}

	public <T> boolean addParameter(final String localName, final T val) {
		return addParameter(new Parameter<>(localName, null, true, val));
	}
}

