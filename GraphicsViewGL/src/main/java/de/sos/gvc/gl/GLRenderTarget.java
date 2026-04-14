package de.sos.gvc.gl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
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
 * <p>Provides a {@link GLCanvas} AWT component for on-screen rendering.
 * The canvas renders directly into the native window framebuffer without
 * readback, providing much better throughput than GLJPanel.  It can be
 * embedded in Swing layouts via {@code frame.add(view.getComponent())}.
 * Mouse events, resizing and all standard GraphicsView interactions work
 * unchanged.</p>
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
	/** Default atlas page size in pixels. */
	private static final int DEFAULT_ATLAS_SIZE = 2048;
	/** Transparent guard band around atlas entries to reduce linear-filter bleeding. */
	private static final int ATLAS_GAP = 1;
	/** Custom property fired by {@link GraphicsItem#markDirty()}. */
	private static final String MANUAL_REPAINT_PROPERTY = "ManualRepaint";

	// ---- core state ----
	private GraphicsView				mView;
	private int							mWidth;
	private int							mHeight;
	private Rectangle					mRectangle;
	private Color						mClearColor = Color.BLACK;

	// ---- GL resources ----
	private final GLProfile				mProfile;
	private GLCanvas					mCanvas;      // headful
	private GLOffscreenAutoDrawable		mOffscreen;   // offscreen
	private final AtomicBoolean			mHeadfulDisplayQueued = new AtomicBoolean(false);
	private final AtomicBoolean			mHeadfulDisplayRequested = new AtomicBoolean(false);
	private final GLSpriteInstancingPipeline mPipeline = new GLSpriteInstancingPipeline();

	// ---- sprite cache ----
	private final Map<GraphicsItem, SpriteEntry> mSpriteCache = new HashMap<>();
	private final List<AtlasPage>		mAtlasPages = new ArrayList<>();
	/** Textures pending destruction (from invalidation between frames). */
	private final List<Texture>			mStaleTextures = new ArrayList<>();
	private int							mAtlasWidth = DEFAULT_ATLAS_SIZE;
	private int							mAtlasHeight = DEFAULT_ATLAS_SIZE;

	// ---- event-based sprite invalidation ----
	/** Listener that invalidates a single sprite when its drawable, style or shape changes. */
	private final PropertyChangeListener mItemSpriteListener = this::onItemPropertyChanged;
	/** Listener that handles items being added to or removed from the scene. */
	private final PropertyChangeListener mSceneItemListener  = this::onSceneItemListChanged;

	// ---- result image (offscreen readback) ----
	private BufferedImage				mResultImage;

	// -----------------------------------------------------------------------
	//  Inner types
	// -----------------------------------------------------------------------

	private static final class AtlasSlot {
		final AtlasPage	page;
		final int		outerX;
		final int		outerY;
		final int		outerWidth;
		final int		outerHeight;
		final int		x;
		final int		y;
		final int		width;
		final int		height;

		AtlasSlot(final AtlasPage page, final int outerX, final int outerY,
				final int outerWidth, final int outerHeight,
				final int x, final int y, final int width, final int height) {
			this.page = page;
			this.outerX = outerX;
			this.outerY = outerY;
			this.outerWidth = outerWidth;
			this.outerHeight = outerHeight;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
	}

	private static final class AtlasPage {
		final Texture texture;
		final int width;
		final int height;
		final List<AtlasSlot> freeSlots = new ArrayList<>();
		int cursorX;
		int cursorY;
		int rowHeight;

		AtlasPage(final Texture texture, final int width, final int height) {
			this.texture = texture;
			this.width = width;
			this.height = height;
		}

		AtlasSlot allocate(final int spriteWidth, final int spriteHeight) {
			final int requiredWidth = spriteWidth + 2 * ATLAS_GAP;
			final int requiredHeight = spriteHeight + 2 * ATLAS_GAP;
			if (requiredWidth > width || requiredHeight > height)
				return null;

			for (int i = 0; i < freeSlots.size(); i++) {
				final AtlasSlot slot = freeSlots.get(i);
				if (slot.outerWidth >= requiredWidth && slot.outerHeight >= requiredHeight) {
					freeSlots.remove(i);
					return new AtlasSlot(this, slot.outerX, slot.outerY, slot.outerWidth, slot.outerHeight,
							slot.outerX + ATLAS_GAP, slot.outerY + ATLAS_GAP, spriteWidth, spriteHeight);
				}
			}

			if (cursorX + requiredWidth > width) {
				cursorX = 0;
				cursorY += rowHeight;
				rowHeight = 0;
			}
			if (cursorY + requiredHeight > height)
				return null;

			final int outerX = cursorX;
			final int outerY = cursorY;
			cursorX += requiredWidth;
			rowHeight = Math.max(rowHeight, requiredHeight);
			return new AtlasSlot(this, outerX, outerY, requiredWidth, requiredHeight,
					outerX + ATLAS_GAP, outerY + ATLAS_GAP, spriteWidth, spriteHeight);
		}

		void release(final AtlasSlot slot) {
			freeSlots.add(slot);
		}
	}

	/** Cached sprite: texture region + quad bounds in item-local coordinates. */
	private static final class SpriteEntry {
		final Texture				texture;
		final TextureCoords			texCoords;
		final Rectangle2D.Double	localBounds;
		final AtlasSlot				atlasSlot;

		SpriteEntry(final Texture texture, final TextureCoords texCoords,
				final Rectangle2D.Double localBounds, final AtlasSlot atlasSlot) {
			this.texture = texture;
			this.texCoords = texCoords;
			this.localBounds = localBounds;
			this.atlasSlot = atlasSlot;
		}

		boolean isAtlasBacked() {
			return atlasSlot != null;
		}
	}

	private static final class InstanceBatch {
		private Texture texture;
		private FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(32);
		private int instanceCount;

		void clear() {
			texture = null;
			vertexBuffer.clear();
			instanceCount = 0;
		}

		boolean isEmpty() {
			return instanceCount == 0;
		}

		boolean accepts(final Texture nextTexture) {
			return isEmpty() || texture == nextTexture;
		}

		Texture getTexture() {
			return texture;
		}

		int getInstanceCount() {
			return instanceCount;
		}

		FloatBuffer prepareForDraw() {
			vertexBuffer.flip();
			return vertexBuffer;
		}

		void appendQuad(final Texture texture, final double[] pts, final TextureCoords texCoords) {
			if (this.texture == null)
				this.texture = texture;
			ensureCapacity(instanceCount + 1);
			vertexBuffer.put((float) pts[0]).put((float) pts[1]);
			vertexBuffer.put((float) (pts[2] - pts[0])).put((float) (pts[3] - pts[1]));
			vertexBuffer.put((float) (pts[6] - pts[0])).put((float) (pts[7] - pts[1]));
			vertexBuffer.put(texCoords.left()).put(texCoords.top())
					.put(texCoords.right()).put(texCoords.bottom());
			instanceCount++;
		}

		private void ensureCapacity(final int requiredInstanceCount) {
			final int requiredFloats = requiredInstanceCount * 10;
			if (vertexBuffer.capacity() < requiredFloats)
				vertexBuffer = grow(vertexBuffer, requiredFloats);
		}

		private FloatBuffer grow(final FloatBuffer oldBuffer, final int requiredFloats) {
			final FloatBuffer grown = GLBuffers.newDirectFloatBuffer(Math.max(requiredFloats, oldBuffer.capacity() * 2));
			oldBuffer.flip();
			grown.put(oldBuffer);
			return grown;
		}
	}

	// -----------------------------------------------------------------------
	//  Construction
	// -----------------------------------------------------------------------

	/**
	 * Creates a <b>headful</b> GL render target backed by a {@link GLCanvas}.
	 * The canvas is available via {@link #getComponent()} and can be embedded
	 * in any Swing/AWT container.  Unlike GLJPanel, GLCanvas renders directly
	 * into the native framebuffer without a per-frame {@code glReadPixels}
	 * readback, resulting in significantly lower CPU usage.
	 *
	 * <p>The {@code GLCanvas} is a heavyweight AWT component.  Lightweight
	 * Swing popups/menus may not overlap it correctly on all platforms; for
	 * map-style applications this is usually not a problem.</p>
	 *
	 * @param width  initial preferred width
	 * @param height initial preferred height
	 */
	public GLRenderTarget(final int width, final int height) {
		this(width, height, DEFAULT_ATLAS_SIZE, DEFAULT_ATLAS_SIZE);
	}

	public GLRenderTarget(final int width, final int height, final int atlasSize) {
		this(width, height, atlasSize, atlasSize);
	}

	public GLRenderTarget(final int width, final int height, final int atlasWidth, final int atlasHeight) {
		mWidth  = Math.max(1, width);
		mHeight = Math.max(1, height);
		mAtlasWidth = Math.max(0, atlasWidth);
		mAtlasHeight = Math.max(0, atlasHeight);
		mProfile = selectProfile();

		final GLCapabilities caps = new GLCapabilities(mProfile);
		caps.setAlphaBits(8);
		caps.setHardwareAccelerated(true);
		caps.setDoubleBuffered(true);

		mCanvas = createGLCanvas(caps);
		mCanvas.addGLEventListener(new GLRenderListener());
		mCanvas.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				mWidth  = Math.max(1, mCanvas.getSurfaceWidth());
				mHeight = Math.max(1, mCanvas.getSurfaceHeight());
				mRectangle = null;
			}
		});
	}

	/**
	 * Creates the GLCanvas.  GLCanvas is a heavyweight AWT component that
	 * renders directly into the native window surface — no readback overhead.
	 */
	private GLCanvas createGLCanvas(final GLCapabilities caps) {
		final GLCanvas canvas = new GLCanvas(caps);
		canvas.setPreferredSize(new Dimension(mWidth, mHeight));
		return canvas;
	}

	/**
	 * Private constructor for offscreen mode.
	 */
	private GLRenderTarget(final int width, final int height, @SuppressWarnings("unused") final boolean offscreen) {
		this(width, height, DEFAULT_ATLAS_SIZE, DEFAULT_ATLAS_SIZE, true);
	}

	private GLRenderTarget(final int width, final int height, final int atlasWidth, final int atlasHeight,
			@SuppressWarnings("unused") final boolean offscreen) {
		mWidth  = Math.max(1, width);
		mHeight = Math.max(1, height);
		mAtlasWidth = Math.max(0, atlasWidth);
		mAtlasHeight = Math.max(0, atlasHeight);
		mProfile = selectProfile();

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

	private static GLProfile selectProfile() {
		try {
			return GLProfile.getMaxProgrammableCore(true);
		} catch (final Exception e) {
			return GLProfile.getMaxProgrammable(true);
		}
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

	public static GLRenderTarget createOffscreen(final int width, final int height, final int atlasSize) {
		return new GLRenderTarget(width, height, atlasSize, atlasSize, true);
	}

	public static GLRenderTarget createOffscreen(final int width, final int height,
			final int atlasWidth, final int atlasHeight) {
		return new GLRenderTarget(width, height, atlasWidth, atlasHeight, true);
	}

	// -----------------------------------------------------------------------
	//  IRenderTarget
	// -----------------------------------------------------------------------

	@Override
	public void setGraphicsView(final GraphicsView view) {
		// unregister from previous scene
		if (mView != null) {
			final GraphicsScene oldScene = mView.getScene();
			if (oldScene != null) {
				unregisterSceneListeners(oldScene);
			}
		}
		mView = view;
		// register on new scene
		if (mView != null) {
			final GraphicsScene newScene = mView.getScene();
			if (newScene != null) {
				registerSceneListeners(newScene);
			}
		}
	}

	@Override
	public synchronized void requestRepaint() {
		if (mView == null)
			return;
		if (mCanvas != null) {
			queueHeadfulDisplay();
		} else if (mOffscreen != null) {
			// offscreen: render directly
			renderOffscreen();
		}
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

	@Override
	public Rectangle getVisibleRect() {
		if (mRectangle == null)
			mRectangle = new Rectangle(0, 0, getWidth(), getHeight());
		return mRectangle;
	}

	@Override
	public int getWidth() {
		if (mCanvas != null) {
			final int sw = mCanvas.getSurfaceWidth();
			if (sw > 0) return sw;
		}
		return mWidth;
	}

	@Override
	public int getHeight() {
		if (mCanvas != null) {
			final int sh = mCanvas.getSurfaceHeight();
			if (sh > 0) return sh;
		}
		return mHeight;
	}

	@Override
	public Component getComponent() {
		return mCanvas;
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

	public void setAtlasPageSize(final int size) {
		setAtlasPageSize(size, size);
	}

	public synchronized void setAtlasPageSize(final int width, final int height) {
		final int newWidth = Math.max(0, width);
		final int newHeight = Math.max(0, height);
		if (mAtlasWidth == newWidth && mAtlasHeight == newHeight)
			return;
		mAtlasWidth = newWidth;
		mAtlasHeight = newHeight;
		invalidateAllSprites();
		requestRepaint();
	}

	public int getAtlasPageWidth() {
		return mAtlasWidth;
	}

	public int getAtlasPageHeight() {
		return mAtlasHeight;
	}

	/** Invalidates the sprite cache for a specific item. */
	public void invalidateSprite(final GraphicsItem item) {
		final SpriteEntry removed = mSpriteCache.remove(item);
		if (removed == null)
			return;
		if (removed.isAtlasBacked())
			removed.atlasSlot.page.release(removed.atlasSlot);
		else
			mStaleTextures.add(removed.texture);
	}

	/** Invalidates the entire sprite cache. */
	public void invalidateAllSprites() {
		markAllSpritesStale();
	}

	/** Releases all GL resources.  Call when this target is no longer needed. */
	public void dispose() {
		mHeadfulDisplayRequested.set(false);
		mHeadfulDisplayQueued.set(false);
		// unregister listeners
		if (mView != null) {
			final GraphicsScene scene = mView.getScene();
			if (scene != null)
				unregisterSceneListeners(scene);
		}
		// destroy textures if possible
		if (mOffscreen != null) {
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
		mSpriteCache.clear();
		mStaleTextures.clear();
		if (mCanvas != null) {
			mCanvas.destroy();
			mCanvas = null;
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
			final GL3 gl = gl0.getGL3();

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
			final GL3 gl = drawable.getGL().getGL3();
			destroyAllTextures(gl);
		}

		@Override
		public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int w, final int h) {
			mWidth  = Math.max(1, w);
			mHeight = Math.max(1, h);
			mRectangle = null;
		}

		@Override
		public void display(final GLAutoDrawable drawable) {
			if (mView == null) return;
			final GL3 gl = drawable.getGL().getGL3();

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

	/** Reusable buffer for transforming quad vertices on the CPU. */
	private final double[] mQuadPts = new double[8];
	private final InstanceBatch mSpriteBatch = new InstanceBatch();

	private void doRender(final GL3 gl, final List<GraphicsItem> items) {
		final int w = getWidth(), h = getHeight();
		if (w <= 1 || h <= 1) return;
		mSpriteBatch.clear();

		// -- cleanup stale textures --
		cleanupStaleTextures(gl);

		// -- clear --
		final float cr = mClearColor.getRed()   / 255f;
		final float cg = mClearColor.getGreen() / 255f;
		final float cb = mClearColor.getBlue()  / 255f;
		gl.glClearColor(cr, cg, cb, 1f);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);

		// -- precompute view transform (applied to every quad on CPU) --
		final AffineTransform viewTx = computeViewTransform();

		// -- set GL state once for the entire frame --
		gl.glViewport(0, 0, w, h);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		// -- draw items --
		for (final GraphicsItem item : items)
			if (item.isVisible())
				renderItem(gl, item, viewTx, mSpriteBatch);
		flushBatch(gl, mSpriteBatch, w, h);

		// -- cleanup --
		gl.glDisable(GL.GL_BLEND);
	}

	/**
	 * Renders a single item (and its children) as a textured sprite quad.
	 * <p>
	 * All vertex positions are computed on the CPU by concatenating the
	 * view and world transforms.  This avoids per-item GL matrix stack
	 * operations ({@code glPushMatrix/glPopMatrix/glMultMatrixd}) that
	 * cause expensive driver round-trips.
	 * </p>
	 */
	private void renderItem(final GL3 gl, final GraphicsItem item,
			final AffineTransform viewTx, final InstanceBatch batch) {
		final SpriteEntry sprite = getOrCreateSprite(gl, item);
		if (sprite != null) {
			final double x1 = sprite.localBounds.x;
			final double y1 = sprite.localBounds.y;
			final double x2 = x1 + sprite.localBounds.width;
			final double y2 = y1 + sprite.localBounds.height;

			// transform quad corners: worldTransform then viewTransform
			final double[] pts = mQuadPts;
			pts[0] = x1; pts[1] = y1;   // top-left
			pts[2] = x2; pts[3] = y1;   // top-right
			pts[4] = x2; pts[5] = y2;   // bottom-right
			pts[6] = x1; pts[7] = y2;   // bottom-left
			item.getWorldTransform().transform(pts, 0, pts, 0, 4);
			if (viewTx != null)
				viewTx.transform(pts, 0, pts, 0, 4);

			if (!batch.accepts(sprite.texture))
				flushBatch(gl, batch, getWidth(), getHeight());
			batch.appendQuad(sprite.texture, pts, sprite.texCoords);
		}

		// -- children --
		if (item.hasChildren()) {
			final List<GraphicsItem> children = new ArrayList<>(item.getChildren());
			Collections.sort(children, Comparator.comparing(GraphicsItem::getZOrder));
			for (final GraphicsItem child : children)
				if (child.isVisible())
					renderItem(gl, child, viewTx, batch);
		}
	}

	private void flushBatch(final GL3 gl, final InstanceBatch batch, final int width, final int height) {
		if (batch.isEmpty())
			return;
		mPipeline.render(gl, batch.getTexture(), batch.prepareForDraw(), batch.getInstanceCount(), width, height);
		batch.clear();
	}

	// -----------------------------------------------------------------------
	//  Sprite creation & caching
	// -----------------------------------------------------------------------

	private SpriteEntry getOrCreateSprite(final GL3 gl, final GraphicsItem item) {
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

		// -- quad bounds in item-local coordinates (original shape units) --
		final double padScene = pad / Math.max(scaleToPixelX, scaleToPixelY);
		final Rectangle2D.Double localBounds = new Rectangle2D.Double(
				bx - padScene,
				by - padScene,
				shapeW + 2 * padScene,
				shapeH + 2 * padScene);

		entry = uploadSprite(gl, spriteImg, spriteW, spriteH, localBounds);
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

	private SpriteEntry uploadSprite(final GL gl, final BufferedImage spriteImg,
			final int spriteW, final int spriteH, final Rectangle2D.Double localBounds) {
		final AtlasSlot slot = allocateAtlasSlot(gl, spriteW, spriteH);
		if (slot != null) {
			final TextureData data = AWTTextureIO.newTextureData(mProfile, spriteImg, false);
			try {
				final int dstY = slot.page.height - slot.y - spriteH;
				slot.page.texture.updateSubImage(gl, data, 0, slot.x, dstY);
			} finally {
				data.flush();
			}
			final Texture texture = slot.page.texture;
			final TextureCoords texCoords = texture.getSubImageTexCoords(
					slot.x, slot.y, slot.x + spriteW, slot.y + spriteH);
			return new SpriteEntry(texture, texCoords, localBounds, slot);
		}

		final Texture texture = AWTTextureIO.newTexture(mProfile, spriteImg, false);
		configureSpriteTexture(gl, texture);
		return new SpriteEntry(texture, texture.getImageTexCoords(), localBounds, null);
	}

	private AtlasSlot allocateAtlasSlot(final GL gl, final int spriteW, final int spriteH) {
		if (mAtlasWidth <= 0 || mAtlasHeight <= 0)
			return null;
		for (final AtlasPage page : mAtlasPages) {
			final AtlasSlot slot = page.allocate(spriteW, spriteH);
			if (slot != null)
				return slot;
		}

		final AtlasPage page = createAtlasPage(gl);
		if (page == null)
			return null;
		mAtlasPages.add(page);
		return page.allocate(spriteW, spriteH);
	}

	private AtlasPage createAtlasPage(final GL gl) {
		if (mAtlasWidth <= 0 || mAtlasHeight <= 0)
			return null;
		final BufferedImage atlasImage = new BufferedImage(mAtlasWidth, mAtlasHeight, BufferedImage.TYPE_INT_ARGB);
		final Texture texture = AWTTextureIO.newTexture(mProfile, atlasImage, false);
		configureSpriteTexture(gl, texture);
		return new AtlasPage(texture, mAtlasWidth, mAtlasHeight);
	}

	private void configureSpriteTexture(final GL gl, final Texture texture) {
		texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
		texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
	}

	// -----------------------------------------------------------------------
	//  Sprite cache management
	// -----------------------------------------------------------------------

	private void markAllSpritesStale() {
		for (final SpriteEntry e : mSpriteCache.values())
			if (!e.isAtlasBacked())
				mStaleTextures.add(e.texture);
		mSpriteCache.clear();
		queueAtlasPagesForDestroy();
	}

	private void cleanupStaleTextures(final GL gl) {
		if (!mStaleTextures.isEmpty()) {
			for (final Texture t : mStaleTextures) {
				t.destroy(gl);
			}
			mStaleTextures.clear();
		}
	}

	private void destroyAllTextures(final GL3 gl) {
		for (final SpriteEntry e : mSpriteCache.values())
			if (!e.isAtlasBacked())
				e.texture.destroy(gl);
		mSpriteCache.clear();
		mPipeline.dispose(gl);
		destroyAtlasPages(gl);
		cleanupStaleTextures(gl);
	}

	private void queueAtlasPagesForDestroy() {
		for (final AtlasPage page : mAtlasPages)
			mStaleTextures.add(page.texture);
		mAtlasPages.clear();
	}

	private void destroyAtlasPages(final GL gl) {
		for (final AtlasPage page : mAtlasPages)
			page.texture.destroy(gl);
		mAtlasPages.clear();
	}

	// -----------------------------------------------------------------------
	//  Event-based sprite invalidation
	// -----------------------------------------------------------------------

	/**
	 * Registers listeners on the scene and all its current items so that
	 * sprite textures are invalidated automatically when drawables, styles
	 * or shapes change, or when items are added/removed.
	 */
	private void registerSceneListeners(final GraphicsScene scene) {
		scene.addPropertyListener(GraphicsScene.ITEM_LIST_PROPERTY, mSceneItemListener);
		for (final GraphicsItem item : scene.getItems())
			registerItemListener(item);
	}

	private void unregisterSceneListeners(final GraphicsScene scene) {
		scene.removePropertyListener(GraphicsScene.ITEM_LIST_PROPERTY, mSceneItemListener);
		for (final GraphicsItem item : scene.getItems())
			unregisterItemListener(item);
	}

	private void registerItemListener(final GraphicsItem item) {
		item.addPropertyChangeListener(GraphicsItem.PROP_DRAWABLE, mItemSpriteListener);
		item.addPropertyChangeListener(GraphicsItem.PROP_STYLE, mItemSpriteListener);
		item.addPropertyChangeListener(GraphicsItem.PROP_SHAPE, mItemSpriteListener);
		item.addPropertyChangeListener(MANUAL_REPAINT_PROPERTY, mItemSpriteListener);
	}

	private void unregisterItemListener(final GraphicsItem item) {
		item.removePropertyChangeListener(GraphicsItem.PROP_DRAWABLE, mItemSpriteListener);
		item.removePropertyChangeListener(GraphicsItem.PROP_STYLE, mItemSpriteListener);
		item.removePropertyChangeListener(GraphicsItem.PROP_SHAPE, mItemSpriteListener);
		item.removePropertyChangeListener(MANUAL_REPAINT_PROPERTY, mItemSpriteListener);
	}

	/** Called when an item's drawable, style or shape changes. */
	private void onItemPropertyChanged(final PropertyChangeEvent evt) {
		final Object source = evt.getSource();
		if (source instanceof GraphicsItem)
			invalidateSprite((GraphicsItem) source);
	}

	/** Called when items are added to or removed from the scene. */
	private void onSceneItemListChanged(final PropertyChangeEvent evt) {
		final GraphicsItem added   = (evt.getNewValue() instanceof GraphicsItem)
				? (GraphicsItem) evt.getNewValue() : null;
		final GraphicsItem removed = (evt.getOldValue() instanceof GraphicsItem)
				? (GraphicsItem) evt.getOldValue() : null;

		if (removed != null) {
			unregisterItemListener(removed);
			invalidateSprite(removed);
		}
		if (added != null) {
			registerItemListener(added);
		}
	}

	// -----------------------------------------------------------------------
	//  Framebuffer readback (offscreen)
	// -----------------------------------------------------------------------

	private void readback(final GL gl) {
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
}
