package de.sos.gvc.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.IDrawContext;
import de.sos.gvc.drawables.DrawableStyle;
import de.sos.gvc.gt.GeoGraphicsItem;
import de.sos.gvc.gt.GeoUtils;
import de.sos.gvc.gt.TileHandler;
import de.sos.gvc.gt.proj.LatLonPoint;
import de.sos.gvc.gt.tiles.ITileFactory;
import de.sos.gvc.gt.tiles.TileFactory;
import de.sos.gvc.gt.tiles.cache.MemoryCache;
import de.sos.gvc.gt.tiles.cache.factories.BufferedImageFactory;
import de.sos.gvc.gt.tiles.cache.factories.ByteDataFactory;
import de.sos.gvc.gt.tiles.osm.OSMTileDescription;
import de.sos.gvc.gt.tiles.osm.OSMTileFactory;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.handler.SelectionHandler;
import de.sos.gvc.param.ParameterContext;


/**
 * 
 * @author scholvac
 *
 */
public class GeoItemsIntroduction {
	private static JTextField textField;
	private static double sMoveSpeed = 100;
	public static boolean sFollowMovingObject = false;
	private static GraphicsView view;
	
	public static void main(String[] args) {
		
		//Create a new Scene and a new View 
		GraphicsScene scene = new GraphicsScene();
		view = new GraphicsView(scene, new ParameterContext());
		
		
		//Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());
		
		/**
		 * Create new TileFactory Hierarchie, using different caches that shall speedup the loading and reuse of downloaded tiles. 
		 * The most deepest backup solution will be the OSMTileFactory, that downloads tiles from the default OSM server (https://tile.openstreetmap.org/). 
		 * The first real cache - byteCache - stores the downloaded image as compressed byte[], that is much smaller as the uncompressed BufferedImage. However
		 * to use the data, it first has to be uncompressed into an image. 
		 * The last cache - memImgCache - stores an uncompressed image that can directly be reused and thus is the fastest cache. On the other side it consumes the 
		 * most amount of memory. 
		 * 
		 * @see MemoryCache and OSMTileFactory
		 */
		ITileFactory<OSMTileDescription> webCache = new OSMTileFactory();
		ITileFactory<OSMTileDescription> byteCache = new MemoryCache<>(new ByteDataFactory<>(), webCache, 20*1024*1024, "Byte Cache");
		ITileFactory<OSMTileDescription> memImgcache = new MemoryCache<>(new BufferedImageFactory<>(), byteCache, 2*1024*1024, "Image Cache");
		TileFactory<OSMTileDescription> factory = new TileFactory<>(memImgcache, 8);
		view.addHandler(new TileHandler(factory));
		
		/**
		 * Display a building on the north and south hemisphere, once with and once without corrections of the Latitudal scale effects
		 */
		showBuildingNorth(scene);
		showBuildingSouth(scene);
		
		/**
		 * Creates a number of 100m long items along the null-meridian, with and without correction of the scale effect, to display
		 * the effects with increasing / decreasing latitude
		 */
		showScaleError(scene);
		
		/**
		 * Moves an corrected and one not corrected item along the null-meridian, to show what happens if no scale correction is applied.
		 */
		showMovingObject(scene);
		
		GeoUtils.setViewCenter(view, new LatLonPoint.Double(53.14872455, 8.201263999999998));//center the north building
		view.setScale(3);
		
		JFrame frame = new JFrame("GeoitemIntroduction");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));
		frame.getContentPane().add(view);
		
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.WEST);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0};
		gbl_panel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_panel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		JButton btnCenternorthbuilding = new JButton("Center North Building");
		btnCenternorthbuilding.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GeoUtils.setViewCenter(view, new LatLonPoint.Double(53.14872455, 8.201263999999998));
				sFollowMovingObject = false;
			}
		});
		GridBagConstraints gbc_btnCenternorthbuilding = new GridBagConstraints();
		gbc_btnCenternorthbuilding.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnCenternorthbuilding.insets = new Insets(0, 0, 5, 0);
		gbc_btnCenternorthbuilding.gridx = 0;
		gbc_btnCenternorthbuilding.gridy = 0;
		panel.add(btnCenternorthbuilding, gbc_btnCenternorthbuilding);
		
		JButton btnCenterSouthBuilding = new JButton("Center South Building");
		btnCenterSouthBuilding.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GeoUtils.setViewCenter(view, new LatLonPoint.Double(-33.899142350000005, 18.52832455));
				sFollowMovingObject = false;
			}
		});
		GridBagConstraints gbc_btnCenterSouthBuilding = new GridBagConstraints();
		gbc_btnCenterSouthBuilding.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnCenterSouthBuilding.insets = new Insets(0, 0, 5, 0);
		gbc_btnCenterSouthBuilding.gridx = 0;
		gbc_btnCenterSouthBuilding.gridy = 1;
		panel.add(btnCenterSouthBuilding, gbc_btnCenterSouthBuilding);
		
		JButton btnCenterAquator = new JButton("Center Aquator 0/0");
		btnCenterAquator.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GeoUtils.setViewCenter(view, new LatLonPoint.Double(0, 0));
				sFollowMovingObject = false;
			}
		});
		GridBagConstraints gbc_btnCenterAquator = new GridBagConstraints();
		gbc_btnCenterAquator.insets = new Insets(0, 0, 5, 0);
		gbc_btnCenterAquator.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnCenterAquator.gridx = 0;
		gbc_btnCenterAquator.gridy = 2;
		panel.add(btnCenterAquator, gbc_btnCenterAquator);
		
		JButton btnCenterMediterian = new JButton("Center Mediterian 38/0");
		btnCenterMediterian.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GeoUtils.setViewCenter(view, new LatLonPoint.Double(38, 0));
				sFollowMovingObject = false;
			}
		});
		GridBagConstraints gbc_btnCenterMediterian = new GridBagConstraints();
		gbc_btnCenterMediterian.insets = new Insets(0, 0, 5, 0);
		gbc_btnCenterMediterian.gridx = 0;
		gbc_btnCenterMediterian.gridy = 3;
		panel.add(btnCenterMediterian, gbc_btnCenterMediterian);
		
		JButton btnCenterNorth = new JButton("Center North 80/0");
		btnCenterNorth.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GeoUtils.setViewCenter(view, new LatLonPoint.Double(85, 0));
				sFollowMovingObject = false;
			}
		});
		GridBagConstraints gbc_btnCenterNorth = new GridBagConstraints();
		gbc_btnCenterNorth.insets = new Insets(0, 0, 5, 0);
		gbc_btnCenterNorth.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnCenterNorth.gridx = 0;
		gbc_btnCenterNorth.gridy = 4;
		panel.add(btnCenterNorth, gbc_btnCenterNorth);
		
		JButton btnFollowMovingObject = new JButton("Follow Moving Object");
		btnFollowMovingObject.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sFollowMovingObject = true;
			}
		});
		GridBagConstraints gbc_btnFollowMovingObject = new GridBagConstraints();
		gbc_btnFollowMovingObject.insets = new Insets(0, 0, 5, 0);
		gbc_btnFollowMovingObject.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnFollowMovingObject.gridx = 0;
		gbc_btnFollowMovingObject.gridy = 6;
		panel.add(btnFollowMovingObject, gbc_btnFollowMovingObject);
		
		JButton btnStopFollowing = new JButton("Stop Following");
		btnStopFollowing.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sFollowMovingObject = false;
			}
		});
		GridBagConstraints gbc_btnStopFollowing = new GridBagConstraints();
		gbc_btnStopFollowing.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnStopFollowing.insets = new Insets(0, 0, 5, 0);
		gbc_btnStopFollowing.gridx = 0;
		gbc_btnStopFollowing.gridy = 7;
		panel.add(btnStopFollowing, gbc_btnStopFollowing);
		
		JLabel lblSpeed = new JLabel("Speed");
		GridBagConstraints gbc_lblSpeed = new GridBagConstraints();
		gbc_lblSpeed.insets = new Insets(0, 0, 5, 0);
		gbc_lblSpeed.gridx = 0;
		gbc_lblSpeed.gridy = 8;
		panel.add(lblSpeed, gbc_lblSpeed);
		
		textField = new JTextField();
		textField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sMoveSpeed = Double.parseDouble(textField.getText());
			}
		});
		textField.setText("100");
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 0;
		gbc_textField.gridy = 9;
		panel.add(textField, gbc_textField);
		textField.setColumns(10);
		frame.setVisible(true);
		
		view.addHandler(new SelectionHandler());
	}

	
	private static void showMovingObject(GraphicsScene scene) {
		DrawableStyle markerStyle = new DrawableStyle(); markerStyle.setFillPaint(Color.ORANGE);
		DrawableStyle errorStyle = new DrawableStyle(); errorStyle.setFillPaint(Color.RED);errorStyle.setLinePaint(Color.BLACK);
		DrawableStyle correctedStyle = new DrawableStyle(); correctedStyle.setFillPaint(Color.GREEN);correctedStyle.setLinePaint(Color.BLACK);
		Rectangle2D shape = new Rectangle2D.Double(-500, -3000, 1000, 6000);
		
		/** The error (non - scaling) item that will shrink if it moves further away from the aquator */
		GraphicsItem errorItem = new GraphicsItem(shape);
		errorItem.setStyle(errorStyle);
		errorItem.setCenter(-500, 0);
		errorItem.setZOrder(104);
		scene.addItem(errorItem);
		
		/** The corrected item, that will hold its size, independent from its position on earth */
		GeoGraphicsItem correctedItem = new GeoGraphicsItem(shape);
		correctedItem.setStyle(correctedStyle);
		correctedItem.setCenter(-900, 0);
		scene.addItem(correctedItem); 
		
		GraphicsItem markerItem = new GraphicsItem(new Arc2D.Double(-3, -3, 6, 6, 0, 360, Arc2D.CHORD)) {
			@Override
			public void draw(Graphics2D g, IDrawContext ctx) {
				setScale(ctx.getScale());
				super.draw(g, ctx);
			}
		};
		markerItem.setStyle(markerStyle);
		markerItem.setCenter(-2000, 0);
		markerItem.setZOrder(105);
		scene.addItem(markerItem);
		
		//add a thread that will move the item along the null meridian
		Thread t = new Thread() {
			@Override
			public void run() {
				double upperY = GeoUtils.getPosition(85, 0).getY(), lowerY = GeoUtils.getPosition(-85, 0).getY();
				double dir = 1;
				while(true) {
					try{Thread.sleep(10);}catch(Exception e) {}
					double step = dir * sMoveSpeed;
					double y = errorItem.getCenterY() + step;
					correctedItem.setCenterY(y);
					errorItem.setCenterY(y);
					markerItem.setCenterY(y);
					if (sFollowMovingObject)
						GeoUtils.setViewCenter(view, GeoUtils.getLatLon(errorItem.getCenterX(), errorItem.getCenterY()));
					if (y > upperY) {
						dir *= -1;//run back
					}
					if (y < lowerY) {
						dir *= -1; //also run back
					}
				}
			}
		};
		t.setDaemon(true); //close the thread when application shut down
		t.start();
	}


	private static void showScaleError(GraphicsScene scene) {
		DrawableStyle arrowStyle = new DrawableStyle(); arrowStyle.setFillPaint(Color.BLUE);
		DrawableStyle errorStyle = new DrawableStyle(); errorStyle.setLinePaint(Color.RED);
		DrawableStyle correctedStyle = new DrawableStyle(); correctedStyle.setLinePaint(Color.GREEN);
		//create an arrow shape that points with fix size to the scale error items
		Shape arrowShape = ExampleUtils.wkt2Shape("POLYGON ((-10 0, 0 10, 10 0, 5 0, 5 -10, -5 -10, -5 0, -10 0))");
		//create an 100m (high) rectangle to show the scale effect, correct and error shape only differ in with to be distinguishable even near the aquator
		Rectangle2D errorRect = new Rectangle2D.Double(-1, -50, 2, 100);
		Rectangle2D correctRect = new Rectangle2D.Double(-2, -50, 4, 100);
		
		for (double lat = -85; lat < 85; lat += 0.1) {
			LatLonPoint llp = new LatLonPoint.Double(lat, 0);
			Point2D center_point = GeoUtils.getPosition(llp);
			
			//create the non scaled shape 
			GraphicsItem errorItem = new GraphicsItem(errorRect);
			errorItem.setStyle(errorStyle);
			errorItem.setCenter(center_point);
			errorItem.setZOrder(101); //we want the error item to be displayed above the corrected shape (in most cases this shape is smaller - expect near the aquator)
			scene.addItem(errorItem);
			
			//create the scaled shape
			GeoGraphicsItem correctItem = new GeoGraphicsItem(correctRect);
			correctItem.setStyle(correctedStyle);
			correctItem.setCenter(center_point);
			correctItem.setZOrder(100);
			scene.addItem(correctItem);
			
			//draw an error with fix size
			GraphicsItem arrowItem = new GraphicsItem(arrowShape) {
				@Override
				public void draw(Graphics2D g, IDrawContext ctx) {
					setScale(ctx.getScale()); //shall have a fix size
					super.draw(g, ctx);
				}
			};
			arrowItem.setStyle(arrowStyle);
			arrowItem.setCenter(50, center_point.getY());
			arrowItem.setRotation(270); //point to the rectangles
			scene.addItem(arrowItem);
		}
	}

	/**
	 * Displays some building in south africa (Cape Town)
	 * the coordinates have been extracted manually. See showBuilding doc for more information
	 */
	private static void showBuildingSouth(GraphicsScene scene) {
		LatLonPoint ll1 = new LatLonPoint.Double(-33.8988712, 18.5281386);
		LatLonPoint ll2 = new LatLonPoint.Double(-33.8988801, 18.5285282);
		LatLonPoint ll3 = new LatLonPoint.Double(-33.8994135, 18.5285105);
		LatLonPoint ll4 = new LatLonPoint.Double(-33.8994046, 18.5281209);
		
		LatLonPoint[] llpoints = new LatLonPoint[] {ll1, ll2, ll3, ll4};		
		LatLonPoint center = new LatLonPoint.Double(-33.899142350000005, 18.52832455);
		Point2D[] points = new Point2D[] {
			new Point2D.Double(-17.19922723558501, 30.076126261922475),
			new Point2D.Double(18.836367983317384, 29.08893637574352),
			new Point2D.Double(17.199227235146747, -30.076096455211356),
			new Point2D.Double(-18.836367983098942, -29.088900273540286)
		};
		showBuilding(scene, llpoints, center, points);
	}
	
	/**
	 * Displays a building in north germany (Oldenburg)
	 * @param scene
	 */
	private static void showBuildingNorth(GraphicsScene scene) {
		LatLonPoint ll1 = new LatLonPoint.Double(53.1487781, 8.2010379);
		LatLonPoint ll2 = new LatLonPoint.Double(53.1488183, 8.2014501);
		LatLonPoint ll3 = new LatLonPoint.Double(53.1486710, 8.2014901);
		LatLonPoint ll4 = new LatLonPoint.Double(53.1486308, 8.2010779);
		
		LatLonPoint[] llpoints = new LatLonPoint[] {ll1, ll2, ll3, ll4};		
		LatLonPoint center = new LatLonPoint.Double(53.14872455, 8.201263999999998);
		Point2D[] points = new Point2D[] {
			new Point2D.Double(-15.127513694076859, 5.959505732328196),
			new Point2D.Double(12.451261824645632, 10.433334024232243),
			new Point2D.Double(15.127513694339848, -5.9595534473056375),
			new Point2D.Double(-12.451261824191882, -10.43336622138262)
		};
		showBuilding(scene, llpoints, center, points);
	}

	/**
	 * Creates an Item that has the same form as an building on the OSM tiles. The coordinates have been extracted from OSM using the overpass turbo api (https://overpass-turbo.eu)
	 * @param points 
	 * @param center 
	 * @param llpoints 
	 * @code
	 * 	[out:xml][timeout:25];
		// gather results
		(
  			// query part for: “building”
  			node["building"]({{bbox}});
  			way["building"]({{bbox}});
  			relation["building"]({{bbox}});
		);
		// print results
		out body;
		>;
		out skel qt;
	 * @endcode
	 * The resulting WGS84 Coordinates have been transformed into local coordinates around the center of the bounding envelope of the shape
	 * 
	 * This method creates the following items: 
	 * 1) an unscaled (standard GraphicsItem) Item in RED, to display the error
	 * 2) displays for the corner latLonPoints (BLUE). The corner points will be drawn with a fix size
	 * 3) the corrected (GeoGraphicsItem) in GREEN
	 */
	private static void showBuilding(GraphicsScene scene, LatLonPoint[] llpoints, LatLonPoint center, Point2D[] points) {
		DrawableStyle cornerPointStyle = new DrawableStyle(); cornerPointStyle.setFillPaint(Color.BLUE);
		DrawableStyle errorShapeStyle = new DrawableStyle(); errorShapeStyle.setLinePaint(Color.RED);
		DrawableStyle correctedShapeStyle = new DrawableStyle(); correctedShapeStyle.setLinePaint(Color.GREEN);
		
		//add the corner points
		for (LatLonPoint llp : llpoints) {
			GraphicsItem cornerItem = new GraphicsItem(new Arc2D.Double(-3, -3, 6, 6, 0, 360, Arc2D.CHORD)) {
				@Override
				public void draw(Graphics2D g, IDrawContext ctx) {
					setScale(ctx.getScale()); //allways the same size, to easy find the shape
					super.draw(g, ctx);
				}
			};
			cornerItem.setStyle(cornerPointStyle);
			cornerItem.setCenter(GeoUtils.getPosition(llp));
			cornerItem.setZOrder(101); //shall be displayed over the other items
			scene.addItem(cornerItem);
			
		}
		//build the shape, based on the points array, e.g. the cartesian coordinates
		Path2D shape = new Path2D.Double();
		shape.moveTo(points[0].getX(), points[0].getY());
		for (int i = 1; i < points.length; i++) shape.lineTo(points[i].getX(), points[i].getY());
		shape.closePath();
		
		//create the standard item with no correction
		GraphicsItem errorItem = new GraphicsItem(shape);
		errorItem.setStyle(errorShapeStyle);
		errorItem.setCenter(GeoUtils.getPosition(center)); //set the shape to the correct location
		scene.addItem(errorItem); 
		
		//now do the same but with the correcting shape; NOTE that we can use the same! shape
		GeoGraphicsItem correctedItem = new GeoGraphicsItem(shape);
		correctedItem.setStyle(correctedShapeStyle);
		correctedItem.setCenter(GeoUtils.getPosition(center)); //set the shape to the correct location
		scene.addItem(correctedItem);
	}

}
