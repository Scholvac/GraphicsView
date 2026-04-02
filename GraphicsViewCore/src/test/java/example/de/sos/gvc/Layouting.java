package example.de.sos.gvc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.styles.DrawableStyle;

/**
 *
 * @author scholvac
 *
 */
public class Layouting {

	public static void main(final String[] args) {

		final GraphicsScene scene = new GraphicsScene();
		final GraphicsView view = new GraphicsView(scene);

		final GraphicsItem frameView = GraphicsItem.createFromShape(new Rectangle2D.Double(0, 0, 1, 1));
		frameView.setStyle(new DrawableStyle(null, Color.black, null, null));
		scene.addItem(frameView);

		final GraphicsItem monitorView = GraphicsItem.createFromShape(new Rectangle2D.Double(0, 0, 0.3, 0.7));
		monitorView.setStyle(new DrawableStyle(null, null, null, Color.yellow));
		scene.addItem(monitorView);
		monitorView.setCenter(-0.3, 0);


		final GraphicsItem mapView = GraphicsItem.createFromShape(new Rectangle2D.Double(0, 0, 0.3, 0.7));
		mapView.setStyle(new DrawableStyle(null, null, null, Color.blue));
		scene.addItem(mapView);


		view.setCenter(0.49983038011966974, -0.42551166535885976);
		view.setScale(0.0023211375736600917);

		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view.getComponent(), BorderLayout.CENTER);
		frame.setVisible(true);

		view.addHandler(new MouseDelegateHandler() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				System.out.println(view.getCenterX() + "; " + view.getCenterY() + "; " + view.getScaleX());
			}
		});
		view.addHandler(new DefaultViewDragHandler());

	}

}
