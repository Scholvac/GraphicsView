package de.sos.gvc.gt.tiles.cache;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Weigher;


import de.sos.gvc.gt.tiles.ITileDescription;
import de.sos.gvc.gt.tiles.ITileFactory;
import de.sos.gvc.gt.tiles.ITileProvider;
import de.sos.gvc.gt.tiles.LatLonBoundingBox;
import de.sos.gvc.gt.tiles.LazyTileItem;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.log.GVLog;

public class MultiCacheFactory<DESC extends ITileDescription> implements ITileFactory<DESC> {

	private static final Logger				LOG = GVLog.getLogger(MultiCacheFactory.class);
	
	private BufferedImage					mLoadingImage;
	private BufferedImage					mErrorImage;
	
	ITileProvider<DESC>						mTileProvider = null;
	AsyncLoadingCache<DESC, BufferedImage>	mRootCache = null;
	
	/**
	 * Directory to store the downloaded images into. 
	 * if set to null, no file cache is used
	 */
	private File							mFileCacheDirectory; 
	/** allowed size of the file cache directory on the disk (default = 100MB). 
	 * if this value is set to a value < 1 (Byte) no file caching will be done
	 */
	private long							mFileCacheSizeInBytes = 100 * 1024 * 1024;

	/**
	 * Tile loader that loads the buffered image as byte[] from an external source (for example OSM, Google, Bing, WMS, ...)
	 */
	private IByteTileLoader<DESC> 			mTileLoader;

	/**
	 * Maximum size (in bytes) of compressed byte array images shall be hold in memory
	 */
	private long 							mMaxByteCacheSizeInBytes = 20 * 1024 * 1024;

	/**
	 * Maximum size (in bytes) of Images that shall be hold in memory
	 */
	private long 							mMaxImageSizeInBytes = 20 * 1024 * 1024;;
	
	
	public static interface IByteTileLoader<DESC extends ITileDescription> {
		byte[] loadTile(DESC description);
	}
	
	/**
	 * converts an Buffered Image that has been removed from a cache into an compressed byte array and feed the array into an 
	 * unload target (another cache)
	 * @author sschweigert
	 *
	 */
	public static class ImageUnloader<DESC extends ITileDescription> implements RemovalListener<DESC, BufferedImage> {
		private LoadingCache<DESC, byte[]> mUnloadTarget;

		public ImageUnloader(LoadingCache<DESC, byte[]> unloadTarget) {
			mUnloadTarget = unloadTarget;
		}
		@Override
		public synchronized void onRemoval(DESC key, BufferedImage value, RemovalCause cause) {
			byte[] present = mUnloadTarget.getIfPresent(key);
			if (present != null) {
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(value, "PNG", baos);
					mUnloadTarget.put(key, baos.toByteArray());
					baos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * Calculates the weight of an file (its bytes) for the file cache
	 * @author sschweigert
	 *
	 */
	public static class FileWeigher<DESC extends ITileDescription> implements Weigher<DESC, File> {
		@Override
		public int weigh(DESC key, File value) {
			return (int)value.length();
		}
	}
	
	/**
	 * Removes a file from the file cache and deletes it from disk
	 * @author sschweigert
	 *
	 */
	public static class FileRemovalListener<DESC extends ITileDescription> implements RemovalListener<DESC, File> {
		@Override
		public void onRemoval(DESC key, File value, RemovalCause cause) {
			if (value != null && value.exists()) {
				if (value.delete())
					if (LOG.isDebugEnabled()) LOG.debug("Remove File {} from CacheDirectory", value.getAbsolutePath());
			}
		}
	}
	
	
	public static class TileLoader<DESC extends ITileDescription> implements CacheLoader<DESC, byte[]> {
		private Cache<DESC, File> 			mFileLoader;
		private IByteTileLoader<DESC>		mLoader;

		public TileLoader(IByteTileLoader<DESC> loader, Cache<DESC, File> fileCache) {
			mLoader = loader;
			mFileLoader = fileCache;
		}

		@Override
		public byte[] load(DESC key) throws Exception {
			if (mFileLoader != null) {
				File f = mFileLoader.getIfPresent(key);
				if (f != null && f.exists()) {
					if (LOG.isTraceEnabled()) LOG.trace("Load byte[] image from File {}", f.getAbsolutePath());
					return Files.readAllBytes(f.toPath());
				}
			}
			if (LOG.isTraceEnabled()) LOG.trace("Load byte[] image for {} from loader {}", key, mLoader);
			return mLoader.loadTile(key);
		}
	}
	
	public static class DirectoryLoader<DESC extends OSMTileDescription> implements 	CacheLoader<DESC, byte[]>, 
																						RemovalListener<DESC, byte[]>
	{		
		Cache<DESC, File>						mFileCache = null;
		private File 							mBaseDir;
		ITileProvider<DESC>						mProvider; //used to generate the names

		public DirectoryLoader(Cache<DESC, File> fileCache, File dir, ITileProvider<DESC> provider) {
			mBaseDir = dir;
			if (mBaseDir.exists() == false) mBaseDir.mkdirs();
			mFileCache = fileCache;
			mProvider = provider;
		}

		@Override
		public byte[] load(DESC key) throws Exception {
			File f = mFileCache.getIfPresent(key);
			if (f != null && f.exists())
				return Files.readAllBytes(f.toPath());
			return null;
		}

		@Override
		public void onRemoval(DESC key, byte[] value, RemovalCause cause) {
			String fileName = mProvider.getStringDescription(key);
			File f = new File(mBaseDir.getAbsolutePath() + "/" + fileName + ".png");
			if (!f.exists()) {
				try {
					Files.write(f.toPath(), value);
					mFileCache.put(key, f);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}	
	
	/**
	 * Converts an byte[] array into an Buffered image by utilizing the ImageIO class from java. 
	 * @author sschweigert
	 *
	 * @param <DESC>
	 */
	public static class ByteToImageLoader<DESC extends ITileDescription> implements CacheLoader<DESC, BufferedImage> {
		private LoadingCache<DESC, byte[]> mLoaderDelegate;

		public ByteToImageLoader(LoadingCache<DESC, byte[]> loader) {
			mLoaderDelegate = loader;
		}

		@Override
		public BufferedImage load(DESC key) throws Exception {
			byte[] data = mLoaderDelegate.get(key);
			if (data != null) 
				try {
					return ImageIO.read(new ByteArrayInputStream(data));
				}catch(Exception e) {e.printStackTrace();}
			return null;
		}
	}
	
	
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	//									MultiCacheFactory - Implementation									 //
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	public MultiCacheFactory(ITileProvider<DESC> provider, IByteTileLoader<DESC> tileLoader) {
		this(provider, tileLoader, new File(System.getProperty("user.home")+"/.OSMCache"));
	}
	public MultiCacheFactory(ITileProvider<DESC> provider, IByteTileLoader<DESC> tileLoader, long byteSizeInByte, long imgSizeInByte) {
		this(provider, tileLoader, null, -1, byteSizeInByte, imgSizeInByte);
	}
	public MultiCacheFactory(ITileProvider<DESC> provider, IByteTileLoader<DESC> tileLoader, File baseDir) {
		this(provider, tileLoader, baseDir, 100*1024*1024, 20*1024*1024, 20*1024*1024);
	}
	public MultiCacheFactory(ITileProvider<DESC> provider, IByteTileLoader<DESC> tileLoader, File baseDir, long fileSizeInBytes, long byteSizeInByte, long imgSizeInByte) {
		mTileProvider = provider;
		mTileLoader = tileLoader;
		mFileCacheDirectory = baseDir;
		mFileCacheSizeInBytes = fileSizeInBytes;
		mMaxByteCacheSizeInBytes = byteSizeInByte;
		mMaxImageSizeInBytes = imgSizeInByte;
		
		try {
			mLoadingImage = ImageIO.read(getClass().getClassLoader().getResource("loading.png"));
			mErrorImage = ImageIO.read(getClass().getClassLoader().getResource("error.png"));
		} catch (IOException e) {
			e.printStackTrace();
			mLoadingImage = new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR);
		}
		
		rebuildCache();
	}
	
	
	@SuppressWarnings("unchecked")
	private void rebuildCache() {
		Cache<DESC, File> fileCache = null;
		RemovalListener<DESC, byte[]> dirLoader = null;
		if (mFileCacheDirectory != null && mFileCacheSizeInBytes > 1) {
			fileCache = Caffeine.newBuilder()
					.maximumWeight(mFileCacheSizeInBytes) 
					.weigher(new FileWeigher())
					.removalListener(new FileRemovalListener())
					.build();
			fillFileCache(mFileCacheDirectory, fileCache);
			dirLoader = new DirectoryLoader(fileCache, mFileCacheDirectory, mTileProvider);
		}
		//the dirLoader is used therefore we provide an empty implementation to hold the @NotNull requirement of caffeine
		if (dirLoader == null) dirLoader = new RemovalListener<DESC, byte[]>() {
			@Override
			public void onRemoval(DESC key, byte[] value, RemovalCause cause) {
				//do nothing, if not a file loader				
			}
		};
		CacheLoader<DESC, byte[]> externalLoader = new TileLoader<DESC>(mTileLoader, fileCache); //fileCache may be null, thats fine with the TileLoader
		//latest cache version, that will be asked when we do not have the image in one of our caches. 
		LoadingCache<DESC, byte[]> byteCache = Caffeine.newBuilder()
				.removalListener(dirLoader)
				.weigher(new Weigher<DESC, byte[]>() {
					@Override
					public int weigh(DESC key, byte[] value) {
						return value.length;
					}
				})
				.maximumWeight(mMaxByteCacheSizeInBytes)
				.build(externalLoader);
		
		mRootCache = Caffeine.newBuilder()
				.removalListener(new ImageUnloader(byteCache))
				.maximumWeight(mMaxImageSizeInBytes)
				.weigher(new Weigher<DESC, BufferedImage>() {
					@Override
					public int weigh(DESC key, BufferedImage bimg) {
						if (bimg != null)
							return bimg.getRaster().getDataBuffer().getSize() + 100; // + 100 as I do not now if there are some other variables
						return 0;
					}
				})
				.buildAsync(new ByteToImageLoader(byteCache));
	}
	
	
	
	private void fillFileCache(File baseDir, Cache<DESC, File> fileCache) {
		File[] files = baseDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) { return pathname.getName().endsWith(".png"); }
		});
		if (files == null) 
			return ;
		for (File f : files) {
			try {
				DESC desc = mTileProvider.createDescriptionFromString(f.getName().substring(0, f.getName().length()-4));
				fileCache.put(desc, f);
			}catch(Exception e) {}
		}
	}
	@Override
	public Collection<DESC> getTileDescriptions(LatLonBoundingBox area, Rectangle2D viewArea) {
		return mTileProvider.getTileDescriptions(area, viewArea);
	}

	@Override
	public LazyTileItem<DESC> createTileItem(DESC desc) {
		LazyTileItem<DESC> tile = new LazyTileItem<DESC>(desc, mLoadingImage);
		CompletableFuture<BufferedImage> imgFut = mRootCache.get(desc);
		imgFut.thenAcceptAsync(new Consumer<BufferedImage>() {
			@Override
			public void accept(BufferedImage t) {
				if (t != null) {
					tile.setImage(t); //set the image. This will trigger a redraw of the view
//					mRootCache.synchronous().invalidate(desc); //remove the data from the cache to save memory. it will be put back, when we unload the tile
				}else {
					//some error occures, its hard to find the error at this position (there should be no exception) but at least
					//we can show the user that we found an error by replaceing the loading image with the error image
					tile.setImage(mErrorImage); //this will automatically trigger an redraw
				}
			}
		});
		return tile;
	}

	@Override
	public void unloadTileItem(LazyTileItem<DESC> tile) {
		if (tile.getImage() != null)
			mRootCache.synchronous().put(tile.getDescription(), tile.getImage());				
	}

}
