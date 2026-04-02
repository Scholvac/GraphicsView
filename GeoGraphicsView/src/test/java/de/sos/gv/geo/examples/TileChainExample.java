package de.sos.gv.geo.examples;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.example.geocache.ImageIOTranscoder;
import com.example.geocache.StageDisk;
import com.example.geocache.StageMemoryBytes;
import com.example.geocache.StageMemoryImage;
import com.example.geocache.StageWeb;
import com.example.geocache.TileChain;
import com.example.geocache.TileChainImageProvider;
import com.example.geocache.TileExecutors;

import de.sos.gv.geo.GeoUtils;
import de.sos.gv.geo.LatLonPoint;
import de.sos.gv.geo.tiles.ITileFactory;
import de.sos.gv.geo.tiles.ITileImageProvider;
import de.sos.gv.geo.tiles.SizeUnit;
import de.sos.gv.geo.tiles.TileFactory;
import de.sos.gv.geo.tiles.TileHandler;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.handler.DefaultViewDragHandler;
import de.sos.gvc.handler.MouseDelegateHandler;

public class TileChainExample extends JFrame {

	private final TileExecutors mExecutors = new TileExecutors();

	private GraphicsScene mScene;
	private GraphicsView mView;

	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final TileChainExample frame = new TileChainExample("Tile Chain Example");
					frame.setVisible(true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public TileChainExample(final String title) {
		super(title);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setBounds(100, 100, 800, 800);
		final JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		contentPane.setLayout(new BorderLayout());

		createScene();
		contentPane.add(mView.getComponent(), BorderLayout.CENTER);
		configure();
		installShutdown();
	}

	public void configure() {
		final LatLonPoint llp_brhv = new LatLonPoint(53.523495, 8.641542);
		GeoUtils.setViewCenter(mView, llp_brhv);
		mView.setScale(20);
	}

	private void createScene() {
		mScene = new GraphicsScene();
		mView = new GraphicsView(mScene);

		mView.addHandler(new MouseDelegateHandler());
		mView.addHandler(new DefaultViewDragHandler());

		setupMap();
	}

	private void setupMap() {
		final ImageIOTranscoder transcoder = new ImageIOTranscoder(mExecutors.decodeCPU);
		final TileChain chain = new TileChain(Arrays.asList(
				new StageMemoryImage(SizeUnit.MegaByte.toBytes(10)),
				new StageMemoryBytes(SizeUnit.MegaByte.toBytes(5)),
				new StageDisk(new File("./.cache-chain").toPath(), SizeUnit.MegaByte.toBytes(100)),
				new StageWeb("https://tile.openstreetmap.org/{z}/{x}/{y}.{ext}", 5_000, 10_000, mExecutors.diskIO)),
				transcoder,
				mExecutors);

		final ITileImageProvider provider = new TileChainImageProvider(chain, "osm", "png");
		final ITileFactory factory = new TileFactory(provider, "TileChainWorker", 4);
		mView.addHandler(new TileHandler(factory));
	}

	private void installShutdown() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(final WindowEvent e) {
				mExecutors.shutdown();
			}

			@Override
			public void windowClosing(final WindowEvent e) {
				mExecutors.shutdown();
			}
		});
	}

}
