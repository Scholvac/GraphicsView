package de.sos.gvc.gl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.IDrawable;
import de.sos.gvc.log.GVLog;
import de.sos.gvc.rt.IRenderTarget;
import de.sos.gvc.styles.DrawableStyle;

/**
 * OpenGL-backed {@link IRenderTarget} that renders {@link GraphicsItem}s as
 * textured sprites via JOGL.
 *
 * <p>Each item is rendered once into a {@link BufferedImage} (sprite) using
 * its {@link IDrawable}, uploaded as an OpenGL texture, and then composited
 * as a textured quad.  Item positioning and orientation are handled by
 * applying the item's world transform and the view transform in GL.</p>
 *
 * <h3>Headful mode (default)</h3>
 * <p>Provides a {@link GLJPanel} Swing component for on-screen rendering.
 * The component can be embedded in any Swing layout.  Mouse events, resizing
 * and all standard GraphicsView interactions work unchanged.</p>
 * <pre>{@code
 * GLRenderTarget rt = new GLRenderTarget(800, 600);
 * GraphicsView view = new GraphicsView(scene, rt);
 * frame.add(view.getComponent());
 * }</pre>
 *
 * <h3>Offscreen mode</h3>
 * <p>For headless rendering and testing, create via the factory method:</p>
 * <pre>{@code
 * GLRenderTarget rt = GLRenderTarget.createOffscreen(800, 600);
 * GraphicsView view = new GraphicsView(scene, rt);
 * rt.requestRepaint();
 * BufferedImage img = rt.getResultImage();
 * }</pre>
 *
 * @author GraphicsViewGL
 */
public class GLRenderTarget implements IRenderTarget {

	private static final Logger LOG = GVLog.getLogger(GLRenderTarget.class);

	/** Extra pixels around each sprite to avoid clipping strokes. */
	private static final int SPRITE_PADDING = 4;
	/** Maximum sprite texture dimension (pixels). */
	private static final int MAX_SPRITE_SIZE = 2048;

	// ---- core state ----
	private GraphicsView				mView;
	private int							mWidth;
	private int							mHeight;
	private Rectangle					mRectangle;
	private Color						mClearColor = Color.BLACK;

	// ---- GL resources ----
	private final GLProfile				mProfile;
	private GLJPanel					mPanel;       // headful
	private GLOffscreenAutoDrawable		mOffscreen;   // offscreen

	// ---- sprite cache ----
	private final Map<GraphicsItem, SpriteEntry> mSpriteCache = new HashMap<>();
	/** Textures pending destruction (from invalidation between frames). */
	private final List<Texture>			mStaleTextures = new ArrayList<>();
	/** View scale at which sprites were rasterized. */
	private double						mCachedScaleX = Double.NaN;
	private double						mCachedScaleY = Double.NaN;

	// ---- result image (offscreen readback) ----
	private BufferedImage				mResultImage;

	// -----------------------------------------------------------------------
	//  Inner types
	// -----------------------------------------------------------------------

	/** Cached sprite: GL texture + quad bounds in item-local coordinates. */
	private static final class SpriteEntry {
		final Texture				texture;
		final Rectangle2D.Double	localBounds;

		SpriteEntry(final Texture texture, final Rectangle2D.Double localBounds) {
			this.texture = texture;
			this.localBounds = localBounds;
		}
	}

	// -----------------------------------------------------------------------
	//  Construction
	// -----------------------------------------------------------------------

	/**
	 * Creates a <b>headful</b> GL render target backed by a {@link GLJPanel}.
	 * The panel is available via {@link #getComponent()} and can be embedded
	 * in any Swing container.
	 *
	 * @param width  initial preferred width
	 * @param height initial preferred height
	 */
	public GLRenderTarget(final int width, final int height) {
		mWidth  = Math.max(1, width);
		mHeight = Math.max(1, height);
		mProfile = GLProfile.get(GLProfile.GL2);

		final GLCapabilities caps = new GLCapabilities(mProfile);
		caps.setAlphaBits(8);
		caps.setHardwareAccelerated(true);

		mPanel = new GLJPanel(caps);
		mPanel.setPreferredSize(new Dimension(mWidth, mHeight));
		mPanel.addGLEventListener(new GLRenderListener());
		mPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				mWidth  = Math.max(1, mPanel.getSurfaceWidth());
				mHeight = Math.max(1, mPanel.getSurfaceHeight());
				mRectangle = null;
				markAllSpritesStale();
			}
		});
	}

	/**
	 * Private constructor for offscreen mode.
	 */
	private GLRenderTarget(final int width, final int height, @SuppressWarnings("unused") final boolean offscreen) {
		mWidth  = Math.max(1, width);
		mHeight = Math.max(1, height);
		mProfile = GLProfile.get(GLProfile.GL2);

		final GLCapabilities caps = new GLCapabilities(mProfile);
		caps.setOnscreen(false);
		caps.setAlphaBits(8);
		caps.setHardwareAccelerated(true);
		caps.setDoubleBuffered(false);

		final GLDrawableFactory factory = GLDrawableFactory.getFactory(mProfile);
		mOffscreen = factory.createOffscreenAutoDrawable(
				factory.getDefaultDevice(), caps, null, mWidth, mHeight);
		mOffscreen.display(); // force context creation
	}

	/**
	 * Creates an <b>offscreen</b> GL render target (no Swing component).
	 * Use {@link #getResultImage()} to retrieve the rendered frame.
	 *
	 * @param width  pixel width
	 * @param height pixel height
	 * @return a new offscreen render target
	 */
	public static GLRenderTarget createOffscreen(final int width, final int height) {
		return new GLRenderTarget(width, height, true);
	}

	// -----------------------------------------------------------------------
	//  IRenderTarget
	// -----------------------------------------------------------------------

	@Override
	public void setGraphicsView(final GraphicsView view) {
		mView = view;
	}

	@Override
	public synchronized void requestRepaint() {
		if (mView == null)
			return;
		if (mPanel != null) {
			// headful: trigger GLJPanel display cycle
			mPanel.display();
		} else if (mOffscreen != null) {
			// offscreen: render directly
			renderOffscreen();
		}
	}

	@Override
	public Rectangle getVisibleRect() {
		if (mRectangle == null)
			mRectangle = new Rectangle(0, 0, getWidth(), getHeight());
		return mRectangle;
	}

	@Override
	public int getWidth() {
		if (mPanel != null) {
			final int sw = mPanel.getSurfaceWidth();
			if (sw > 0) return sw;
		}
		return mWidth;
	}

	@Override
	public int getHeight() {
		if (mPanel != null) {
			final int sh = mPanel.getSurfaceHeight();
			if (sh > 0) return sh;
		}
		return mHeight;
	}

	@Override
	public Component getComponent() {
		return mPanel;
	}

	// -----------------------------------------------------------------------
	//  Public helpers
	// -----------------------------------------------------------------------

	/** Sets the clear color used before each frame. */
	public void setClearColor(final Color color) {
		mClearColor = color;
	}

	/** Returns the last rendered frame as a {@link BufferedImage} (offscreen mode). */
	public BufferedImage getResultImage() {
		return mResultImage;
	}

	/** Invalidates the sprite cache for a specific item. */
	public void invalidateSprite(final GraphicsItem item) {
		final SpriteEntry removed = mSpriteCache.remove(item);
		if (removed != null)
			mStaleTextures.add(removed.texture);
	}

	/** Invalidates the entire sprite cache. */
	public void invalidateAllSprites() {
		markAllSpritesStale();
	}

	/** Releases all GL resources.  Call when this target is no longer needed. */
	public void dispose() {
		// destroy textures if possible
		if (mOffscreen != null) {
			final GLContext ctx = mOffscreen.getContext();
			if (ctx.makeCurrent() != GLContext.CONTEXT_NOT_CURRENT) {
				try {
					final GL2 gl = ctx.getGL().getGL2();
					destroyAllTextures(gl);
				} finally {
					ctx.release();
				}
			}
			mOffscreen.destroy();
			mOffscreen = null;
		}
		mSpriteCache.clear();
		mStaleTextures.clear();
		if (mPanel != null) {
			mPanel.destroy();
			mPanel = null;
		}
	}

	// -----------------------------------------------------------------------
	//  Offscreen rendering
	// -----------------------------------------------------------------------

	private void renderOffscreen() {
		renderOffscreen(true);
	}

	/**
	 * Renders the current frame offscreen.
	 *
	 * @param doReadback if {@code true}, the result is read back into a
	 *                   {@link BufferedImage} accessible via {@link #getResultImage()}.
	 *                   Set to {@code false} when only GPU rendering time matters
	 *                   (e.g. benchmarks).
	 */
	void renderOffscreen(final boolean doReadback) {
		final GLContext ctx = mOffscreen.getContext();
		final int res = ctx.makeCurrent();
		if (res == GLContext.CONTEXT_NOT_CURRENT) {
			LOG.debug("Could not make GL context current — skipping repaint");
			return;
		}
		try {
			final GL gl0 = ctx.getGL();
			if (gl0 == null) return;
			final GL2 gl = gl0.getGL2();

			final List<GraphicsItem> items = mView.beginFrame();
			try {
				doRender(gl, items);
			} finally {
				mView.endFrame();
			}

			if (doReadback)
				readback(gl);
		} catch (final Exception e) {
			LOG.error("GL render failed: {}", e.getMessage(), e);
		} finally {
			ctx.release();
		}
	}

	// -----------------------------------------------------------------------
	//  GLEventListener for headful mode
	// -----------------------------------------------------------------------

	private class GLRenderListener implements GLEventListener {
		@Override
		public void init(final GLAutoDrawable drawable) { /* nothing */ }

		@Override
		public void dispose(final GLAutoDrawable drawable) {
			final GL2 gl = drawable.getGL().getGL2();
			destroyAllTextures(gl);
		}

		@Override
		public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int w, final int h) {
			mWidth  = Math.max(1, w);
			mHeight = Math.max(1, h);
			mRectangle = null;
			markAllSpritesStale();
		}

		@Override
		public void display(final GLAutoDrawable drawable) {
			if (mView == null) return;
			final GL2 gl = drawable.getGL().getGL2();

			final List<GraphicsItem> items = mView.beginFrame();
			try {
				doRender(gl, items);
			} finally {
				mView.endFrame();
			}
		}
	}

	// -----------------------------------------------------------------------
	//  Shared GL rendering
	// -----------------------------------------------------------------------

	private void doRender(final GL2 gl, final List<GraphicsItem> items) {
		final int w = getWidth(), h = getHeight();
		if (w <= 1 || h <= 1) return;

		// -- cleanup stale textures --
		cleanupStaleTextures(gl);

		// -- invalidate sprites if view scale changed --
		final double sx = mView.getScaleX(), sy = mView.getScaleY();
		if (sx != mCachedScaleX || sy != mCachedScaleY) {
			markAllSpritesStale();
			cleanupStaleTextures(gl);
			mCachedScaleX = sx;
			mCachedScaleY = sy;
		}

		// -- clear --
		final float cr = mClearColor.getRed()   / 255f;
		final float cg = mClearColor.getGreen() / 255f;
		final float cb = mClearColor.getBlue()  / 255f;
		gl.glClearColor(cr, cg, cb, 1f);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);

		// -- projection: Y-down like Java2D --
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, w, h, 0, -1, 1);

		// -- modelview: apply view transform --
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		final AffineTransform viewTransform = computeViewTransform();
		if (viewTransform != null)
			applyTransform(gl, viewTransform);

		// -- blending for alpha sprites --
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		// -- draw items --
		for (final GraphicsItem item : items)
			if (item.isVisible())
				renderItem(gl, item);

		// -- cleanup --
		gl.glDisable(GL.GL_BLEND);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glFlush();
	}

	/**
	 * Renders a single item (and its children) as a textured sprite quad.
	 */
	private void renderItem(final GL2 gl, final GraphicsItem item) {
		final SpriteEntry sprite = getOrCreateSprite(gl, item);
		if (sprite != null) {
			gl.glPushMatrix();
			applyTransform(gl, item.getWorldTransform());

			sprite.texture.enable(gl);
			sprite.texture.bind(gl);

			final TextureCoords tc = sprite.texture.getImageTexCoords();
			final float x1 = (float) sprite.localBounds.x;
			final float y1 = (float) sprite.localBounds.y;
			final float x2 = x1 + (float) sprite.localBounds.width;
			final float y2 = y1 + (float) sprite.localBounds.height;

			gl.glColor4f(1f, 1f, 1f, 1f);
			gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(tc.left(),  tc.top());    gl.glVertex2f(x1, y1);
			gl.glTexCoord2f(tc.right(), tc.top());    gl.glVertex2f(x2, y1);
			gl.glTexCoord2f(tc.right(), tc.bottom()); gl.glVertex2f(x2, y2);
			gl.glTexCoord2f(tc.left(),  tc.bottom()); gl.glVertex2f(x1, y2);
			gl.glEnd();

			sprite.texture.disable(gl);
			gl.glPopMatrix();
		}

		// -- children --
		if (item.hasChildren()) {
			final List<GraphicsItem> children = new ArrayList<>(item.getChildren());
			Collections.sort(children, Comparator.comparing(GraphicsItem::getZOrder));
			for (final GraphicsItem child : children)
				if (child.isVisible())
					renderItem(gl, child);
		}
	}

	// -----------------------------------------------------------------------
	//  Sprite creation & caching
	// -----------------------------------------------------------------------

	private SpriteEntry getOrCreateSprite(final GL2 gl, final GraphicsItem item) {
		SpriteEntry entry = mSpriteCache.get(item);
		if (entry != null)
			return entry;

		final IDrawable drawable = item.getDrawable();
		final Shape shape = item.getShape();
		if (drawable == null || shape == null)
			return null;

		final Rectangle2D shapeBounds = shape.getBounds2D();
		// use absolute values — some shapes (e.g. TileItem) have negative height
		final double shapeW = Math.abs(shapeBounds.getWidth());
		final double shapeH = Math.abs(shapeBounds.getHeight());
		if (shapeW <= 0 || shapeH <= 0)
			return null;

		// normalized origin (always at min corner)
		final double bx = Math.min(shapeBounds.getX(), shapeBounds.getX() + shapeBounds.getWidth());
		final double by = Math.min(shapeBounds.getY(), shapeBounds.getY() + shapeBounds.getHeight());

		// -- compute pixel size of the sprite using the combined transform --
		final AffineTransform worldTx = item.getWorldTransform();
		final AffineTransform viewTx  = computeViewTransform();
		final AffineTransform combined = new AffineTransform(viewTx);
		combined.concatenate(worldTx);
		final double pxW = Math.abs(shapeW * combined.getScaleX());
		final double pxH = Math.abs(shapeH * combined.getScaleY());

		final int pad = computePadding(item.getStyle());
		int spriteW = (int) Math.ceil(pxW) + 2 * pad;
		int spriteH = (int) Math.ceil(pxH) + 2 * pad;
		spriteW = Math.max(1, Math.min(spriteW, MAX_SPRITE_SIZE));
		spriteH = Math.max(1, Math.min(spriteH, MAX_SPRITE_SIZE));

		final double scaleToPixelX = (spriteW - 2.0 * pad) / shapeW;
		final double scaleToPixelY = (spriteH - 2.0 * pad) / shapeH;

		// -- render sprite via Java2D --
		final BufferedImage spriteImg = new BufferedImage(
				spriteW, spriteH, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = spriteImg.createGraphics();
		try {
			final RenderingHints hints = mView.getREnderHints();
			if (hints != null)
				g2d.setRenderingHints(hints);

			// scale from shape-units to pixels, then translate to place shape at (pad,pad)
			g2d.translate(pad, pad);
			g2d.scale(scaleToPixelX, scaleToPixelY);
			g2d.translate(-bx, -by);

			final IDrawContext ctx = createDrawContext();
			drawable.paintItem(g2d, item.getStyle(), ctx);
		} finally {
			g2d.dispose();
		}

		// -- upload texture --
		final Texture texture = AWTTextureIO.newTexture(mProfile, spriteImg, true);
		texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

		// -- quad bounds in item-local coordinates (original shape units) --
		final double padScene = pad / Math.max(scaleToPixelX, scaleToPixelY);
		final Rectangle2D.Double localBounds = new Rectangle2D.Double(
				bx - padScene,
				by - padScene,
				shapeW + 2 * padScene,
				shapeH + 2 * padScene);

		entry = new SpriteEntry(texture, localBounds);
		mSpriteCache.put(item, entry);
		return entry;
	}

	private int computePadding(final DrawableStyle style) {
		if (style == null)
			return SPRITE_PADDING;
		final Stroke stroke = style.getLineStroke();
		if (stroke instanceof BasicStroke) {
			final float lw = ((BasicStroke) stroke).getLineWidth();
			return (int) Math.ceil(lw) + SPRITE_PADDING;
		}
		return SPRITE_PADDING;
	}

	// -----------------------------------------------------------------------
	//  Sprite cache management
	// -----------------------------------------------------------------------

	private void markAllSpritesStale() {
		for (final SpriteEntry e : mSpriteCache.values())
			mStaleTextures.add(e.texture);
		mSpriteCache.clear();
	}

	private void cleanupStaleTextures(final GL2 gl) {
		if (!mStaleTextures.isEmpty()) {
			for (final Texture t : mStaleTextures)
				t.destroy(gl);
			mStaleTextures.clear();
		}
	}

	private void destroyAllTextures(final GL2 gl) {
		for (final SpriteEntry e : mSpriteCache.values())
			e.texture.destroy(gl);
		mSpriteCache.clear();
		cleanupStaleTextures(gl);
	}

	// -----------------------------------------------------------------------
	//  Framebuffer readback (offscreen)
	// -----------------------------------------------------------------------

	private void readback(final GL2 gl) {
		final AWTGLReadBufferUtil reader = new AWTGLReadBufferUtil(mProfile, false);
		mResultImage = reader.readPixelsToBufferedImage(gl, true);
	}

	// -----------------------------------------------------------------------
	//  View-transform reconstruction
	// -----------------------------------------------------------------------

	/**
	 * Reconstructs the view transform from the public parameters of
	 * {@link GraphicsView}.  Mirrors the logic in
	 * {@code GraphicsView.getViewTransform()}.
	 */
	private AffineTransform computeViewTransform() {
		if (mView == null) return null;
		try {
			final double w2 = getWidth()  / 2.0;
			final double h2 = getHeight() / 2.0;
			final double sx = mView.getScaleX();
			final double sy = mView.getScaleY();
			final double r  = Math.toRadians(-mView.getRotationDegrees());

			final AffineTransform t = new AffineTransform();
			t.translate(-w2 * sx,  h2 * sy);
			t.translate(mView.getCenterX(), -mView.getCenterY());
			t.scale(sx, -sy);
			t.translate(w2, h2);
			t.rotate(r);
			t.translate(-w2, -h2);
			return t.createInverse();
		} catch (final NoninvertibleTransformException e) {
			LOG.error("Could not invert view transform", e);
			return new AffineTransform();
		}
	}

	// -----------------------------------------------------------------------
	//  Draw context for sprite painting
	// -----------------------------------------------------------------------

	private IDrawContext createDrawContext() {
		return new IDrawContext() {
			@Override
			public GraphicsView getView() { return mView; }
			@Override
			public AffineTransform getViewTransform() { return computeViewTransform(); }
			@Override
			public Rectangle2D getVisibleSceneRect() { return mView.getVisibleSceneRect(); }
		};
	}

	// -----------------------------------------------------------------------
	//  AffineTransform → GL matrix
	// -----------------------------------------------------------------------

	/**
	 * Multiplies the current GL matrix by the given {@link AffineTransform}.
	 */
	private static void applyTransform(final GL2 gl, final AffineTransform at) {
		final double[] m = new double[6];
		at.getMatrix(m); // [m00, m10, m01, m11, m02, m12]
		final double[] gl4x4 = {
				m[0], m[1], 0, 0,   // column 0
				m[2], m[3], 0, 0,   // column 1
				0,    0,    1, 0,   // column 2
				m[4], m[5], 0, 1    // column 3
		};
		gl.glMultMatrixd(gl4x4, 0);
	}
}
