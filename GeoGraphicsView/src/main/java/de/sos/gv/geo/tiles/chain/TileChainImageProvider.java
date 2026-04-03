package de.sos.gv.geo.tiles.chain;

import java.awt.image.BufferedImage;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import de.sos.gv.geo.tiles.ICancellableTileImageProvider;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.TileInfo;
import de.sos.gv.geo.tiles.chain.Cancellation.CancellationSource;
import de.sos.gv.geo.tiles.chain.Cancellation.CancellationToken;

/**
 * Adapter between the generic {@link TileChain} and the synchronous
 * {@link ITileImageProvider} used by {@link de.sos.gv.geo.tiles.TileFactory}.
 */
public class TileChainImageProvider implements ITileImageProvider, ICancellableTileImageProvider {

	private final TileChain mChain;
	private final String mStyle;
	private final String mExt;
	private final ConcurrentHashMap<String, ConcurrentLinkedQueue<CancellationSource>> mInFlight = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CancellationSource>>();

	public TileChainImageProvider(final TileChain chain) {
		this(chain, "osm", "png");
	}

	public TileChainImageProvider(final TileChain chain, final String style, final String ext) {
		mChain = chain;
		mStyle = style;
		mExt = ext;
	}

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
		// The chain owns the cache lifecycle, no per-image cleanup required here.
	}

	private void register(final String key, final CancellationSource source) {
		mInFlight.compute(key, (k, active) -> {
			final ConcurrentLinkedQueue<CancellationSource> queue = active != null ? active : new ConcurrentLinkedQueue<CancellationSource>();
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
