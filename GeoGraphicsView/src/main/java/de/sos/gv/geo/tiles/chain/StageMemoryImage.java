package de.sos.gv.geo.tiles.chain;

/**
 * L1 cache stage — stores decoded {@link TilePayload.Image} objects in heap memory.
 *
 * <p>This is the fastest stage in the pipeline: a hit delivers a paint-ready
 * {@link java.awt.image.BufferedImage} with no I/O and no decode step.
 * Memory usage is estimated as {@code width × height × 4 bytes} (ARGB).
 *
 * <p>When the budget is exceeded the least-recently-used image is evicted.
 * If an {@link EvictionListener} is set (done automatically by {@link TileChain}),
 * the evicted image is re-encoded as PNG and demoted to the next stage.
 *
 * <p>Touch this class if the memory-estimation formula needs adjusting
 * (e.g. for non-ARGB formats) or if a different eviction policy is required.
 */
public class StageMemoryImage extends AbstractLRUMemoryStage<TilePayload.Image> {

	public StageMemoryImage(final long maxBytes) { super(maxBytes); }

	@Override protected TilePayload.Kind kind()              { return TilePayload.Kind.IMAGE; }
	@Override protected TilePayload.Image cast(final TilePayload p) { return (TilePayload.Image) p; }

	/** Estimates heap size as width × height × 4 bytes (ARGB). */
	@Override protected long sizeOf(final TilePayload.Image v) {
		return (long) v.value().getWidth() * v.value().getHeight() * 4L;
	}
}
