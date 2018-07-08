package de.sos.gvc.gt.tiles;

import java.awt.geom.Rectangle2D;
import java.util.Collection;

public interface ITileFactory<DESC extends ITileDescription> {

	Collection<DESC> getTileDescriptions(LatLonBoundingBox area, Rectangle2D viewArea);

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
	LazyTileItem<DESC> createTileItem(DESC desc);

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
	void unloadTileItem(LazyTileItem<DESC> tile);

}