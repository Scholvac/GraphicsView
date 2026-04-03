# Tile Cache HowTo

**Package:** `de.sos.gv.geo.tiles.chain`

The chain cache is a drop-in replacement for the legacy `ITileFactory.buildCache()` cache.
It uses a multi-stage pipeline with two key behaviours:

- **Single-copy promotion** — on a hit the tile is moved to the fastest stage and removed from the slower one.
- **Eviction-to-demotion** — when a stage is full the oldest tile is pushed to the next stage instead of being dropped.

---

## Quick start

### One-liner (mirrors legacy API)

```java
TileChainImageProvider provider = TileChainImageProvider.build(
    TileChainImageProvider.OSM_URL,   // tile server URL
    50, SizeUnit.MegaByte,            // decoded-image memory budget (L1)
    new File("./.cache"),             // disk cache directory
    500, SizeUnit.MegaByte            // disk budget
);
mView.addHandler(new TileHandler(new TileFactory(provider)));
```

Skip a stage by passing `0` for its size or `null` for the directory.

### Builder (fine-grained control)

Use the builder when you need to tune individual stages or add a separate byte-level
memory buffer between the image cache and disk:

```java
TileChainImageProvider provider = TileChainImageProvider.builder(TileChainImageProvider.OSM_URL)
    .memoryImage(50, SizeUnit.MegaByte)    // L1: decoded BufferedImages
    .memoryBytes(20, SizeUnit.MegaByte)    // L2: encoded bytes (cheaper than images)
    .disk(new File("./.cache"), 500, SizeUnit.MegaByte)
    .connectTimeout(5_000)                 // optional — default 5 s
    .readTimeout(10_000)                   // optional — default 10 s
    .build();
```

The builder always appends a web stage automatically; you only configure the caching layers.

### Custom tile server

Replace the URL constant with any XYZ tile server. Available placeholders: `{z}` `{x}` `{y}` `{ext}` `{style}`.

```java
TileChainImageProvider provider = TileChainImageProvider.builder(
        "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}")
    .memoryImage(50, SizeUnit.MegaByte)
    .disk(new File("./.cache/google"), 500, SizeUnit.MegaByte)
    .style("google").ext("png")
    .build();
```

### Manual construction (alternative)

When the builder does not cover your use case — for example when inserting a custom stage
into the pipeline — you can assemble the chain by hand.
You then own the `TileExecutors` lifecycle and must call `execs.shutdown()` yourself
(note: `provider.shutdown()` is a no-op in this case).

```java
TileExecutors execs = new TileExecutors();

List<TileStage> stages = Arrays.asList(
    new StageMemoryImage(50L * 1024 * 1024),          // L1: 50 MB decoded images
    new StageMemoryBytes(20L * 1024 * 1024),           // L2: 20 MB encoded bytes
    new StageDisk(Paths.get(".cache"), 500L * 1024 * 1024), // L3: 500 MB on disk
    new StageWeb(TileChainImageProvider.OSM_URL,
            5_000, 10_000, execs.diskIO)               // source: always use diskIO here
);

TileChain chain = new TileChain(
    stages,
    new ImageIOTranscoder(execs.decodeCPU),
    execs
);

// style and ext must match the URL and disk layout
TileChainImageProvider provider = new TileChainImageProvider(chain, "osm", "png");

mView.addHandler(new TileHandler(new TileFactory(provider)));

// shutdown on exit — provider.shutdown() is a no-op here, so shut down execs directly
addWindowListener(new WindowAdapter() {
    @Override public void windowClosing(WindowEvent e) { execs.shutdown(); }
});
```

---

## Synchronisation and shutdown

### Shutdown

The provider owns internal thread pools that must be released on exit.
Without `shutdown()` the threads keep the JVM alive.

```java
// In a WindowListener — the standard pattern
addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosing(final WindowEvent e) {
        provider.shutdown();
    }
});
```

For tests, wait for the pools to drain completely:

```java
@AfterEach
void teardown() throws InterruptedException {
    provider.shutdown();
    // optional: wait until all queued work finishes
    provider.getChain().whenBackgroundIdle().join();
}
```

`shutdown()` only has an effect when the provider was created via `build()` or the
`Builder`. When you construct `TileChainImageProvider` manually you own the
`TileExecutors` and must shut them down yourself.

### Waiting for in-flight requests

`getChain()` gives access to the underlying `TileChain` for synchronisation:

```java
TileChain chain = provider.getChain();

// Wait until all active tile requests have finished.
// Useful before a screenshot, test assertion, or state snapshot.
chain.whenIdle().join();

// Wait additionally for background eviction-demotion writes.
// A full quiesce looks like this:
chain.whenIdle()
     .thenCompose(v -> chain.whenBackgroundIdle())
     .join();
```

`whenBackgroundIdle()` covers operations that `whenIdle()` does not: when a stage
evicts a tile the chain re-encodes it (if necessary) and writes it to the next stage
asynchronously. These writes finish after the original request is done.

```java
// Polls until no new requests have appeared — use only in controlled scenarios
// (shutdown, test teardown) where you know load has stopped.
// ⚠ May never complete under sustained load.
chain.whenTrulyIdle().join();
```

---

## Implementing a custom stage

Implement `TileStage` to plug in any storage backend (Redis, CDN, etc.).
Implement `SupportsEvictionListener` as well if evicted tiles should be demoted
to the next stage rather than dropped.

```java
public class MyNetworkStage implements TileStage, SupportsEvictionListener {

    private EvictionListener mEvictionListener;

    @Override public EnumSet<TilePayload.Kind> provides() { return EnumSet.of(Kind.ENCODED); }
    @Override public EnumSet<TilePayload.Kind> accepts()  { return EnumSet.of(Kind.ENCODED); }

    @Override
    public CompletableFuture<Optional<TilePayload>> get(TileId id, CancellationToken ct) {
        return CompletableFuture.supplyAsync(() -> {
            if (ct != null && ct.isCancelled()) throw new CancellationException();
            byte[] data = myBackend.fetch(id.cacheKey());
            return data != null
                ? Optional.of(new TilePayload.Encoded(data, "image/png"))
                : Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Void> put(TileId id, TilePayload payload, CancellationToken ct) {
        return CompletableFuture.runAsync(() -> myBackend.store(id.cacheKey(), /* bytes */));
    }

    @Override
    public CompletableFuture<Void> invalidate(TileId id) {
        return CompletableFuture.runAsync(() -> myBackend.delete(id.cacheKey()));
    }

    @Override
    public void setEvictionListener(EvictionListener l) { mEvictionListener = l; }
}
```

Custom stages can only be added via the low-level constructor — not through the builder:

```java
TileExecutors execs = new TileExecutors();
TileChain chain = new TileChain(
    Arrays.asList(
        new StageMemoryImage(50L * 1024 * 1024),
        new MyNetworkStage(),
        new StageDisk(Paths.get(".cache"), 500L * 1024 * 1024),
        new StageWeb(TileChainImageProvider.OSM_URL, 5_000, 10_000, execs.diskIO)
    ),
    new ImageIOTranscoder(execs.decodeCPU),
    execs
);
TileChainImageProvider provider = new TileChainImageProvider(chain);
// caller owns execs.shutdown()
```

---

## Full example

See `GeoGraphicsView/src/test/java/de/sos/gv/geo/examples/OSMWithChainCacheExample.java`
for a complete, annotated Swing application using the chain cache.
