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
import javax.swing.WindowConstants;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsViewComponent;
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
		final Path2D.Double p = new Path2D.Double(new Arc2D.Double(new Rectangle2D.Double(-50, -50, 100, 100), 0, 360, Arc2D.OPEN));
		p.append(new Rectangle2D.Double(-10, 0, 20, 150), false);
		return p;
		//		return new Rectangle2D.Double(-100, -100, 200, 200);
	}

	public static void main(final String[] args) {

		final GraphicsScene scene = new GraphicsScene();
		final GraphicsViewComponent view = new GraphicsViewComponent(scene);

		final GraphicsItem item = new GraphicsItem(createShape()) {
			@Override
			public void draw(final Graphics2D g, final IDrawContext ctx) {
				super.draw(g, ctx);
				g.setColor(Color.RED);
				g.draw(getSceneBounds());
			}
		};
		scene.addItem(item);
		item.setSelectable(false);
		item.setMouseWheelSupport(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(final MouseWheelEvent e) {
				System.out.println("MouseWheel: " + e.getWheelRotation());
				e.consume();
			}
		});

		item.setRotation(0);

		final DrawableStyle style = new DrawableStyle();
		style.setName("default");
		style.setFillPaint(Color.GREEN);
		style.setLinePaint(Color.BLACK);
		item.setStyle(style);

		view.setCenter(0, 00);
		view.setScale(1);

		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLocation(2000, 200);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);
		frame.setVisible(true);

		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());

		final JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		final JButton l = new JButton("Left");
		l.addActionListener( al -> item.setRotation(item.getRotation()+1));
		p.add(l, BorderLayout.WEST);
		final JButton r = new JButton("Right");
		r.addActionListener( al -> item.setRotation(item.getRotation()-1));
		p.add(r, BorderLayout.EAST);
		frame.add(p, BorderLayout.SOUTH);


		item.setCenter(150, 150);

	}

}
