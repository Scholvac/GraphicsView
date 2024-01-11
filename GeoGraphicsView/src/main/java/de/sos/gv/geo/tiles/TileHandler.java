package de.sos.gv.geo.tiles;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
		public void propertyChange(final PropertyChangeEvent evt) {
			mTileUpdateRequired = true;
		}
	};
	private final ITileFactory				mFactory;
	private final Map<String, TileItem> 	mActiveTiles = new HashMap<>();

	public TileHandler(final ITileFactory factory) {
		mFactory = factory;
		assert factory != null;
	}

	@Override
	public void install(final GraphicsView view) {
		view.addPaintListener(this);
		view.getProperty(GraphicsView.PROP_VIEW_CENTER_X).addPropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_CENTER_Y).addPropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_SCALE_X).addPropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_SCALE_Y).addPropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_WIDTH).addPropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_HEIGHT).addPropertyChangeListener(mTileUpdateListener);

		mActiveTiles.clear();//if added a second time
		mTileUpdateRequired = true;
	}

	@Override
	public void uninstall(final GraphicsView view) {
		view.getProperty(GraphicsView.PROP_VIEW_CENTER_X).removePropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_CENTER_Y).removePropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_SCALE_X).removePropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_SCALE_Y).removePropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_WIDTH).removePropertyChangeListener(mTileUpdateListener);
		view.getProperty(GraphicsView.PROP_VIEW_HEIGHT).removePropertyChangeListener(mTileUpdateListener);
		view.removePaintListener(this);

		final GraphicsScene scene = view.getScene();
		_tilesToRemove.addAll(mActiveTiles.keySet());
		for (final String key : _tilesToRemove)
			removeTile(scene, key);

		mTileUpdateRequired = true;
	}

	private final LatLonPoint			_ll = new LatLonPoint();
	private final LatLonPoint			_ur = new LatLonPoint();
	private final LatLonBox				_viewBB = new LatLonBox();
	private final LinkedList<int[]>		_tilesToAdd = new LinkedList<>();
	private final HashSet<String>		_tilesToRemove = new HashSet<>();
	/** The milliseconds to wait for all (active) tiles to be completed. If == 0 no waiting at all, if < 0; wait without timeout.
	 * Default is == 0, e.g. no waiting at all*/
	private	long						mTimeToWaitForAllTiles = 0;
	private boolean 					mLoadingComplete; //Whether all tiles have been loaded or not (invalid if #isWaitForAllTilesEnabled() == false)

	@Override
	public void prePaint(final Graphics2D graphics, final IDrawContext context) {
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

			final int[][] requiredTiles = mFactory.getRequiredTileInfos(_viewBB, view.getImageWidth(), view.getImageHeight());
			_tilesToRemove.addAll(mActiveTiles.keySet());
			if (requiredTiles != null) {
				for (int i = 0; i < requiredTiles.length; i++) {
					final int[] tileArray = requiredTiles[i];
					final String tileId = TileInfo.getUniqueIdentifier(tileArray);
					if (_tilesToRemove.remove(tileId) == false)
						_tilesToAdd.add(tileArray);
				}
				if (_tilesToRemove.isEmpty() == false) {
					for (final String id : _tilesToRemove)
						removeTile(scene, id);
					_tilesToRemove.clear();
				}
				while(_tilesToAdd.isEmpty() == false) {
					addTile(scene, _tilesToAdd.removeFirst());
				}
			}
			waitForAllActiveTiles();
			mTileUpdateRequired = false;
		}
	}

	private void waitForAllActiveTiles() {
		if (mTimeToWaitForAllTiles == 0)
			return ;
		try {
			mLoadingComplete = false;
			final CompletableFuture[] futures = mActiveTiles.values().stream().map(TileItem::getImageFuture).toArray(CompletableFuture[]::new);
			if (mTimeToWaitForAllTiles < 0)
				CompletableFuture.allOf(futures).get();
			else
				CompletableFuture.allOf(futures).get(mTimeToWaitForAllTiles, TimeUnit.MILLISECONDS);
			for (final CompletableFuture f : futures) {
				f.get();
			}
			mLoadingComplete = true;
		}catch(final Exception e) {
			e.printStackTrace();
			mLoadingComplete = false;
		}

	}

	private void addTile(final GraphicsScene scene, final int[] tileInfo) {
		final TileItem item = mFactory.load(tileInfo);
		mActiveTiles.put(item.getHash(), item);
		scene.addItem(item);
	}

	private void removeTile(final GraphicsScene scene, final String itemHash) {
		final TileItem item = mActiveTiles.remove(itemHash);
		if (item != null) {
			scene.removeItem(item);
			mFactory.release(item);
		}
	}

	@Override
	public void postPaint(final Graphics2D graphics, final IDrawContext context) {} //not used

	@Override
	public void notifySceneCleared() {
		mActiveTiles.values().forEach(ti -> mFactory.release(ti));
		mActiveTiles.clear();
		mTileUpdateRequired = true;
	}

	/**
	 * Enable or disable waiting for all tiles to be completly loaded.
	 * If enabled, the tile handler will wait without timeout that all tiles have been loaded or an exception has been thrown.
	 * <br>
	 * @see #waitForAllTiles(long, TimeUnit)
	 * @param doWait
	 */
	public void waitForAllTiles(final boolean doWait) {
		if (doWait)
			waitForAllTiles(-1, TimeUnit.MILLISECONDS);
		else
			waitForAllTiles(0, TimeUnit.MILLISECONDS);
	}
	/**
	 * Wait for a certain amount of time, that all tiles have been loaded.
	 * <br>
	 * if not all tiles have been loaded in time, a timeout exception is logged and the rendering process is continued.
	 *
	 * @param time The time in <code>unit</code> to wait until all tiles have been loaded. Set to <ul>
	 * <li> > 0: the time in given unit
	 * <li> 0:  do not wait at all, e.g. complete asynchron loading
	 * <li> < 0: wait without timeout
	 * @param unit The unit of time
	 */
	public void waitForAllTiles(final long time, final TimeUnit unit) {
		mTimeToWaitForAllTiles = unit.convert(time, TimeUnit.MILLISECONDS);
	}
	public boolean isWaitForAllTilesEnabled() {
		return mTimeToWaitForAllTiles != 0;
	}

	public boolean isComplete() {
		return !isWaitForAllTilesEnabled() || mLoadingComplete;
	}
}
