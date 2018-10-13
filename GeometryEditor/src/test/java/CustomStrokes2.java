
/*
 * Copyright (c) 2000 David Flanagan.  All rights reserved.
 * This code is from the book Java Examples in a Nutshell, 2nd Edition.
 * It is provided AS-IS, WITHOUT ANY WARRANTY either expressed or implied.
 * You may study, use, and modify it for any non-commercial purpose.
 * You may distribute it non-commercially as long as you retain this notice.
 * For a commercial use license, or to purchase the book (recommended),
 * visit http://www.davidflanagan.com/javaexamples2.
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.Arc2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/** A demonstration of writing custom Stroke classes */
public class CustomStrokes2 extends JPanel {
	static final int WIDTH = 750, HEIGHT = 200; // Size of our example

	public String getName() {
		return "Custom Strokes";
	}

	

	// These are the various stroke objects we'll demonstrate
	Stroke[] strokes = new Stroke[] { new BasicStroke(777.0f), // The standard,
			// predefined
			// stroke
			new NullStroke(), // A Stroke that does nothing
			new DoubleStroke(80.0f, 20.0f), // A Stroke that strokes twice
			new ControlPointsStroke(2.0f), // Shows the vertices & control
			// points
			new SloppyStroke(425.0f, 0.0f) // Perturbs the shape before stroking
	};

	/** Draw the example */
	public void paint(Graphics g1) {
		Graphics2D g = (Graphics2D) g1;
		// Get a shape to work with. Here we'll use the letter B
		Font f = new Font("Serif", Font.BOLD, 200);
		Shape shape = new Arc2D.Double(-200, -200, 400, 400, 0, 360, Arc2D.CHORD);//gv.getOutline();

		// Set drawing attributes and starting position
		g.setColor(Color.black);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.translate(350, 350);

		
		
		
		g.setPaint(Color.green);
		g.setStroke(new MyStroke(100));
		g.draw(shape);
		
		g.setPaint(Color.red);
		g.setStroke(new BasicStroke(1));
		g.draw(shape);
	}

	public static void main(String[] a) {
		JFrame f = new JFrame();
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		f.setContentPane(new CustomStrokes2());
		f.setSize(800, 800);
		f.setVisible(true);
	}

	
	static class MyStroke implements Stroke {
		private BasicStroke mStroke;

		MyStroke(float w){
			mStroke = new BasicStroke(w);
		}
		protected GeneralPath getGappedShape(Shape s) {
			GeneralPath middle = new GeneralPath();

			PathIterator pi2 = s.getPathIterator(null);
			FlatteningPathIterator pi = new FlatteningPathIterator(pi2, 01);
			
			
			double[] coords = new double[6];
			double[] first = null;
			for (; pi.isDone() == false; pi.next()) {
				int type = pi.currentSegment(coords);
				if (first == null) {
					first = new double[] {coords[0], coords[1]};
				}
				switch(type) {
				case PathIterator.SEG_MOVETO:
					middle.moveTo(coords[0], coords[1]);
//					System.out.println("Move To: " + coords[0] + coords[1]);
					break;
				case PathIterator.SEG_LINETO:
					middle.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					middle.closePath();
					break;
				default:
					System.out.println();
				}
			}
			return middle;
		}

		@Override
		public Shape createStrokedShape(Shape p) {
			return mStroke.createStrokedShape(getGappedShape(p));
		}
	}
}

