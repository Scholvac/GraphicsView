package de.sos.gv.geo.tiles.chain;

/**
 * Marker for {@link TileStage} implementations that can notify when a tile is evicted.
 *
 * <p>{@link TileChain} checks for this interface on each stage during construction and
 * wires the demotion chain automatically: evictions from stage&nbsp;i push the payload
 * into the first lower stage that accepts it.
 *
 * <p>Implement this alongside {@link TileStage} in any custom stage that enforces a budget
 * and should participate in the demotion chain rather than silently dropping tiles.
 */
public interface SupportsEvictionListener {
    /** Called once by {@link TileChain} during construction. Must not be {@code null}. */
    void setEvictionListener(EvictionListener l);
}
