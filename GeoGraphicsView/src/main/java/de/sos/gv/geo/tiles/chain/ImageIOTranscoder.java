package de.sos.gv.geo.tiles.chain;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import javax.imageio.ImageIO;

import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;

/**
 * Converts between the two {@link TilePayload} forms using {@link javax.imageio.ImageIO}.
 *
 * <p>Used internally by {@link TileChain} when a tile is found in a stage that provides
 * {@link TilePayload.Kind#ENCODED} but the promotion target only accepts
 * {@link TilePayload.Kind#IMAGE} (or vice versa). Not called directly in normal usage.
 *
 * <p>All work runs on the {@code decodeExec} executor — always pass {@link TileExecutors#decodeCPU}.
 * Touch this class only if PNG needs to be replaced with a different codec (e.g. WebP).
 */
public class ImageIOTranscoder {
	private final Executor decodeExec;

	public ImageIOTranscoder(final Executor decodeExec){ this.decodeExec = decodeExec; }

	public CompletableFuture<TilePayload.Image> decode(final TilePayload.Encoded encoded, final CancellationToken ct){
		return CompletableFuture.supplyAsync(() -> {
			if (ct!=null && ct.isCancelled()) throw new CancellationException();
			try (ByteArrayInputStream bais = new ByteArrayInputStream(encoded.value())) {
				final BufferedImage bi = ImageIO.read(bais);
				if (bi == null) throw new IllegalStateException("Unsupported image");
				return new TilePayload.Image(bi);
			} catch (final Exception e){ throw new CompletionException(e); }
		}, decodeExec);
	}

	public CompletableFuture<TilePayload.Encoded> encodePng(final TilePayload.Image image, final CancellationToken ct){
		return CompletableFuture.supplyAsync(() -> {
			if (ct!=null && ct.isCancelled()) throw new CancellationException();
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				ImageIO.write(image.value(), "png", baos);
				return new TilePayload.Encoded(baos.toByteArray(), "image/png");
			} catch (final Exception e){ throw new CompletionException(e); }
		}, decodeExec);
	}
}
