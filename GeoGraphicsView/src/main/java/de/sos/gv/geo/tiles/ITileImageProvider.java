package de.sos.gv.geo.tiles;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import de.sos.gv.geo.tiles.cache.ThreadedTileProvider;
import de.sos.gv.geo.tiles.downloader.DefaultTileDownloader;

public interface ITileImageProvider {

	/**
	 * Supplier for new NON Threadsafe OSM Tile downloader
	 */
	Supplier<ITileImageProvider>		OSM_SUPPLIER = () -> new DefaultTileDownloader("https://tile.openstreetmap.org/{z}/{x}/{y}.png");
	/**
	 * Tile Provider that may be used from different threads.
	 * Instantiates a new Downloader for each thread.
	 */
	ThreadedTileProvider 				OSM = new ThreadedTileProvider(OSM_SUPPLIER);

	BufferedImage load(final TileInfo info);

	void free(final TileInfo info, final BufferedImage img);

}
