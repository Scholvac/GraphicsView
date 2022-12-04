package example.de.sos.gvc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.LongSummaryStatistics;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.Utils;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.rt.ImageRenderTarget.ImageRenderAdapter;
import de.sos.gvc.rt.ImageRenderTarget.VolatileImageRenderTarget;
import de.sos.gvc.storage.ListStorage;

/**
 *
 * @author scholvac
 *
 */
public class DrawBenchmark {

	public static void main(final String[] args) throws InterruptedException {
		System.setProperty("sun.java2d.ddblit", "true");
		final DrawBenchmark example = new DrawBenchmark();
		example.run();
	}

	private GraphicsScene 						mScene;
	private GraphicsView 						mView;
	private ListStorage 						mItemStore;
	private VolatileImageRenderTarget 			mRenderTarget;
	private Shape mShapeShape;
	private AffineTransform mShapeTransform;
	private LongSummaryStatistics mShapeStat;
	private int[][] mPolygonPoints;
	private AffineTransform mPolygonTransform;
	private LongSummaryStatistics mPolygonStat;



	public static int[][] wkt2Shape(final String wkt) {
		final int idx1 = wkt.lastIndexOf("(")+1;
		final int idx2 = wkt.indexOf(")");
		final String coords1 = wkt.substring(idx1, idx2);
		final String coordArr[] = coords1.split(",");

		final int[] xpoints = new int[coordArr.length];
		final int[] ypoints = new int[coordArr.length];
		for (int i = 0; i < coordArr.length; i++) {
			final String c[] = coordArr[i].trim().split(" ");
			final float cx = Float.parseFloat(c[0]);
			final float cy = Float.parseFloat(c[1]);
			xpoints[i] = (int)cx;
			ypoints[i] = (int)cy;
		}
		return new int[][] {xpoints, ypoints};
	}
	public DrawBenchmark() {
		final JFrame frame = new JFrame("Draw Performance");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());

		setupScene();

		final Component gc = mView.getComponent();
		if (gc != null)
			frame.getContentPane().add(gc, BorderLayout.CENTER);
		frame.setSize(1024, 878);
		frame.setVisible(true);

		final String wkt = "POLYGON ((0 150, 25 75, 100 50, 25 25, 0 -50, -25 25, -100 50, -25 75, 0 150))";
		mShapeTransform = new AffineTransform();
		mShapeShape = Utils.wkt2Shape(wkt);
		mShapeStat = new LongSummaryStatistics();

		mPolygonPoints = wkt2Shape(wkt);
		mPolygonTransform = new AffineTransform();
		mPolygonStat = new LongSummaryStatistics();

		mRenderTarget.addRenderListener(new ImageRenderAdapter() {
			int co = 0;
			@Override
			public void preRender(final Image renderTarget) {
				final Graphics2D g2d = (Graphics2D)renderTarget.getGraphics();
				benchmarkShape2D(g2d, -100);
				benchmarkPolygon(g2d,  100);

				if (co++ % 100 == 0) {
					System.out.println("Shape2D  : " + mShapeStat.getAverage()/1000.);
					System.out.println("Polygon2D: " + mPolygonStat.getAverage() / 1000.);
					System.out.println(mView.getMovingWindowDurationStatistic() + "\n");
				}
			}
		});
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> new Thread(r, "Paint-Scheduler"));
		scheduler.scheduleAtFixedRate(() -> {
			mRenderTarget.requestRepaint();
			//
		}, 100, 10, TimeUnit.MILLISECONDS);
	}

	public void benchmarkShape2D(final Graphics2D g, final int centerX) {
		g.setColor(Color.red);
		mShapeTransform.setToTranslation(100, 500);
		mShapeTransform.scale(0.1, 0.1);
		final long s = System.nanoTime();
		for (int i = 0; i < 100; i++) {
			g.setTransform(mShapeTransform);
			g.fill(mShapeShape);
			mShapeTransform.translate(88, 0);
		}
		final long e = System.nanoTime();
		mShapeStat.accept(e-s);
	}
	public void benchmarkPolygon(final Graphics2D g, final int centerX) {
		g.setColor(Color.blue);
		mPolygonTransform.setToTranslation(100, 100);
		mPolygonTransform.scale(0.1, 0.1);
		final long s = System.nanoTime();
		for (int i = 0; i < 100; i++) {
			g.setTransform(mPolygonTransform);
			g.fillPolygon(mPolygonPoints[0], mPolygonPoints[1], mPolygonPoints[0].length);
			mPolygonTransform.translate(88, 0);
		}
		final long e = System.nanoTime();
		mPolygonStat.accept(e-s);
	}


	public void setupScene() {
		mItemStore = new ListStorage(true);
		mScene = new GraphicsScene(mItemStore);

		mRenderTarget = new VolatileImageRenderTarget(1024, 768, true);
		mRenderTarget.setClearColor(Color.LIGHT_GRAY);
		mView = new GraphicsView(mScene, mRenderTarget);
		mView.enableRepaintTrigger(false);

		mView.addHandler(new DefaultViewDragHandler());
		mView.setScale(2);

	}


	public void run() throws InterruptedException {

	}

}
