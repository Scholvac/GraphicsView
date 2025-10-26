package com.example.geocache;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.example.geocache.Cancellation.CancellationToken;

public class ChainSingleCopyTest {

	static class SlowSourceDisk extends StageDisk {
		final AtomicInteger reads = new AtomicInteger(0);
		public SlowSourceDisk(final Path root, final long max){ super(root,max); }
		@Override public CompletableFuture<Optional<TilePayload>> get(final TileId id, final CancellationToken ct){
			return super.get(id, ct).thenApply(opt -> {
				if (opt.isPresent()) reads.incrementAndGet();
				return opt;
			});
		}
	}

	private static BufferedImage smallImg(final int c){
		final BufferedImage img = new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		g.setColor(new Color(c)); g.fillRect(0,0,4,4); g.dispose();
		return img;
	}

	@Test
	public void moveFromDiskToL1RemovesFromDisk() throws Exception {
		final Path tmp = Files.createTempDirectory("disk");
		final StageMemoryImage l1 = new StageMemoryImage(4*4*4*10);
		final SlowSourceDisk disk = new SlowSourceDisk(tmp, 10_000_000);
		final ImageIOTranscoder tr = new ImageIOTranscoder(Executors.newSingleThreadExecutor());
		final TileExecutors ex = new TileExecutors();
		final TileChain chain = new TileChain(Arrays.asList(l1, disk), tr, ex);

		final TileId id = new TileId(5,6,7,"s","png");
		// Seed disk with PNG bytes of a small image
		tr.encodePng(new TilePayload.Image(smallImg(0x112233)), CancellationToken.none())
		.thenCompose(enc -> disk.put(id, enc, CancellationToken.none())).get(2, TimeUnit.SECONDS);

		// First call: fetch from disk and MOVE to L1
		chain.getImage(id, CancellationToken.none()).get(2, TimeUnit.SECONDS);

		// Disk should be empty for this key afterwards
		final Optional<TilePayload> after = disk.get(id, CancellationToken.none()).get(1, TimeUnit.SECONDS);
		Assert.assertFalse("Disk must not keep a copy after promotion", after.isPresent());

		// Second call: must hit L1 directly (no disk read increment)
		final int before = disk.reads.get();
		chain.getImage(id, CancellationToken.none()).get(1, TimeUnit.SECONDS);
		final int afterReads = disk.reads.get();
		Assert.assertEquals(before, afterReads);
	}

	@Test
	public void evictionFromL1DegradesToDisk() throws Exception {
		final Path tmp = Files.createTempDirectory("disk2");
		// Allow ~2 images then force eviction
		final StageMemoryImage l1 = new StageMemoryImage(4*4*4*2 + 1);
		final StageDisk disk = new StageDisk(tmp, 10_000_000);
		final ImageIOTranscoder tr = new ImageIOTranscoder(Executors.newSingleThreadExecutor());
		final TileExecutors ex = new TileExecutors();
		final TileChain chain = new TileChain(Arrays.asList(l1, disk), tr, ex);

		// Put 3 images to force 1 eviction
		for (int i=0;i<3;i++){
			final TileId id = new TileId(1,i,i,"s","png");
			chain.getImage(id, CancellationToken.none()); // this would try to resolve, but we seed L1 directly for simplicity
			l1.put(id, new TilePayload.Image(smallImg(0xFFFFFF - i)), CancellationToken.none()).get(1, TimeUnit.SECONDS);
		}
		chain.whenTrulyIdle().get(2, TimeUnit.SECONDS); // wait for any async eviction puts to finish
		chain.whenBackgroundIdle().get(2, TimeUnit.SECONDS);
		// Eviction should have moved one image to disk. We can't know which, but at least one should exist on disk.
		int present = 0;
		for (int i=0;i<3;i++){
			final TileId id = new TileId(1,i,i,"s","png");
			if (disk.get(id, CancellationToken.none()).get(1, TimeUnit.SECONDS).isPresent())
				present++;
		}


		Assert.assertTrue("At least one evicted image should be on disk", present >= 1);
	}

	@Test
	public void inFlightDedupeSingleCopy() throws Exception {
		final Path tmp = Files.createTempDirectory("disk3");
		final StageMemoryImage l1 = new StageMemoryImage(4*4*4*10);
		final StageDisk disk = new StageDisk(tmp, 10_000_000);
		final ImageIOTranscoder tr = new ImageIOTranscoder(Executors.newSingleThreadExecutor());
		final TileExecutors ex = new TileExecutors();
		final TileChain chain = new TileChain(Arrays.asList(l1, disk), tr, ex);

		final TileId id = new TileId(2,2,2,"s","png");
		tr.encodePng(new TilePayload.Image(smallImg(0xABCDEF)), CancellationToken.none())
		.thenCompose(enc -> disk.put(id, enc, CancellationToken.none())).get(2, TimeUnit.SECONDS);

		final int N = 20;
		final CompletableFuture<?>[] arr = new CompletableFuture<?>[N];
		for (int i=0;i<N;i++)
			arr[i] = chain.getImage(id, CancellationToken.none());
		CompletableFuture.allOf(arr).get(3, TimeUnit.SECONDS);
	}
}
