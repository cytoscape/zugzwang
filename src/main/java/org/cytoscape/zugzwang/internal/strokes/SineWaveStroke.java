package org.cytoscape.zugzwang.internal.strokes;

import java.awt.Shape;
import java.awt.geom.GeneralPath;

public class SineWaveStroke extends ShapeStroke 
{	
	private static final float wavelength = 20f;
	private static final float amplitude = 10f;

	public SineWaveStroke(float width) 
	{
		// second arg here is the advance - advance must equal wavelength below
		super(new Shape[] { getSineWave(width) }, wavelength, width);
	}

	public WidthStroke newInstanceForWidth(float w) 
	{
		return new SineWaveStroke(w);
	}

	private static Shape getSineWave(final float width) 
	{
		GeneralPath shape = new GeneralPath();

		shape.moveTo(0f,0f);
		shape.lineTo(0f,width);
		shape.quadTo(0.25f * wavelength,  amplitude + width, 0.5f * wavelength, width);
		shape.quadTo(0.75f * wavelength, -amplitude - width,        wavelength, width);
		shape.lineTo(wavelength, 0f);
		shape.quadTo(0.75f * wavelength, -amplitude - width, 0.5f * wavelength, 0f);
		shape.quadTo(0.25f * wavelength,  amplitude + width,                0f, 0f);

		return shape;
	}
}


