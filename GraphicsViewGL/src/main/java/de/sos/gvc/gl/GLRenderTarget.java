package de.sos.gvc.gl;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.AnimatorBase;

/**
 * On-screen OpenGL render target backed by a {@link GLCanvas}.
 *
 * <p>Two rendering modes are supported (see {@link RenderMode}):</p>
 * <ul>
 *   <li>{@link RenderMode#ON_DEMAND} (default): {@link #requestRepaint()} calls
 *     {@link GLCanvas#repaint()} which enqueues a native paint event.  Multiple
 *     paint requests are coalesced by AWT, similar to the software
 *     {@code JPanelRenderTarget}.  The GL work still runs on the EDT but does
 *     <b>not</b> block the EDT with a synchronous {@code display()} call.</li>
 *   <li>{@link RenderMode#ANIMATED}: a JOGL {@link FPSAnimator} drives rendering
 *     on its own thread.  {@code requestRepaint()} only sets a dirty flag; the
 *     animator renders at most once per tick and only when the scene is dirty.
 *     This frees the EDT entirely from GL work and is the recommended mode for
 *     scenes with many items.</li>
 * </ul>
 *
 * <p>For offscreen rendering use {@link GLOffscreenRenderTarget} directly or
 * the compatibility factory methods on this class.</p>
 */
public class GLRenderTarget extends AbstractGLRenderTarget {

	/** Controls how {@link #requestRepaint()} triggers rendering. */
	public enum RenderMode {
		/** Default. Each repaint is coalesced by AWT and scheduled on the EDT. */
		ON_DEMAND,
		/** A dedicated JOGL animator thread drives rendering at a fixed rate. */
		ANIMATED
	}

	private GLCanvas mCanvas;
	private RenderMode mRenderMode = RenderMode.ON_DEMAND;
	private AnimatorBase mAnimator;
	private int mAnimatorFps = 60;
	private final AtomicBoolean mDirty = new AtomicBoolean(true);

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

	/** Switches the on-screen render mode.  In {@link RenderMode#ANIMATED} a
	 * JOGL animator is started that renders at {@code fps} frames per second
	 * — but only frames for which a repaint has been requested are actually
	 * drawn.  In {@link RenderMode#ON_DEMAND} repaint requests are coalesced
	 * by AWT and dispatched to the EDT (the default).
	 *
	 * @param mode the desired render mode, never {@code null}.
	 * @param fps  target frame rate when {@code mode == ANIMATED}; ignored
	 *             otherwise.  Must be positive.
	 */
	public synchronized void setRenderMode(final RenderMode mode, final int fps) {
		if (mode == null)
			throw new IllegalArgumentException("RenderMode must not be null");
		mAnimatorFps = Math.max(1, fps);
		if (mode == mRenderMode) {
			if (mode == RenderMode.ANIMATED)
				restartAnimator();
			return;
		}
		stopAnimator();
		mRenderMode = mode;
		if (mRenderMode == RenderMode.ANIMATED)
			startAnimator();
	}

	public synchronized void setRenderMode(final RenderMode mode) {
		setRenderMode(mode, mAnimatorFps);
	}

	public RenderMode getRenderMode() {
		return mRenderMode;
	}

	private void startAnimator() {
		if (mCanvas == null || mAnimator != null)
			return;
		final FPSAnimator fpsAnimator = new FPSAnimator(mCanvas, mAnimatorFps, true);
		fpsAnimator.setUpdateFPSFrames(60, null);
		mAnimator = fpsAnimator;
		mAnimator.start();
	}

	private void stopAnimator() {
		if (mAnimator == null)
			return;
		try {
			mAnimator.stop();
		} finally {
			mAnimator = null;
		}
	}

	private void restartAnimator() {
		stopAnimator();
		startAnimator();
	}

	/** Replaces the default {@link Animator} so callers can plug in a
	 * custom render loop (e.g. a shared animator for multiple views). */
	public synchronized void setAnimator(final AnimatorBase animator) {
		stopAnimator();
		mAnimator = animator;
		if (mAnimator != null && mCanvas != null) {
			mAnimator.add(mCanvas);
			mAnimator.start();
			mRenderMode = RenderMode.ANIMATED;
		}
	}

	@Override
	public synchronized void requestRepaint() {
		if (getGraphicsView() == null || mCanvas == null)
			return;
		mDirty.set(true);
		if (mRenderMode == RenderMode.ANIMATED) {
			// Animator will pick up the dirty flag on its next tick.
			return;
		}
		// ON_DEMAND: use the native paint queue. Unlike display(), repaint()
		// is non-blocking and AWT coalesces duplicate requests — matching the
		// behaviour of JPanelRenderTarget and preventing EDT starvation when
		// many repaint requests are issued in quick succession.
		if (mCanvas.isDisplayable() && mCanvas.isShowing())
			mCanvas.repaint();
		else
			mCanvas.repaint();
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
		stopAnimator();
		mDirty.set(false);
		if (mCanvas != null) {
			mCanvas.destroy();
			mCanvas = null;
		}
	}

	void renderOffscreen(final boolean doReadback) {
		throw new UnsupportedOperationException("On-screen GLRenderTarget does not support offscreen rendering");
	}

	private class GLRenderListener implements GLEventListener {
		@Override
		public void init(final GLAutoDrawable drawable) {
			// Disable vsync at the GL level — it is still possible to run vsync'd
			// via a display-timer but blocking inside display() with swap-interval
			// 1 adds considerable EDT jitter. The view-level FPS cap already
			// provides sufficient rate limiting.
			drawable.getGL().setSwapInterval(0);
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
			// In ANIMATED mode: skip rendering when nothing has changed.
			// In ON_DEMAND mode: repaint was explicitly requested, always render.
			if (mRenderMode == RenderMode.ANIMATED && !mDirty.compareAndSet(true, false))
				return;
			if (mRenderMode != RenderMode.ANIMATED)
				mDirty.set(false);
			final GL3 gl = drawable.getGL().getGL3();
			renderCurrentFrame(gl, false);
		}
	}
}
