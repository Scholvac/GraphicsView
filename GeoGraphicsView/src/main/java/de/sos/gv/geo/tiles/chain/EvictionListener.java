package de.sos.gv.geo.tiles.chain;

/**
 * Callback fired by a {@link TileStage} when it evicts a tile to enforce its budget.
 *
 * <p>{@link TileChain} sets this on every stage that implements {@link SupportsEvictionListener}
 * so that evicted payloads are demoted to the next lower stage instead of being dropped.
 * Normal users never implement or call this directly.
 */
public interface EvictionListener {
    /**
     * Called on the stage's internal write thread when a tile is evicted.
     *
     * @param key     the tile's cache key (see {@link TileId#cacheKey()})
     * @param payload the evicted payload — already removed from the stage
     */
    void onEvict(String key, TilePayload payload);
}
