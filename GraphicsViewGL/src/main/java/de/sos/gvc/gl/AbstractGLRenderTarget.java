package de.sos.gvc.gl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GL3;
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

abstract class AbstractGLRenderTarget implements IRenderTarget {

	private static final Logger LOG = GVLog.getLogger(AbstractGLRenderTarget.class);

	protected static final int SPRITE_PADDING = 4;
	protected static final int MAX_SPRITE_SIZE = 2048;
	protected static final int DEFAULT_ATLAS_SIZE = 2048;
	protected static final int ATLAS_GAP = 1;
	protected static final String MANUAL_REPAINT_PROPERTY = "ManualRepaint";

	private GraphicsView mView;
	private int mWidth;
	private int mHeight;
	private Rectangle mRectangle;
	private Color mClearColor = Color.BLACK;

	protected final GLProfile mProfile;
	private final GLSpriteInstancingPipeline mPipeline = new GLSpriteInstancingPipeline();

	private final Map<GraphicsItem, SpriteEntry> mSpriteCache = new HashMap<>();
	private final List<AtlasPage> mAtlasPages = new ArrayList<>();
	private final List<Texture> mStaleTextures = new ArrayList<>();
	private int mAtlasWidth = DEFAULT_ATLAS_SIZE;
	private int mAtlasHeight = DEFAULT_ATLAS_SIZE;

	private final PropertyChangeListener mItemSpriteListener = this::onItemPropertyChanged;
	private final PropertyChangeListener mSceneItemListener = this::onSceneItemListChanged;

	private final double[] mQuadPts = new double[8];
	private final InstanceBatch mSpriteBatch = new InstanceBatch();

	protected AbstractGLRenderTarget(final int width, final int height, final int atlasWidth, final int atlasHeight) {
		mWidth = Math.max(1, width);
		mHeight = Math.max(1, height);
		mAtlasWidth = Math.max(0, atlasWidth);
		mAtlasHeight = Math.max(0, atlasHeight);
		mProfile = selectProfile();
	}

	protected static GLProfile selectProfile() {
		try {
			return GLProfile.getMaxProgrammableCore(true);
		} catch (final Exception e) {
			return GLProfile.getMaxProgrammable(true);
		}
	}

	protected final GraphicsView getGraphicsView() {
		return mView;
	}

	protected final void updateSurfaceSize(final int width, final int height) {
		mWidth = Math.max(1, width);
		mHeight = Math.max(1, height);
		mRectangle = null;
	}

	@Override
	public void setGraphicsView(final GraphicsView view) {
		if (mView != null) {
			final GraphicsScene oldScene = mView.getScene();
			if (oldScene != null)
				unregisterSceneListeners(oldScene);
		}
		mView = view;
		if (mView != null) {
			final GraphicsScene newScene = mView.getScene();
			if (newScene != null)
				registerSceneListeners(newScene);
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
		return mWidth;
	}

	@Override
	public int getHeight() {
		return mHeight;
	}

	@Override
	public Component getComponent() {
		return null;
	}

	public void setClearColor(final Color color) {
		mClearColor = color;
	}

	public BufferedImage getResultImage() {
		return null;
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

	public void invalidateSprite(final GraphicsItem item) {
		final SpriteEntry removed = mSpriteCache.remove(item);
		if (removed == null)
			return;
		if (removed.isAtlasBacked())
			removed.atlasSlot.page.release(removed.atlasSlot);
		else
			mStaleTextures.add(removed.texture);
	}

	public void invalidateAllSprites() {
		markAllSpritesStale();
	}

	public final void dispose() {
		if (mView != null) {
			final GraphicsScene scene = mView.getScene();
			if (scene != null)
				unregisterSceneListeners(scene);
		}
		disposeSurface();
		mSpriteCache.clear();
		mStaleTextures.clear();
	}

	protected abstract void disposeSurface();

	protected final void renderCurrentFrame(final GL3 gl, final boolean doReadback) {
		if (mView == null)
			return;

		final List<GraphicsItem> items = mView.beginFrame();
		try {
			doRender(gl, items);
		} finally {
			mView.endFrame();
		}

		if (doReadback)
			storeReadback(readbackToBufferedImage(gl));
	}

	protected void storeReadback(final BufferedImage image) {
		// only offscreen targets expose a readback image
	}

	protected final BufferedImage readbackToBufferedImage(final GL gl) {
		final AWTGLReadBufferUtil reader = new AWTGLReadBufferUtil(mProfile, false);
		return reader.readPixelsToBufferedImage(gl, true);
	}

	protected final void destroyAllTextures(final GL3 gl) {
		for (final SpriteEntry entry : mSpriteCache.values())
			if (!entry.isAtlasBacked())
				entry.texture.destroy(gl);
		mSpriteCache.clear();
		mPipeline.dispose(gl);
		destroyAtlasPages(gl);
		cleanupStaleTextures(gl);
	}

	private static void clearBuffer(final Buffer buffer) {
		buffer.clear();
	}

	private static void flipBuffer(final Buffer buffer) {
		buffer.flip();
	}

	private void doRender(final GL3 gl, final List<GraphicsItem> items) {
		final int width = getWidth();
		final int height = getHeight();
		if (width <= 1 || height <= 1)
			return;

		mSpriteBatch.clear();
		cleanupStaleTextures(gl);

		final float cr = mClearColor.getRed() / 255f;
		final float cg = mClearColor.getGreen() / 255f;
		final float cb = mClearColor.getBlue() / 255f;
		gl.glViewport(0, 0, width, height);
		gl.glClearColor(cr, cg, cb, 1f);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		final AffineTransform viewTx = computeViewTransform();
		for (final GraphicsItem item : items)
			if (item.isVisible())
				renderItem(gl, item, viewTx, mSpriteBatch);
		flushBatch(gl, mSpriteBatch, width, height);

		gl.glDisable(GL.GL_BLEND);
	}

	private void renderItem(final GL3 gl, final GraphicsItem item, final AffineTransform viewTx, final InstanceBatch batch) {
		final SpriteEntry sprite = getOrCreateSprite(gl, item);
		if (sprite != null) {
			final double x1 = sprite.localBounds.x;
			final double y1 = sprite.localBounds.y;
			final double x2 = x1 + sprite.localBounds.width;
			final double y2 = y1 + sprite.localBounds.height;

			final double[] pts = mQuadPts;
			pts[0] = x1; pts[1] = y1;
			pts[2] = x2; pts[3] = y1;
			pts[4] = x2; pts[5] = y2;
			pts[6] = x1; pts[7] = y2;
			item.getWorldTransform().transform(pts, 0, pts, 0, 4);
			if (viewTx != null)
				viewTx.transform(pts, 0, pts, 0, 4);

			if (!batch.accepts(sprite.texture))
				flushBatch(gl, batch, getWidth(), getHeight());
			batch.appendQuad(sprite.texture, pts, sprite.texCoords);
		}

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

	private SpriteEntry getOrCreateSprite(final GL3 gl, final GraphicsItem item) {
		SpriteEntry entry = mSpriteCache.get(item);
		if (entry != null)
			return entry;

		final IDrawable drawable = item.getDrawable();
		final Shape shape = item.getShape();
		if (drawable == null || shape == null)
			return null;

		final Rectangle2D shapeBounds = shape.getBounds2D();
		final double shapeW = Math.abs(shapeBounds.getWidth());
		final double shapeH = Math.abs(shapeBounds.getHeight());
		if (shapeW <= 0 || shapeH <= 0)
			return null;

		final double bx = Math.min(shapeBounds.getX(), shapeBounds.getX() + shapeBounds.getWidth());
		final double by = Math.min(shapeBounds.getY(), shapeBounds.getY() + shapeBounds.getHeight());

		final AffineTransform worldTx = item.getWorldTransform();
		final AffineTransform viewTx = computeViewTransform();
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

		final BufferedImage spriteImg = new BufferedImage(spriteW, spriteH, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = spriteImg.createGraphics();
		try {
			final RenderingHints hints = mView.getREnderHints();
			if (hints != null)
				g2d.setRenderingHints(hints);

			g2d.translate(pad, pad);
			g2d.scale(scaleToPixelX, scaleToPixelY);
			g2d.translate(-bx, -by);

			final IDrawContext ctx = createDrawContext();
			drawable.paintItem(g2d, item.getStyle(), ctx);
		} finally {
			g2d.dispose();
		}

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
		if (stroke instanceof BasicStroke)
			return (int) Math.ceil(((BasicStroke) stroke).getLineWidth()) + SPRITE_PADDING;
		return SPRITE_PADDING;
	}

	private SpriteEntry uploadSprite(final GL gl, final BufferedImage spriteImg, final int spriteW, final int spriteH,
			final Rectangle2D.Double localBounds) {
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
			final TextureCoords texCoords = texture.getSubImageTexCoords(slot.x, slot.y, slot.x + spriteW, slot.y + spriteH);
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

	private void markAllSpritesStale() {
		for (final SpriteEntry entry : mSpriteCache.values())
			if (!entry.isAtlasBacked())
				mStaleTextures.add(entry.texture);
		mSpriteCache.clear();
		queueAtlasPagesForDestroy();
	}

	private void cleanupStaleTextures(final GL gl) {
		if (mStaleTextures.isEmpty())
			return;
		for (final Texture texture : mStaleTextures)
			texture.destroy(gl);
		mStaleTextures.clear();
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

	private void onItemPropertyChanged(final PropertyChangeEvent evt) {
		final Object source = evt.getSource();
		if (source instanceof GraphicsItem)
			invalidateSprite((GraphicsItem) source);
	}

	private void onSceneItemListChanged(final PropertyChangeEvent evt) {
		final GraphicsItem added = (evt.getNewValue() instanceof GraphicsItem) ? (GraphicsItem) evt.getNewValue() : null;
		final GraphicsItem removed = (evt.getOldValue() instanceof GraphicsItem) ? (GraphicsItem) evt.getOldValue() : null;

		if (removed != null) {
			unregisterItemListener(removed);
			invalidateSprite(removed);
		}
		if (added != null)
			registerItemListener(added);
	}

	private AffineTransform computeViewTransform() {
		if (mView == null)
			return null;
		try {
			final double w2 = getWidth() / 2.0;
			final double h2 = getHeight() / 2.0;
			final double sx = mView.getScaleX();
			final double sy = mView.getScaleY();
			final double r = Math.toRadians(-mView.getRotationDegrees());

			final AffineTransform tx = new AffineTransform();
			tx.translate(-w2 * sx, h2 * sy);
			tx.translate(mView.getCenterX(), -mView.getCenterY());
			tx.scale(sx, -sy);
			tx.translate(w2, h2);
			tx.rotate(r);
			tx.translate(-w2, -h2);
			return tx.createInverse();
		} catch (final NoninvertibleTransformException e) {
			LOG.error("Could not invert view transform", e);
			return new AffineTransform();
		}
	}

	private IDrawContext createDrawContext() {
		return new IDrawContext() {
			@Override
			public GraphicsView getView() {
				return mView;
			}

			@Override
			public AffineTransform getViewTransform() {
				return computeViewTransform();
			}

			@Override
			public Rectangle2D getVisibleSceneRect() {
				return mView.getVisibleSceneRect();
			}
		};
	}

	private static final class AtlasSlot {
		final AtlasPage page;
		final int outerX;
		final int outerY;
		final int outerWidth;
		final int outerHeight;
		final int x;
		final int y;
		final int width;
		final int height;

		AtlasSlot(final AtlasPage page, final int outerX, final int outerY, final int outerWidth, final int outerHeight,
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

	private static final class SpriteEntry {
		final Texture texture;
		final TextureCoords texCoords;
		final Rectangle2D.Double localBounds;
		final AtlasSlot atlasSlot;

		SpriteEntry(final Texture texture, final TextureCoords texCoords, final Rectangle2D.Double localBounds,
				final AtlasSlot atlasSlot) {
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
			clearBuffer(vertexBuffer);
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
			flipBuffer(vertexBuffer);
			return vertexBuffer;
		}

		void appendQuad(final Texture texture, final double[] pts, final TextureCoords texCoords) {
			if (this.texture == null)
				this.texture = texture;
			ensureCapacity(instanceCount + 1);
			vertexBuffer.put((float) pts[0]).put((float) pts[1]);
			vertexBuffer.put((float) (pts[2] - pts[0])).put((float) (pts[3] - pts[1]));
			vertexBuffer.put((float) (pts[6] - pts[0])).put((float) (pts[7] - pts[1]));
			vertexBuffer.put(texCoords.left()).put(texCoords.top()).put(texCoords.right()).put(texCoords.bottom());
			instanceCount++;
		}

		private void ensureCapacity(final int requiredInstanceCount) {
			final int requiredFloats = requiredInstanceCount * 10;
			if (vertexBuffer.capacity() < requiredFloats)
				vertexBuffer = grow(vertexBuffer, requiredFloats);
		}

		private FloatBuffer grow(final FloatBuffer oldBuffer, final int requiredFloats) {
			final FloatBuffer grown = GLBuffers.newDirectFloatBuffer(Math.max(requiredFloats, oldBuffer.capacity() * 2));
			flipBuffer(oldBuffer);
			grown.put(oldBuffer);
			return grown;
		}
	}
}
