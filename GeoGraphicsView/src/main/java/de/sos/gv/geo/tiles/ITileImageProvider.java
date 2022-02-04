package de.sos.gv.geo.tiles;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import de.sos.gv.geo.tiles.impl.TileImageProvider;

public interface ITileImageProvider {
	public static final Supplier<ITileImageProvider> OSM = () -> new TileImageProvider("https://tile.openstreetmap.org/{z}/{x}/{y}.png");


	public BufferedImage provide(final TileInfo info);
}
