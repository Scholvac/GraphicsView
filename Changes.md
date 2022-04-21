Changes
===========================

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

