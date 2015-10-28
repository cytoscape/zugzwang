package org.cytoscape.zugzwang.internal.strokes;

import java.awt.BasicStroke;

import org.cytoscape.view.presentation.property.LineTypeVisualProperty;

public class SolidStroke extends BasicStroke implements WidthStroke 
{
	private float width;

	public SolidStroke(float width) 
	{
		super(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		this.width = width;
	}

	public WidthStroke newInstanceForWidth(float w) 
	{
		return new SolidStroke(w);
	}

	@Override public String toString() 
	{ 
		return LineTypeVisualProperty.SOLID.toString() + " " + Float.toString(width); 
	}
}


