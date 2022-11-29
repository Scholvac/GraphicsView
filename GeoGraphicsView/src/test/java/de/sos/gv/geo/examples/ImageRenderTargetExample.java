package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;

public class ImageRenderTargetExample extends JFrame {

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final ImageRenderTargetExample frame = new ImageRenderTargetExample("Image Render Target");
					frame.setVisible(true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private GraphicsScene 		mScene;
	private GraphicsView		mView;


	/**
	 * Create the frame.
	 */
	public ImageRenderTargetExample(final String title) {
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
		configure();
	}

	// TODO: do the example specific part
	public void configure() {
		final LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);
		GeoUtils.setViewCenter(mView, llp_brhv);
		mView.setScale(20);
	}


	private void createScene() {
		mScene = new GraphicsScene();
		//@since Version 2.0.0 the render target to which the GraphicsView shall
		//paint its content, can be defined when creating the view.
		//To paint into a UI, use a JPanelRenderTarget, however the resulting image may only displayed once
		//and can not be reused. For this purpose the ImageRenderTarget can be used.
		//There are two ImageREnderTargets available:
		// 1) BufferedImageRenderTarget: The view draws into a BufferedImage, with easy handling.
		// 2) VolatileImageRenderTarget: Draws into a VolatileImage, which is way more performant as an BufferedImage, but not that easy to handle.
		// The rendered image can be accessed either using a callback: {@link ImageRenderTarget#getImage()}
		final BufferedImageRenderTarget rt = new BufferedImageRenderTarget(1,1, true);
		mView = new GraphicsView(mScene, rt);

		//Standard Handler
		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());

		setupMap();
	}
	private void setupMap() {
		/**
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and initializes a
		 * standard TileFactory with 4 threads. For more informations on how to
		 * initialize the Tile Background, see OSMExample
		 */
		final ITileImageProvider cache = ITileFactory.buildCache(ITileImageProvider.OSM, 10, SizeUnit.MegaByte, new File("./.cache"), 100, SizeUnit.MegaByte);
		mView.addHandler(new TileHandler(new TileFactory(cache)));
	}

}
