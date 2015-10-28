package org.cytoscape.zugzwang.internal.strokes;

import java.awt.BasicStroke;

public class AnimatedDashDotStroke extends BasicStroke implements WidthStroke, AnimatedStroke 
{
	static float nsteps = 4.0f;
	private float width;
	private float offset;

	public AnimatedDashDotStroke(float width) 
	{
		super(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f, new float[] { width * 4f, width * 2f, width, width * 2f }, 0.0f);

		this.width = width;
		this.offset = -1;
	}

	public AnimatedDashDotStroke(float width, float offset) 
	{
		super(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f, new float[] { width * 4f, width * 2f, width, width * 2f }, width * 9f * offset);

		this.width = width;
		this.offset = offset;
	}

	public WidthStroke newInstanceForWidth(float w) 
	{
		if (offset >= 0)
			return new AnimatedDashDotStroke(w, offset);
		else
			return new AnimatedDashDotStroke(w);
	}

	public AnimatedStroke newInstanceForNextOffset() 
	{
		float stepSize = 1.0f / nsteps;
		float newOffset = offset - stepSize;
		if (newOffset < 0)
			newOffset = 1.0f - stepSize;

		return new AnimatedDashDotStroke(width, newOffset);
	}

	public float getOffset() 
	{ 
		return offset; 
	}

	public String toString() 
	{
		return this.getClass().getSimpleName() + " " + Float.toString(width);
	}
}
