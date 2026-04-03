package de.sos.gv.geo.tiles.chain;

/**
 * @deprecated Eviction support is now part of {@link TileStage#setEvictionListener}.
 *             This interface is no longer used and will be removed in a future version.
 */
@Deprecated
public interface SupportsEvictionListener {
    void setEvictionListener(EvictionListener l);
}
