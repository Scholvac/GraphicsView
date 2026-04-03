package de.sos.gv.geo.tiles.chain;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;

/**
 * Contract for a single cache stage in the {@link TileChain} pipeline.
 *
 * <p>Each stage declares which {@link TilePayload.Kind}s it can <em>provide</em> (read) and
 * <em>accept</em> (write). {@link TileChain} uses these sets to route payloads and trigger
 * format conversion via {@link ImageIOTranscoder} when needed.
 *
 * <p>Existing implementations, fastest to slowest:
 * <ol>
 *   <li>{@link StageMemoryImage} — decoded images in heap (L1)</li>
 *   <li>{@link StageMemoryBytes} — encoded bytes in heap (L2)</li>
 *   <li>{@link StageDisk}        — files on disk (L3)</li>
 *   <li>{@link StageWeb}         — HTTP download, read-only (source)</li>
 * </ol>
 *
 * <p>Implement this interface to add a custom backend (Redis, CDN, etc.).
 * Also implement {@link SupportsEvictionListener} so evicted tiles are demoted
 * to the next stage rather than dropped.
 */
public interface TileStage {

	/** @return payload kinds this stage can deliver on {@link #get}. */
	EnumSet<TilePayload.Kind> provides();

	/** @return payload kinds this stage accepts on {@link #put}. */
	EnumSet<TilePayload.Kind> accepts();

	/** Asynchronous read. Returns empty if the tile is not present (cache miss). */
	CompletableFuture<Optional<TilePayload>> get(TileId id, CancellationToken ct);

	/** Asynchronous write. Ignored (completed immediately) if the kind is not in {@link #accepts()}. */
	CompletableFuture<Void> put(TileId id, TilePayload payload, CancellationToken ct);

	/**
	 * Read-then-delete (move semantics). Default implementation calls {@link #get} + {@link #invalidate}.
	 * Override for atomic behaviour if the backend supports it (e.g. {@link StageDisk}).
	 */
	default CompletableFuture<Optional<TilePayload>> take(final TileId id, final CancellationToken ct){
		return get(id, ct).thenApply(opt -> {
			if (opt.isPresent()) invalidate(id);
			return opt;
		});
	}
	/** Removes the tile from this stage. No-op if not present. */
	CompletableFuture<Void> invalidate(TileId id);

	/** Removes all tiles from this stage. Default: no-op. */
	default CompletableFuture<Void> clear(){ return CompletableFuture.completedFuture(null); }

	/** Current storage usage in bytes, or {@code -1} if not tracked. */
	default long currentSizeBytes(){ return -1L; }

	/** Configured storage budget in bytes, or {@code -1} if unlimited. */
	default long maxSizeBytes(){ return -1L; }
}
