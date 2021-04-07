package de.sos.gv.ge;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import de.sos.gv.ge.callbacks.GeometryInteractions;
import de.sos.gv.ge.items.AxisItem;
import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.items.GridItem;
import de.sos.gv.ge.menu.DefaultContextMenuCallback;
import de.sos.gv.ge.menu.MenuManager;
import de.sos.gv.ge.model.geom.GeometryUtils;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.handler.SelectionHandler;

public class GeometryEditorMainWindow {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MenuManager mm = new MenuManager();
					mm.registerCallback(new DefaultContextMenuCallback());
					GeometryEditorMainWindow window = new GeometryEditorMainWindow(mm);
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private GraphicsView			mView;
	private GraphicsScene			mScene;
	
	
	private GridItem 				mGridItem; //can be activated or deactivated
	private MenuManager				mMenuManager;
	private AxisItem mAxisItem;
	
	/**
	 * Create the application.
	 */
	public GeometryEditorMainWindow(MenuManager mm) {
		mMenuManager = mm;
		initialize();
		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 869, 629);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JToolBar toolBar = new JToolBar();
		frame.getContentPane().add(toolBar, BorderLayout.NORTH);
		
		JSplitPane splitPane = new JSplitPane();
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);
		
		splitPane.setRightComponent(createSceneAndView());
		
		JPanel mOptionPalette = new JPanel();
		splitPane.setLeftComponent(mOptionPalette);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
	}

	private Component createSceneAndView() {
		mScene = new GraphicsScene();
		mView = new GraphicsView(mScene);
		
		//Standard Handler
		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());
		
		SelectionHandler selectionHandler = new SelectionHandler();
		GeometryInteractions callback = new GeometryInteractions();
		selectionHandler.addMoveCallback(callback);
		selectionHandler.addScaleCallback(callback);
		selectionHandler.addRotationCallback(callback);
	
		mView.addHandler(selectionHandler);
		
		mScene.addItem(mGridItem = new GridItem());
		mScene.addItem(mAxisItem = new AxisItem());
		GeometryItem testItem = new GeometryItem(mMenuManager, GeometryUtils.geometryFromWKT("POLYGON ((60 -90, -70 -90, 70 10, 120 -30, 60 -90))"));
		mScene.addItem(testItem);
		
		testItem = new GeometryItem(mMenuManager, GeometryUtils.geometryFromWKT("POLYGON ((40 -70, -50 -70, 50 10, 100 -30, 40 -90))"));
		mScene.addItem(testItem);
		
		return mView;
	}

}
