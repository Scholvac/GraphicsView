package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.sos.gv.geo.examples.ExampleUtils.WKTCollection;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.IPaintListener.PaintAdapter;
import de.sos.gvc.Utils.WindowStat;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.rt.IRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget.VolatileImageRenderTarget;
import de.sos.gvc.styles.DrawableStyle;

public class RenderBenchmarkExample extends JFrame {

	/**
	 * Launch the application.
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final RenderBenchmarkExample frame = new RenderBenchmarkExample("Render Benchmark");
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
	public RenderBenchmarkExample(final String title) {
		super(title);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setBounds(100, 100, 900, 800);
		final JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		contentPane.setLayout(new BorderLayout());

		createScene();
		createView();


		final JPanel statParent = new JPanel();
		contentPane.add(statParent, BorderLayout.WEST);
		final GridBagLayout gbl_statParent = new GridBagLayout();
		gbl_statParent.columnWidths = new int[]{0, 0};
		gbl_statParent.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_statParent.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_statParent.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		statParent.setLayout(gbl_statParent);

		final JLabel lblNewLabel = new JLabel("Mode");
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 12));
		final GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		statParent.add(lblNewLabel, gbc_lblNewLabel);

		final JLabel lblMode = new JLabel("New label");
		lblMode.setFont(new Font("Tahoma", Font.ITALIC, 10));
		final GridBagConstraints gbc_lblMode = new GridBagConstraints();
		gbc_lblMode.insets = new Insets(0, 0, 5, 0);
		gbc_lblMode.gridx = 0;
		gbc_lblMode.gridy = 1;
		statParent.add(lblMode, gbc_lblMode);

		final JLabel lblNewLabel_1 = new JLabel("FPS");
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 12));
		final GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 2;
		statParent.add(lblNewLabel_1, gbc_lblNewLabel_1);

		final JLabel lblFPS = new JLabel("New label");
		lblFPS.setFont(new Font("Tahoma", Font.ITALIC, 10));
		final GridBagConstraints gbc_lblFPS = new GridBagConstraints();
		gbc_lblFPS.anchor = GridBagConstraints.WEST;
		gbc_lblFPS.insets = new Insets(0, 0, 5, 0);
		gbc_lblFPS.gridx = 0;
		gbc_lblFPS.gridy = 3;
		statParent.add(lblFPS, gbc_lblFPS);

		final JLabel lblNewLabel_2 = new JLabel("Frame Count:");
		lblNewLabel_2.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_2.setFont(new Font("Tahoma", Font.BOLD, 12));
		final GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 4;
		statParent.add(lblNewLabel_2, gbc_lblNewLabel_2);

		final JLabel lblFrmCount = new JLabel("New label");
		lblFrmCount.setFont(new Font("Tahoma", Font.ITALIC, 10));
		final GridBagConstraints gbc_lblFrmCount = new GridBagConstraints();
		gbc_lblFrmCount.anchor = GridBagConstraints.WEST;
		gbc_lblFrmCount.insets = new Insets(0, 0, 5, 0);
		gbc_lblFrmCount.gridx = 0;
		gbc_lblFrmCount.gridy = 5;
		statParent.add(lblFrmCount, gbc_lblFrmCount);

		final JLabel lblNewLabel_3 = new JLabel("Average Duration");
		lblNewLabel_3.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_3.setFont(new Font("Tahoma", Font.BOLD, 12));
		final GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 6;
		statParent.add(lblNewLabel_3, gbc_lblNewLabel_3);

		final JLabel lblAvg = new JLabel("New label");
		lblAvg.setFont(new Font("Tahoma", Font.ITALIC, 10));
		final GridBagConstraints gbc_lblAvg = new GridBagConstraints();
		gbc_lblAvg.anchor = GridBagConstraints.WEST;
		gbc_lblAvg.gridx = 0;
		gbc_lblAvg.gridy = 7;
		statParent.add(lblAvg, gbc_lblAvg);

		final JPanel viewParent = new JPanel();
		contentPane.add(viewParent, BorderLayout.CENTER);

		viewParent.setLayout(new BorderLayout());
		final Component viewComponent = mView.getComponent();
		if (viewComponent != null)
			viewParent.add(viewComponent, BorderLayout.CENTER);

		lblMode.setText(mView.getRenderTarget().getClass().getSimpleName());
		mView.addPaintListener(new PaintAdapter() {
			int counter = 0;
			@Override
			public void postPaint(final Graphics2D graphics, final IDrawContext context) {
				counter++;
				if (counter % 250 == 0) {
					SwingUtilities.invokeLater(() -> {
						final WindowStat stat = mView.getMovingWindowDurationStatistic();
						final double fps = 1.0f / stat.avg();
						lblFPS.setText(String.format("%1.2f [FPS]", fps));
						lblAvg.setText(String.format("%1.5f[sec]", stat.avg()));
						lblFrmCount.setText("" + stat.count());
						//						System.out.println(fps + "; " + mView.getCenterX() + "; " + mView.getCenterY() + "; " + mView.getScaleX());
						System.out.println(stat + " : " + mView.getPaintDurationStatistic().getAverage());
					});
				}
			}
		});
	}

	IRenderTarget createRenderTarget(){
		return new VolatileImageRenderTarget(800, 800, true);
	}

	private void createView() {
		mView = new GraphicsView(mScene, createRenderTarget());


		// Standard Handler
		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());

		//We always have a maximum Frames per second, (default = 30).
		//for this benchmark we set the FPS to value we do not expect to reach...
		mView.setMaximumFPS(1000);
		//However as long as nothing changes, the view is not repainted (for performance reasons)
		//thus we have to trigger the repaint manually.
		//that can either be done by:
		// - Change an Item (move, scale, rotate, change shape, style, selection, ...)
		// - Change the View (move, scale, rotate)
		// - mark the scene (or an item) as dirty

		//we choose the last one - as the easiest one....
		final Thread updater = new Thread(() -> {
			while(true) {
				//try to reach a update rate of 1000FPS / e.g. sleep 1ms
				try { Thread.sleep(1);}catch(final Exception e) {e.printStackTrace();}
				mScene.markDirty();
			}
		}, "Updater");
		updater.setDaemon(true);
		updater.start();

		mView.setCenter(50, -50);
		mView.setScale(0.13);
	}

	private void createScene() {
		mScene = new GraphicsScene();

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
			//			s1.setLinePaint(Color.BLACK);
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
