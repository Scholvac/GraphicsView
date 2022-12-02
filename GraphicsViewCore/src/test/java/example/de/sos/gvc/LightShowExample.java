package example.de.sos.gvc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.rt.ImageRenderTarget.VolatileImageRenderTarget;
import de.sos.gvc.storage.ListStorage;
import de.sos.gvc.styles.DrawableStyle;

/**
 *
 * @author scholvac
 *
 */
public class LightShowExample {

	public static final int ROWS = 200;
	public static final int COLUMNS = 2048;

	public static void main(final String[] args) throws InterruptedException {
		System.out.println("Expect: " + ROWS * COLUMNS + " Elements");
		final LightShowExample example = new LightShowExample();
		example.run();
	}

	private GraphicsScene mScene;
	private GraphicsView mView;
	private ListStorage mItemStore;


	public LightShowExample() {

		final JFrame frame = new JFrame("LightShow");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());

		setupScene();

		final Component gc = mView.getComponent();
		if (gc != null)
			frame.getContentPane().add(gc, BorderLayout.CENTER);
		frame.setSize(1024, 878);
		frame.setVisible(true);

		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> new Thread(r, "Paint-Scheduler"));
		scheduler.scheduleAtFixedRate(() -> {
			mRenderTarget.requestRepaint();
			System.out.println(mView.getMovingWindowDurationStatistic());
		}, 100, 1000, TimeUnit.MILLISECONDS);
	}

	private final RowItem[] mRowItems = new RowItem[ROWS];
	private VolatileImageRenderTarget mRenderTarget;
	RowItem getRow(final int r) {
		if (mRowItems[r] == null) {
			mRowItems[r] = new RowItem(r);
			mRowItems[r].setCenter(-1024, r);
			mScene.addItem(mRowItems[r]);
		}
		return mRowItems[r];
	}
	public void run() throws InterruptedException {
		double angle = -Math.PI;
		final double deltaAngle = 2*Math.PI / ROWS;
		int idx = 0;
		while(true) {
			if (idx >= ROWS) idx = 0;
			if (angle > Math.PI) angle = -Math.PI;

			final RowItem ri = getRow(idx);
			ri.update(angle += deltaAngle);
			Thread.sleep(1);

			idx++;
		}
	}
	public void setupScene() {
		mItemStore = new ListStorage(true);
		mScene = new GraphicsScene(mItemStore);

		final GraphicsItem ref = GraphicsItem.createFromWKT("POLYGON ((0 150, 25 75, 100 50, 25 25, 0 -50, -25 25, -100 50, -25 75, 0 150))");
		ref.setStyle(new DrawableStyle(null, null, null, Color.red));
		mScene.addItem(ref);

		mRenderTarget = new VolatileImageRenderTarget(1024, 768, true);
		mRenderTarget.setClearColor(Color.LIGHT_GRAY);
		mView = new GraphicsView(mScene, mRenderTarget);
		mView.enableRepaintTrigger(false);

		mView.addHandler(new DefaultViewDragHandler());
	}

	static class RowItem extends GraphicsItem {

		static class CellItem extends GraphicsItem {
			public final GeneralPath gp = new GeneralPath();
			CellItem(){
				setShape(gp);
			}
		}

		private int mRowIndex;
		private CellItem[] mCellItems;

		public RowItem(final int r) {
			mRowIndex = r;
			mCellItems = new CellItem[COLUMNS];
		}

		CellItem getCellItem(final int idx) {
			if (mCellItems[idx] == null) {
				mCellItems[idx] = new CellItem();
				addItem(mCellItems[idx]);
			}
			return mCellItems[idx];
		}

		final double[] _newVertices = new double[8];
		final AffineTransform _transform = new AffineTransform();


		void updatePath(final GeneralPath p, final double distance, final double angleRad) {
			_transform.setToTranslation(distance, 0);
			_transform.rotate(angleRad);
			_transform.scale(0.7, 0.5);

			final double mix = -0.5;
			final double max = 0.5;
			final double miy = -0.5;
			final double may = 0.5;
			//build all 4 vertices
			final double[] vertices = {
					mix, may, //ul
					max, may, //ur
					max, miy, //lr
					mix, miy  //ll
			};

			_transform.transform(vertices, 0, _newVertices, 0, 4);

			p.reset();
			p.moveTo(_newVertices[0], _newVertices[1]);
			p.lineTo(_newVertices[2], _newVertices[3]);
			p.lineTo(_newVertices[4], _newVertices[5]);
			p.lineTo(_newVertices[6], _newVertices[7]);
			p.closePath();
		}

		public void update(final double phase) {
			final double deltaAngle = 2.0 * Math.PI / COLUMNS;
			double angleRad = phase;
			for (int c = 0; c < COLUMNS; c++, angleRad+=deltaAngle) {
				final CellItem cell = getCellItem(c);
				updatePath(cell.gp, c, angleRad);
				final int color = (int)(Math.sin(angleRad) * 255 + 128);
				cell.setStyle(getStyle(color));
			}
		}

		private static DrawableStyle[] sStyles = new DrawableStyle[255];
		private static DrawableStyle getStyle(final int color) {
			final int c = Math.abs(color % 255);
			if (sStyles[c] == null) {
				final Color fc = new Color(c, (c+23)%255, (c+53)%255);
				sStyles[c] = new DrawableStyle("Cell_" + c, null, null, fc);
			}
			return sStyles[c];
		}
	}
}
