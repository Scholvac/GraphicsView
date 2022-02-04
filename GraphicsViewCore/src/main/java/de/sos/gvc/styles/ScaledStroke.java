package de.sos.gvc.styles;

import java.awt.BasicStroke;

public class ScaledStroke extends BasicStroke {

	private double mScale = 1;



	public ScaledStroke() {
		super();
	}
	public ScaledStroke(float width, int cap, int join, float miterlimit, float[] dash, float dash_phase) {
		super(width, cap, join, miterlimit, dash, dash_phase);
	}
	public ScaledStroke(float width, int cap, int join, float miterlimit) {
		super(width, cap, join, miterlimit);
	}
	public ScaledStroke(float width, int cap, int join) {
		super(width, cap, join);
	}
	public ScaledStroke(float width) {
		super(width);
	}


	@Override
	public float getLineWidth() {
		return super.getLineWidth() * (float)mScale;
	}

	@Override
	public float[] getDashArray() {
		float[] da = super.getDashArray();
		if (da != null && mScale != 1) {
			float[] scaled = new float[da.length];
			for (int i = 0; i < scaled.length; i++) scaled[i] = (float)(da[i] * mScale);
			return scaled;
		}
		return da;
	}

	public void setScale(double scale) { mScale = scale; }
	public double getScale() { return mScale; }


//	@Override
//	public Shape createStrokedShape(Shape s) {
//		//that is actually the same implementation as the java.awt.BasicStroke but scales the linewith.
//		sun.java2d.pipe.RenderingEngine re = sun.java2d.pipe.RenderingEngine.getInstance();
//		return re.createStrokedShape(s, getLineWidth(), getEndCap(), getLineJoin(), getMiterLimit(), getDashArray(), getDashPhase());
//	}
}
