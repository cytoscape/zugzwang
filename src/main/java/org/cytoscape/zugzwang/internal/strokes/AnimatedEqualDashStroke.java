package org.cytoscape.zugzwang.internal.strokes;

import java.awt.BasicStroke;

public class AnimatedEqualDashStroke extends BasicStroke implements WidthStroke, AnimatedStroke 
{
	static float nsteps = 4.0f;
	private float width;
	private float offset;

	public AnimatedEqualDashStroke(float width) 
	{
		super(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { width * 2f, width * 2f }, 0.0f);

		this.width = width;
		this.offset = -1;
	}

	public AnimatedEqualDashStroke(float width, float offset) 
	{
		super(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { width * 2f,width * 2f }, width * 4f * offset);

		this.width = width;
		this.offset = offset;
	}

	public WidthStroke newInstanceForWidth(float w) 
	{
		if (offset >= 0.0f)
			return new AnimatedEqualDashStroke(w, offset);
		else
			return new AnimatedEqualDashStroke(w);
	}

	public AnimatedStroke newInstanceForNextOffset() 
	{
		float stepSize = 1.0f / nsteps;
		float newOffset = offset - stepSize;
		if (newOffset < 0)
			newOffset = 1.0f - stepSize;

		return new AnimatedEqualDashStroke(width, newOffset);
	}

	public float getOffset() 
	{ 
		return offset; 
	}

	@Override 
	public String toString() 
	{ 
		return this.getClass().getSimpleName() + " " + Float.toString(width); 
	}
}