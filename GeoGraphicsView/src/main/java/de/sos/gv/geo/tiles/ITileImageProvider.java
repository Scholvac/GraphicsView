package de.sos.gv.geo.tiles;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import de.sos.gv.geo.tiles.cache.ThreadedTileProvider;
import de.sos.gv.geo.tiles.downloader.DefaultTileDownloader;

public interface ITileImageProvider {

	/**
	 * Supplier for new NON Threadsafe OSM Tile downloader
	 */
	final Supplier<ITileImageProvider>		OSM_SUPPLIER = () -> new DefaultTileDownloader("https://tile.openstreetmap.org/{z}/{x}/{y}.png");
	/**
	 * Tile Provider that may be used from different threads.
	 * Instantiates a new Downloader for each thread.
	 */
	final ThreadedTileProvider 				OSM = new ThreadedTileProvider(OSM_SUPPLIER);

	CompletableFuture<BufferedImage> load(final TileInfo info);

	void free(final TileInfo info, final BufferedImage img);

}
