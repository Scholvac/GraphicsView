package de.sos.gv.geo.tiles.chain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;

/**
 * Source-only web stage using java.net.URL (Java 8).
 * Provides ENCODED bytes; accepts nothing.
 */
public class StageWeb implements TileStage {
	private final String baseUrl;   // e.g. "https://tile.server/{z}/{x}/{y}.{ext}" or with {style}
	private final int connectTimeoutMs;
	private final int readTimeoutMs;
	private final Executor ioExecutor; // run blocking IO off the caller thread
	private final int attempts;
	private final String userAgent;

	public StageWeb(String baseUrl, int connectTimeoutMs, int readTimeoutMs, Executor ioExecutor) {
		this(baseUrl, connectTimeoutMs, readTimeoutMs, ioExecutor, 3, "GeoGraphView/1.0");
	}

	public StageWeb(final String baseUrl, final int connectTimeoutMs, final int readTimeoutMs, final Executor ioExecutor, final int attempts, final String userAgent) {
		this.baseUrl = baseUrl;
		this.connectTimeoutMs = connectTimeoutMs;
		this.readTimeoutMs = readTimeoutMs;
		this.ioExecutor = ioExecutor; // e.g. TileExecutors.diskIO or a dedicated small pool
		this.attempts = attempts;
		this.userAgent = userAgent;
	}

	@Override
	public EnumSet<TilePayload.Kind> provides() { return EnumSet.of(TilePayload.Kind.ENCODED); }

	@Override
	public EnumSet<TilePayload.Kind> accepts() { return EnumSet.noneOf(TilePayload.Kind.class); }

	@Override
	public CompletableFuture<Optional<TilePayload>> get(final TileId id, final CancellationToken ct) {
		return CompletableFuture.supplyAsync(() -> {
			if (ct != null && ct.isCancelled()) throw new CancellationException();

			final String urlStr = baseUrl
					.replace("{style}", id.getStyle())
					.replace("{z}", Integer.toString(id.getZ()))
					.replace("{x}", Integer.toString(id.getX()))
					.replace("{y}", Integer.toString(id.getY()))
					.replace("{ext}", id.getExt());

			try {
				final URL url = new URL(urlStr);
				final DownloadResult result = download(url, ct);
				if (result == null)
					return Optional.<TilePayload>empty();
				final String contentType = isBlank(result.contentType)
						? guessContentType(id)
						: result.contentType;
				final byte[] body = result.body;
				return Optional.<TilePayload>of(new TilePayload.Encoded(body, contentType));
			} catch (final IOException e) {
				return Optional.<TilePayload>empty();
			}
		}, ioExecutor);
	}

	@Override
	public CompletableFuture<Void> put(TileId id, TilePayload payload, CancellationToken ct) {
		return CompletableFuture.completedFuture(null); // source-only
	}

	@Override
	public CompletableFuture<Void> invalidate(TileId id) {
		return CompletableFuture.completedFuture(null); // nothing to invalidate
	}

	// ---- helpers ----

	private DownloadResult download(final URL url, final CancellationToken ct) throws IOException {
		int remaining = attempts;
		IOException last = null;
		while (remaining >= 0) {
			try {
				return readOnce(url, ct);
			} catch (final SocketTimeoutException e) {
				last = e;
			} catch (final IOException e) {
				last = e;
			}
			remaining--;
		}
		if (last != null)
			throw last;
		return null;
	}

	private DownloadResult readOnce(final URL url, final CancellationToken ct) throws IOException {
		final URLConnection connection = url.openConnection();
		connection.setConnectTimeout(connectTimeoutMs);
		connection.setReadTimeout(readTimeoutMs);
		connection.setRequestProperty("User-Agent", userAgent);
		final InputStream in = connection.getInputStream();
		try {
			return new DownloadResult(readAllBytesCancelable(in, ct), connection.getContentType());
		} finally {
			safeClose(in);
		}
	}

	private static void safeClose(final InputStream in) {
		if (in != null) try { in.close(); } catch (IOException ignore) {}
	}

	private static byte[] readAllBytesCancelable(final InputStream in, final CancellationToken ct) throws IOException {
		final byte[] buf = new byte[8192];
		final ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
		int n;
		while ((n = in.read(buf)) != -1) {
			if (ct != null && ct.isCancelled())
				throw new CancellationException();
			out.write(buf, 0, n);
		}
		return out.toByteArray();
	}

	private static boolean isBlank(final String str) {
		return str == null || str.trim().isEmpty();
	}

	private static String guessContentType(final TileId id) {
		return "png".equalsIgnoreCase(id.getExt()) ? "image/png" : "image/jpeg";
	}

	private static final class DownloadResult {
		private final byte[] body;
		private final String contentType;

		private DownloadResult(final byte[] body, final String contentType) {
			this.body = body;
			this.contentType = contentType;
		}
	}
}
