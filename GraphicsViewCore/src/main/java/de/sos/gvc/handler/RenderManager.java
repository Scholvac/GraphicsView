package de.sos.gvc.handler;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.sos.gvc.GraphicsScene;
import de.sos.gvc.GraphicsScene.DirtyListener;
import de.sos.gvc.GraphicsView;

public class RenderManager implements DirtyListener {
	private GraphicsView 				mView;
	private GraphicsScene 				mScene;
	private ChangeDetectionHandler 		mDetectionHandler;


	private ArrayList<DirtyListener> 	mDirtyListener;

	/** Whether the GraphicsView shall trigger repaints, if a change in the scene or the view has been detected. */
	private boolean 					mPaintProposalsEnabled=true;
	private final AtomicBoolean			mDirty = new AtomicBoolean(false);
	private AtomicInteger 				mUpdateCounter = new AtomicInteger(0);
	private AtomicInteger 				mRequestCounter = new AtomicInteger(0);
	private ScheduledExecutorService 	mScheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<Void> 		mScheduledFuture;
	/** maximum repaints (triggered by dirty scene) per second */
	private int							mPaintProposalDelay = 1000/30; //default: maximum of 30 repaints per second, triggered by dirty scene


	public RenderManager(final GraphicsScene scene, final GraphicsView view) {
		mScene = scene;
		mScene.registerDirtyListener(this);
		mView = view;
	}

	public void enablePartialDrawing(final boolean partial) {
		if (partial) {
			mDetectionHandler = new ChangeDetectionHandler();
			if (mView != null)
				mDetectionHandler.install(mView);
		}else {
			if (mDetectionHandler != null && mView != null)
				mDetectionHandler.uninstall(mView);
			mDetectionHandler = null;
		}
	}

	@Override
	public void notifyClean() {
		if (mDirty.getAndSet(false)) {//was dirty and is clean now
			//TODO: forward to listeners
			mScene.markClean(); //remember that the scene is no longer dirty, at least not in terms of visualisation
		}
	}
	@Override
	public void notifyDirty() {
		if (false == mDirty.getAndSet(true)) {
			//TODO: notify listeners
			//depending on the repaint strategy either schedule a paint proposal or propose directly (using the scheduler)
			if (isPaintProposalEnabled()) {
				if (mScheduledFuture == null) { //nothing scheduled yet
					mScheduledFuture = mScheduler.schedule(() -> {
						mScheduledFuture = null;
						proposePaint();
						return null;
					}, mPaintProposalDelay, TimeUnit.MILLISECONDS);
				}

			}
		}
	}
	public void proposePaint() {
		if (isPaintProposalEnabled()) //for the case that it's called from outside
			mView.getRenderTarget().proposeRepaint(this);
	}

	public void enablePaintProposals(final boolean enabled) {
		mPaintProposalsEnabled = enabled;
	}
	public boolean isPaintProposalEnabled() { return mPaintProposalsEnabled;}
	public void setMaximumFPS(final int fps) {
		mPaintProposalDelay = 1000 / fps;
	}

	public void addDirtyListener(final DirtyListener listener) {
		if (mDirtyListener == null) mDirtyListener = new ArrayList();
		if (mDirtyListener.contains(listener) == false)
			mDirtyListener.add(listener);
	}
	public boolean removeDirtyListener(final DirtyListener listener) {
		if (mDirtyListener == null) return false;
		final boolean res = mDirtyListener.remove(listener);
		if (mDirtyListener.isEmpty())
			mDirtyListener = null;
		return res;
	}
	public void setSchedulerService(final ScheduledExecutorService scheduler) {
		mScheduler = scheduler;
	}

	public void doPaint(final Graphics2D g) {
		mView.doPaint(g);
	}



}