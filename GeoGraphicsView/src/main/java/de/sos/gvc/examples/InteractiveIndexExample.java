package de.sos.gvc.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.gt.TileHandler;
import de.sos.gvc.gt.tiles.ITileFactory;
import de.sos.gvc.gt.tiles.TileFactory;
import de.sos.gvc.gt.tiles.cache.MemoryCache;
import de.sos.gvc.gt.tiles.cache.factories.BufferedImageFactory;
import de.sos.gvc.gt.tiles.cache.factories.ByteDataFactory;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;
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
	
	static class MyBigLeaf extends BIGLeaf<DefaultEntry<GraphicsItem>> implements INodeItemProvider
	{
		public MyBigLeaf(Rectangle2D geom, IBIGNode<DefaultEntry<GraphicsItem>> parent) {
			super(geom, parent);
		}		
		
		private GraphicsItem mItem = null;
		@Override
		public GraphicsItem getItem() {
			if (mItem == null) {
				mItem = new GraphicsItem(getQTBounds());
				DrawableStyle ls = new DrawableStyle();
				ls.setLinePaint(Color.RED);
				mItem.setStyle(ls);
				mItem.setZOrder(20);
			}
			return mItem;
		}
	}
	
	static class MyBigNode extends BIGNode<DefaultEntry<GraphicsItem>> implements INodeItemProvider
	{
		public MyBigNode(Rectangle2D geom, IBIGNode<DefaultEntry<GraphicsItem>> parent) {
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
						ArrayList<GraphicsItem> out = new ArrayList<>();
						for (int i = 0; i < 4; i++) {
							IBIGNode c = getChild(i);
							if (c != null && c instanceof INodeItemProvider)
								out.add(((INodeItemProvider)c).getItem());
						}
						return out;
					}
				};
				DrawableStyle ls = new DrawableStyle();
				ls.setLinePaint(Color.BLUE);
				mItem.setStyle(ls);
			}
			return mItem;
		}
	}
	
	static class MyBIGNodeFactory implements IBIGNodeFactory<DefaultEntry<GraphicsItem>>{
		@Override
		public IBIGNode<DefaultEntry<GraphicsItem>> createNode(Rectangle2D geometry, IBIGNode<DefaultEntry<GraphicsItem>> parent) { return new MyBigNode(geometry, parent); }
		@Override
		public IBIGNode<DefaultEntry<GraphicsItem>> createLeaf(Rectangle2D geometry, IBIGNode<DefaultEntry<GraphicsItem>> parent) { return new MyBigLeaf(geometry, parent); }
		
	}
	
	
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("Interactive Index Example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		
		GraphicsScene treeScene = new GraphicsScene(new ListStorage());
		
		GraphicsScene itemScene = new GraphicsScene(new ListStorage());
		
		
		ITileFactory<OSMTileDescription> webCache = new OSMTileFactory();
		ITileFactory<OSMTileDescription> byteCache = new MemoryCache<>(new ByteDataFactory<>(), webCache, 20*1024*1024, "Byte Cache");
		ITileFactory<OSMTileDescription> memImgcache = new MemoryCache<>(new BufferedImageFactory<>(), byteCache, 2*1024*1024, "Image Cache");
		TileFactory<OSMTileDescription> factory = new TileFactory<>(memImgcache, 8);
		
		JPanel actionPanel = new JPanel();
		actionPanel.setLayout(new BorderLayout());
		frame.getContentPane().add(actionPanel, BorderLayout.SOUTH);
		JButton btn = new JButton("Add");
		actionPanel.add(btn, BorderLayout.WEST);
		JButton btn10 = new JButton("Add 50");
		actionPanel.add(btn10, BorderLayout.CENTER);
		JButton btn100 = new JButton("Add 500");
		actionPanel.add(btn100, BorderLayout.EAST);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.5);
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);
		
		GraphicsView treeView = new GraphicsView(treeScene);
		splitPane.setLeftComponent(treeView);
		ParameterContext	itemViewContext = new ParameterContext();
		GraphicsView itemView = new GraphicsView(itemScene, itemViewContext);
		splitPane.setRightComponent(itemView);
		itemView.addHandler(new MouseDelegateHandler());
		itemView.addHandler(new DefaultViewDragHandler());
		itemView.addHandler(new TileHandler(factory));
		treeView.addHandler(new MouseDelegateHandler());
		treeView.addHandler(new DefaultViewDragHandler());
		
		treeView.addHandler(new TileHandler(factory));
		
		Rectangle2D r2d = new Rectangle2D.Double(-1000, -1000, 2000, 2000);
		
		BIGQuadTree<DefaultEntry<GraphicsItem>> quadTree = new BIGQuadTree<DefaultEntry<GraphicsItem>>(r2d, 4, new MyBIGNodeFactory()) {
			@Override
			protected void replaceRoot(IBIGNode<DefaultEntry<GraphicsItem>> newNode) {
				treeScene.removeItem(((INodeItemProvider)getRoot()).getItem());
				super.replaceRoot(newNode);
				treeScene.addItem(((INodeItemProvider)getRoot()).getItem());
			}
		};
		MinimumLeafSizeSplitHandler mlssh = new MinimumLeafSizeSplitHandler(200*20);
		quadTree.addHandler(mlssh);
		
		treeScene.addItem(((INodeItemProvider)quadTree.getRoot()).getItem());
		
		GraphicsItem bgItem = new GraphicsItem(r2d);
		DrawableStyle s = new DrawableStyle();
		s.setFillPaint(Color.WHITE);
		bgItem.setStyle(s);
		itemScene.addItem(bgItem);
		
		
		btn.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				addItem(itemScene, treeScene, quadTree);
			}
		});
		btn10.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < 50; i++)
					addItem(itemScene, treeScene, quadTree);
			}
		});
		btn100.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < 500; i++)
					addItem(itemScene, treeScene, quadTree);
			}
		});
		
		itemViewContext.registerListener(new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				treeView.setScale(itemView.getScaleX(), itemView.getScaleY());
				treeView.setCenter(itemView.getCenterX(), itemView.getCenterY());
			}
		});
		
		frame.setSize(1000, 600);
		frame.setVisible(true);
		
		
	}

	public static Random mRng = new Random();
	
	protected static void addItem(GraphicsScene itemScene, GraphicsScene treeScene, BIGQuadTree<DefaultEntry<GraphicsItem>> qt) {
		int w = mRng.nextInt(70);
		int h = mRng.nextInt(70);
		float x = (mRng.nextFloat() * 2000) - 1000;
		float y = (mRng.nextFloat() * 2000) - 1000;
		Rectangle2D r = new Rectangle2D.Double(-w/2.0, -h/2.0, w, h);
		GraphicsItem gi = new GraphicsItem(r);
		DrawableStyle st = new DrawableStyle();
		st.setFillPaint(new Color(mRng.nextFloat(), mRng.nextFloat(), mRng.nextFloat()));
		gi.setStyle(st);
		gi.setCenter(x, y);
		
		itemScene.addItem(gi);
		DefaultEntry<GraphicsItem> de = new DefaultEntry<GraphicsItem>(gi, r) {
			@Override
			public Rectangle2D getGeometry() {
				return gi.getSceneBounds();
			}
		};
		qt.insert(de);
		treeScene.markDirty();
		
	}
}
