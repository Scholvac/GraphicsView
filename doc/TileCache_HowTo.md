# TileCache HowTo — Chain-based Tile Cache

> **Current package:** `com.example.geocache`
> **Planned target package:** `de.sos.gv.geo.tiles.chain`

---

## Overview

The chain cache replaces the legacy implementation (`MemoryCache` / `FileCache`) with a fully
asynchronous, multi-stage pipeline built on two core principles:

| Principle | Description |
|---|---|
| **Single-copy** | A tile exists in exactly one stage at a time. After retrieval it is promoted to the fastest accepting stage and removed from the source stage. |
| **In-flight deduplication** | Concurrent requests for the same tile share a single network/disk operation internally. |

---

## Architecture

```
Request
  │
  ▼
TileChain.getImage(id, ct)
  │   ┌─────────────────────────────────────────┐
  │   │  mInFlight map (deduplication)          │
  │   │  Same TileId → shared CompletableFuture │
  │   └─────────────────────────────────────────┘
  │
  ├─▶ Stage[0]: StageMemoryImage   (L1 — decoded BufferedImage, LRU by pixel bytes)
  ├─▶ Stage[1]: StageMemoryBytes   (L2 — encoded byte[], LRU by byte size)
  ├─▶ Stage[2]: StageDisk          (L3 — file system, LRU by mtime)
  └─▶ Stage[3]: StageWeb           (source — HTTP download, read-only)

         ↕ Eviction callback: Stage[i] evicts → Stage[i+1] receives the payload
         ↕ Promotion:         Hit in Stage[i] → moved to Stage[0] or Stage[1]
```

### Payload types and format conversion

| Stage | provides | accepts | Conversion |
|---|---|---|---|
| `StageMemoryImage` | IMAGE | IMAGE | — |
| `StageMemoryBytes` | ENCODED | ENCODED | — |
| `StageDisk` | ENCODED | ENCODED, IMAGE | ImageIO writes IMAGE as PNG |
| `StageWeb` | ENCODED | *(nothing)* | — |

When a stage requires a different format than what is available, `ImageIOTranscoder`
handles PNG conversion on the `decodeCPU` thread pool.

---

## Class reference

### `TileId`

Immutable key identifying a single tile.

```java
TileId(int z, int x, int y, String style, String ext)
```

| Method | Return | Description |
|---|---|---|
| `getZ()` | `int` | Zoom level |
| `getX()` | `int` | Tile X coordinate |
| `getY()` | `int` | Tile Y coordinate |
| `getStyle()` | `String` | Style name (e.g. `"osm"`) |
| `getExt()` | `String` | File extension (e.g. `"png"`) |
| `cacheKey()` | `String` | Canonical key: `style/z/x/y.ext` |

---

### `TilePayload` (interface + inner classes)

Carrier object for tile image data.

```java
TilePayload.Kind { IMAGE, ENCODED }
```

| Class | Methods | Description |
|---|---|---|
| `TilePayload.Image` | `value(): BufferedImage`, `kind()` | Decoded raster image |
| `TilePayload.Encoded` | `value(): byte[]`, `contentType(): String`, `kind()` | Raw bytes (PNG/JPEG) |

---

### `TileStage` (interface)

Implement this interface to add a custom cache stage.

| Method | Return | Description |
|---|---|---|
| `provides()` | `EnumSet<Kind>` | Payload kinds this stage can deliver |
| `accepts()` | `EnumSet<Kind>` | Payload kinds this stage can store |
| `get(id, ct)` | `CF<Optional<TilePayload>>` | Read access; empty = cache miss |
| `put(id, payload, ct)` | `CF<Void>` | Write access |
| `take(id, ct)` | `CF<Optional<TilePayload>>` | Read + delete atomically (move semantics) |
| `invalidate(id)` | `CF<Void>` | Remove entry |
| `clear()` | `CF<Void>` | Remove all entries (default: no-op) |
| `currentSizeBytes()` | `long` | Current memory usage (-1 = unknown) |
| `maxSizeBytes()` | `long` | Configured budget (-1 = unlimited) |

---

### `StageMemoryImage`

L1 cache for decoded `BufferedImage` objects.

```java
StageMemoryImage(long maxBytes)
```

Memory usage is estimated as `width × height × 4 bytes` (ARGB).
Implements `SupportsEvictionListener` — evicted images are automatically demoted to Stage[1].

---

### `StageMemoryBytes`

L2 cache for encoded raw data (`byte[]`).

```java
StageMemoryBytes(long maxBytes)
```

Memory usage equals the exact byte length of the array.
Implements `SupportsEvictionListener` — evicted bytes are automatically demoted to `StageDisk`.

---

### `StageDisk`

Persistent disk cache. Directory layout: `root/style/z/x/y.ext`.

```java
StageDisk(Path root, long maxBytes)
```

| Method | Notes |
|---|---|
| `get(id, ct)` | Reads file and updates `mtime` (LRU tracking) |
| `take(id, ct)` | Reads and deletes file in one step |
| `put(id, payload, ct)` | Writes file, then calls `enforceBudget()` (O(n) directory walk) |
| `invalidate(id)` | Deletes the file |

> **Note:** `enforceBudget()` walks the entire cache directory on every `put()`.
> This can become a bottleneck for very large caches.

---

### `StageWeb`

HTTP source stage. Fetches tiles from the network; stores nothing (`accepts()` is empty).

```java
// Short form (3 attempts, User-Agent "GeoGraphView/1.0")
StageWeb(String baseUrl, int connectTimeoutMs, int readTimeoutMs, Executor ioExecutor)

// Full form
StageWeb(String baseUrl, int connectTimeoutMs, int readTimeoutMs,
         Executor ioExecutor, int attempts, String userAgent)
```

URL template variables: `{style}`, `{z}`, `{x}`, `{y}`, `{ext}`

Example: `"https://tile.openstreetmap.org/{z}/{x}/{y}.{ext}"`

---

### `TileExecutors`

Manages two thread pools for I/O and CPU-bound work.

```java
TileExecutors()  // configured automatically based on availableProcessors()
```

| Field | Threads | Purpose |
|---|---|---|
| `diskIO` | 4–8 | Blocking network I/O and disk read/write |
| `decodeCPU` | ≥ 2 | PNG/JPEG decoding (CPU-intensive) |

```java
execs.shutdown();  // call on application exit
```

---

### `ImageIOTranscoder`

Converts between `TilePayload.Image` and `TilePayload.Encoded` via `javax.imageio.ImageIO`.

```java
ImageIOTranscoder(Executor decodeExec)
```

| Method | Return | Description |
|---|---|---|
| `decode(encoded, ct)` | `CF<TilePayload.Image>` | Decodes PNG/JPEG bytes to `BufferedImage` |
| `encodePng(image, ct)` | `CF<TilePayload.Encoded>` | Encodes `BufferedImage` to PNG bytes |

---

### `TileChain`

Core orchestrator. Manages the stage list, in-flight deduplication, and single-copy promotion.

```java
TileChain(List<TileStage> stages, ImageIOTranscoder transcoder, TileExecutors execs)
```

The `stages` list must be ordered from fastest (index 0) to slowest (last index).
Eviction callbacks are wired automatically in the constructor.

| Method | Description |
|---|---|
| `getImage(id, ct)` | Main entry point — returns `CF<TilePayload.Image>` |
| `whenIdle()` | Completes when all active tile requests have finished |
| `whenBackgroundIdle()` | Completes when all background eviction-demotion operations have finished |
| `whenTrulyIdle()` | Completes only when the chain is completely empty (may never complete under sustained load) |

---

### `Cancellation`

Cooperative cancellation model.

```java
// No-op token (never cancelled)
CancellationToken ct = CancellationToken.none();

// Controllable cancellation
CancellationSource src = new CancellationSource();
CancellationToken  ct  = src.token();
// ... later:
src.cancel();
```

---

### `TileChainImageProvider`

Adapter that bridges `TileChain` to the synchronous `ITileImageProvider` interface.
Enables a **drop-in replacement** for the legacy cache in `TileFactory`.

```java
// Default: style "osm", extension "png"
TileChainImageProvider(TileChain chain)

// Explicit style and extension
TileChainImageProvider(TileChain chain, String style, String ext)
```

| Method | Description |
|---|---|
| `load(TileInfo)` | Synchronous load — blocks until the future completes |
| `cancel(TileInfo)` | Cancels all in-flight requests for this tile |
| `free(TileInfo, BufferedImage)` | No-op — cache lifecycle is owned by the chain |

---

## Eviction and demotion flow

```
StageMemoryImage (full)
  └─ evictionListener.onEvict(key, IMAGE payload)
       │
       ├─ StageMemoryBytes accepts ENCODED → transcoder encodes to PNG → StageMemoryBytes.put()
       └─ or: StageMemoryBytes accepts IMAGE directly → StageMemoryBytes.put()

StageMemoryBytes (full)
  └─ evictionListener.onEvict(key, ENCODED payload)
       └─ StageDisk accepts ENCODED → StageDisk.put()
```

`TileChain` wires these callbacks automatically in its constructor (`wireEvictions()`).

---

## Implementing a custom stage

```java
public class MyRedisStage implements TileStage, SupportsEvictionListener {

    @Override
    public EnumSet<TilePayload.Kind> provides() {
        return EnumSet.of(TilePayload.Kind.ENCODED);
    }

    @Override
    public EnumSet<TilePayload.Kind> accepts() {
        return EnumSet.of(TilePayload.Kind.ENCODED);
    }

    @Override
    public CompletableFuture<Optional<TilePayload>> get(TileId id, CancellationToken ct) {
        return CompletableFuture.supplyAsync(() -> {
            if (ct != null && ct.isCancelled()) throw new CancellationException();
            byte[] data = redis.get(id.cacheKey());
            if (data == null) return Optional.empty();
            return Optional.of(new TilePayload.Encoded(data, "image/png"));
        });
    }

    @Override
    public CompletableFuture<Void> put(TileId id, TilePayload payload, CancellationToken ct) {
        // ...
    }

    @Override
    public CompletableFuture<Void> invalidate(TileId id) {
        // ...
    }

    @Override
    public void setEvictionListener(EvictionListener l) {
        this.mEvictionListener = l;
    }
}
```

Insert it into the chain between existing stages:

```java
List<TileStage> stages = Arrays.asList(
    new StageMemoryImage(50 * MB),
    new StageMemoryBytes(20 * MB),
    new MyRedisStage(...),           // inserted between memory and disk
    new StageDisk(cachePath, 500 * MB),
    new StageWeb(OSM_URL, 5000, 10000, execs.diskIO)
);
```

---

## Synchronisation and shutdown

```java
// Wait until all active tile requests have finished (e.g. before a test assertion)
chain.whenIdle().join();

// Wait until all background eviction demotions have completed
chain.whenBackgroundIdle().join();

// Shut down thread pools cleanly on application exit
execs.shutdown();
```
