package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.styles.DrawableStyle;


public class ManualGeoGraphicsTest {

	static LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);

	public static void main(final String[] args) throws IOException {

		// Create a new Scene and a new View
		final GraphicsScene scene = new GraphicsScene();
		final GraphicsView view = new GraphicsView(scene, new ParameterContext());

		// Standard Handler
		view.addHandler(new MouseDelegateHandler());
		view.addHandler(new DefaultViewDragHandler());

		view.setScale(3);

		addTiles(scene, view);
		addItems(scene);
		buildAndShowFrame(view);
	}

	private static void addTiles(final GraphicsScene scene, final GraphicsView view) {
		final ITileImageProvider cache = ITileFactory.buildCache(ITileImageProvider.OSM, 10, SizeUnit.MegaByte, new File("./.cache"), 100, SizeUnit.MegaByte);
		view.addHandler(new TileHandler(new TileFactory(cache)));

		GeoUtils.setViewCenter(view, llp_brhv);
		view.setScale(3);
	}

	private static void buildAndShowFrame(final GraphicsView view) {
		final JFrame frame = new JFrame("TilesDevMain");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setLayout(new BorderLayout());
		frame.add(view, BorderLayout.CENTER);

		final JPanel rotPanel = new JPanel();
		rotPanel.setLayout(new BorderLayout());
		final JButton rLeft = new JButton("<<");
		final JButton rRight = new JButton(">>");
		rotPanel.add(rLeft, BorderLayout.WEST);
		rotPanel.add(rRight, BorderLayout.EAST);
		frame.add(rotPanel, BorderLayout.SOUTH);

		rLeft.addActionListener( al -> rotate(view, -5));
		rRight.addActionListener( al -> rotate(view, +5));

		frame.setVisible(true);
	}

	private static final String p1 = "POLYGON ((37.25 0, 28.46875 0, 28.46875 56, 24.80078125 52.98046875, 20.140625 49.953125, 15.25 47.30078125, 10.890625 45.40625, 10.890625 53.90625, 14.4619140625 55.7314453125, 17.80078125 57.73828125, 20.9072265625 59.9267578125, 23.78125 62.296875, 26.337890625 64.7470703125, 28.4921875 67.16015625, 30.244140625 69.5361328125, 31.59375 71.875, 37.25 71.875, 37.25 0))";
	private static final String p2 = "POLYGON ((37.25 4.709518452774109, 37.25 0, 10.890625 0, 11.005969028649274 1.7345130345937914, 11.456463631109644 3.4037369963672393, 12.766597692371203 6.074060074719287, 14.67739122357992 8.705209709379128, 17.391240350478867 11.521345050363276, 21.09313077939234 14.746625247688243, 24.080649936529888 17.272224214621865, 26.540415755036328 19.521432773282697, 28.47242823491166 21.49425092367074, 29.87668737615588 23.190678665786, 30.878330568341315 24.72769225509825, 31.59378999133091 26.22226794707728, 32.023065645124674 27.674405741723085, 32.16615752972259 29.08410563903567, 32.0355793840819 30.480747721784184, 31.64384494715984 31.763133927097094, 30.990954218956407 32.93126425497441, 30.0769071994716 33.98513870541612, 28.957743676209546 34.86110043242239, 27.680798226964995 35.48678738028402, 26.246070851737947 35.86219954900099, 24.6535615505284 35.987336938573314, 22.974544227831903 35.85458249050529, 21.471807401750333 35.45631914630119, 20.145351072283688 34.79254690596103, 18.99517523943197 33.86326576948481, 18.06426187613524 32.70329657571004, 17.395592955333555 31.338754953764862, 16.989168477026915 29.769640903649275, 16.844988441215325 27.995954425363276, 11.813377229194188 28.509561798216644, 12.248637714663145 31.158121852295245, 13.040811798216644 33.47153133256275, 14.18989947985469 35.44979023901916, 15.69590075957728 37.09289857166447, 17.529435554615258 38.38398998668676, 19.65241857249009 39.30619814027411, 22.064849813201782 39.85952303242652, 24.766729276750333 40.04396466314399, 27.4854750841108 39.84483299104194, 29.90497930771136 39.2474379747358, 32.02524194755202 38.25177961422556, 33.846263003632764 36.85785790951123, 35.31254676405631 35.17339983074637, 36.359892307215986 33.29742713837517, 36.988299633111794 31.229939832397623, 37.19776874174373 28.97093791281374, 36.95184656745377 26.596591964580583, 36.21408004458388 24.26141946003963, 34.91482749545905 21.891426116661165, 32.96703682298547 19.3952072324967, 29.944152751403568 16.361441648778072, 25.419620004953767 12.378808206737121, 21.796076463424704 9.273224642916118, 19.752528484147952 7.355902204425363, 18.614322314646632 6.0370629334544255, 17.6893937830251 4.709518452774109, 37.25 4.709518452774109))";
	private static final String p3 = "POLYGON ((10.890625 11.33453125, 15.828614583333334 11.993515625, 16.329442708333335 10.040176106770833, 16.979640625000002 8.380084635416667, 17.779208333333337 7.0132412109375, 18.728145833333336 5.9396458333333335, 19.807232747395837 5.128545898437499, 20.997248697916667 4.549188802083333, 22.298193684895836 4.201574544270834, 23.710067708333334 4.085703125, 25.38608463541667 4.236720377604167, 26.925911458333335 4.689772135416667, 28.329548177083336 5.4448583984375, 29.596994791666667 6.501979166666667, 30.650271484375 7.78260546875, 31.402611979166668 9.208208333333333, 31.85401627604167 10.778787760416666, 32.004484375000004 12.49434375, 31.865548502604167 14.119838541666667, 31.44874088541667 15.595963541666666, 30.7540615234375 16.92271875, 29.781510416666666 18.100104166666668, 28.594240234375 19.068811197916666, 27.255403645833333 19.760744791666667, 25.765000651041667 20.175904947916667, 24.12303125 20.314291666666666, 22.552451822916666 20.1781015625, 20.634807291666668 19.76953125, 21.18835416666667 24.110041666666667, 21.97913541666667 24.048536458333334, 23.520609700520836 24.151228190104167, 24.981907552083335 24.459303385416668, 26.36302897135417 24.972762044270834, 27.663973958333333 25.691604166666668, 28.771067708333334 26.629009440104166, 29.561848958333336 27.78937109375, 30.036317708333335 29.172689127604166, 30.194473958333333 30.778963541666666, 30.079151692708333 32.07826106770833, 29.733184895833336 33.261138020833336, 29.156573567708335 34.327594401041665, 28.349317708333334 35.277630208333335, 27.364136067708333 36.06182161458334, 26.2449609375 36.62195833333333, 24.991792317708335 36.95804036458333, 23.604630208333333 37.07006770833333, 22.2235087890625 36.956392903645835, 20.962102864583336 36.61536848958333, 19.820412434895836 36.04699446614583, 18.7984375 35.251270833333336, 17.922537434895837 34.23313997395834, 17.219071614583335 32.9887578125, 16.6880400390625 31.518124348958334, 16.329442708333335 29.821239583333334, 11.382666666666667 30.699885416666667, 11.979047526041668 33.042574869791665, 12.863184895833335 35.10629427083333, 14.035078776041669 36.89104361979167, 15.494729166666668 38.396822916666665, 17.200400390625 39.59233040364583, 19.1015703125 40.44626432291667, 21.198238932291666 40.95862467447917, 23.49040625 41.12941145833334, 25.10491796875 41.04099772135417, 26.65353125 40.77575651041667, 28.13624609375 40.33368782552083, 29.553062500000003 39.71479166666667, 30.8594990234375 38.94323079427083, 32.002287760416664 38.04316796875, 32.9814287109375 37.01460319010417, 33.796921875 35.857536458333335, 34.4427265625 34.616998372395834, 34.904015625 33.33801953125, 35.1807890625 32.020599934895834, 35.273046875000006 30.664739583333333, 35.18518229166666 29.392899739583335, 34.921588541666665 28.178171875, 34.482265625 27.020555989583332, 33.86721354166667 25.920052083333335, 33.08466959635417 24.90301953125, 32.13408463541667 23.995817708333334, 31.01545865885417 23.198446614583332, 29.72879166666667 22.51090625, 31.413045898437503 21.990857747395832, 32.89850651041667 21.247852864583333, 34.18517350260417 20.2818916015625, 35.273046875000006 19.092973958333335, 36.1379638671875 17.7151474609375, 36.75576171875 16.173673177083334, 37.126440429687506 14.468551106770834, 37.25 12.59978125, 37.009470703125004 10.0643388671875, 36.2878828125 7.7298867187499996, 35.085236328125006 5.5964248046875, 33.401531250000005 3.6639531250000004, 31.354286458333334 2.0609736328125, 29.052234375 0.91598828125, 26.495375000000003 0.2289970703125, 23.683708333333335 0, 21.146618489583332 0.1971461588541666, 18.833583333333337 0.7885846354166667, 16.744602864583335 1.7743154296875, 14.879677083333334 3.1543385416666667, 13.318982421875 4.8517724609375, 12.133908854166668 6.78094921875, 11.324456380208334 8.941868815104167, 10.890625 11.33453125))";
	private static final String p4 = "POLYGON ((27.42657877090565 0, 27.42657877090565 9.124718957084253, 10.890625 9.124718957084253, 10.890625 13.416747199431997, 28.28332084253708 38.10422747712212, 32.10122968602083 38.10422747712212, 32.10122968602083 13.416747199431997, 37.25 13.416747199431997, 37.25 9.124718957084253, 32.10122968602083 9.124718957084253, 32.10122968602083 0, 27.42657877090565 0), \r\n"
			+ "  (27.42657877090565 13.416747199431997, 27.42657877090565 30.59317805301357, 15.49041495739981 13.416747199431997, 27.42657877090565 13.416747199431997))";
	private static final String pCenter = "POLYGON ((32.20789194915254 11.770997276029055, 37.25 11.114520278450362, 36.67387305160412 8.648172669491524, 35.77520618946731 6.464474878934625, 34.553999413589594 4.563426906779661, 33.01025272397094 2.945028753026634, 31.212349141192494 1.656578673577482, 29.21955394975787 0.7362571882566585, 27.03186714966707 0.1840642970641646, 24.6492887409201 0, 21.69685182733051 0.2535870346549636, 19.048719355326877 1.0143481386198547, 17.838767284919797 1.584918966593523, 16.7048913249092 2.282283311894673, 14.665367736077481 4.057392554479419, 13.780662407309322 5.128869008209746, 13.013917789043585 6.310043224122277, 12.365133881280267 7.60091520221701, 11.834310684019371 9.001484942493946, 11.126546421004843 12.13171770959443, 10.890625 15.700741525423728, 10.992059813861985 18.072492622578693, 11.296364255447942 20.291521640435835, 11.80353832475787 22.357828578995157, 12.513582021791768 24.27141343825666, 13.431054214588379 25.997514849424938, 14.560513771186441 27.492253707627118, 15.901960691585957 28.755630012863193, 17.455394975786923 29.787643765133172, 19.15186374470339 30.59342369098063, 20.922414119249396 31.16898078087167, 22.767046099424938 31.514315034806295, 24.685759685230025 31.629426452784504, 27.035856159200968 31.473285222457626, 29.162568099273606 31.004861531476998, 31.06589550544794 30.224155379842614, 32.74583837772397 29.13116676755448, 34.17333393235472 27.755528336864405, 35.31931938559322 26.11775499394673, 36.18379473743947 24.217846738801452, 36.766759987893465 22.05580357142857, 31.779358353510897 21.280796004842614, 31.354813767403147 22.712850427512105, 30.792363423123486 23.954572109564165, 30.092007320671915 25.00596105099879, 29.253745460048428 25.86701725181598, 28.303221473970943 26.541159863044793, 27.256961259079905 27.022690299636803, 26.114964815375302 27.31160856159201, 24.877232142857142 27.40791464891041, 23.03032072866223 27.234107804933412, 21.364624319007262 26.71268727300242, 19.88014291389225 25.843653053117432, 18.576876513317192 24.62700514527845, 17.519788986834143 23.03880949228208, 16.764726467917676 21.046014300847457, 16.3116889565678 18.648619570974574, 16.160676452784504 15.846625302663437, 16.306560230024214 13.00531079751816, 16.74421156174334 10.581132717917674, 17.47363044794189 8.574091063861985, 18.494816888619855 6.98418583535109, 19.754774042826877 5.775515946579903, 21.19138733353511 4.912180311743342, 22.804656760744553 4.394178930841404, 24.594582324455207 4.221511803874092, 26.041453068250604 4.337193080357143, 27.364094657990314 4.6842369098062955, 28.562507093674334 5.26264329222155, 29.636690375302663 6.072412227602905, 30.556442002118644 7.122661452027845, 31.291559473365616 8.42250870157385, 31.842042789043585 9.971953976240918, 32.20789194915254 11.770997276029055))";
	private static final String star = "POLYGON ((100 0, 61.193502094049606 22.993546861223116, 47.37629522998975 42.01130058032534, 37.5 86.02387002944835, 3.6399700797813024 56.22209406247465, -18.71674025585594 48.9579585314524, -63.62712429686843 53.16567552200251, -45.74727574170163 11.753618188079914, -45.74727574170165 -11.75361818807988, -63.627124296868445 -53.16567552200249, -18.71674025585592 -48.957958531452284, 3.6399700797812438 -56.222094062474724, 37.49999999999998 -86.02387002944836, 47.376295229989815 -42.01130058032546, 61.19350209404953 -22.993546861223134, 100 0))";

	private static final Point2D pp1 = new Point2D.Double(961883.56381, 7079903.30690);
	private static final Point2D pp2 = new Point2D.Double(961883.56381, 7080514.80313);
	private static final Point2D pp3 = new Point2D.Double(962495.06003, 7080514.80313);
	private static final Point2D pp4 = new Point2D.Double(962495.06003, 7079903.30690);
	private static final Point2D ppCenter = new Point2D.Double(962189.31192, 7080209.04912);


	private static void addItems(final GraphicsScene scene) {

		addItem(scene, p1, pp1);
		addItem(scene, p2, pp2);
		addItem(scene, p3, pp3);
		//		addItem(scene, p4, pp4);
		addItem(scene, pCenter, ppCenter);
		addItem(scene, star, GeoUtils.getXY(llp_brhv));
	}

	private static void addItem(final GraphicsScene scene, final String wkt, final Point2D pos) {
		//create a number of items and simulations that shall be drawn to the view
		final DrawableStyle style = new DrawableStyle();
		style.setName("default");
		final Point2D start = new Point2D.Float(-100, -100);
		final Point2D end = new Point2D.Float(100, 100);
		final float[] dist = {0.0f, 0.5f, 1.0f};
		final Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
		final LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
		style.setFillPaint(p);
		style.setLinePaint(Color.BLACK);

		//build also a mixed item to show how it works on item hierarchies
		final GraphicsItem mixedItem = new GraphicsItem(ExampleUtils.wkt2Shape(wkt));
		mixedItem.setScale(1, 1);
		mixedItem.setStyle(style);
		mixedItem.setCenter(pos);
		mixedItem.setZOrder(500);
		scene.addItem(mixedItem);
	}

	private static void rotate(final GraphicsView view, final double delta) {
		final double newRot = view.getRotationDegrees() + delta;
		view.setRotation(newRot);
	}

}
