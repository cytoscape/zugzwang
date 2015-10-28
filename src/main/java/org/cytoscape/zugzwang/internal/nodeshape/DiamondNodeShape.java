package org.cytoscape.zugzwang.internal.nodeshape;


import java.awt.geom.GeneralPath;
import java.awt.Shape;

/**
 * Taken from Ding.
 */
public class DiamondNodeShape extends RendererNodeShape 
{
	public DiamondNodeShape() 
	{
		super(RendererNodeShape.SHAPE_DIAMOND);
	}
		
	public Shape getShape(float xMin, float yMin, float xMax, float yMax) 
	{
		GeneralPath path = new GeneralPath();
		
		path.reset();

		path.moveTo((xMin + xMax) * 0.5f, yMin);
		path.lineTo(xMax, (yMin + yMax) * 0.5f);
		path.lineTo((xMin + xMax) * 0.5f, yMax);
		path.lineTo(xMin, (yMin + yMax) * 0.5f);

		path.closePath();

		return path;
	}
}