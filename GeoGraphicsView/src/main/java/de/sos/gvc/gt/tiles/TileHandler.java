package de.sos.gvc.gt.tiles;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.IGraphicsViewHandler;
import de.sos.gvc.IPaintListener;
import de.sos.gvc.gt.GeoUtils;
import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.proj.Planet;
import de.sos.gvc.log.GVLog;


/**
 * 
 * @author scholvac
 *
 */
public class TileHandler implements IPaintListener, IGraphicsViewHandler{

	
	@SuppressWarnings("rawtypes")
	private ITileFactory						mTileFactory;
	
	/** if set to true, we do update the tiles within the scene before we repaint the window*/
	private boolean							mTileUpdateRequired = true;
	private PropertyChangeListener			mTileUpdateListener = new PropertyChangeListener() {		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			mTileUpdateRequired = true;
		}
	};
	
	@SuppressWarnings("rawtypes")
	private HashMap<Integer, LazyTileItem>		mActiveTiles = new HashMap<>();
	
	public TileHandler(ITileFactory factory) {
		mTileFactory = factory;
	}
	
	
	@Override
	public void install(GraphicsView view) {
		view.addPaintListener(this);
		view.getProperty(GraphicsView.PROP_VIEW_CENTER_X).addPropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_CENTER_Y).addPropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_SCALE_X).addPropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_SCALE_Y).addPropertyChangeListener(mTileUpdateListener);
		
	}

	@Override
	public void uninstall(GraphicsView view) {
		view.getProperty(GraphicsView.PROP_VIEW_CENTER_X).removePropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_CENTER_Y).removePropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_SCALE_X).removePropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_SCALE_Y).removePropertyChangeListener(mTileUpdateListener);
		view.removePaintListener(this);
	}
	
	
	@Override
	public void prePaint(Graphics2D graphics, IDrawContext context) {
		updateTiles(context.getView());
	}

	@Override
	public void postPaint(Graphics2D graphics, IDrawContext context) {
		
	}



	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateTiles(GraphicsView view) {
		if (mTileFactory != null) {
			//calculate observed area
			Rectangle2D rect = view.getVisibleSceneRect();
//			System.out.println("Visible Scene Rect: = " + rect);
			GraphicsScene scene = view.getScene();
			LatLonPoint ll = GeoUtils.getLatLon(rect.getMinX(), rect.getMinY());
			LatLonPoint ur = GeoUtils.getLatLon(rect.getMaxX(), rect.getMaxY());
			if (rect.getWidth() > Planet.wgs84_earthEquatorialCircumferenceMeters_D) {
				ll.setLongitude(-179.999);
				ur.setLongitude(179.999);
			}
			LatLonBoundingBox geoBB = new LatLonBoundingBox(ll, ur);
			Collection<ITileDescription> requiredTiles = mTileFactory.getTileDescriptions(geoBB, view.getBounds());
			HashSet<Integer> toRemove = new HashSet<>(mActiveTiles.keySet());
			for (ITileDescription desc : requiredTiles) {
				int id = desc.getIdentifier();
				boolean exists = toRemove.remove(id);
				if (exists == false) {
					LazyTileItem newTile = mTileFactory.createTileItem(desc);
					mActiveTiles.put(id, newTile);
					scene.addItem(newTile);
				}
			}
			//remove all tiles that are no longer required from both lists (mActiveTiles and scene)
			for (Integer id : toRemove) {
				
				LazyTileItem t = mActiveTiles.remove(id);
				if (t != null) {
					scene.removeItem(t);
					mTileFactory.unloadTileItem(t);
				}
			}
		}else {
			GVLog.debug("Missing Tile Factory");
		}
		mTileUpdateRequired = false;
	}


	public void setFactory(ITileFactory value) {
		mTileFactory = value;
	}

}
