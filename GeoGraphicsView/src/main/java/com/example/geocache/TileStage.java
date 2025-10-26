package com.example.geocache;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.example.geocache.Cancellation.CancellationToken;

public interface TileStage {
    EnumSet<TilePayload.Kind> provides();
    EnumSet<TilePayload.Kind> accepts();
    CompletableFuture<Optional<TilePayload>> get(TileId id, CancellationToken ct);
    CompletableFuture<Void> put(TileId id, TilePayload payload, CancellationToken ct);
    // Move semantics helper: default implementation uses get+invalidate
    default CompletableFuture<Optional<TilePayload>> take(TileId id, CancellationToken ct){
        return get(id, ct).thenApply(opt -> {
            if (opt.isPresent()) invalidate(id);
            return opt;
        });
    }
    CompletableFuture<Void> invalidate(TileId id);
    default CompletableFuture<Void> clear(){ return CompletableFuture.completedFuture(null); }
    default long currentSizeBytes(){ return -1L; }
    default long maxSizeBytes(){ return -1L; }
}
