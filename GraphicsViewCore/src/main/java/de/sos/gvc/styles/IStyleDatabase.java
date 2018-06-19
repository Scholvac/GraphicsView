package de.sos.gvc.styles;

import java.io.InputStream;
import java.io.OutputStream;

public interface IStyleDatabase {
	default DrawableStyle addStyle(DrawableStyle style) {
		return addStyle(style.getName(), style); //nullpointer is wanted, would be thrown anyway if the style is null
	}
	/**
	 * adds the given style under the given name. if there is already a style with the name available, 
	 * the old style will be replaced (and returned)
	 * @param name
	 * @param style
	 * @return old style which was registered under the same name or null, if no style with the same name was available
	 */
	public DrawableStyle addStyle(String name, DrawableStyle style);
	public DrawableStyle removeStyle(String name);
	
	default boolean hasStyle(String name) {
		return getStyle(name) != null;
	}
	public DrawableStyle getStyle(String name);
	
	public DrawableStyle loadStyleFromXML(String xml);
	public String writeStyleToXML(DrawableStyle style);
	
	public void loadStyles(InputStream stream);
	public void writeStyles(OutputStream stream);
}
