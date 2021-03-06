package de.sos.gvc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.helpers.MarkerIgnoringBase;

import de.sos.gvc.Utils.TmpVars;
import de.sos.gvc.drawables.ShapeDrawable;
import de.sos.gvc.drawables.ShapeDrawable.IShapeProvider;
import de.sos.gvc.param.IParameter;
import de.sos.gvc.param.ParameterContext;
import de.sos.gvc.styles.DrawableStyle;

/**
 * Handles the visual state of an item within the scene. 
 * Each GraphicsItem can contain child items, to build up some kind of SceneGraph. 
 * Each GraphicsItem is represented through its center location and shape, that is handled like in a SceneGraph, e.g. relative to its parent object. 
 * 
 * 
 * \section State State
 * the state of the item is represented through its mPropertyContext variable, which is a store for a number of properties. 
 * Some of the properties are also available as member variables (for faster / easier access). 
 * Each of this properties may be observed with an PropertyChangeListener. 
 * \subsection ChildStates Child States
 * to observe states of children, there is an delegate listener, that registers itself for all events of the own PropertyContext and also for 
 * all events of all children. See addPropertyChangeListener(...)
 * 
 * \section Shape Shape
 * Each Item contains at least one shape. This shape is used for collision checks (for example if it is inside a view). 
 * If no special Drawable has been registered, this shape will also be used for rendering
 * 
 * \section Properties Properties
 * As pointed out, each GraphicsItem is represented through at least the following Properties
 * - PROP_VISIBLE ("VISIBLE") : shall the Item be rendered
 * - PROP_SELECTED ("SELECTED") : if the item is currently selected
 * - PROP_SELECTABLE ("SELECTABLE") : if the item can be selected
 * - PROP_HOVERED ("HOVERED") : if the mouse is over this item and remains for a certain time there
 * - PROP_SHAPE ("SHAPE"): The shape of the item, see \ref Shape
 * - PROP_CENTER_X ("CENTER_X") : X - Center of the shape in local coordinates (either the scene or its parent)
 * - PROP_CENTER_Y ("CENTER_Y") : Y - Center of the shape in local coordinates (either the scene or its parent)
 * - PROP_ROTATION ("ROTATION") : Rotation of the shape (Radians) in local coordinates (either the scene or its parent)
 * - PROP_STYLE ("STYLE") : Drawing style for this item (e.g. filled, or border or both, colors)
 * - PROP_DRAWABLE ("DRAWABLE") : Drawable that does the actual painting (see IDrawable)
 * - PROP_PARENT ("PARENT") : Pointer to the parent item, if this item is a sub item
 * - PROP_Z_ORDER ("Z_ORDER"): defines the order in which the items shall be painted by a view (the lower the z-value, the earlier the item is painted)
 * 
 *  The following properties are used to handle the mouse input for this item and are used in combination with \ref MouseInput
 *  
 *  - PROP_MOUSE_WEEL_SUPPORT ("MOUSE_WHEEL_SUPPORT"): Pointer to the MouseWheelListener that shall be notified if the mouse wheel is used while the mouse is in the boundary of this item
 * 	- PROP_MOUSE_MOTION_SUPPORT ("MOUSE_MOTION_SUPPORT"): Pointer to the MouseMotionListener that shall be notified if the mouse wheel is used while the mouse is in the boundary of this item
 * 	- PROP_MOUSE_SUPPORT ("MOUSE_SUPPORT"): Pointer to the MouseListener that shall be notified if the mouse wheel is used while the mouse is in the boundary of this item
 * 
 * The following properties are mainly used to cache some often used values. 
 * 	- PROP_LOCAL_BOUNDS ("LOCAL_BOUNDS"): 
 *	- PROP_SCENE_BOUNDS ("SCENE_BOUNDS")
 *
 * 
 * \section MouseInput Mouse Input
 * Each GraphicsItem can have its own Mouse listener (MouseListener, MouseMotionListener and MouseWheelListener) which are available through 
 * the relevant properties.
 * @note this feature is only available if the MouseDelegateHandler has been installed within the displaying GraphicsView. 
 * 
 * @author scholvac
 *
 */
public class GraphicsItem implements IShapeProvider  {
	//inherit from IShapeProvider to support the default behaviour if no Drawable is set, the item's shape is drawn

	public static final String PROP_VISIBLE 				= "VISIBLE";
	public static final String PROP_SELECTED		 		= "SELECTED";
	public static final String PROP_SELECTABLE 				= "SELECTABLE";
	public static final String PROP_SHAPE 					= "SHAPE";
	public static final String PROP_CENTER_X 				= "CENTER_X";
	public static final String PROP_CENTER_Y 				= "CENTER_Y";
	public static final String PROP_ROTATION 				= "ROTATION";
	public static final String PROP_SCALE_X 				= "SCALE_X";
	public static final String PROP_SCALE_Y 				= "SCALE_Y";
	public static final String PROP_STYLE 					= "STYLE";
	public static final String PROP_DRAWABLE 				= "DRAWABLE";
	public static final String PROP_PARENT 					= "PARENT";
	public static final String PROP_Z_ORDER 				= "Z_ORDER";
	
	public static final String PROP_MOUSE_WHEEL_SUPPORT 	= "MOUSE_WHEEL_SUPPORT";
	public static final String PROP_MOUSE_MOTION_SUPPORT 	= "MOUSE_MOTION_SUPPORT";
	public static final String PROP_MOUSE_SUPPORT 			= "MOUSE_SUPPORT";
	
	public static final String PROP_LOCAL_BOUNDS 			= "LOCAL_BOUNDS";
	public static final String PROP_SCENE_BOUNDS 			= "SCENE_BOUNDS";
	
	public static final String PROP_CHILD_ADDED				= "child added";
	public static final String PROP_CHILD_REMOVED			= "child removed";
	
	
	
		
	private transient PropertyChangeSupport 			mAllEventDelegate = new PropertyChangeSupport(this);
	
	/**
	 * If an item is not visible, the item will neither be drawn nor will it be selected or notified for mouse events
	 * In general, the scene does not return the item in all query methods, except getItems()
	 */
	private boolean										mVisible;
	
	private boolean										mSelected;
	private boolean										mSelectable;
	
	/**
	 * Stores the shape of the property. 
	 * The shape is not nessesarily used to paint the item but used for Collision detection
	 */
	private Shape										mShape;
	/**
	 * X Position of the Item in Scene Coordinates, usually the center of getBounds()
	 */
	private double										mCenterX;
	/**
	 * Y Position of the Item in Scene Coordinates, usually the center of getBounds()
	 */
	private double										mCenterY;
	/**
	 * Rotation of the Item in Scene Coordinates and Degrees
	 */
	private double										mRotation;
	
	/**
	 * Scale on X-Axis of the item in scene coordinates (1 == default)
	 */
	private double										mScaleX;
	
	/**
	 * Scale on Y-Axis of the item in scene coordinates (1 == default)
	 */
	private double										mScaleY;
	
	/**
	 * Controls the order that is used during painting of items by an view. the lower the value the earlier the item is painted
	 * The default value will be initialized to 100. Background Items may use values between 0 and 20.0f
	 */
	private float										mZOrder;
	/**
	 * Contains the object that draws the item into the view
	 * @note if no drawable is defined when the first drawcall occures, the default behaviour 
	 * creates a new ShapeDrawable using the shape that is used for collision detection
	 */
	private IDrawable									mDrawable;

	/**
	 * Style that defines how the drawable shall be drawn (may be null)
	 */
	private DrawableStyle								mStyle; 
	
	
	/**
	 * contains the local bounding box of the shape. 
	 * This variable will be calculated whenever the shape changes
	 * @cached
	 */
	private final Rectangle2D							mLocalBounds = new Rectangle2D.Double();
	/**
	 * Bounding rectangle of the item witin the scene
	 * This variable is bound to changes of mLocalTransform and LocalBounds
	 */
	private final Rectangle2D							mWorldBounds = new Rectangle2D.Double();
	
	private boolean 									mInvalidLocalBound = true;
	private boolean 									mInvalidWorldBound = true;

	
	/**
	 * contains the local transform (e.g. centerX, centerY, rotation)
	 * this variable is bound to changes of 
	 * - mCenterX, mCenterY, mRotation
	 */
	protected final AffineTransform 						mLocalTransform = new AffineTransform();
	protected boolean 									mInvalidLocalTransform = true;
	
	protected final AffineTransform						mWorldTransform = new AffineTransform();
	protected boolean										mInvalidWorldTransform = true;
	
//	private IParameter<GraphicsItem>					mParent;
	private GraphicsItem								mParent;
	private ArrayList<GraphicsItem>						mChildren = new ArrayList<>();
	
	private PropertyChangeListener						mChildListener = new PropertyChangeListener() { //listen to events of child items and delegates them
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			mAllEventDelegate.firePropertyChange(evt);
		}
	};
	private GraphicsScene mScene;
	
	
	
	
		
	public GraphicsItem() {
		this(null);
	}
	public GraphicsItem(Shape shape) {
		this(shape, new ParameterContext());
	}


	public GraphicsItem(Shape shape, ParameterContext propertyContext) {				
		//ensure that we do have the basic properties
		mVisible = propertyContext.getValue(PROP_VISIBLE, true);
		mSelected = propertyContext.getValue(PROP_SELECTED, false);
		mSelectable = propertyContext.getValue(PROP_SELECTABLE, true);
		
		mStyle = propertyContext.getValue(PROP_STYLE, null);
		mDrawable = propertyContext.getValue(PROP_DRAWABLE, null);
		mParent = null;
		
		mCenterX = propertyContext.getValue(PROP_CENTER_X, 0.0);	
		mCenterY = propertyContext.getValue(PROP_CENTER_Y, 0.0);

		mRotation = propertyContext.getValue(PROP_ROTATION, 0.0);
	
		mScaleX = propertyContext.getValue(PROP_SCALE_X, 1.0);
		mScaleY = propertyContext.getValue(PROP_SCALE_Y, 1.0);

		setShape(shape);
		
		mZOrder = propertyContext.getValue(PROP_Z_ORDER, 100.0f);
		
		propertyContext.registerListener(mChildListener);		
	}

	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////		OBSERVER PATTERN				/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Adds a listener that will be notified for all event of this Items as well as all events of children and childs of childs
	 * @param pcl
	 */
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		mAllEventDelegate.addPropertyChangeListener(pcl);
	}
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		mAllEventDelegate.removePropertyChangeListener(pcl);
	}
	public void addPropertyChangeListener(String evtName, PropertyChangeListener pcl) {
		mAllEventDelegate.addPropertyChangeListener(evtName, pcl);
	}
	public void removePropertyChangeListener(String evtName, PropertyChangeListener pcl) {
		mAllEventDelegate.removePropertyChangeListener(evtName, pcl);
	}

	
	
	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////		Local & World Transform			/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public double getRotationRadians() { return Math.toRadians(getRotationDegrees()); }
	public double getRotationDegrees() { return mRotation; }
	public double getLocalRotationDegrees() { return mRotation; }
	public double getLocalRotationRadians() { return getRotationRadians(); }
	
	public double getCenterY() { return mCenterY; }
	public double getLocalTranslationX() { return mCenterX; }
	public double getCenterX() { return mCenterX; }
	public double getLocalTranslationY() { return mCenterY;}
	
	public double getScaleX() { return mScaleX;}
	public double getLocalScaleX() { return mScaleX; }
	public double getScaleY() { return mScaleY;}
	public double getLocalScaleY() { return mScaleY;}
	
	public void setCenterX(double x) {
		if (x != mCenterX) {
			double old = mCenterX;
			mCenterX = x;
			
			mInvalidLocalTransform = mInvalidWorldTransform = true;
			markDirtyWorldBound();
			
			mAllEventDelegate.firePropertyChange(PROP_CENTER_X, old, mCenterX);
		}
	}
	public void setCenterY(double y) { 
		if (y != mCenterY) {
			double old = mCenterY;
			mCenterY = y;

			mInvalidLocalTransform = mInvalidWorldTransform = true;
			markDirtyWorldBound();
			
			mAllEventDelegate.firePropertyChange(PROP_CENTER_Y, old, mCenterY);
		} 
	}
	public void setCenter(double x, double y) {
		setCenterX(x);
		setCenterY(y);
	}
	public void setCenter(Point2D loc) { setCenter(loc.getX(), loc.getY()); }
	public Point2D getCenter() { return getLocalLocation(); }
	
	public void setLocalLocation(Point2D loc) { setCenter(loc.getX(), loc.getY()); }
	public Point2D getLocalLocation() { return new Point2D.Double(getCenterX(), getCenterY()); }
	
	
	public void setRotation(double rot_deg) {
		if (rot_deg != mRotation) {
			double old = mRotation;
			mRotation = rot_deg;
			
			mInvalidLocalTransform = mInvalidWorldTransform = true;
			markDirtyLocalBound();
			
			mAllEventDelegate.firePropertyChange(PROP_ROTATION, old, mRotation);
		}
	}
	public double getRotation() { return mRotation; }
	public double getLocalRotation() { return getRotation(); }
	public double getLocalRotationDeg() { return getRotation(); }
	public double getLocalRotationRad() { return Math.toRadians(getRotation()); }
	
	public double getSceneRotation() {
		GraphicsItem p = getParent();
		if (p != null) {
			//@note the world transform does not allow to calculate the rotation out of the transformation matrix, thus we use this recursive call
			return p.getSceneRotation() + getLocalRotation();
		}
		return getRotation();
	}
	public double getSceneRotationDeg() { return getSceneRotation();}
	public double getSceneRotationRad() { return Math.toRadians(getSceneRotation());}
	
	public void setLocalRotation(final double rot_deg) { setRotation(rot_deg); }
	public void setLocalRotationDeg(final double rot_deg) { setRotation(rot_deg);}
	public void setLocalRotationRad(final double rot_rad) { setRotation(Math.toDegrees(rot_rad));}
	
	public void setSceneRotation(final double rot_deg) {
		GraphicsItem p = getParent();
		if (p != null) {
			double rot = rot_deg - p.getSceneRotation();
			setRotation(rot);
		}
		setRotation(rot_deg);			
	}
	public void setSceneRotationDeg(final double rot_deg) { setSceneRotation(rot_deg); }
	public void setSceneRotationRad(final double rot_rad) { setSceneRotation(Math.toDegrees(rot_rad));}
	
	
	public void setLocalScaleX(final double scaleX) { setScaleX(scaleX); }
	public void setLocalScaleY(final double scaleY) { setScaleY(scaleY); }
	public void setLocalScale(final double x, double y) { setScaleX(x); setScaleY(y); }
	public void setLocalScale(final double xAndy) { setScale(xAndy, xAndy); }
	
	public void setScaleX(final double scaleX) {
		if (scaleX != mScaleX) {
			double old = mScaleX;
			mScaleX = scaleX;
			
			mInvalidLocalTransform = mInvalidWorldTransform = true;
			markDirtyLocalBound();
			
			mAllEventDelegate.firePropertyChange(PROP_SCALE_X, old, mScaleX);
		}
	}
	public void setScaleY(final double scaleY) { 
		if (scaleY != mScaleY) {
			double old = mScaleY;
			mScaleY = scaleY;
			
			mInvalidLocalTransform = mInvalidWorldTransform = true;
			markDirtyLocalBound();
			
			mAllEventDelegate.firePropertyChange(PROP_SCALE_Y, old, mScaleY);
		}
	}
	public void setScale(final double x,final  double y) { setLocalScale(x,y); }
	public void setScale(final double xAndy) { setLocalScale(xAndy, xAndy); }
	
	public double getSceneScaleX() { 
		if (getParent() != null) return getParent().getSceneScaleX() * getScaleX();
		return getScaleX();
	}
	public double getSceneScaleY() {
		if (getParent() != null) return getParent().getSceneScaleY() * getScaleY();
		return getScaleY();
	}
	
	public void setSceneScaleX(final double scaleX) { 
		if (getParent() != null) {
			setScaleX(scaleX / getParent().getSceneScaleX());
		}else
			setScaleX(scaleX);
	}
	public void setSceneScaleY(final double scaleY) {
		if (getParent() != null)
			setScaleY(scaleY / getParent().getSceneScaleY());
		else
			setScaleY(scaleY); 
	}
	public void setSceneScale(final double x, final double y) { setSceneScaleX(x); setSceneScaleY(y); }
	public void setSceneScale(final double xAndy) { setSceneScale(xAndy, xAndy); }
	
	public Point2D getSceneLocation() { return getSceneLocation(null);}
	public Point2D getSceneLocation(Point2D store) {
		doCheckUpdateWorldTransform();
		double x = mWorldTransform.getTranslateX();
		double y = mWorldTransform.getTranslateY();
		if (store == null)
			return new Point2D.Double(x, y);
		store.setLocation(x, y);
		return store;
	}

	/**
	 * Calculates the relative position to its parent and set this as center
	 * if the item has no parent, it is used as local position (e.g. setCenter(sceneLoc))
	 * @param sceneLoc location in scene space
	 */
	public void setSceneLocation(final Point2D sceneLoc) {
		if (getParent() == null)
			setLocalLocation(sceneLoc);
		else {
			try {
				Point2D loc = getParent().getWorldTransform().inverseTransform(sceneLoc, null);
				setLocalLocation(loc);
			}catch(Exception e) {
				e.printStackTrace();
			}			
		}
	}
	public void setSceneLocation(final double centerX, final double centerY) {
		setSceneLocation(new Point2D.Double(centerX, centerY));
	}
	
	
	public void invalidateLocalTransform() {
		mInvalidWorldTransform = mInvalidLocalTransform = true;
	}
	public AffineTransform getLocalTransform() {
		if (mInvalidLocalTransform) {
			updateLocalTransform();
		}
		return mLocalTransform;
	}
	
	protected void updateLocalTransform() {
		synchronized (mLocalTransform) {
			mLocalTransform.setToIdentity();

			double x = mCenterX, y = mCenterY, r = getRotationRadians();
			double sx = mScaleX, sy = mScaleY;
			
			mLocalTransform.translate(x, y);
			mLocalTransform.rotate(-r);
			mLocalTransform.scale(sx, sy);
			
			mInvalidLocalTransform = false;
		}
	}
	
	
	public AffineTransform getWorldTransform() {
		doCheckUpdateWorldTransform();
		return mWorldTransform;
	}
	
	
	private void doCheckUpdateWorldTransform() {
//		updateWorldTransform();
		if (mInvalidWorldTransform == false && mInvalidLocalTransform == false)
			return ;
		if (mParent == null) {
			updateWorldTransform();
		}else {
			TmpVars vars = Utils.TmpVars.get();
			GraphicsItem[] stack = vars.itemStack;
			GraphicsItem rootNode = this;
			int i = 0;
	        while (true) {
	        	GraphicsItem  hisParent = rootNode.mParent;
	        	if (hisParent == null) {
	        		if (rootNode.mInvalidWorldTransform)
	        			rootNode.updateWorldTransform();
	        		i--;
	        		break;
	        	}
	        	stack[i] = rootNode;
	            rootNode = hisParent;
	            i++;
	        }
	        for (int j = i; j >= 0; j--) {
	            rootNode = stack[j];
	            if (rootNode.mInvalidWorldTransform || rootNode.mInvalidLocalTransform)
	            	rootNode.updateWorldTransform();
	        }
	        vars.release();
		}
	}
	
	/** Updates the world transform of this item. 
	 * @note This method assumes that all parent (if any) have a valid worldTransform
	 */
	protected void updateWorldTransform() {
		GraphicsItem parent = getParent();
		if (parent == null) {
			synchronized (mWorldTransform) {
				mWorldTransform.setTransform(getLocalTransform());
				mInvalidWorldTransform = false;
			}			
		}else {
			synchronized (mWorldTransform) {
				mWorldTransform.setTransform(parent.mWorldTransform);
				mWorldTransform.concatenate(getLocalTransform());
				mInvalidWorldTransform = false;
			}
		}
		
		if (mChildren.isEmpty() == false)
			for (GraphicsItem child : mChildren)
				child.notifyParentWorldTransformChanged(); 
		
	}
	
	/** marks this item as dirty and notifies all children to be dirty as well */
	private void notifyParentWorldTransformChanged() {
		markDirtyTransform();
		if (mChildren.isEmpty() == false)
			for (GraphicsItem child : mChildren)
				child.notifyParentWorldTransformChanged();
	}

	public void markDirtyTransform() {
		mInvalidLocalTransform = mInvalidWorldTransform = true;
		markDirtyBounds();
	}
	
	
	/**
	 * returns the local position of a scene point
	 * @param sceneLoc position in scene coordinates
	 * @param store the instance to store the result into (may be null) 
	 * @return location in local coordinates
	 */
	public Point2D scene2Local(final Point2D sceneLoc, Point2D store) {
		try {
			return getWorldTransform().inverseTransform(sceneLoc, store);
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * returns the local position of a scene point
	 * @param sceneLoc position in scene coordinates 
	 * @return location in local coordinates
	 */
	public Point2D scene2Local(final Point2D sceneLoc) {
		return scene2Local(sceneLoc, null);
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////		BOUNDS					/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/** 
	 * returns the local bounds of the item, that is the bound of the shape 
	 * @return
	 */
	public Rectangle2D getLocalBounds() {
		if (mInvalidLocalBound) {
			if (getShape() != null)
				mLocalBounds.setRect(getShape().getBounds2D());
			mInvalidLocalBound = false;
		}
		return mLocalBounds;
	}
	
	/**
	 * returns the bounds of this item within the scene, that is the local bounds transformed with location and rotation
	 * @return
	 */
	public Rectangle2D getSceneBounds() {
		if (mInvalidWorldBound) {
			Rectangle2D lb = getLocalBounds();
			
			Utils.transform(lb, getWorldTransform(), mWorldBounds);
			
			if (mChildren != null && mChildren.isEmpty() == false) {
				synchronized (mChildren) {
					for (GraphicsItem child : mChildren) {
						Rectangle2D cb = child.getSceneBounds();
						if (cb != null)
							Rectangle2D.union(mWorldBounds, cb, mWorldBounds);
					}			
				}
			}
			mInvalidWorldBound = false;
		}
		return mWorldBounds;
	}

	/** 
	 * Invalidates the transformation matrices (local and world) as well as the bounding volumes (local and world) 
	 * of this graphics item
	 */
	public void markDirty() {
		markDirtyTransform();
		markDirtyBounds();
	}
	
	/**
	 * This method may be called by an specialized (subclassed) GraphcisItem to notify the item 
	 * that its shape has been changed. 
	 * @note the setShape(Shape) method does mark this item as dirty if the instance of the shape changes.
	 */
	public void markDirtyBounds() {
		markDirtyLocalBound();
	}
	
	private void markDirtyLocalBound() {
		if (!mInvalidLocalBound) {
			mInvalidLocalBound = true;
		}
		markDirtyWorldBound();
	}
	private void markDirtyWorldBound() {
		mInvalidWorldBound = true;
		mInvalidLocalBound = true;
		if (mParent != null && mParent.mInvalidWorldBound == false) {
			mParent.markDirtyWorldBound(); //recursive mark all parents dirty, until we found one that is already dirty
		}		
	}
		
	/**
	 * returns the bounding box of this item and all its children
	 * this method differs from getLocalBounds() that it includes the children
	 * it differs from getSceneBounds that it does not transform the bounding box into scene coordinates, but uses the local ones
	 * @return
	 */
	public Rectangle2D getBoundingBox() {
		Rectangle2D wt = getSceneBounds();
		return Utils.inverseTransform(wt, getWorldTransform());
	}

	
	
	
	/**
	 * Set the scene
	 * this method is called by the scene, when the item is added to the scene
	 * @note this method is not part of the public API
	 * @param scene
	 */
	void _setScene(GraphicsScene scene){
		if (scene != mScene) {
			if (mScene != null) onRemovedFromScene(mScene);
		}
		mScene = scene;
		if (mScene != null)
			onAddedToScene(mScene);
	}
	/** 
	 * Called if the item has been removed from a scene
	 * @param scene
	 */
	protected void onRemovedFromScene(GraphicsScene scene) {}
	/**
	 * Called if the item has been added to a scene
	 * @note this method is not called if the item is added to another item as child. In such a case, observe the parent property
	 * @param scene
	 */
	protected void onAddedToScene(GraphicsScene scene) {}
	
	protected GraphicsScene getScene() {
		if (getParent() != null)
			return getParent().getScene();
		return mScene;
	}
	/**
	 * returns the first view that is associated with the given scene
	 * @return
	 */
	protected GraphicsView getView() {
		List<GraphicsView> views = getViews();
		if (views != null && views.isEmpty() == false)
			return views.get(0);
		return null;
	}
	/** 
	 * returns all views associated with the scene, this item belongs to
	 * @return
	 */
	protected List<GraphicsView> getViews() {
		GraphicsScene scene = getScene();
		if (scene != null)
			return scene.getViews();
		return null;
	}
	
	


	public void draw(Graphics2D g, IDrawContext ctx) {
		AffineTransform old = g.getTransform();
		g.transform(getWorldTransform());		
		IDrawable drawable = getDrawable();
		drawable.paintItem(g, getStyle(), ctx);

//		g.setColor(Color.BLACK);
//		g.draw(getBoundingBox());
		
//		g.setTransform(ctx.getViewTransform());
		g.setTransform(old);
		
		if (hasChildren()) {
			List<GraphicsItem> children = new ArrayList<>(getChildren());
			Collections.sort(children, new Comparator<GraphicsItem>() {
				@Override
				public int compare(GraphicsItem o1, GraphicsItem o2) {
					return Float.compare(o1.getZOrder(), o2.getZOrder());
				}
			});
			synchronized (children) {
				for (GraphicsItem child : children) {
					if (child.isVisible())
						child.draw(g, ctx);
				}
			}
		}
	}


	public Shape getShape() { return mShape; }
	
	/** Change the shape for this item. 
	 * 
	 * The Shape is used to determine the BoundingVolume of this item, which is used for interactions and drawing.
	 *  
	 * @note 	The setShape(Shape) method does mark this item as dirty if the instance of the shape changes.
	 * 			If the values of the shape change but not the instance of the shape itself, the caller has to mark the item as dirty using the
	 * 			markDirtyBounds() method
	 * @param shape
	 */
	public void setShape(Shape shape) {
		if (shape != mShape) {
			Shape old = mShape;
			mShape = shape;
			
			markDirtyLocalBound();
			mAllEventDelegate.firePropertyChange(PROP_SHAPE, old, mShape);
		}
	}

	
	
	
	
	public void setVisible(final boolean b) {
		if (b != mVisible) {
			mVisible = b;
			mAllEventDelegate.firePropertyChange(PROP_STYLE, !b, b);
		}
	}
	public boolean isVisible() { return mVisible;}

	public void setStyle(DrawableStyle style) {
		if (style != mStyle) {
			DrawableStyle old = mStyle;
			mStyle = style;
			mAllEventDelegate.firePropertyChange(PROP_STYLE, old, mStyle);
		}		
	}
	public DrawableStyle getStyle() {
		return mStyle;
	}
	public void setDrawable(IDrawable drawable) {
		if (drawable != mDrawable) {
			IDrawable old = mDrawable;
			mDrawable = drawable;
			mAllEventDelegate.firePropertyChange(PROP_DRAWABLE, old, mDrawable);
		}
	}
	public IDrawable getDrawable() {
		if (mDrawable == null) {
			setDrawable(new ShapeDrawable(this));
		}
		return mDrawable;
	}
	public GraphicsItem getParent() {
		return mParent;
	}
	protected void setParent(GraphicsItem parent) {
		if (parent != mParent) {
			GraphicsItem old = mParent;
			mParent = parent;
			
			mInvalidWorldTransform = true;
			markDirtyLocalBound();
			
			mAllEventDelegate.firePropertyChange(PROP_PARENT, old, mParent);
		}
	}


	public boolean addItem(GraphicsItem child) {
		synchronized (mChildren) {
			if (child == null || mChildren.contains(child))
				return false;
			child.setParent(this);
			mChildren.add(child);
			markDirtyWorldBound(); //this may not be called, if the child already has an invalid bound. If thats not the case, this method does nothing
			child.mAllEventDelegate.addPropertyChangeListener(mChildListener);
			
			mAllEventDelegate.firePropertyChange(PROP_CHILD_ADDED, null, child);
		}
		return true;
	}
	
	public boolean removeItem(GraphicsItem child) {
		synchronized (mChildren) {
			if (child == null || mChildren.contains(child) == false)
				return false;
			child.setParent(null);
			
			boolean res = mChildren.remove(child);
			mAllEventDelegate.firePropertyChange(PROP_CHILD_REMOVED, child, null);
			return res;
		}
	}

	public void setSelected(boolean b) {
		if (b != mSelected) {
			mSelected = b;
			mAllEventDelegate.firePropertyChange(PROP_SELECTED, !b, b);
		}
	}
	
	public boolean isSelected() { return mSelected;}
	
	public void setSelectable(boolean b) {
		if (mSelectable != b) {
			mSelectable = b;
			mAllEventDelegate.firePropertyChange(PROP_SELECTABLE, !b, b);
		}
	}
	
	public boolean isSelectable() { return mSelectable;}
	public boolean hasChildren() { return getChildren().isEmpty() == false; }
	public List<GraphicsItem> getChildren() {
		synchronized (mChildren) {
			return Collections.unmodifiableList(mChildren);	
		}		 
	}
	
	public float getZOrder() { return mZOrder; }
	public void setZOrder(float z) {
		if (z != mZOrder) {
			float old = mZOrder;
			mZOrder = z;
			mAllEventDelegate.firePropertyChange(PROP_Z_ORDER, old, z);
		}
	}
	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////		INPUT SUPPORT 					/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private MouseWheelListener 		mMouseWheelSupport;
	private MouseMotionListener 	mMouseMotionSupport;
	private MouseListener			mMouseSupport;
	
	public MouseWheelListener getMouseWheelSupport() {
		return mMouseWheelSupport;
	}
	public void setMouseWheelSupport(MouseWheelListener l) {
		if (l != mMouseWheelSupport) {
			MouseWheelListener old = mMouseWheelSupport;
			mMouseWheelSupport = l;
			mAllEventDelegate.firePropertyChange(PROP_MOUSE_WHEEL_SUPPORT, old, mMouseWheelSupport);
		}
	}
	
	
	public MouseMotionListener getMouseMotionSupport() {
		return mMouseMotionSupport;
	}
	public void setMouseMotionSupport(MouseMotionListener l) {
		if (l != mMouseMotionSupport) {
			MouseMotionListener old = mMouseMotionSupport;
			mMouseMotionSupport = l;
			mAllEventDelegate.firePropertyChange(PROP_MOUSE_MOTION_SUPPORT, old, mMouseMotionSupport);
		}
	}
	
	public MouseListener getMouseSupport() {
		return mMouseSupport;
	}
	public void setMouseSupport(MouseListener l) {
		if (mMouseSupport != l) {
			MouseListener old = mMouseSupport;
			mMouseSupport = l;
			mAllEventDelegate.firePropertyChange(PROP_MOUSE_SUPPORT, old, mMouseSupport);
		}
	}


	
	
	
	

}
