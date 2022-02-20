package de.sos.gvc;

import java.awt.geom.Rectangle2D;
import java.util.List;

import de.sos.gvc.GraphicsScene.IItemFilter;

/**
 * Interface that handles the storage of items in the scene
 * @author scholvac
 *
 */
public interface IItemStorage {

	public boolean contains(GraphicsItem item);
	public boolean addItem(GraphicsItem item);
	public boolean removeItem(GraphicsItem item);

	/** Returns an possible unmodifiable list of all registered (root) GraphicsItems. */
	public List<GraphicsItem> getAllItems();
	public List<GraphicsItem> getItems(Rectangle2D rect, IItemFilter filter);
}
