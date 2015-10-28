package org.cytoscape.zugzwang.internal.nodeshape;

import java.awt.Shape;
import java.awt.geom.GeneralPath;

import com.jogamp.opengl.math.FloatUtil;

/**
 * Taken from Ding.
 */
public class HexagonNodeShape extends RendererNodeShape 
{
	private static float[] coordX = new float[6];
	private static float[] coordY = new float[6];
	
	public HexagonNodeShape() 
	{
		super(RendererNodeShape.SHAPE_HEXAGON);
		
		for (int i = 0; i < 6; i++) 
		{
			float angle = (float)i * FloatUtil.PI / 3.0f;
			coordX[i] = (FloatUtil.cos(angle) + 1.0f) * 0.5f;
			coordY[i] = (FloatUtil.sin(angle) + 1.0f) * 0.5f;
		}
	}
	
	public Shape getShape(float xMin, float yMin, float xMax, float yMax) 
	{
		GeneralPath path = new GeneralPath();
	
		float width = xMax - xMin;
		float height = yMax - yMin;
		
		path.reset();
	
		path.moveTo(xMin + coordX[0] * width, yMin + coordY[0] * height);		
		for (int i = 1; i < 6; i++)
			path.lineTo(xMin + coordX[i] * width, yMin + coordY[i] * height);	
		path.closePath();
	
		return path;
	}
}

