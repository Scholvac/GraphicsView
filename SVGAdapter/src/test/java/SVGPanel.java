import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;

public class SVGPanel extends JPanel {
	private SVGDiagram diagram;

	public SVGPanel(final String svgFilePath) {
		try {
			final SVGUniverse universe = new SVGUniverse();
			final URI svgURI = new File("De-facto-territory-control-map-of-the-world-borderless-14-05-2019.svg").toURI();
			diagram = universe.getDiagram(universe.loadSVG(svgURI.toURL()));
			analyse(diagram.getRoot(), "", new HashSet<>());
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void analyse(final SVGElement el, final String pref, final Set<SVGElement> visited) {
		if (visited.contains(el))
			return ;
		visited.add(el);
		// Erstes Path-Element finden
		for (final SVGElement c : el.getChildren(null)) {
			if (c.getId() != null)
				System.out.println(pref+c.getId());

			analyse(c, pref+"\t", visited);
		}

		final Shape s = new Shape() {

			@Override
			public boolean intersects(final double x, final double y, final double w, final double h) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean intersects(final Rectangle2D r) {

				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public PathIterator getPathIterator(final AffineTransform at, final double flatness) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public PathIterator getPathIterator(final AffineTransform at) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Rectangle2D getBounds2D() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Rectangle getBounds() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean contains(final double x, final double y, final double w, final double h) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean contains(final double x, final double y) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean contains(final Rectangle2D r) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean contains(final Point2D p) {
				// TODO Auto-generated method stub
				return false;
			}
		};
	}



	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);
		if (diagram != null) {
			final Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			try {
				final AffineTransform at = new AffineTransform();
				at.setToScale(getWidth()/diagram.getWidth(), getWidth()/diagram.getWidth());
				g2d.transform(at);
				diagram.render(g2d);
			} catch (final SVGException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(final String[] args) {
		final JFrame frame = new JFrame("SVG Salamander Renderer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 400);

		// Passe den Pfad zur SVG-Datei an
		final String svgPath = "path/to/your/gradient_test.svg";

		final SVGPanel svgPanel = new SVGPanel(svgPath);
		frame.add(svgPanel);
		frame.setVisible(true);
	}
}
