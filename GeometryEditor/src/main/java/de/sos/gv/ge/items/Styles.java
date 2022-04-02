package de.sos.gv.ge.items;

import java.awt.BasicStroke;
import java.awt.Color;

import de.sos.gvc.styles.DrawableStyle;
import de.sos.gvc.styles.ScaledStroke;

public interface Styles {
	static final DrawableStyle 	NormalStyle 			= new DrawableStyle("DefaultContourPointStyle", 	Color.BLACK, new BasicStroke(2), Color.RED);
	static final DrawableStyle 	ActiveStyle 			= new DrawableStyle("ActiveContourPointStyle", 		Color.BLUE, new BasicStroke(2), Color.GREEN);

	static final DrawableStyle 	IntermediateStyle 		= new DrawableStyle("ActiveContourPointStyle", 		Color.RED, new BasicStroke(2), null);

	static final DrawableStyle 	GeometryNormalFilled 	= new DrawableStyle("GeometryNormalFilled", 		Color.BLUE, new ScaledStroke(2), new Color(0, 148, 255));
	static final DrawableStyle 	GeometryNormalLine 		= new DrawableStyle("GeometryNormalLine", 			Color.BLUE, new ScaledStroke(2), null);

	static final DrawableStyle 	GeometryEditFilled 		= new DrawableStyle("GeometryEditFilled", 			Color.ORANGE, new ScaledStroke(2), new Color(0, 148, 255, 128).brighter());
	static final DrawableStyle 	GeometryEditLine 		= new DrawableStyle("GeometryEditLine", 			Color.ORANGE, new ScaledStroke(2), null);

	static final DrawableStyle 	GeometryCreateFilled 	= new DrawableStyle("GeometryCreateFilled", 		Color.CYAN, new ScaledStroke(2), new Color(0, 148, 255, 128).brighter());
	static final DrawableStyle 	GeometryCreateLine 		= new DrawableStyle("GeometryCreateLine", 			Color.CYAN, new ScaledStroke(2), null);

	static final DrawableStyle 	FutureSegment			= new DrawableStyle("GeometryEditLine", 			Color.CYAN, new ScaledStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0), null);
}
