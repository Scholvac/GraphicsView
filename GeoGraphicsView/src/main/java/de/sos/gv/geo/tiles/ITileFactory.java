package de.sos.gv.geo.tiles;

import java.awt.Rectangle;
import java.io.File;

import de.sos.gv.geo.LatLonBox;
import de.sos.gv.geo.tiles.cache.FileCache;
import de.sos.gv.geo.tiles.cache.MemoryCache;
import de.sos.gvc.log.GVLog;

public interface ITileFactory {

	int[][] getRequiredTileInfos(LatLonBox area, Rectangle viewBounds);

	TileItem load(int[] tileInfo);

	void release(TileItem item);

	static ITileImageProvider buildCache(final ITileImageProvider web, final long memorySize, final SizeUnit memorySizeUnit, final File imageDirectory, final long fileSize, final SizeUnit fileSizeUnit) {
		ITileImageProvider out = web;
		if (memorySize > 0) {
			GVLog.debug(String.format("Create MemoryCache with %d[%s]", memorySize, memorySizeUnit));
			out = new MemoryCache(out, memorySize, memorySizeUnit);
		}else {
			GVLog.trace("Skip MemoryCache");
		}
		if (imageDirectory != null && fileSize > 0) {
			GVLog.debug(String.format("Create FileCache with %d[%s] at %s", memorySize, memorySizeUnit, imageDirectory.getAbsolutePath()));
			out = new FileCache(out, imageDirectory, fileSize, fileSizeUnit);
		}else
			GVLog.trace("Skip FileCache");
		return out;
	}



}