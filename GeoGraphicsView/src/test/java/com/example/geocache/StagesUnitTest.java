package com.example.geocache;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.example.geocache.Cancellation.CancellationToken;

public class StagesUnitTest {

	@Test
	public void memoryImageEvictsAndCallsListener() throws Exception {
		StageMemoryImage l1 = new StageMemoryImage(4 * 4 * 4); // space for ~1 tiny 2x2 ARGB (16 bytes) times 4 -> ok, we will push to evict
		final int[] evictCount = {0};
		l1.setEvictionListener((key, payload) -> evictCount[0]++);
		CancellationToken ct = CancellationToken.none();
		for (int i=0;i<10;i++){
			BufferedImage img = new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics(); g.setColor(new Color(i)); g.fillRect(0,0,2,2); g.dispose();
			l1.put(new TileId(0,i,i,"s","png"), new TilePayload.Image(img), ct).get(1, TimeUnit.SECONDS);
		}
		Assert.assertTrue("Should have evicted something", evictCount[0] > 0);
	}

	@Test
	public void memoryBytesEvictsAndCallsListener() throws Exception {
		StageMemoryBytes l2 = new StageMemoryBytes(10);
		final int[] evictCount = {0};
		l2.setEvictionListener((key, payload) -> evictCount[0]++);
		CancellationToken ct = CancellationToken.none();
		for (int i=0;i<10;i++){
			byte[] data = new byte[5];
			l2.put(new TileId(0,i,i,"s","png"), new TilePayload.Encoded(data, "image/png"), ct).get(1, TimeUnit.SECONDS);
		}
		Assert.assertTrue("Should have evicted", evictCount[0] > 0);
	}

	@Test
	public void diskPutGetTake() throws Exception {
		Path tmp = Files.createTempDirectory("diskstage");
		StageDisk disk = new StageDisk(tmp, 1_000_000);
		CancellationToken ct = CancellationToken.none();
		TileId id = new TileId(1,2,3,"s","png");
		byte[] data = new byte[]{1,2,3,4};
		disk.put(id, new TilePayload.Encoded(data, "image/png"), ct).get(1, TimeUnit.SECONDS);
		Optional<TilePayload> got = disk.get(id, ct).get(1, TimeUnit.SECONDS);
		Assert.assertTrue(got.isPresent());
		Optional<TilePayload> taken = disk.take(id, ct).get(1, TimeUnit.SECONDS);
		Assert.assertTrue(taken.isPresent());
		Optional<TilePayload> after = disk.get(id, ct).get(1, TimeUnit.SECONDS);
		Assert.assertFalse(after.isPresent());
	}
}
