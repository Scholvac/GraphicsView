package de.sos.gvc.gl;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GL3;

import de.sos.gvc.log.GVLog;

/**
 * Offscreen OpenGL render target backed by a JOGL offscreen drawable.
 */
public class GLOffscreenRenderTarget extends GLRenderTarget {

	private static final Logger LOG = GVLog.getLogger(GLOffscreenRenderTarget.class);

	private GLOffscreenAutoDrawable mOffscreen;
	private BufferedImage mResultImage;

	public GLOffscreenRenderTarget(final int width, final int height) {
		this(width, height, DEFAULT_ATLAS_SIZE, DEFAULT_ATLAS_SIZE);
	}

	public GLOffscreenRenderTarget(final int width, final int height, final int atlasSize) {
		this(width, height, atlasSize, atlasSize);
	}

	public GLOffscreenRenderTarget(final int width, final int height, final int atlasWidth, final int atlasHeight) {
		super(width, height, atlasWidth, atlasHeight, false);
		initializeOffscreenDrawable();
	}

	@Override
	public synchronized void requestRepaint() {
		if (getGraphicsView() == null)
			return;
		renderOffscreen(true);
	}

	@Override
	public BufferedImage getResultImage() {
		return mResultImage;
	}

	@Override
	protected void storeReadback(final BufferedImage image) {
		mResultImage = image;
	}

	@Override
	protected void disposeSurface() {
		if (mOffscreen == null)
			return;

		final GLContext ctx = mOffscreen.getContext();
		if (ctx.makeCurrent() != GLContext.CONTEXT_NOT_CURRENT) {
			try {
				final GL3 gl = ctx.getGL().getGL3();
				destroyAllTextures(gl);
			} finally {
				ctx.release();
			}
		}
		mOffscreen.destroy();
		mOffscreen = null;
	}

	@Override
	void renderOffscreen(final boolean doReadback) {
		if (mOffscreen == null) {
			System.err.println("[DIAG] renderOffscreen: mOffscreen==null");
			return;
		}

		final GLContext ctx = mOffscreen.getContext();
		final int mc = ctx.makeCurrent();
		System.err.println("[DIAG] renderOffscreen: makeCurrent=" + mc + ", view=" + getGraphicsView());
		if (mc == GLContext.CONTEXT_NOT_CURRENT) {
			LOG.debug("Could not make offscreen GL context current, skipping repaint");
			return;
		}

		try {
			final GL gl0 = ctx.getGL();
			if (gl0 == null) {
				System.err.println("[DIAG] renderOffscreen: gl0==null");
				return;
			}
			System.err.println("[DIAG] renderOffscreen: calling renderCurrentFrame");
			renderCurrentFrame(gl0.getGL3(), doReadback);
			System.err.println("[DIAG] renderOffscreen: back from renderCurrentFrame");
		} catch (final Exception e) {
			LOG.error("Offscreen GL render failed: {}", e.getMessage(), e);
		} finally {
			ctx.release();
		}
	}

	private void initializeOffscreenDrawable() {
		final GLCapabilities caps = new GLCapabilities(mProfile);
		caps.setOnscreen(false);
		caps.setAlphaBits(8);
		caps.setHardwareAccelerated(true);
		caps.setDoubleBuffered(false);

		final GLDrawableFactory factory = GLDrawableFactory.getFactory(mProfile);
		mOffscreen = factory.createOffscreenAutoDrawable(factory.getDefaultDevice(), caps, null, getWidth(), getHeight());
		mOffscreen.display();
	}
}
