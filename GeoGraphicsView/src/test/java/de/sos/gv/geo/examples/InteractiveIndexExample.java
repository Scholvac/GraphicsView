package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;

import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsViewComponent;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.index.BIGQuadTree;
import de.sos.gvc.index.IBIGNode;
import de.sos.gvc.index.IBIGNode.IBIGNodeFactory;
import de.sos.gvc.index.handler.MinimumLeafSizeSplitHandler;
import de.sos.gvc.index.impl.BIGLeaf;
import de.sos.gvc.index.impl.BIGNode;
import de.sos.gvc.index.impl.DefaultEntry;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.storage.ListStorage;
import de.sos.gvc.styles.DrawableStyle;

/**
 *
 * @author scholvac
 *
 */
public class InteractiveIndexExample {

	interface INodeItemProvider {
		GraphicsItem getItem();
	}

	static class MyBigLeaf extends BIGLeaf<DefaultEntry<GraphicsItem>> implements INodeItemProvider {
		public MyBigLeaf(final Rectangle2D geom, final IBIGNode<DefaultEntry<GraphicsItem>> parent) {
			super(geom, parent);
		}

		private GraphicsItem mItem = null;
		@Override
		public GraphicsItem getItem() {
			if (mItem == null) {
				mItem = new GraphicsItem(getQTBounds());
				final DrawableStyle ls = new DrawableStyle();
				ls.setLinePaint(Color.RED);
				mItem.setStyle(ls);
				mItem.setZOrder(20);
			}
			return mItem;
		}
	}

	static class MyBigNode extends BIGNode<DefaultEntry<GraphicsItem>> implements INodeItemProvider {
		public MyBigNode(final Rectangle2D geom, final IBIGNode<DefaultEntry<GraphicsItem>> parent) {
			super(geom, parent);
		}

		private GraphicsItem mItem = null;
		@Override
		public GraphicsItem getItem() {
			if (mItem == null) {
				mItem = new GraphicsItem(getQTBounds()) {
					@Override
					public boolean hasChildren() {
						return super.hasChildren();
					}
					@Override
					public List<GraphicsItem> getChildren() {
						final ArrayList<GraphicsItem> out = new ArrayList<>();
						for (int i = 0; i < 4; i++) {
							final IBIGNode c = getChild(i);
							if (c instanceof INodeItemProvider)
								out.add(((INodeItemProvider) c).getItem());
						}
						return out;
					}
				};
				final DrawableStyle ls = new DrawableStyle();
				ls.setLinePaint(Color.BLUE);
				mItem.setStyle(ls);
			}
			return mItem;
		}
	}

	static class MyBIGNodeFactory implements IBIGNodeFactory<DefaultEntry<GraphicsItem>> {
		@Override
		public IBIGNode<DefaultEntry<GraphicsItem>> createNode(final Rectangle2D geometry, final IBIGNode<DefaultEntry<GraphicsItem>> parent) {
			return new MyBigNode(geometry, parent);
		}
		@Override
		public IBIGNode<DefaultEntry<GraphicsItem>> createLeaf(final Rectangle2D geometry, final IBIGNode<DefaultEntry<GraphicsItem>> parent) {
			return new MyBigLeaf(geometry, parent);
		}

	}

	public static void main(final String[] args) {
		final JFrame frame = new JFrame("Interactive Index Example");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());

		final GraphicsScene			treeScene	= new GraphicsScene(new ListStorage());

		final GraphicsScene			itemScene	= new GraphicsScene(new ListStorage());

		/**
		 * Create a cache cascade: RAM (10 MB) -> HDD (100MB) -> WEB and
		 * initializes a standard TileFactory with 4 threads. For more
		 * informations on how to initialize the Tile Background, see OSMExample
		 */
		final ITileImageProvider	cache		= ITileFactory.buildCache(ITileImageProvider.OSM, 10, SizeUnit.MegaByte, new File("./.cache"), 100, SizeUnit.MegaByte);
		final TileFactory			factory		= new TileFactory(cache);

		final JPanel				actionPanel	= new JPanel();
		actionPanel.setLayout(new BorderLayout());
		frame.getContentPane().add(actionPanel, BorderLayout.SOUTH);
		final JButton btn = new JButton("Add");
		actionPanel.add(btn, BorderLayout.WEST);
		final JButton btn10 = new JButton("Add 50");
		actionPanel.add(btn10, BorderLayout.CENTER);
		final JButton btn100 = new JButton("Add 500");
		actionPanel.add(btn100, BorderLayout.EAST);

		final JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.5);
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);

		final GraphicsViewComponent treeView = new GraphicsViewComponent(treeScene);
		splitPane.setLeftComponent(treeView);
		final ParameterContext		itemViewContext	= new ParameterContext();
		final GraphicsViewComponent	itemView		= new GraphicsViewComponent(itemScene, itemViewContext);
		splitPane.setRightComponent(itemView);
		itemView.addHandler(new MouseDelegateHandler());
		itemView.addHandler(new DefaultViewDragHandler());
		itemView.addHandler(new TileHandler(factory));
		treeView.addHandler(new MouseDelegateHandler());
		treeView.addHandler(new DefaultViewDragHandler());

		treeView.addHandler(new TileHandler(factory));

		final Rectangle2D								r2d			= new Rectangle2D.Double(-1000, -1000, 2000, 2000);

		final BIGQuadTree<DefaultEntry<GraphicsItem>>	quadTree	= new BIGQuadTree<DefaultEntry<GraphicsItem>>(r2d, 4, new MyBIGNodeFactory()) {
			@Override
			protected void replaceRoot(final IBIGNode<DefaultEntry<GraphicsItem>> newNode) {
				treeScene.removeItem(((INodeItemProvider) getRoot()).getItem());
				super.replaceRoot(newNode);
				treeScene.addItem(((INodeItemProvider) getRoot()).getItem());
			}
		};
		final MinimumLeafSizeSplitHandler				mlssh		= new MinimumLeafSizeSplitHandler(200 * 20);
		quadTree.addHandler(mlssh);

		treeScene.addItem(((INodeItemProvider) quadTree.getRoot()).getItem());

		final GraphicsItem	bgItem	= new GraphicsItem(r2d);
		final DrawableStyle	s		= new DrawableStyle();
		s.setFillPaint(Color.WHITE);
		bgItem.setStyle(s);
		itemScene.addItem(bgItem);

		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				addItem(itemScene, treeScene, quadTree);
			}
		});
		btn10.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				for (int i = 0; i < 50; i++)
					addItem(itemScene, treeScene, quadTree);
			}
		});
		btn100.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				for (int i = 0; i < 500; i++)
					addItem(itemScene, treeScene, quadTree);
			}
		});

		itemViewContext.registerListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent evt) {
				treeView.setScale(itemView.getScaleX(), itemView.getScaleY());
				treeView.setCenter(itemView.getCenterX(), itemView.getCenterY());
			}
		});

		frame.setSize(1000, 600);
		frame.setVisible(true);

	}

	public static Random mRng = new Random();

	protected static void addItem(final GraphicsScene itemScene, final GraphicsScene treeScene, final BIGQuadTree<DefaultEntry<GraphicsItem>> qt) {
		final int			w	= mRng.nextInt(70);
		final int			h	= mRng.nextInt(70);
		final float			x	= mRng.nextFloat() * 2000 - 1000;
		final float			y	= mRng.nextFloat() * 2000 - 1000;
		final Rectangle2D	r	= new Rectangle2D.Double(-w / 2.0, -h / 2.0, w, h);
		final GraphicsItem	gi	= new GraphicsItem(r);
		final DrawableStyle	st	= new DrawableStyle();
		st.setFillPaint(new Color(mRng.nextFloat(), mRng.nextFloat(), mRng.nextFloat()));
		gi.setStyle(st);
		gi.setCenter(x, y);

		itemScene.addItem(gi);
		final DefaultEntry<GraphicsItem> de = new DefaultEntry<GraphicsItem>(gi, r) {
			@Override
			public Rectangle2D getGeometry() {
				return gi.getSceneBounds();
			}
		};
		qt.insert(de);
		treeScene.markDirty();

	}
}
