package de.sos.gv.geo.tiles;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.IGraphicsViewHandler;
import de.sos.gvc.IPaintListener;

public class TileHandler implements IGraphicsViewHandler, IPaintListener {


	/** if set to true, we do update the tiles within the scene before we repaint the window*/
	private boolean							mTileUpdateRequired = true;
	private PropertyChangeListener			mTileUpdateListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			mTileUpdateRequired = true;
		}
	};
	private final TileFactory				mFactory;
	private final Map<String, TileItem> 	mActiveTiles = new HashMap<>();

	public TileHandler(TileFactory factory) {
		mFactory = factory;
		assert(factory != null);
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

	private final LatLonPoint			_ll = new LatLonPoint();
	private final LatLonPoint			_ur = new LatLonPoint();
	private final LatLonBox				_viewBB = new LatLonBox();
	private final Rectangle				_viewBounds = new Rectangle();
	private final LinkedList<int[]>		_tilesToAdd = new LinkedList<>();
	private final HashSet<String>		_tilesToRemove = new HashSet<>();
	@Override
	public void prePaint(Graphics2D graphics, IDrawContext context) {
		if (mTileUpdateRequired) {
			final GraphicsView view = context.getView();
			final GraphicsScene	scene = view.getScene();

			final Rectangle2D sceneRect = view.getVisibleSceneRect();
			GeoUtils.getLatLon(sceneRect.getMinX(), sceneRect.getMinY(), _ll);
			GeoUtils.getLatLon(sceneRect.getMaxX(), sceneRect.getMaxY(), _ur);
			if (sceneRect.getWidth() > GeoUtils.EARTH_CIRCUMFERENCE) {
				_ll.setLongitude(-179.999);
				_ur.setLongitude(179.999);
			}
			_viewBB.setAndCorrect(_ll, _ur);
			view.getBounds(_viewBounds);

			int[][] requiredTiles = mFactory.getRequiredTileInfos(_viewBB, _viewBounds);
			_tilesToRemove.addAll(mActiveTiles.keySet());
			if (requiredTiles != null) {
				for (int i = 0; i < requiredTiles.length; i++) {
					final int[] tileArray = requiredTiles[i];
					final String tileId = TileInfo.getUniqueIdentifier(tileArray);
					if (_tilesToRemove.remove(tileId) == false)
						_tilesToAdd.add(tileArray);
				}
				if (_tilesToRemove.isEmpty() == false) {
					for (String id : _tilesToRemove)
						removeTile(scene, id);
					_tilesToRemove.clear();
				}
				while(_tilesToAdd.isEmpty() == false) {
					addTile(scene, _tilesToAdd.removeFirst());
				}
			}
			mFactory.check(mActiveTiles.values());
			mTileUpdateRequired = false;
		}
	}

	private void addTile(GraphicsScene scene, int[] tileInfo) {
		TileItem item = mFactory.load(tileInfo);
		mActiveTiles.put(item.getHash(), item);
		scene.addItem(item);
	}

	private void removeTile(GraphicsScene scene, String itemHash) {
		final TileItem item = mActiveTiles.remove(itemHash);
		if (item != null) {
			scene.removeItem(item);
			mFactory.release(item);
		}
	}

	@Override
	public void postPaint(Graphics2D graphics, IDrawContext context) {} //not used

	@Override
	public void notifySceneCleared() {
		mActiveTiles.values().forEach(ti -> mFactory.release(ti));
		mActiveTiles.clear();
		mTileUpdateRequired = true;
	}
}
