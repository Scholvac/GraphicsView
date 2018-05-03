package de.sos.gvc;

/**
 * 
 * @author scholvac
 *
 */
public interface IDrawContext {
	/** returns the current active / drawing view
	 * this may be used to react on specific view properties such as scale or center of the view
	 * @return
	 */
	GraphicsView 		getView();
	
	default double getScaleX() { return getView().getScaleX();}
	default double getScaleY() { return getView().getScaleY();}
	default double getScale() { return Math.max(getView().getScaleX(), getView().getScaleY()); }
}
