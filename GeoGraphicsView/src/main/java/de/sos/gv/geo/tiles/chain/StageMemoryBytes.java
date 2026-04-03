package de.sos.gv.geo.tiles.chain;

/**
 * L2 cache stage — stores encoded {@link TilePayload.Encoded} bytes in heap memory.
 *
 * <p>Sits between the decoded-image cache ({@link StageMemoryImage}) and disk.
 * Keeping compressed bytes in memory avoids repeated disk reads for tiles that were
 * recently evicted from L1 but are still frequently accessed.
 * Memory usage equals the exact compressed byte size, so the budget can be set much
 * lower than for {@link StageMemoryImage} while still covering many tiles.
 *
 * <p>When the budget is exceeded the LRU entry is evicted and demoted to {@link StageDisk}
 * via the {@link EvictionListener} set by {@link TileChain}.
 *
 * <p>This stage is optional. Skip it (omit from the builder or the stages list) if heap
 * memory is scarce and disk access latency is acceptable.
 */
public class StageMemoryBytes extends AbstractLRUMemoryStage<TilePayload.Encoded> {

	public StageMemoryBytes(final long maxBytes) { super(maxBytes); }

	@Override protected TilePayload.Kind kind()                { return TilePayload.Kind.ENCODED; }
	@Override protected TilePayload.Encoded cast(final TilePayload p) { return (TilePayload.Encoded) p; }

	/** Exact byte size of the compressed payload. */
	@Override protected long sizeOf(final TilePayload.Encoded v) { return v.value().length; }
}
