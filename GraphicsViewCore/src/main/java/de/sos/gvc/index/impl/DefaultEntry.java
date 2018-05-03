package de.sos.gvc.index.impl;

import java.awt.geom.Rectangle2D;

import de.sos.gvc.index.IndexEntry;


/**
 * 
 * @author scholvac
 *
 */
public class DefaultEntry<VALUE> implements IndexEntry<VALUE> {

	protected final VALUE 	mValue;
	protected Rectangle2D 	mBound;
	
	public DefaultEntry(VALUE value, Rectangle2D bb) {
		mValue = value; mBound = bb;
	}
	
	@Override
	public VALUE getValue() {
		return mValue;
	}

	@Override
	public Rectangle2D getGeometry() {
		return mBound;
	}
	

}
