package de.sos.gv.geo.tiles.impl;

import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import de.sos.gv.geo.tiles.TileFactory;

public class Utils {
	public static BufferedImage loadImageOrEmpty(final String resourceName) {
		try {
			return ImageIO.read(TileFactory.class.getClassLoader().getResource(resourceName));
		}catch(final Exception e) {
			return new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR);
		}
	}
	public static class SimpleThreadFactory implements ThreadFactory {
		/** The thread name prefix. */
		private final String threadNamePrefix;
		/** The thread index counter, used for assigning unique thread ids. */
		private static final AtomicInteger threadIdx = new AtomicInteger();
		/** Whether to set daemon mode. */
		private final boolean daemon;
		public SimpleThreadFactory(final String threadNamePrefix, final boolean daemon) {
			this.threadNamePrefix = threadNamePrefix;
			this.daemon = daemon;
		}
		@Override
		public Thread newThread(final Runnable r) {
			final Thread t = new Thread(r, threadNamePrefix + threadIdx.getAndIncrement());
			t.setDaemon(daemon);
			return t;
		}
	}
}
