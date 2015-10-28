package org.cytoscape.zugzwang.internal.nodeshape;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2009 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


import java.awt.geom.GeneralPath;

import com.jogamp.opengl.math.FloatUtil;

import java.awt.Shape;


/**
 * Taken from Ding.
 */
public class OctagonNodeShape extends RendererNodeShape 
{
	private static float[] coordX = new float[8];
	private static float[] coordY = new float[8];
	
	public OctagonNodeShape() 
	{
		super(RendererNodeShape.SHAPE_OCTAGON);
		
		float scale = 1.0f / FloatUtil.cos(FloatUtil.PI / 8.0f);
		
		for (int i = 0; i < 8; i++) 
		{
			float angle = ((float)i + 0.5f) * FloatUtil.PI / 4.0f;
			coordX[i] = (FloatUtil.cos(angle) * scale + 1.0f) * 0.5f;
			coordY[i] = (FloatUtil.sin(angle) * scale + 1.0f) * 0.5f;
		}
	}
	
	public Shape getShape(float xMin, float yMin, float xMax, float yMax) 
	{
		GeneralPath path = new GeneralPath();
	
		float width = xMax - xMin;
		float height = yMax - yMin;
		
		path.reset();
	
		path.moveTo(xMin + coordX[0] * width, yMin + coordY[0] * height);		
		for (int i = 1; i < 8; i++)
			path.lineTo(xMin + coordX[i] * width, yMin + coordY[i] * height);	
		path.closePath();
	
		return path;
	}
}

