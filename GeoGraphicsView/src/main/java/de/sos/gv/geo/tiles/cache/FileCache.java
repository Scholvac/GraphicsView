package de.sos.gv.geo.tiles.cache;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.imageio.ImageIO;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Weigher;

import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileInfo;

public class FileCache implements ITileImageProvider {

	private LoadingCache<TileInfo, File>					mCache;
	private ITileImageProvider 						mProvider;
	private Executor 								mExecutor;
	private File 									mDirectory;

	public FileCache(final ITileImageProvider provider, final File directory, final long size, final SizeUnit unit) {
		this(provider, directory, size, unit, null);
	}
	public FileCache(final ITileImageProvider provider, final File directory, final long size, final SizeUnit unit, final Executor executor) {
		mProvider = provider;
		mDirectory = directory;
		mExecutor = executor;
		setMaxmimumSize(size, unit);
	}

	public void setMaxmimumSize(final long size, final SizeUnit unit) {
		final long maxWeight = unit.toBytes(size);
		Caffeine<TileInfo, File> builder = Caffeine.newBuilder()
				.maximumWeight(maxWeight)
				.weigher(new Weigher<TileInfo, File>() {
					@Override
					public @NonNegative int weigh(final TileInfo key, final File value) {
						return (@NonNegative int) value.length();
					}
				})
				.removalListener(new RemovalListener<TileInfo, File>() {
					@Override
					public void onRemoval(@Nullable final TileInfo key, @Nullable final File value, final RemovalCause cause) {
						remove(key, value, cause);
					}
				});
		if (mExecutor != null)
			builder = builder.executor(mExecutor);

		final LoadingCache<TileInfo, File> cache = builder.build(k -> {
			final File file = getFile(k);
			if (file.exists())
				return file;
			try {
				return mProvider.load(k).thenApply(img -> {
					if (img != null)
						return saveToFile(file, img);
					return (File)null;
				}).get();
			} catch (final InterruptedException | ExecutionException e) {
				e.printStackTrace();
				return null;
			}
		});

		if (mCache != null) { //it this is a rebuild with other size
			//			cache.asMap().putAll(mCache.asMap());
		}
		if (mDirectory.exists()) {
			//fill the cache with existing files, to get the size calculation correct.
			final File[] files = mDirectory.listFiles();
			Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
			for (final File f : mDirectory.listFiles()) {
				final String fn = f.getName();
				final TileInfo ti = TileInfo.fromHash(fn.substring(0, fn.lastIndexOf('.')));
				final CompletableFuture<File> cff = new CompletableFuture<>();
				cff.complete(f);
				cache.put(ti, f);
			}
		}
		mCache = cache;
	}

	private @Nullable BufferedImage loadFromFile(final File file) {
		try {
			return ImageIO.read(file);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private @Nullable File saveToFile(final File file, final BufferedImage bufferedImage) {
		try {
			if (file.getParentFile().exists() == false)
				file.getParentFile().mkdirs();
			ImageIO.write(bufferedImage, "PNG", file);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return file;
	}
	private File getFile(final TileInfo info) {
		return new File(mDirectory, info.getHash() + ".png");
	}
	protected void remove(@Nullable final TileInfo key, final @Nullable File value, final RemovalCause cause) {
		if (value != null && value.exists()) {
			value.delete();
		}
	}
	@Override
	public CompletableFuture<BufferedImage> load(final TileInfo info) {
		final CompletableFuture<File> file = new CompletableFuture<>();
		file.complete(mCache.get(info));
		return file.thenApply(this::loadFromFile);
	}

	@Override
	public void free(final TileInfo info, final BufferedImage img) {
	}

}
