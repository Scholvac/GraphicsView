package de.sos.gv.ge;

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.security.spec.MGF1ParameterSpec;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.JMenuBar;
import javax.swing.JLabel;

import de.sos.gv.ge.items.GeometryItem;
import de.sos.gv.ge.items.GridItem;
import de.sos.gv.ge.model.geom.GeometryUtils;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.styles.DrawableStyle;
import de.sos.gvc.styles.ScaledStroke;

import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JTextField;

public class GeometryEditorMainWindow {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GeometryEditorMainWindow window = new GeometryEditorMainWindow();
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
	
	/**
	 * Create the application.
	 */
	public GeometryEditorMainWindow() {
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
		
		
		mScene.addItem(mGridItem = new GridItem());
		
		GeometryItem testItem = new GeometryItem(GeometryUtils.geometryFromWKT("POLYGON ((60 -90, -70 -90, 70 10, 120 -30, 60 -90))"));
		mScene.addItem(testItem);
		
		return mView;
	}

}
