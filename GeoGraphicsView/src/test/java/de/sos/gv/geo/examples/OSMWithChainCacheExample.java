package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
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
 *   ├─▶ Stage[2] StageDisk        500 MB  — file system ~/.cache/gvc-tiles, LRU by mtime
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
 * Call {@link TileChainImageProvider#shutdown()} on exit to release the internal thread pools;
 * otherwise they will prevent JVM exit.
 */
public class OSMWithChainCacheExample extends JFrame {

    // --------------------------------------------------------------------------------------------
    // Fields
    // --------------------------------------------------------------------------------------------

    private GraphicsScene          mScene;
    private GraphicsView           mView;
    private TileChainImageProvider mProvider;

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
                mProvider.shutdown();
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
     * Builds the cache pipeline and registers it as a {@link TileHandler}.
     *
     * <p><b>Default: builder API.</b> The builder appends the web source automatically —
     * only the caching layers need to be configured.
     *
     * <p><b>Alternative: manual construction</b> — shown below in comments. Use this
     * when you need to insert a custom stage or require full control over the pipeline.
     * In that case you own the {@link TileExecutors} lifecycle and must call
     * {@code execs.shutdown()} yourself ({@code provider.shutdown()} is a no-op).
     *
     * <p>{@link TileChainImageProvider#shutdown()} must be called on exit (builder path only).
     */
    private void setupChainCache() {

        // ---- Option A: builder (recommended) ------------------------------------------------
        mProvider = TileChainImageProvider.builder(TileChainImageProvider.OSM_URL)
                .memoryImage(50, SizeUnit.MegaByte)   // L1: decoded images in heap
                .memoryBytes(20, SizeUnit.MegaByte)   // L2: encoded bytes in heap
                .disk(new File(System.getProperty("user.home"), ".cache/gvc-tiles"),
                        500, SizeUnit.MegaByte)        // L3: persistent disk cache
                .build();

        // ---- Option B: manual construction (alternative) ------------------------------------
        // Use when inserting custom stages or needing direct control over TileExecutors.
        // Uncomment the block below and remove Option A to switch.
        //
        // TileExecutors execs = new TileExecutors();
        // List<TileStage> stages = Arrays.asList(
        //     new StageMemoryImage(50L * 1024 * 1024),
        //     new StageMemoryBytes(20L * 1024 * 1024),
        //     new StageDisk(Paths.get(System.getProperty("user.home"), ".cache/gvc-tiles"),
        //                   500L * 1024 * 1024),
        //     new StageWeb(TileChainImageProvider.OSM_URL, 5_000, 10_000, execs.diskIO)
        // );
        // TileChain chain = new TileChain(stages, new ImageIOTranscoder(execs.decodeCPU), execs);
        // mProvider = new TileChainImageProvider(chain, "osm", "png");
        // // shutdown: call execs.shutdown() in windowClosing, not mProvider.shutdown()

        mView.addHandler(new TileHandler(new TileFactory(mProvider)));
    }
}
