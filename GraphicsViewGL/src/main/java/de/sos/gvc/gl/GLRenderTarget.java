package de.sos.gvc.gl;

import java.awt.Component;
import java.awt.Rectangle;

import de.sos.gv.geo.tiles.TileItem;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.rt.IRenderTarget;

/**
 * OpenGL-backed {@link IRenderTarget} that renders {@link TileItem}s as
 * textured quads via the {@link GLTileRenderer}.
 *
 * <p>Usage: pass a {@code GLRenderTarget} to the standard
 * {@link GraphicsView} constructor to switch from Java2D to OpenGL rendering.
 * The existing scene/handler/tile infrastructure works unchanged.</p>
 *
 * <pre>{@code
 * GraphicsView view = new GraphicsView(scene, new GLRenderTarget());
 * }</pre>
 *
 * @author GraphicsViewGL
 */
public class GLRenderTarget implements IRenderTarget {

	@Override
	public Component getComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setGraphicsView(final GraphicsView view) {
		// TODO Auto-generated method stub

	}

	@Override
	public void requestRepaint() {
		// TODO Auto-generated method stub

	}

	@Override
	public Rectangle getVisibleRect() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getHeight() {
		// TODO Auto-generated method stub
		return 0;
	}


}
