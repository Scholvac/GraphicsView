package de.sos.gvc.gt.tiles;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import de.sos.gvc.log.GVLog;


/**
 * 
 * @author scholvac
 *
 */
public class TileFactory<DESC extends ITileDescription> {
	
	private ExecutorService		mExecutorService;
	private ITileFactory<DESC> 	mFactory;
	
	private BufferedImage		mLoadingImage;
	private BufferedImage		mErrorImage;
	private UnloadWorker 		mTileUnloader;
	
	public TileFactory(ITileFactory<DESC> factory, int threadPoolSize) {
		mFactory = factory;
		try {
			mLoadingImage = ImageIO.read(getClass().getClassLoader().getResource("loading.png"));
			mErrorImage = ImageIO.read(getClass().getClassLoader().getResource("error.png"));
		} catch (IOException e) {
			e.printStackTrace();
			mLoadingImage = new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR);
		}
		mExecutorService = Executors.newFixedThreadPool(threadPoolSize, new ThreadFactory()
        {
            private int count = 0;

            @Override
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread(r, "TileDownloaderWorker-" + count++);
                t.setPriority(Thread.MIN_PRIORITY);
                t.setDaemon(true);
                return t;
            }
        });
		mTileUnloader = new UnloadWorker();
		mTileUnloader.start();
	}

	
	class TileWorker implements Runnable {
		private ITileLoader<DESC> 	loader;

		public TileWorker(ITileLoader<DESC> l) {
			loader = l;
		}
		@Override
		public void run() {
			try {
				//get the first (highest priority) tile from the queue
				LazyTileItem<DESC> tile = mTilesToLoad.poll();
				BufferedImage bimg = loader.getTileImage(tile.getDescription());
				if (bimg == null) {
					GVLog.warn("Failed to download Tile Image: " + tile.toString());
					tile.setImage(mErrorImage);
				}else
					tile.setImage(bimg);
			}catch(Exception | Error e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean 		mAlive = true;
	private ArrayBlockingQueue<LazyTileItem<DESC>>	mTilesToLoad = new ArrayBlockingQueue<>(500);
	private BlockingQueue<LazyTileItem<DESC>> 		mTilesToUnload = new ArrayBlockingQueue<>(500);
	
	class UnloadWorker extends Thread {
		public UnloadWorker() {
			setName("TileFactory-UnloadWorker");
			setPriority(MIN_PRIORITY);
			setDaemon(true);			
		}
		@Override
		public void run() {
			GVLog.debug("Start TileFactory-UnloadWorker");
			while(mAlive) {
				try {
					LazyTileItem<DESC> tile = mTilesToUnload.poll(1000, TimeUnit.MILLISECONDS);
					if (tile != null && tile.getImage() != mErrorImage && tile.getImage() != mLoadingImage) {
//						GVLog.debug("Unload Tile: " + tile.toString());
						mFactory.notifyParentUnloadedTile(tile);//.getDescription());
					}
				}catch(Exception | Error e) {
					e.printStackTrace();
				}
			}
			GVLog.debug("Finish TileFactory-UnloadWorker");
		}
	}
	
	
	public Collection<DESC> getTileDescriptions(LatLonBoundingBox area, Rectangle2D viewArea){
		return mFactory.getTileDescriptions(area, viewArea);
	}
	
	
	
	
	
	/*
	Creating a Tile uses the following sequenceflow asuming that a three level cache system is used, containing a 
	memory cache (that is registered to the TileFactory), that is backuped by an file system cache which again is backuped by an web cache aka. OSM/Google/WMS/...
	
	@startuml
	actor GeoView as View
	boundary TileFactory as TF
	entity LazyTile as LT
	participant ImageWorker as IW
	collections MemoryCache as MC
	database FileCache as FC
	database WebCache as WC
	entity Image as Img

	View -> TF : createTile(x,y,zoom)
	create LT
	TF -> LT : init<x,y,zoom, defaultImage>
	create IW
	TF -> IW : init<tile>
	TF ->> IW : run()
	IW ->> MC: loadImage(x,y,zoom)
	TF --> View : tile

	MC -> MC : resolveLocal(x,y,zoom)
	MC -> FC : loadImage(x,y,zoom)

	FC -> FC : resolveLocal(x,y,zoom)
	FC -> WC : loadImage(x,y,zoom)

	WC -> WC : DownloadImage(x,y,zoom)
	create Img 
	WC -> Img : init<>
	WC --> FC: image
	FC --> MC: image
	MC --> IW: image

	IW -> LT : setImage(image)
	LT ->> View: setDirty(true)
	LT --> IW

	View -> View : repaint


	@enduml

	*/
	public LazyTileItem<DESC> createTileItem(DESC desc)  {
		LazyTileItem<DESC> tile = new LazyTileItem<DESC>(desc, mLoadingImage);
		try {
			mTilesToLoad.put(tile);
			mExecutorService.execute(new TileWorker(mFactory.createTileLoader()));
		}catch(Exception e) {e.printStackTrace();}
		return tile;		
	}
	
	/**
	 * If a tile is no longer needed by the view (e.g. it is no longer within the visible area) 
	 * the view unloads the tile. That does not necessarily mean that the tile is destroyed. Instead
	 * the TileFactory notifies the ITileFactory (e.g. the Memory Cache) that the tile is no longer needed
	 * the MemoryCache now can decide wheater the tile will be destroyed or not. 
	 * If a Cache Entity destroys a tile, it shall notify its backup that the tile has been destroyed, to give the same 
	 * change to the backup entry (usually the WebCache does nothing on this method)
	 * 
	 * @note this method may also be called from the previous cache if he did decide that he need to destroy an unused tile, without being notified by the view
	 * for example if a new tile has been created and the memory consumption reaches a threshold. 
	 * 
	 * @startuml
		actor GeoView as View
		boundary TileFactory as TF
		collections MemoryCache as MC
		database FileCache as FC
		database WebCache as WC
		
		View -> TF : unloadTile(tile)
		TF ->> MC : unloadTile(tile)
		MC -> MC : checkIfUnload() : true
		MC -> FC : unloadTile(tile)
		FC -> FC : checkIfUnload() : false
	 * @enduml
	 * @param tile
	 */
	public void unloadTileItem(LazyTileItem<DESC> tile) {
		try {
			mTilesToUnload.put(tile);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
