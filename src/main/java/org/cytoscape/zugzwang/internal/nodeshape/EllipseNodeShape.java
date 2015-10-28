package org.cytoscape.zugzwang.internal.nodeshape;


/**
 * Taken from Ding.
 */
import java.awt.Shape;
import java.awt.geom.Ellipse2D;


public class EllipseNodeShape extends RendererNodeShape 
{

	public EllipseNodeShape() 
	{
		super(RendererNodeShape.SHAPE_ELLIPSE);
	}
		
	public Shape getShape(float xMin, float yMin, float xMax, float yMax) 
	{
		Ellipse2D.Float ellipse = new Ellipse2D.Float(0.0f,0.0f,1.0f,1.0f);		
		ellipse.setFrame(xMin, yMin, xMax - xMin, yMax - yMin);
		
		return ellipse;
	}
}