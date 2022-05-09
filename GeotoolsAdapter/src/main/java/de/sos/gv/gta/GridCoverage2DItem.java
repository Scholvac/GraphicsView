package de.sos.gv.gta;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.geometry.Envelope;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonBox;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.IDrawable;

public class GridCoverage2DItem extends GraphicsItem {

	public static GridCoverage2DItem createGeoTiffItem(final File tiffFile) throws Exception {
		final GeoTiffReader reader = new GeoTiffReader(tiffFile);
		final GridCoverage2D grid = reader.read(null);
		final GridCoverage2DItem item = new GridCoverage2DItem(grid);

		return item;
	}

	public GridCoverage2DItem(final GridCoverage2D coverage) {
		this(coverage, null);
	}

	public GridCoverage2DItem(final GridCoverage2D coverage, final ColorModel colorModel) {
		final Envelope env = coverage.getEnvelope();
		final LatLonBox bb = GTUtils.getLatLonBox(env);

		final Point2D.Double ll = GeoUtils.getXY(bb.getLowerLeft());
		final Point2D.Double ur = GeoUtils.getXY(bb.getUpperRight());
		final double w = ur.getX() - ll.getX(), h = ur.getY() - ll.getY();
		final Rectangle2D.Double shape = new Rectangle2D.Double(-w*0.5, h*0.5, w, -h);
		setShape(shape);

		final RenderedImage image = coverage.getRenderedImage();
		final ColorModel _colorModel = colorModel != null ? colorModel : image.getColorModel();
		//TODO: if we get some performance issues, we may split the image into it's tiles and
		//		use the  image.getDate(new Rectangle()) method to get only parts of the image.
		setDrawable(new RasterDrawable(getBoundingBox(), image.getData(), _colorModel));
		setCenter(GeoUtils.getXY(bb.getCenter()));
	}

	public ColorModel setColorTable(final ColorModel customColorTable) {
		final IDrawable drawable = getDrawable();
		if (!(drawable instanceof RasterDrawable))
			throw new ClassCastException("Drawable is not a RasterDrawable");
		final ColorModel old = ((RasterDrawable)drawable).setColorTable(customColorTable);
		markDirty();
		return old;
	}

}