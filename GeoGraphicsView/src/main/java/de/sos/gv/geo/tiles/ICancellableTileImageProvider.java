package de.sos.gv.geo.tiles;

/**
 * Optional extension for providers that can abort an in-flight tile request.
 */
public interface ICancellableTileImageProvider extends ITileImageProvider {

	void cancel(TileInfo info);

}
