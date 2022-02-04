package de.sos.gv.geo.tiles.cache;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gv.geo.tiles.cache.impl.AbstractTileCacheCascade;
import de.sos.gv.geo.tiles.cache.impl.TileImageProviderCache;

public interface ITileCache {

	public BufferedImage load(final TileInfo info);
	public void release(final TileInfo key, BufferedImage image);



	public static ITileCache build(Supplier<ITileImageProvider> osm, AbstractTileCacheCascade...caches) {
		for (int i = 0; i < caches.length-1; i++) {
			caches[i].setFollowUpCache(caches[i+1]);
		}
		caches[caches.length-1].setFollowUpCache( new TileImageProviderCache(osm) );
		return caches[0];
	}


	public interface IForwardCache extends ITileCache {
		public void add(final TileInfo key, BufferedImage value);
	}

}
