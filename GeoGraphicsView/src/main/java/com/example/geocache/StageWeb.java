package com.example.geocache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.zip.GZIPInputStream;

import com.example.geocache.Cancellation.CancellationToken;

/**
 * Source-only web stage using java.net.URL (Java 8).
 * Provides ENCODED bytes; accepts nothing.
 */
public class StageWeb implements TileStage {
	private final String baseUrl;   // e.g. "https://tile.server/{z}/{x}/{y}.{ext}" or with {style}
	private final int connectTimeoutMs;
	private final int readTimeoutMs;
	private final Executor ioExecutor; // run blocking IO off the caller thread

	public StageWeb(String baseUrl, int connectTimeoutMs, int readTimeoutMs, Executor ioExecutor) {
		this.baseUrl = baseUrl;
		this.connectTimeoutMs = connectTimeoutMs;
		this.readTimeoutMs = readTimeoutMs;
		this.ioExecutor = ioExecutor; // e.g. TileExecutors.diskIO or a dedicated small pool
	}

	@Override
	public EnumSet<TilePayload.Kind> provides() { return EnumSet.of(TilePayload.Kind.ENCODED); }

	@Override
	public EnumSet<TilePayload.Kind> accepts() { return EnumSet.noneOf(TilePayload.Kind.class); }

	@Override
	public CompletableFuture<Optional<TilePayload>> get(final TileId id, final CancellationToken ct) {
		return CompletableFuture.supplyAsync(() -> {
			if (ct != null && ct.isCancelled()) throw new CancellationException();

			String urlStr = baseUrl
					.replace("{style}", id.getStyle())
					.replace("{z}", Integer.toString(id.getZ()))
					.replace("{x}", Integer.toString(id.getX()))
					.replace("{y}", Integer.toString(id.getY()))
					.replace("{ext}", id.getExt());

			HttpURLConnection conn = null;
			try {
				URL url = new URL(urlStr);
				conn = (HttpURLConnection) url.openConnection();
				conn.setInstanceFollowRedirects(true);
				conn.setConnectTimeout(connectTimeoutMs);
				conn.setReadTimeout(readTimeoutMs);
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
				conn.setRequestProperty("Accept-Encoding", "gzip");
				conn.connect();

				int code = conn.getResponseCode();
				if (code != HttpURLConnection.HTTP_OK) {
					// Treat non-200 as miss (no retries here; keep it minimal)
					safeClose(conn);
					return Optional.<TilePayload>empty();
				}

				String contentEncoding = nullSafe(conn.getContentEncoding());
				String contentType = nullSafe(conn.getContentType());
				InputStream in = conn.getInputStream();
				if ("gzip".equalsIgnoreCase(contentEncoding))
					in = new GZIPInputStream(in);

				// Read fully with periodic cancellation checks
				byte[] body = readAllBytesCancelable(in, ct);
				safeClose(in);
				safeClose(conn);

				if (contentType == null || contentType.trim().isEmpty())
					// best-effort guess from extension
					contentType = "png".equalsIgnoreCase(id.getExt()) ? "image/png" : "image/jpeg";

				return Optional.<TilePayload>of(new TilePayload.Encoded(body, contentType));

			} catch (IOException e) {
				safeClose(conn);
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

	private static String nullSafe(String s) { return s == null ? "" : s; }

	private static void safeClose(HttpURLConnection c) {
		if (c != null) c.disconnect();
	}
	private static void safeClose(InputStream in) {
		if (in != null) try { in.close(); } catch (IOException ignore) {}
	}

	private static byte[] readAllBytesCancelable(InputStream in, CancellationToken ct) throws IOException {
		byte[] buf = new byte[8192];
		ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
		int n;
		while ((n = in.read(buf)) != -1) {
			if (ct != null && ct.isCancelled())
				throw new CancellationException();
			out.write(buf, 0, n);
		}
		return out.toByteArray();
	}
}
