package com.example.geocache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gv.geo.tiles.cache.MemoryCache;
import de.sos.gv.geo.tiles.chain.ImageIOTranscoder;
import de.sos.gv.geo.tiles.chain.StageMemoryImage;
import de.sos.gv.geo.tiles.chain.TileChain;
import de.sos.gv.geo.tiles.chain.TileChainImageProvider;
import de.sos.gv.geo.tiles.chain.TileExecutors;
import de.sos.gv.geo.tiles.chain.TileId;
import de.sos.gv.geo.tiles.chain.TilePayload;
import de.sos.gv.geo.tiles.chain.TileStage;
import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;

public class CacheComparisonTests {

	private static BufferedImage createImage(final int rgb) {
		final BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		g.setColor(new Color(rgb, true));
		g.fillRect(0, 0, 4, 4);
		g.dispose();
		return img;
	}

	private static final class SlowLegacyProvider implements ITileImageProvider {
		private final AtomicInteger loads = new AtomicInteger();
		private final long delayMs;
		private final BufferedImage image = createImage(0xFF33AA55);

		private SlowLegacyProvider(final long delayMs) {
			this.delayMs = delayMs;
		}

		@Override
		public synchronized BufferedImage load(final TileInfo info) {
			loads.incrementAndGet();
			sleep(delayMs);
			return image;
		}

		@Override
		public void free(final TileInfo info, final BufferedImage img) {
		}
	}

	private static final class SlowImageSourceStage implements TileStage {
		private final AtomicInteger loads = new AtomicInteger();
		private final long delayMs;
		private final BufferedImage image = createImage(0xFF3355AA);
		private final ExecutorService executor = Executors.newFixedThreadPool(4);

		private SlowImageSourceStage(final long delayMs) {
			this.delayMs = delayMs;
		}

		@Override
		public EnumSet<TilePayload.Kind> provides() {
			return EnumSet.of(TilePayload.Kind.IMAGE);
		}

		@Override
		public EnumSet<TilePayload.Kind> accepts() {
			return EnumSet.noneOf(TilePayload.Kind.class);
		}

		@Override
		public CompletableFuture<Optional<TilePayload>> get(final TileId id, final CancellationToken ct) {
			return CompletableFuture.supplyAsync(() -> {
				loads.incrementAndGet();
				sleep(delayMs);
				return Optional.<TilePayload>of(new TilePayload.Image(image));
			}, executor);
		}

		@Override
		public CompletableFuture<Void> put(final TileId id, final TilePayload payload, final CancellationToken ct) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Void> invalidate(final TileId id) {
			return CompletableFuture.completedFuture(null);
		}

		private void shutdown() {
			executor.shutdownNow();
		}
	}

	private static final class BlockingLegacyProvider implements ITileImageProvider {
		private final CountDownLatch started = new CountDownLatch(1);
		private final AtomicInteger completed = new AtomicInteger();
		private final BufferedImage image = createImage(0xFFAA6633);

		@Override
		public BufferedImage load(final TileInfo info) {
			started.countDown();
			sleep(250);
			completed.incrementAndGet();
			return image;
		}

		@Override
		public void free(final TileInfo info, final BufferedImage img) {
		}
	}

	private static final class CancellableSlowStage implements TileStage {
		private final CountDownLatch started = new CountDownLatch(1);
		private final AtomicInteger completed = new AtomicInteger();
		private final AtomicInteger cancelled = new AtomicInteger();
		private final BufferedImage image = createImage(0xFFAA3366);
		private final ExecutorService executor = Executors.newSingleThreadExecutor();

		@Override
		public EnumSet<TilePayload.Kind> provides() {
			return EnumSet.of(TilePayload.Kind.IMAGE);
		}

		@Override
		public EnumSet<TilePayload.Kind> accepts() {
			return EnumSet.noneOf(TilePayload.Kind.class);
		}

		@Override
		public CompletableFuture<Optional<TilePayload>> get(final TileId id, final CancellationToken ct) {
			return CompletableFuture.supplyAsync(() -> {
				started.countDown();
				for (int i = 0; i < 50; i++) {
					if (ct != null && ct.isCancelled()) {
						cancelled.incrementAndGet();
						throw new java.util.concurrent.CancellationException();
					}
					sleep(10);
				}
				completed.incrementAndGet();
				return Optional.<TilePayload>of(new TilePayload.Image(image));
			}, executor);
		}

		@Override
		public CompletableFuture<Void> put(final TileId id, final TilePayload payload, final CancellationToken ct) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Void> invalidate(final TileId id) {
			return CompletableFuture.completedFuture(null);
		}

		private void shutdown() {
			executor.shutdownNow();
		}
	}

	@Test
	public void chainAvoidsRedundantParallelDownloadsBetterThanLegacyCache() throws Exception {
		final int requests = 6;
		final long delayMs = 120;
		final TileInfo info = new TileInfo(new int[] { 1, 2, 3 });

		final SlowLegacyProvider legacySource = new SlowLegacyProvider(delayMs);
		final ITileImageProvider legacyCache = new MemoryCache(legacySource, 10, SizeUnit.MegaByte);

		final SlowImageSourceStage chainSource = new SlowImageSourceStage(delayMs);
		final TileExecutors execs = new TileExecutors();
		final TileChain chain = new TileChain(
				Arrays.asList(new StageMemoryImage(SizeUnit.MegaByte.toBytes(10)), chainSource),
				new ImageIOTranscoder(execs.decodeCPU),
				execs);
		final TileId id = new TileId(info.tileZ(), info.tileX(), info.tileY(), "osm", "png");

		final long legacyElapsed = runParallel(requests, () -> legacyCache.load(info));
		final long chainElapsed = runParallel(requests, () -> {
			try {
				return chain.getImage(id, CancellationToken.none()).get(2, TimeUnit.SECONDS).value();
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		});

		assertTrue(legacySource.loads.get() > 1, "Legacy cache should trigger redundant source loads");
		assertEquals(1, chainSource.loads.get(), "Chain should fetch the source tile only once");
		assertTrue(chainElapsed < legacyElapsed, "Chain should complete faster than legacy duplicate loads");

		chainSource.shutdown();
		execs.shutdown();
	}

	@Test
	public void chainCanAbortObsoleteRunningDownloadsWhereLegacyCacheCannot() throws Exception {
		final BlockingLegacyProvider legacyProvider = new BlockingLegacyProvider();
		final TileFactory legacyFactory = new TileFactory(legacyProvider, "LegacyCmp", 1);
		final int[] tile = new int[] { 5, 6, 7 };

		final de.sos.gv.geo.tiles.TileItem legacyItem = legacyFactory.load(tile);
		assertTrue(legacyProvider.started.await(1, TimeUnit.SECONDS), "Legacy download should have started");
		legacyFactory.release(legacyItem);
		Thread.sleep(350);
		assertEquals(1, legacyProvider.completed.get(), "Legacy cache cannot abort a running download");

		final CancellableSlowStage cancellableStage = new CancellableSlowStage();
		final TileExecutors execs = new TileExecutors();
		final TileChain chain = new TileChain(
				Arrays.asList(new StageMemoryImage(SizeUnit.MegaByte.toBytes(10)), cancellableStage),
				new ImageIOTranscoder(execs.decodeCPU),
				execs);
		final TileFactory chainFactory = new TileFactory(new TileChainImageProvider(chain), "ChainCmp", 1);

		final de.sos.gv.geo.tiles.TileItem chainItem = chainFactory.load(tile);
		assertTrue(cancellableStage.started.await(1, TimeUnit.SECONDS), "Chain download should have started");
		chainFactory.release(chainItem);
		Thread.sleep(350);

		assertEquals( 1, cancellableStage.cancelled.get(), "Chain should cancel the obsolete running download");
		assertEquals( 0, cancellableStage.completed.get(), "Cancelled chain download must not complete");

		cancellableStage.shutdown();
		execs.shutdown();
	}

	private static long runParallel(final int count, final java.util.concurrent.Callable<BufferedImage> task) throws Exception {
		final ExecutorService executor = Executors.newFixedThreadPool(count);
		final CountDownLatch ready = new CountDownLatch(count);
		final CountDownLatch start = new CountDownLatch(1);
		final CompletableFuture<?>[] futures = new CompletableFuture<?>[count];
		for (int i = 0; i < count; i++) {
			futures[i] = CompletableFuture.supplyAsync(() -> {
				ready.countDown();
				await(ready);
				await(start);
				try {
					return task.call();
				} catch (final Exception e) {
					throw new RuntimeException(e);
				}
			}, executor);
		}
		final long startTime = System.nanoTime();
		start.countDown();
		CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);
		executor.shutdownNow();
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
	}

	private static void await(final CountDownLatch latch) {
		try {
			latch.await(2, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

	private static void sleep(final long millis) {
		try {
			Thread.sleep(millis);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
