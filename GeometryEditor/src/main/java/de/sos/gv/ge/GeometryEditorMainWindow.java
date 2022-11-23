package de.sos.gv.ge;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import de.sos.gv.ge.items.AxisItem;
import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.items.GridItem;
import de.sos.gv.ge.menu.DefaultContextMenuCallback;
import de.sos.gv.ge.menu.MenuManager;
import de.sos.gv.ge.model.geom.GeometryUtils;
import de.sos.gv.ge.tools.AbstractTool;
import de.sos.gv.ge.tools.EditTool;
import de.sos.gv.ge.tools.LineStringTool;
import de.sos.gv.ge.tools.LinearRingTool;
import de.sos.gv.ge.tools.PointTool;
import de.sos.gv.ge.tools.PolygonTool;
import de.sos.gv.ge.tools.SelectionTool;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsViewComponent;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;

public class GeometryEditorMainWindow {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final MenuManager mm = new MenuManager();
					mm.registerCallback(new DefaultContextMenuCallback());
					final GeometryEditorMainWindow window = new GeometryEditorMainWindow(mm);
					window.frame.setVisible(true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private GraphicsViewComponent	mView;
	private GraphicsScene			mScene;

	private GridItem				mGridItem;		// can be activated or
	// deactivated
	private MenuManager				mMenuManager;
	private AxisItem				mAxisItem;

	/**
	 * Create the application.
	 */
	public GeometryEditorMainWindow(final MenuManager mm) {
		mMenuManager = mm;
		initialize();

	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 869, 629);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		final JToolBar		toolBar	= new JToolBar();
		final ButtonGroup	bg		= new ButtonGroup();

		frame.getContentPane().add(toolBar, BorderLayout.NORTH);

		final JSplitPane splitPane = new JSplitPane();
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);

		splitPane.setRightComponent(createSceneAndView());

		final JPanel mOptionPalette = new JPanel();
		splitPane.setLeftComponent(mOptionPalette);

		final JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		addToogleButton("Select", toolBar, bg, new SelectionTool()).setSelected(true);
		addToogleButton("Edit", toolBar, bg, new EditTool());
		toolBar.add(new JSeparator(SwingConstants.HORIZONTAL));
		addToogleButton("Point", toolBar, bg, new PointTool(mMenuManager));
		addToogleButton("LineString", toolBar, bg, new LineStringTool(mMenuManager));
		addToogleButton("LinearRing", toolBar, bg, new LinearRingTool(mMenuManager));
		addToogleButton("Polygon", toolBar, bg, new PolygonTool(mMenuManager));

	}

	private JToggleButton addToogleButton(final String name, final JToolBar tb, final ButtonGroup bg, final AbstractTool tool) {
		final JToggleButton btn = new JToggleButton(name);
		tb.add(btn);
		bg.add(btn);
		btn.addChangeListener(cl -> {
			final boolean selected = btn.getModel().isSelected();
			if (selected) {
				if (!tool.isActive())
					tool.install(mView);
			} else if (tool.isActive())
				tool.uninstall(mView);
		});
		return btn;
	}

	private Component createSceneAndView() {
		mScene = new GraphicsScene();
		mView = new GraphicsViewComponent(mScene);

		// Standard Handler
		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());

		mScene.addItem(mGridItem = new GridItem());
		mScene.addItem(mAxisItem = new AxisItem());
		GeometryItem testItem = new GeometryItem(mMenuManager, GeometryUtils.geometryFromWKT("POLYGON ((60 -90, -70 -90, 70 10, 120 -30, 60 -90))"));
		mScene.addItem(testItem);

		testItem = new GeometryItem(mMenuManager, GeometryUtils.geometryFromWKT("POLYGON ((40 -70, -50 -70, 50 10, 100 -30, 40 -90))"));
		testItem.setCenter(150, 150);
		mScene.addItem(testItem);

		return mView;
	}

}
