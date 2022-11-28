package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.sos.gv.geo.examples.ExampleUtils.WKTCollection;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.styles.DrawableStyle;

public class TriangleExample extends JFrame {

	/**
	 * Launch the application.
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final TriangleExample frame = new TriangleExample("Triangle Example");
					frame.setVisible(true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private GraphicsScene	mScene;
	private GraphicsView	mView;



	/**
	 * Create the frame.
	 */
	public TriangleExample(final String title) {
		super(title);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setBounds(100, 100, 800, 800);
		final JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		contentPane.setLayout(new BorderLayout());

		createScene();
		contentPane.add(mView.getComponent(), BorderLayout.CENTER);

	}

	private void createScene() {
		mScene = new GraphicsScene();
		mView = new GraphicsView(mScene);

		// Standard Handler
		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());

		try {
			final ClassLoader cl = getClass().getClassLoader();
			//The file contains a Skierpinski carpet plane fractal (https://en.wikipedia.org/wiki/Sierpi%C5%84ski_carpet)
			//that has been triangulated with a restricted Delaunay triangulation (https://en.wikipedia.org/wiki/Delaunay_triangulation)
			//using the JTS Topology Suite Test Builder: https://github.com/locationtech/jts
			final WKTCollection collection = ExampleUtils.parseWKTGeometryCollection(cl.getResourceAsStream("delaunaySierpinskiCarpet.wkt"));

			//we do use the texture mode of the Drawable Style to colorize the triangles
			//Each triangle of the geometry gets it's own texture, thus none of the images will be fully visible.
			final BufferedImage texture1 = ImageIO.read(cl.getResourceAsStream("texture/pexels-anni-roenkae-2832432.jpg"));
			final BufferedImage texture2 = ImageIO.read(cl.getResourceAsStream("texture/pexels-scott-webb-1029604.jpg"));
			final DrawableStyle s1 = new DrawableStyle("Style1");
			s1.setTexture(texture1);
			s1.setLinePaint(Color.BLACK);
			final DrawableStyle s2 = new DrawableStyle("Style2");
			s2.setTexture(texture2);

			for (int i = 0; i < collection.geometries.length; i++) {
				final GraphicsItem item = new GraphicsItem(collection.geometries[i].shape);
				item.setStyle(i % 2 == 0 ? s1 : s2);
				mScene.addItem(item);
			}
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
