package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;
import de.sos.gv.geo.tiles.chain.ImageIOTranscoder;
import de.sos.gv.geo.tiles.chain.StageDisk;
import de.sos.gv.geo.tiles.chain.StageMemoryBytes;
import de.sos.gv.geo.tiles.chain.StageMemoryImage;
import de.sos.gv.geo.tiles.chain.StageWeb;
import de.sos.gv.geo.tiles.chain.TileChain;
import de.sos.gv.geo.tiles.chain.TileChainImageProvider;
import de.sos.gv.geo.tiles.chain.TileExecutors;
import de.sos.gv.geo.tiles.chain.TileStage;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;

/**
 * Best-practice example: OSM tile display using the chain-based cache.
 *
 * <h2>Cache pipeline</h2>
 * <pre>
 * Request
 *   │
 *   ├─▶ Stage[0] StageMemoryImage  50 MB  — decoded BufferedImage, LRU
 *   ├─▶ Stage[1] StageMemoryBytes  20 MB  — encoded byte[], LRU
 *   ├─▶ Stage[2] StageDisk        500 MB  — file system ~/.cache/tiles, LRU by mtime
 *   └─▶ Stage[3] StageWeb                 — HTTP download from tile.openstreetmap.org
 * </pre>
 *
 * <h2>Core principles</h2>
 * <ul>
 *   <li><b>Single-copy:</b> Each tile exists in exactly one stage at a time. On a cache
 *       hit the tile is promoted to the fastest accepting stage and removed from the
 *       source stage.</li>
 *   <li><b>Eviction-to-demotion:</b> When a stage reaches its budget the oldest tile is
 *       automatically moved to the next lower stage instead of being dropped.</li>
 *   <li><b>In-flight deduplication:</b> Concurrent requests for the same tile share a
 *       single network operation internally.</li>
 * </ul>
 *
 * <h2>Shutdown</h2>
 * The application must call {@link TileExecutors#shutdown()} to release the thread pools;
 * otherwise they will prevent JVM exit.
 */
public class OSMWithChainCacheExample extends JFrame {

    // --------------------------------------------------------------------------------------------
    // Configuration
    // --------------------------------------------------------------------------------------------

    /** OSM tile URL template. Placeholders: {z}, {x}, {y}, {ext} */
    private static final String OSM_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.{ext}";

    /** L1 memory budget for decoded images (estimated as width × height × 4 bytes, ARGB). */
    private static final long L1_IMAGE_BYTES = 50L * 1024 * 1024;   // 50 MB

    /** L2 memory budget for encoded raw bytes. */
    private static final long L2_BYTES_BYTES = 20L * 1024 * 1024;   // 20 MB

    /** L3 disk cache budget. */
    private static final long L3_DISK_BYTES = 500L * 1024 * 1024;   // 500 MB

    /** Root directory for the disk cache. */
    private static final Path CACHE_DIR = Paths.get(System.getProperty("user.home"), ".cache", "gvc-tiles");

    /** HTTP connection timeout in milliseconds. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    /** HTTP read timeout in milliseconds. */
    private static final int READ_TIMEOUT_MS = 10_000;

    // --------------------------------------------------------------------------------------------
    // Fields
    // --------------------------------------------------------------------------------------------

    private GraphicsScene mScene;
    private GraphicsView  mView;

    /** Shared thread pools — must be released via {@link TileExecutors#shutdown()} on exit. */
    private TileExecutors mExecs;

    // --------------------------------------------------------------------------------------------
    // Entry point
    // --------------------------------------------------------------------------------------------

    public static void main(final String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                final OSMWithChainCacheExample frame = new OSMWithChainCacheExample();
                frame.setVisible(true);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    // --------------------------------------------------------------------------------------------
    // Constructor
    // --------------------------------------------------------------------------------------------

    public OSMWithChainCacheExample() {
        super("OSM — Chain Cache Example");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(100, 100, 1024, 768);

        final JPanel contentPane = new JPanel(new BorderLayout(0, 0));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        // (1) Build scene and view
        createScene();
        contentPane.add(mView.getComponent(), BorderLayout.CENTER);

        // (2) Set up the chain cache and attach OSM tiles
        setupChainCache();

        // (3) Initial viewport: Bremerhaven, Germany
        GeoUtils.setViewCenter(mView, new LatLonPoint(53.523495, 8.641542));
        mView.setScale(20);

        // (4) Release thread pools when the window closes
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(final java.awt.event.WindowEvent e) {
                mExecs.shutdown();
            }
        });
    }

    // --------------------------------------------------------------------------------------------
    // Scene setup
    // --------------------------------------------------------------------------------------------

    private void createScene() {
        mScene = new GraphicsScene();
        mView  = new GraphicsView(mScene);

        // Default interaction: drag to pan, delegate mouse events to items
        mView.addHandler(new MouseDelegateHandler());
        mView.addHandler(new DefaultViewDragHandler());
    }

    // --------------------------------------------------------------------------------------------
    // Chain cache setup
    // --------------------------------------------------------------------------------------------

    /**
     * Builds the four-stage cache pipeline and registers it as a {@link TileHandler}.
     *
     * <p>Stages must be ordered fastest (index 0) to slowest (last index).
     * {@link TileChain} wires the eviction-to-demotion callbacks automatically in its constructor.
     */
    private void setupChainCache() {
        // Shared thread pools for blocking I/O and CPU-bound decode work
        mExecs = new TileExecutors();

        // Stage[0]: L1 — decoded BufferedImages in heap memory (fastest access)
        final StageMemoryImage l1Image = new StageMemoryImage(L1_IMAGE_BYTES);

        // Stage[1]: L2 — encoded raw bytes in heap memory (cheaper than L1, fallback before disk)
        final StageMemoryBytes l2Bytes = new StageMemoryBytes(L2_BYTES_BYTES);

        // Stage[2]: L3 — persistent disk cache (survives restarts)
        final StageDisk l3Disk = new StageDisk(CACHE_DIR, L3_DISK_BYTES);

        // Stage[3]: web source — read-only, no budget
        // Use diskIO for all blocking network calls, not the decode pool
        final StageWeb webSource = new StageWeb(
                OSM_URL,
                CONNECT_TIMEOUT_MS,
                READ_TIMEOUT_MS,
                mExecs.diskIO
        );

        // TileChain wires eviction callbacks automatically:
        //   l1Image full → oldest IMAGE → transcoder encodes to PNG → l2Bytes
        //   l2Bytes full → oldest ENCODED → l3Disk
        final List<TileStage> stages = Arrays.asList(l1Image, l2Bytes, l3Disk, webSource);
        final ImageIOTranscoder transcoder = new ImageIOTranscoder(mExecs.decodeCPU);
        final TileChain chain = new TileChain(stages, transcoder, mExecs);

        // Adapt TileChain to ITileImageProvider — drop-in replacement for the legacy cache
        // "osm" = style prefix in the cache key, "png" = file extension
        final ITileImageProvider provider = new TileChainImageProvider(chain, "osm", "png");

        // Register with TileFactory using its default thread count
        mView.addHandler(new TileHandler(new TileFactory(provider)));
    }

    // --------------------------------------------------------------------------------------------
    // Advanced pattern: cancelling in-flight tile requests
    // --------------------------------------------------------------------------------------------

    /**
     * Illustrates how to cancel in-flight tile requests manually.
     *
     * <p>In practice {@code cancel()} is called by {@link TileFactory} when a tile
     * scrolls out of the viewport. This method shows the low-level pattern for custom use.
     *
     * <pre>
     *   CancellationSource src = new CancellationSource();
     *   CancellationToken  ct  = src.token();
     *   chain.getImage(id, ct)
     *        .whenComplete((img, ex) -> { ... });
     *
     *   // Later: cooperative cancellation
     *   src.cancel();
     * </pre>
     *
     * <p>When using {@link TileChainImageProvider}, {@code provider.cancel(TileInfo)}
     * handles this automatically.
     */
    @SuppressWarnings("unused")
    private static void cancellationPattern() {
        // For illustration only — never called at runtime
        final de.sos.gv.geo.tiles.chain.Cancellation.CancellationSource src =
                new de.sos.gv.geo.tiles.chain.Cancellation.CancellationSource();
        final CancellationToken ct = src.token();
        // ... chain.getImage(id, ct) ...
        src.cancel();
    }
}
