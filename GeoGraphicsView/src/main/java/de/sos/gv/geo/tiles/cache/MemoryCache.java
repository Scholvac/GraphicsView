package de.sos.gv.geo.tiles.cache;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.checkerframework.checker.index.qual.NonNegative;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Weigher;

import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileInfo;

public class MemoryCache implements ITileImageProvider {

	private LoadingCache<TileInfo, BufferedImage>		mCache;
	private ITileImageProvider 								mProvider;
	private Executor mExecutor;

	public MemoryCache(final ITileImageProvider provider, final long size, final SizeUnit unit) {
		this(provider, size, unit, null);
	}
	public MemoryCache(final ITileImageProvider provider, final long size, final SizeUnit unit, final Executor executor) {
		mProvider = provider;
		mExecutor = executor;
		setMaxmimumSize(size, unit);
	}

	public void setMaxmimumSize(final long size, final SizeUnit unit) {
		final long maxWeight = unit.toBytes(size);
		Caffeine<TileInfo, BufferedImage> builder = Caffeine.newBuilder()
				.maximumWeight(maxWeight)
				.weigher(new Weigher<TileInfo, BufferedImage>() {
					@Override
					public @NonNegative int weigh(final TileInfo key, final BufferedImage value) {
						return value.getWidth() * value.getHeight() * 4;
					}
				});
		if (mExecutor != null)
			builder = builder.executor(mExecutor);
		else
			builder = builder.executor(Executors.newFixedThreadPool(1));

		final LoadingCache<TileInfo, BufferedImage> cache = builder.build(k -> {
			final CompletableFuture<BufferedImage> future = mProvider.load(k);
			return future.get();
		});
		//		final AsyncLoadingCache<TileInfo, BufferedImage> cache = builder.buildAsync(k -> {
		//			final CompletableFuture<BufferedImage> future = mProvider.load(k);
		//			return future.get();
		//		})


		//		if (mCache != null) { //it this is a rebuild with other size
		//			cache.asMap().putAll(mCache.asMap());
		//		}
		mCache = cache;
	}

	@Override
	public CompletableFuture<BufferedImage> load(final TileInfo info) {
		final CompletableFuture<BufferedImage> cf = new CompletableFuture<>();
		cf.complete(mCache.get(info));
		return cf;
		//		return mCache.get(info);
	}

	@Override
	public void free(final TileInfo info, final BufferedImage img) {

	}

}
