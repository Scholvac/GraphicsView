package de.sos.gvc;

/**
 *
 * @author scholvac
 *
 */
public interface IGraphicsViewHandler {

	public void install(IGraphicsView view);
	public void uninstall(IGraphicsView view);

	/** Notification from scene that the content has been cleaned.
	 * @note this notification is redundant: The scene removes each
	 * 		item individually and thus sends out an PropertyChangeEvent
	 * 		however. using this notification, the events do not need to
	 * 		be observed (for performance reasons).
	 */
	public default void notifySceneCleared() { }
}
