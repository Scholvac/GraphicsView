package de.sos.gv.geo.tiles.chain;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import de.sos.gv.geo.tiles.ICancellableTileImageProvider;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gv.geo.tiles.chain.Cancellation.CancellationSource;
import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;

/**
 * Adapter between the generic {@link TileChain} and the synchronous
 * {@link ITileImageProvider} used by {@link de.sos.gv.geo.tiles.TileFactory}.
 *
 * <h2>Quick setup</h2>
 * <pre>
 * // One-liner — mirrors the legacy ITileFactory.buildCache() API
 * TileChainImageProvider provider = TileChainImageProvider.build(
 *     TileChainImageProvider.OSM_URL,
 *     50, SizeUnit.MegaByte,
 *     new File("./.cache"), 500, SizeUnit.MegaByte
 * );
 *
 * // Advanced — choose which stages to include
 * TileChainImageProvider provider = TileChainImageProvider.builder(TileChainImageProvider.OSM_URL)
 *     .memoryImage(50, SizeUnit.MegaByte)
 *     .memoryBytes(20, SizeUnit.MegaByte)
 *     .disk(new File("./.cache"), 500, SizeUnit.MegaByte)
 *     .build();
 * </pre>
 *
 * <p>Call {@link #shutdown()} when the provider is no longer needed to release
 * the internal thread pools.
 */
public class TileChainImageProvider implements ITileImageProvider, ICancellableTileImageProvider {

	/** OSM tile URL template. */
	public static final String OSM_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.{ext}";

	// -----------------------------------------------------------------------------------------
	// Factory methods
	// -----------------------------------------------------------------------------------------

	/**
	 * Builds a provider with memory-image, memory-bytes, disk, and web stages —
	 * the same signature as {@link de.sos.gv.geo.tiles.ITileFactory#buildCache}.
	 *
	 * <p>Pass {@code memorySize <= 0} or {@code diskSize <= 0} to skip that stage.
	 *
	 * @param tileUrl         tile server URL template ({z}, {x}, {y}, {ext} placeholders)
	 * @param memoryImageSize budget for decoded images (L1)
	 * @param memoryImageUnit unit for {@code memoryImageSize}
	 * @param cacheDir        root directory for the disk cache (may be {@code null} to skip)
	 * @param diskSize        disk budget
	 * @param diskUnit        unit for {@code diskSize}
	 * @return a ready-to-use provider; call {@link #shutdown()} on application exit
	 */
	public static TileChainImageProvider build(
			final String tileUrl,
			final long memoryImageSize, final SizeUnit memoryImageUnit,
			final File cacheDir,
			final long diskSize, final SizeUnit diskUnit) {
		return builder(tileUrl)
				.memoryImage(memoryImageSize, memoryImageUnit)
				.disk(cacheDir, diskSize, diskUnit)
				.build();
	}

	/**
	 * Returns a {@link Builder} for constructing a provider with fine-grained stage control.
	 *
	 * @param tileUrl tile server URL template ({z}, {x}, {y}, {ext} placeholders)
	 */
	public static Builder builder(final String tileUrl) {
		return new Builder(tileUrl);
	}

	// -----------------------------------------------------------------------------------------
	// Builder
	// -----------------------------------------------------------------------------------------

	/**
	 * Fluent builder for {@link TileChainImageProvider}.
	 * All stages are optional; only the web source is always present.
	 * Stages are added in the order fastest → slowest: memoryImage, memoryBytes, disk, web.
	 */
	public static final class Builder {

		private final String mTileUrl;
		private long   mMemoryImageBytes = 0;
		private long   mMemoryBytesBytes = 0;
		private Path   mDiskPath         = null;
		private long   mDiskBytes        = 0;
		private String mStyle            = "osm";
		private String mExt              = "png";
		private int    mConnectTimeoutMs = 5_000;
		private int    mReadTimeoutMs    = 10_000;

		private Builder(final String tileUrl) {
			mTileUrl = tileUrl;
		}

		/**
		 * Adds an L1 decoded-image memory stage.
		 *
		 * @param size budget value
		 * @param unit budget unit
		 */
		public Builder memoryImage(final long size, final SizeUnit unit) {
			mMemoryImageBytes = unit.toBytes(size);
			return this;
		}

		/**
		 * Adds an L2 encoded-bytes memory stage.
		 *
		 * @param size budget value
		 * @param unit budget unit
		 */
		public Builder memoryBytes(final long size, final SizeUnit unit) {
			mMemoryBytesBytes = unit.toBytes(size);
			return this;
		}

		/**
		 * Adds a disk cache stage.
		 *
		 * @param dir  root directory (pass {@code null} to skip)
		 * @param size disk budget value
		 * @param unit budget unit
		 */
		public Builder disk(final File dir, final long size, final SizeUnit unit) {
			if (dir != null && size > 0) {
				mDiskPath  = dir.toPath();
				mDiskBytes = unit.toBytes(size);
			}
			return this;
		}

		/**
		 * Overrides the style token used in tile cache keys (default: {@code "osm"}).
		 *
		 * @param style style name
		 */
		public Builder style(final String style) {
			mStyle = style;
			return this;
		}

		/**
		 * Overrides the file extension used in tile cache keys (default: {@code "png"}).
		 *
		 * @param ext extension without leading dot
		 */
		public Builder ext(final String ext) {
			mExt = ext;
			return this;
		}

		/**
		 * Overrides the HTTP connect timeout (default: 5 000 ms).
		 *
		 * @param ms timeout in milliseconds
		 */
		public Builder connectTimeout(final int ms) {
			mConnectTimeoutMs = ms;
			return this;
		}

		/**
		 * Overrides the HTTP read timeout (default: 10 000 ms).
		 *
		 * @param ms timeout in milliseconds
		 */
		public Builder readTimeout(final int ms) {
			mReadTimeoutMs = ms;
			return this;
		}

		/**
		 * Assembles all configured stages and returns a ready-to-use provider.
		 * Call {@link TileChainImageProvider#shutdown()} on application exit.
		 *
		 * @return configured {@link TileChainImageProvider}
		 */
		public TileChainImageProvider build() {
			final TileExecutors execs = new TileExecutors();
			final List<TileStage> stages = new ArrayList<TileStage>();

			if (mMemoryImageBytes > 0)
				stages.add(new StageMemoryImage(mMemoryImageBytes));
			if (mMemoryBytesBytes > 0)
				stages.add(new StageMemoryBytes(mMemoryBytesBytes));
			if (mDiskPath != null && mDiskBytes > 0)
				stages.add(new StageDisk(mDiskPath, mDiskBytes));

			stages.add(new StageWeb(mTileUrl, mConnectTimeoutMs, mReadTimeoutMs, execs.diskIO));

			final TileChain chain = new TileChain(
					stages,
					new ImageIOTranscoder(execs.decodeCPU),
					execs);

			return new TileChainImageProvider(chain, execs, mStyle, mExt);
		}
	}

	// -----------------------------------------------------------------------------------------
	// Instance
	// -----------------------------------------------------------------------------------------

	private final TileChain    mChain;
	private final TileExecutors mExecs; // null when constructed manually (caller owns shutdown)
	private final String       mStyle;
	private final String       mExt;
	private final ConcurrentHashMap<String, ConcurrentLinkedQueue<CancellationSource>> mInFlight
			= new ConcurrentHashMap<String, ConcurrentLinkedQueue<CancellationSource>>();

	/**
	 * Creates a provider from an already-constructed chain.
	 * The caller is responsible for shutting down any {@link TileExecutors} used by the chain.
	 *
	 * @param chain tile chain to use
	 */
	public TileChainImageProvider(final TileChain chain) {
		this(chain, null, "osm", "png");
	}

	/**
	 * Creates a provider from an already-constructed chain with explicit style and extension.
	 *
	 * @param chain tile chain to use
	 * @param style style prefix used in cache keys
	 * @param ext   file extension used in cache keys
	 */
	public TileChainImageProvider(final TileChain chain, final String style, final String ext) {
		this(chain, null, style, ext);
	}

	private TileChainImageProvider(final TileChain chain, final TileExecutors execs,
			final String style, final String ext) {
		mChain = chain;
		mExecs = execs;
		mStyle = style;
		mExt   = ext;
	}

	// -----------------------------------------------------------------------------------------
	// ITileImageProvider
	// -----------------------------------------------------------------------------------------

	@Override
	public BufferedImage load(final TileInfo info) {
		final TileId id = new TileId(info.tileZ(), info.tileX(), info.tileY(), mStyle, mExt);
		final CancellationSource source = new CancellationSource();
		final CancellationToken ct = source.token();
		register(info.getHash(), source);
		try {
			return mChain.getImage(id, ct).get().value();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (final ExecutionException e) {
			return null;
		} catch (final CancellationException e) {
			return null;
		} finally {
			unregister(info.getHash(), source);
		}
		return null;
	}

	@Override
	public void cancel(final TileInfo info) {
		final ConcurrentLinkedQueue<CancellationSource> active = mInFlight.remove(info.getHash());
		if (active != null)
			for (final CancellationSource source : active)
				source.cancel();
	}

	@Override
	public void free(final TileInfo info, final BufferedImage img) {
		// The chain owns the cache lifecycle; no per-image cleanup required here.
	}

	// -----------------------------------------------------------------------------------------
	// Lifecycle
	// -----------------------------------------------------------------------------------------

	/**
	 * Shuts down the internal thread pools.
	 *
	 * <p>Only has an effect when the provider was created via {@link #build} or
	 * {@link Builder#build()}. When constructed manually the caller owns the
	 * {@link TileExecutors} and must shut them down independently.
	 */
	public void shutdown() {
		if (mExecs != null)
			mExecs.shutdown();
	}

	/**
	 * Returns the underlying {@link TileChain} for advanced use
	 * (e.g. {@code whenIdle()}, {@code whenBackgroundIdle()}).
	 *
	 * @return the chain
	 */
	public TileChain getChain() {
		return mChain;
	}

	// -----------------------------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------------------------

	private void register(final String key, final CancellationSource source) {
		mInFlight.compute(key, (k, active) -> {
			final ConcurrentLinkedQueue<CancellationSource> queue =
					active != null ? active : new ConcurrentLinkedQueue<CancellationSource>();
			queue.add(source);
			return queue;
		});
	}

	private void unregister(final String key, final CancellationSource source) {
		mInFlight.computeIfPresent(key, (k, active) -> {
			active.remove(source);
			return active.isEmpty() ? null : active;
		});
	}
}
