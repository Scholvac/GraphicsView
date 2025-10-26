package com.example.geocache;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import com.example.geocache.Cancellation.CancellationToken;

public class StageMemoryBytes implements TileStage, SupportsEvictionListener {
    private final long maxBytes;
    private long curBytes = 0L;
    private final ReentrantLock lock = new ReentrantLock();
    private final LinkedHashMap<String, TilePayload.Encoded> lru = new LinkedHashMap<String, TilePayload.Encoded>(16,0.75f,true);
    private volatile EvictionListener evictionListener;

    public StageMemoryBytes(long maxBytes){ this.maxBytes = maxBytes; }

    @Override public EnumSet<TilePayload.Kind> provides(){ return EnumSet.of(TilePayload.Kind.ENCODED); }
    @Override public EnumSet<TilePayload.Kind> accepts(){ return EnumSet.of(TilePayload.Kind.ENCODED); }

    @Override public CompletableFuture<Optional<TilePayload>> get(final TileId id, final CancellationToken ct){
        return CompletableFuture.supplyAsync(() -> {
            if (ct!=null && ct.isCancelled()) throw new CancellationException();
            lock.lock();
            try {
                TilePayload.Encoded v = lru.get(id.cacheKey());
                return v != null ? Optional.<TilePayload>of(v) : Optional.<TilePayload>empty();
            } finally { lock.unlock(); }
        });
    }

    @Override public CompletableFuture<Void> put(final TileId id, final TilePayload payload, final CancellationToken ct){
        if (payload.kind()!=TilePayload.Kind.ENCODED) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            if (ct!=null && ct.isCancelled()) throw new CancellationException();
            TilePayload.Encoded p = (TilePayload.Encoded) payload;
            long size = p.value().length;
            lock.lock();
            try {
                TilePayload.Encoded prev = lru.remove(id.cacheKey());
                if (prev != null) curBytes -= prev.value().length;
                lru.put(id.cacheKey(), p);
                curBytes += size;
                while (curBytes > maxBytes && !lru.isEmpty()) {
                    Map.Entry<String, TilePayload.Encoded> eldest = lru.entrySet().iterator().next();
                    curBytes -= eldest.getValue().value().length;
                    String key = eldest.getKey();
                    TilePayload.Encoded ev = eldest.getValue();
                    lru.remove(key);
                    if (evictionListener != null) evictionListener.onEvict(key, ev);
                }
            } finally { lock.unlock(); }
        });
    }

    @Override public CompletableFuture<Void> invalidate(final TileId id){
        return CompletableFuture.runAsync(() -> {
            lock.lock();
            try {
                TilePayload.Encoded prev = lru.remove(id.cacheKey());
                if (prev != null) curBytes -= prev.value().length;
            } finally { lock.unlock(); }
        });
    }

    @Override public long currentSizeBytes(){ return curBytes; }
    @Override public long maxSizeBytes(){ return maxBytes; }

    @Override public void setEvictionListener(EvictionListener l){ this.evictionListener = l; }
}
