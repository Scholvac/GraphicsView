package de.sos.gvc.gt.test.cache2;

import de.sos.gvc.gt.tiles.LatLonBoundingBox;

public interface ITileManager {
	
	interface ITileDesc {}
	
	public interface ITileCalculator {
		ITileDesc[] calculateRequiredTiles(LatLonBoundingBox area);
	}
	public interface ITileProvider<TileData> {
		TileData provide(ITileDesc desc);
	}
	
	public interface ITileStorage<TileData> {
		public void store(ITileDesc desc, TileData data);
		public TileData remove(ITileDesc desc);
		public TileData getIfPresend(ITileDesc desc); //may return null
	}
	
	public interface ITileDataConverter<TileDataSRC, TileDataDST> {
		TileDataDST convert(TileDataSRC input);
	}
	
	
	
}
