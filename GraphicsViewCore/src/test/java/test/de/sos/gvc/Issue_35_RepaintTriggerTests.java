package test.de.sos.gvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.Test;

import de.sos.gvc.GraphicsItem;
import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsView;
import de.sos.gvc.rt.IRenderTarget;
import de.sos.gvc.rt.ImageRenderTarget.BufferedImageRenderTarget;

public class Issue_35_RepaintTriggerTests {


	class MyView extends GraphicsView {

		int mSchedulerCreateCounter = 0;

		public MyView(final GraphicsScene scene, final IRenderTarget renderTarget) {
			super(scene, renderTarget);
		}
		@Override
		protected ScheduledExecutorService createRepaintScheduler() {
			mSchedulerCreateCounter ++;
			return super.createRepaintScheduler();
		}
		@Override
		public void markViewAsClean() {//make it public
			super.markViewAsClean();
		}
	}

	class MyScene extends GraphicsScene {
		@Override
		public void markClean() {
			// TODO Auto-generated method stub
			super.markClean();
		}
	}

	@Test
	public void test_NormalSchedulerService_WithEnabledRepaint() {
		final MyScene scene = new MyScene();
		final BufferedImageRenderTarget rt = new BufferedImageRenderTarget(100, 100, false);

		final MyView view = new MyView(scene, rt);
		assertTrue(view.isViewDirty());
		view.markViewAsClean();
		scene.markClean();


		assertEquals(0, view.mSchedulerCreateCounter);
		scene.addItem(new GraphicsItem());
		assertEquals(1, view.mSchedulerCreateCounter);
	}

	@Test
	public void test_NoSchedulerService_WithDisabledRepaint() {
		final MyScene scene = new MyScene();
		final BufferedImageRenderTarget rt = new BufferedImageRenderTarget(100, 100, false);

		final MyView view = new MyView(scene, rt);
		view.enableRepaintTrigger(false); // deactivate the repaint requests
		assertTrue(view.isViewDirty());
		view.markViewAsClean();
		scene.markClean();


		assertEquals(0, view.mSchedulerCreateCounter);
		scene.addItem(new GraphicsItem());
		assertEquals(0, view.mSchedulerCreateCounter); //no creation of the scheduler service requested
	}

	@Test
	public void test_NamedSchedulerThread() {
		final MyScene scene = new MyScene();
		final BufferedImageRenderTarget rt = new BufferedImageRenderTarget(100, 100, false);

		final MyView view = new MyView(scene, rt);
		assertTrue(view.isViewDirty());
		view.markViewAsClean();
		scene.markClean();


		assertEquals(0, view.mSchedulerCreateCounter);
		scene.addItem(new GraphicsItem());
		assertEquals(1, view.mSchedulerCreateCounter); //Ensure that the thread is created

		final Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
		Thread schedulerThread = null;
		for (final Thread thread : allThreads.keySet())
			if (thread.getName().equals("GraphicsView-Scheduler")){
				schedulerThread = thread;
				break;
			}
		assertNotNull(schedulerThread);
		assertTrue(schedulerThread.isDaemon()); //to ensure that it does not keep the program active
	}
}
