package com.example.geocache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import de.sos.gv.geo.tiles.chain.StageDisk;
import de.sos.gv.geo.tiles.chain.StageMemoryBytes;
import de.sos.gv.geo.tiles.chain.StageMemoryImage;
import de.sos.gv.geo.tiles.chain.TileId;
import de.sos.gv.geo.tiles.chain.TilePayload;
import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;

public class StagesUnitTest {

	@Test
	public void memoryImageEvictsAndCallsListener() throws Exception {
		final StageMemoryImage l1 = new StageMemoryImage(4 * 4 * 4); // space for ~1 tiny 2x2 ARGB (16 bytes) times 4 -> ok, we will push to evict
		final int[] evictCount = {0};
		l1.setEvictionListener((key, payload) -> evictCount[0]++);
		final CancellationToken ct = CancellationToken.none();
		for (int i=0;i<10;i++){
			final BufferedImage img = new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = img.createGraphics(); g.setColor(new Color(i)); g.fillRect(0,0,2,2); g.dispose();
			l1.put(new TileId(0,i,i,"s","png"), new TilePayload.Image(img), ct).get(1, TimeUnit.SECONDS);
		}
		assertTrue(evictCount[0] > 0, "Should have evicted something");
	}

	@Test
	public void memoryBytesEvictsAndCallsListener() throws Exception {
		final StageMemoryBytes l2 = new StageMemoryBytes(10);
		final int[] evictCount = {0};
		l2.setEvictionListener((key, payload) -> evictCount[0]++);
		final CancellationToken ct = CancellationToken.none();
		for (int i=0;i<10;i++){
			final byte[] data = new byte[5];
			l2.put(new TileId(0,i,i,"s","png"), new TilePayload.Encoded(data, "image/png"), ct).get(1, TimeUnit.SECONDS);
		}
		assertTrue(evictCount[0] > 0, "Should have evicted");
	}

	@Test
	public void diskPutGetTake() throws Exception {
		final Path tmp = Files.createTempDirectory("diskstage");
		final StageDisk disk = new StageDisk(tmp, 1_000_000);
		final CancellationToken ct = CancellationToken.none();
		final TileId id = new TileId(1,2,3,"s","png");
		final byte[] data = new byte[]{1,2,3,4};
		disk.put(id, new TilePayload.Encoded(data, "image/png"), ct).get(1, TimeUnit.SECONDS);
		final Optional<TilePayload> got = disk.get(id, ct).get(1, TimeUnit.SECONDS);
		assertTrue(got.isPresent());
		final Optional<TilePayload> taken = disk.take(id, ct).get(1, TimeUnit.SECONDS);
		assertTrue(taken.isPresent());
		final Optional<TilePayload> after = disk.get(id, ct).get(1, TimeUnit.SECONDS);
		assertFalse(after.isPresent());
	}
}
