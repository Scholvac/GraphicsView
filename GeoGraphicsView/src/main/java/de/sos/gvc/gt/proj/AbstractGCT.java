//**********************************************************************
//
//<copyright>
//
//BBN Technologies
//10 Moulton Street
//Cambridge, MA 02138
//(617) 873-8000
//
//Copyright (C) BBNT Solutions LLC. All rights reserved.
//
//</copyright>
//**********************************************************************
//
//$Source:
///cvs/darwars/ambush/aar/src/com/bbn/ambush/mission/MissionHandler.java,v
//$
//$RCSfile: AbstractGCT.java,v $
//$Revision: 1.2 $
//$Date: 2008/01/29 22:04:13 $
//$Author: dietrick $
//
//**********************************************************************

package de.sos.gvc.gt.proj;

import java.awt.geom.Point2D;

public abstract class AbstractGCT implements GeoCoordTransformation {

	public Point2D forward(LatLonPoint ll) {
		return forward(ll.getLatitude(), ll.getLongitude());
	}
    @Override
	public Point2D forward(double lat, double lon) {
        return forward(lat, lon, new Point2D.Double());
    }

    @Override
	public abstract Point2D forward(double lat, double lon, Point2D ret);

    @Override
	public LatLonPoint inverse(double x, double y) {
        return inverse(x, y, new LatLonPoint.Double());
    }

    @Override
	public abstract LatLonPoint inverse(double x, double y, LatLonPoint ret);

}
