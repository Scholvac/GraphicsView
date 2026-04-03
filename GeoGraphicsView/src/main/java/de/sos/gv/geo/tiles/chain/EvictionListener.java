package de.sos.gv.geo.tiles.chain;

public interface EvictionListener {
    void onEvict(String key, TilePayload payload);
}
