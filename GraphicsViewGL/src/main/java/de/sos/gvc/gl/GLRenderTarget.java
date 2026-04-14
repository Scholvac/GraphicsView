package de.sos.gvc.gl;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.awt.GLCanvas;

/**
 * On-screen OpenGL render target backed by a {@link GLCanvas}.
 *
 * <p>For offscreen rendering use {@link GLOffscreenRenderTarget} directly or
 * the compatibility factory methods on this class.</p>
 */
public class GLRenderTarget extends AbstractGLRenderTarget {

	private GLCanvas mCanvas;
	private final AtomicBoolean mHeadfulDisplayQueued = new AtomicBoolean(false);
	private final AtomicBoolean mHeadfulDisplayRequested = new AtomicBoolean(false);

	public GLRenderTarget(final int width, final int height) {
		this(width, height, DEFAULT_ATLAS_SIZE, DEFAULT_ATLAS_SIZE);
	}

	public GLRenderTarget(final int width, final int height, final int atlasSize) {
		this(width, height, atlasSize, atlasSize);
	}

	public GLRenderTarget(final int width, final int height, final int atlasWidth, final int atlasHeight) {
		this(width, height, atlasWidth, atlasHeight, true);
	}

	protected GLRenderTarget(final int width, final int height, final int atlasWidth, final int atlasHeight,
			final boolean createOnscreenSurface) {
		super(width, height, atlasWidth, atlasHeight);
		if (createOnscreenSurface)
			initializeCanvas();
	}

	public static GLRenderTarget createOffscreen(final int width, final int height) {
		return new GLOffscreenRenderTarget(width, height);
	}

	public static GLRenderTarget createOffscreen(final int width, final int height, final int atlasSize) {
		return new GLOffscreenRenderTarget(width, height, atlasSize);
	}

	public static GLRenderTarget createOffscreen(final int width, final int height,
			final int atlasWidth, final int atlasHeight) {
		return new GLOffscreenRenderTarget(width, height, atlasWidth, atlasHeight);
	}

	private void initializeCanvas() {
		final GLCapabilities caps = new GLCapabilities(mProfile);
		caps.setAlphaBits(8);
		caps.setHardwareAccelerated(true);
		caps.setDoubleBuffered(true);

		mCanvas = new GLCanvas(caps);
		mCanvas.setPreferredSize(new Dimension(super.getWidth(), super.getHeight()));
		mCanvas.addGLEventListener(new GLRenderListener());
		mCanvas.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				updateSurfaceSize(mCanvas.getSurfaceWidth(), mCanvas.getSurfaceHeight());
			}
		});
	}

	@Override
	public synchronized void requestRepaint() {
		if (getGraphicsView() == null || mCanvas == null)
			return;
		queueHeadfulDisplay();
	}

	@Override
	public int getWidth() {
		if (mCanvas != null) {
			final int width = mCanvas.getSurfaceWidth();
			if (width > 0)
				return width;
		}
		return super.getWidth();
	}

	@Override
	public int getHeight() {
		if (mCanvas != null) {
			final int height = mCanvas.getSurfaceHeight();
			if (height > 0)
				return height;
		}
		return super.getHeight();
	}

	@Override
	public Component getComponent() {
		return mCanvas;
	}

	@Override
	protected void disposeSurface() {
		mHeadfulDisplayRequested.set(false);
		mHeadfulDisplayQueued.set(false);
		if (mCanvas != null) {
			mCanvas.destroy();
			mCanvas = null;
		}
	}

	void renderOffscreen(final boolean doReadback) {
		throw new UnsupportedOperationException("On-screen GLRenderTarget does not support offscreen rendering");
	}

	private void queueHeadfulDisplay() {
		mHeadfulDisplayRequested.set(true);
		if (!mHeadfulDisplayQueued.compareAndSet(false, true))
			return;
		EventQueue.invokeLater(this::drainHeadfulDisplayQueue);
	}

	private void drainHeadfulDisplayQueue() {
		try {
			mHeadfulDisplayRequested.set(false);
			final GLCanvas canvas = mCanvas;
			if (canvas == null)
				return;
			if (!canvas.isDisplayable() || !canvas.isShowing()) {
				canvas.repaint();
				return;
			}
			canvas.display();
		} finally {
			mHeadfulDisplayQueued.set(false);
			if (mHeadfulDisplayRequested.get())
				queueHeadfulDisplay();
		}
	}

	private class GLRenderListener implements GLEventListener {
		@Override
		public void init(final GLAutoDrawable drawable) {
			// no-op
		}

		@Override
		public void dispose(final GLAutoDrawable drawable) {
			destroyAllTextures(drawable.getGL().getGL3());
		}

		@Override
		public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
			updateSurfaceSize(width, height);
		}

		@Override
		public void display(final GLAutoDrawable drawable) {
			if (getGraphicsView() == null)
				return;
			final GL3 gl = drawable.getGL().getGL3();
			renderCurrentFrame(gl, false);
		}
	}
}
