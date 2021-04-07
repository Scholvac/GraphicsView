package de.sos.gvc.gt.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Weigher;

import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileDownloader;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;

public class CaffeineCacheTest {

	public static class FileWeigher implements Weigher<OSMTileDescription, File> {
		@Override
		public int weigh(OSMTileDescription key, File value) {
			return (int)value.length();
		}
	}
	public static class FileRemovalListener implements RemovalListener<OSMTileDescription, File> {
		@Override
		public void onRemoval(OSMTileDescription key, File value, RemovalCause cause) {
			if (value != null && value.exists())
				value.delete();
		}
	}
	public static class DirectoryLoader implements 	CacheLoader<OSMTileDescription, byte[]>, 
													RemovalListener<OSMTileDescription, byte[]>
	{		
		Cache<OSMTileDescription, File>			mFileCache = null;
		private File 							mBaseDir;
		
		public DirectoryLoader(Cache<OSMTileDescription, File> fileCache, File dir) {
			mBaseDir = dir;
			if (mBaseDir.exists() == false) mBaseDir.mkdirs();
			mFileCache = fileCache;
		}

		@Override
		public byte[] load(OSMTileDescription key) throws Exception {
			File f = mFileCache.getIfPresent(key);
//			File f = new File(mBaseDir.getAbsolutePath() + "/" + key.getZoom() + "_" + key.getTileX() + "_" + key.getTileY() + ".png");
			if (f != null && f.exists())
				return Files.readAllBytes(f.toPath());
			return null;
		}

		@Override
		public void onRemoval(OSMTileDescription key, byte[] value, RemovalCause cause) {
			File f = new File(mBaseDir.getAbsolutePath() + "/" + key.getZoom() + "_" + key.getTileX() + "_" + key.getTileY() + ".png");
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
	
	public static class OSMLoader implements CacheLoader<OSMTileDescription, byte[]> {

		private Cache<OSMTileDescription, File> mFileLoader;

		public OSMLoader(Cache<OSMTileDescription, File> fileCache) {
			mFileLoader = fileCache;
		}

		@Override
		public byte[] load(OSMTileDescription key) throws Exception {
			File f = mFileLoader.getIfPresent(key);
			if (f != null && f.exists()) {
				return Files.readAllBytes(f.toPath());
			}
			System.out.println("Loading: " + key);
			return new OSMTileDownloader().getCompressedTileImage(key);
		}
	}
	
	public static class ImageUnloader implements RemovalListener<OSMTileDescription, BufferedImage> {
		private LoadingCache<OSMTileDescription, byte[]> mUnloadTarget;

		public ImageUnloader(LoadingCache<OSMTileDescription, byte[]> unloadTarget) {
			mUnloadTarget = unloadTarget;
		}
		@Override
		public synchronized void onRemoval(OSMTileDescription key, BufferedImage value, RemovalCause cause) {
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
	
	public static class ByteToImageLoader implements CacheLoader<OSMTileDescription, BufferedImage> {
		private LoadingCache<OSMTileDescription, byte[]> mLoaderDelegate;

		public ByteToImageLoader(LoadingCache<OSMTileDescription, byte[]> loader) {
			mLoaderDelegate = loader;
		}

		@Override
		public BufferedImage load(OSMTileDescription key) throws Exception {
			byte[] data = mLoaderDelegate.get(key);
			if (data != null) 
				try {
					return ImageIO.read(new ByteArrayInputStream(data));
				}catch(Exception e) {e.printStackTrace();}
			return null;
		}
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		LatLonPoint ul = new LatLonPoint.Double(53.52589, 8.62223);
		LatLonPoint lr = new LatLonPoint.Double(53.51660, 8.65886);
		
		int zoom = 17;		
		int[][] tileAdresses = OSMTileFactory.getTileNumbers(ul, lr, zoom);
		List<OSMTileDescription> tiles = new ArrayList<>();
		for (int i = 0; i < tileAdresses.length; i++) tiles.add(new OSMTileDescription(tileAdresses[i][0], tileAdresses[i][1], tileAdresses[i][2]));
		
		
		File baseDir = new File(System.getProperty("user.home") + "/.OSMCache");
		Cache<OSMTileDescription, File> fileCache = Caffeine.newBuilder()
				.maximumWeight(100 * 1024 * 1024)
				.weigher(new FileWeigher())
				.removalListener(new FileRemovalListener())
				.build();
		fillFileCache(baseDir, fileCache);
				
		RemovalListener<OSMTileDescription, byte[]> dirLoader = new DirectoryLoader(fileCache, baseDir);
		CacheLoader<OSMTileDescription, byte[]> webLoader = new OSMLoader(fileCache);
		LoadingCache<OSMTileDescription, byte[]> byteCache = Caffeine.newBuilder()
				.removalListener(dirLoader)
				.maximumSize(0)
				.build(webLoader);
		AsyncLoadingCache<OSMTileDescription, BufferedImage> imgCache = Caffeine.newBuilder()
			.removalListener(new ImageUnloader(byteCache))
			.maximumSize(50)
			.buildAsync(new ByteToImageLoader(byteCache));
			
		for (int i = 0; i <  1000; i++) {
			CompletableFuture<Map<OSMTileDescription, BufferedImage>> cachevalues = imgCache.getAll(tiles);
			Map<OSMTileDescription, BufferedImage> values = cachevalues.get();
		}
	}

	private static void fillFileCache(File baseDir, Cache<OSMTileDescription, File> fileCache) {
		File[] files = baseDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) { return pathname.getName().endsWith(".png"); }
		});
		for (File f : files) {
			try {
				String[] n = f.getName().substring(0, f.getName().length()-4).split("_");
				int z = Integer.parseInt(n[0]), x = Integer.parseInt(n[1]), y = Integer.parseInt(n[2]);
				fileCache.put(new OSMTileDescription(x, y, z), f);
			}catch(Exception e) {}
		}
	}
}
