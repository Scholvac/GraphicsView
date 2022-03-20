package de.sos.gv.geo.examples;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.styles.DrawableStyle;


/**
 * 
 * @author scholvac
 *
 */
public class RotationExample {
		
	private static Shape createShape() {
		Path2D.Double p = new Path2D.Double(new Arc2D.Double(new Rectangle2D.Double(-50, -50, 100, 100), 0, 360, Arc2D.OPEN));
		p.append(new Rectangle2D.Double(-10, 0, 20, 150), false);
		return p;
//		return new Rectangle2D.Double(-100, -100, 200, 200);
	}
	
	public static void main(String[] args) {
		
		GraphicsScene scene = new GraphicsScene();
		GraphicsView view = new GraphicsView(scene);
		
		GraphicsItem item = new GraphicsItem(createShape()) {
			@Override
			public void draw(Graphics2D g, IDrawContext ctx) {
				super.draw(g, ctx);
				g.setColor(Color.RED);
				g.draw(getSceneBounds());
			}
		};
		scene.addItem(item);
		item.setSelectable(false);
		item.setMouseWheelSupport(new MouseWheelListener() {			
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				System.out.println("MouseWheel: " + e.getWheelRotation());
				e.consume();
			}
		});
		
		item.setRotation(0);
				
		DrawableStyle style = new DrawableStyle();
		style.setName("default");
		style.setFillPaint(Color.GREEN);
		style.setLinePaint(Color.BLACK);
		item.setStyle(style);
		
		view.setCenter(0, 00);
		view.setScale(1);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLocation(2000, 200);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);
		
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JButton l = new JButton("Left");
		l.addActionListener( al -> item.setRotation(item.getRotation()-1));
		p.add(l, BorderLayout.WEST);
		JButton r = new JButton("Right");
		r.addActionListener( al -> item.setRotation(item.getRotation()+1));
		p.add(r, BorderLayout.EAST);
		frame.add(p, BorderLayout.SOUTH);
		
		
	}

}
