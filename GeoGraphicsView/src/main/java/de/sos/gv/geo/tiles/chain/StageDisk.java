package de.sos.gv.geo.tiles.chain;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;

/**
 * L3 cache stage — persists encoded tile bytes on disk.
 *
 * <p>Files are stored under {@code root/style/z/x/y.ext} (matching {@link TileId#cacheKey()}).
 * Last-modified timestamps serve as the LRU ordering; the oldest files are deleted when the
 * budget is exceeded ({@code enforceBudget()} runs after every {@code put()}).
 *
 * <p>Unlike the memory stages, this stage does not override {@link TileStage#setEvictionListener}:
 * tiles deleted during budget enforcement are simply dropped (no further demotion possible).
 *
 * <p>Known limitation: {@code enforceBudget()} walks the entire cache tree on every write (O(n)).
 * For caches with millions of files this can be slow — consider reducing write frequency or
 * adding an in-memory LRU index if it becomes a bottleneck.
 */
public class StageDisk implements TileStage {
	private final Path root;
	private final long maxBytes;

	public StageDisk(final Path root, final long maxBytes){ this.root=root; this.maxBytes=maxBytes; }

	@Override public EnumSet<TilePayload.Kind> provides(){ return EnumSet.of(TilePayload.Kind.ENCODED); }
	@Override public EnumSet<TilePayload.Kind> accepts(){ return EnumSet.of(TilePayload.Kind.ENCODED, TilePayload.Kind.IMAGE); }

	@Override
	public CompletableFuture<Optional<TilePayload>> get(final TileId id, final CancellationToken ct){
		return CompletableFuture.supplyAsync(() -> {
			if (ct!=null && ct.isCancelled()) throw new CancellationException();
			final Path p = root.resolve(id.cacheKey());
			if (!Files.isRegularFile(p)) return Optional.<TilePayload>empty();
			try {
				final byte[] b = Files.readAllBytes(p);
				touch(p);
				return Optional.<TilePayload>of(new TilePayload.Encoded(b, id.contentType()));
			} catch (final IOException e){ return Optional.<TilePayload>empty(); }
		});
	}

	@Override
	public CompletableFuture<Optional<TilePayload>> take(final TileId id, final CancellationToken ct){
		return CompletableFuture.supplyAsync(() -> {
			if (ct!=null && ct.isCancelled()) throw new CancellationException();
			final Path p = root.resolve(id.cacheKey());
			if (!Files.isRegularFile(p)) return Optional.<TilePayload>empty();
			try {
				final byte[] b = Files.readAllBytes(p);
				Files.deleteIfExists(p);
				return Optional.<TilePayload>of(new TilePayload.Encoded(b, id.contentType()));
			} catch (final IOException e){ return Optional.<TilePayload>empty(); }
		});
	}

	@Override
	public CompletableFuture<Void> put(final TileId id, final TilePayload payload, final CancellationToken ct){
		return CompletableFuture.runAsync(() -> {
			if (ct!=null && ct.isCancelled())
				throw new CancellationException();
			final Path p = root.resolve(id.cacheKey());
			try {
				Files.createDirectories(p.getParent());
				if (payload.kind()==TilePayload.Kind.ENCODED)
					Files.write(p, ((TilePayload.Encoded)payload).value(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				else {
					final BufferedImage bi = ((TilePayload.Image)payload).value();
					try (OutputStream os = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
						ImageIO.write(bi, id.getExt(), os);
					}
				}
				touch(p);
				enforceBudget();
			} catch (final IOException ignored) {}
		});
	}

	@Override
	public CompletableFuture<Void> invalidate(final TileId id){
		return CompletableFuture.runAsync(() -> {
			try { Files.deleteIfExists(root.resolve(id.cacheKey())); } catch (final IOException ignored) {}
		});
	}

	@Override public long maxSizeBytes(){ return maxBytes; }

	private void touch(final Path p){ try { Files.setLastModifiedTime(p, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis())); } catch (final IOException ignored) {} }

	private void enforceBudget() throws IOException {
		long size = 0L;
		try (java.util.stream.Stream<Path> s = Files.walk(root)) {
			size = s.filter(Files::isRegularFile).mapToLong(pp -> { try { return Files.size(pp); } catch(final IOException e){ return 0L; } }).sum();
		}
		if (size <= maxBytes) return;
		final java.util.List<Path> files = new java.util.ArrayList<Path>();
		try (java.util.stream.Stream<Path> s = Files.walk(root)) { s.filter(Files::isRegularFile).forEach(files::add); }
		java.util.Collections.sort(files, (a, b) -> {
			try {
				final long ma = Files.getLastModifiedTime(a).toMillis();
				final long mb = Files.getLastModifiedTime(b).toMillis();
				return Long.compare(ma, mb);
			} catch (final IOException e){ return 0; }
		});
		for (final Path f : files) {
			final long len = Files.size(f);
			Files.deleteIfExists(f);
			size -= len;
			if (size <= maxBytes) break;
		}
	}
}
