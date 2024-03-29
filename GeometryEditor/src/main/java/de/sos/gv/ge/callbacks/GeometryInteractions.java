package de.sos.gv.ge.callbacks;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import de.sos.gv.ge.items.GeometryItem;
import de.sos.gvc.handler.SelectionHandler.IMoveCallback;
import de.sos.gvc.handler.SelectionHandler.IRotateCallback;
import de.sos.gvc.handler.SelectionHandler.IScaleCallback;
import de.sos.gvc.handler.SelectionHandler.ItemMoveEvent;
import de.sos.gvc.handler.SelectionHandler.ItemRotateEvent;
import de.sos.gvc.handler.SelectionHandler.ItemScaleEvent;

public class GeometryInteractions implements IMoveCallback, IScaleCallback, IRotateCallback {

	@Override
	public void onItemMoved(final ItemMoveEvent event) {
		for (int i = 0; i < event.items.size(); i++) {
			if (event.items.get(i) instanceof GeometryItem) {
				final GeometryItem item = (GeometryItem)event.items.get(i);
				final Point2D translate = event.moveDistance.get(i);
				final AffineTransform transform = new AffineTransform();
				transform.translate(translate.getX(), translate.getY());
				item.getGeometry().applyTransform(transform);
			}
		}
	}

	@Override
	public void onItemScaled(final ItemScaleEvent event) {
		for (int i = 0; i < event.items.size(); i++) {
			if (event.items.get(i) instanceof GeometryItem) {
				final GeometryItem item = (GeometryItem)event.items.get(i);
				final double dx = event.getNewSceneBounds().get(i).getCenterX() - event.getOldSceneBounds().get(i).getCenterX();
				final double dy = event.getNewSceneBounds().get(i).getCenterY() - event.getOldSceneBounds().get(i).getCenterY();
				final double[] scale = event.getScaleFactors(i);
				final AffineTransform transform_t = new AffineTransform();
				transform_t.translate(dx, dy);
				final AffineTransform transform_s = new AffineTransform();
				transform_s.scale(scale[0], scale[1]);
				//				transform_s.concatenate(transform_t);
				item.getGeometry().applyTransform(transform_s);
			}
		}
	}

	@Override
	public void onItemRotated(final ItemRotateEvent event) {
		for (int i = 0; i < event.items.size(); i++) {
			if (event.items.get(i) instanceof GeometryItem) {
				final GeometryItem item = (GeometryItem)event.items.get(i);
				final Rectangle2D sb = item.getLocalBounds();
				final double angle = event.endAngles.get(i) - event.startAngles.get(i);
				final double angleRad = Math.toRadians(angle);
				final AffineTransform transf = new AffineTransform();
				transf.rotate(angleRad);//, sb.getCenterX(), sb.getCenterY());
				item.getGeometry().applyTransform(transf);
			}
		}
	}

}
