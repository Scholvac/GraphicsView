Changes
===========================

Version 2.0.1
------------------------
* [#35] Consider RepaintTrigger flag in markDirty method
Version 2.0.0
-------------------------
* Support off screen rendering into image render targets and outside of the Event Dispatcher Thread. 
* Support rendering hints 
* Notify render target size
* Add render filter for views, e.g. do not render specific items in a view
* (GeoGraphicsView) Support maximum zoom level for tiles
* (GeoGraphicsView) Support synchronous loading of tiles, e.g wait till all requested tiles are available
* Minor bugfixes and CI/CD improvements

Version 1.1.0
-------------------------
* let the TileHandler use an interface instead of an class, thus the (new) ITileFactory can be changed (see. CaffeineCacheExample.java)
* added new Cache version based on Caffeine library 
	* GeoGraphicsView got new dependency (caffeine)
* added ScaledStroke to support the fixed shape outlines when scaleing
* support View-Rotation (see RotateViewExample)
* SelectionHandler now previews the new pose of an moved / rotated / scaled item during the operation, by showing an merged shape
* added option to register listener as "permanent" listener that will be notified about mouse events, even if the mouse is not "over" the registered item
* enhanced move, scale and rotate operation by getting rid of the bug to lose the item under the mouse


### Unfinished ###
* added geometry editor project (not yet part of the main pom tree)

