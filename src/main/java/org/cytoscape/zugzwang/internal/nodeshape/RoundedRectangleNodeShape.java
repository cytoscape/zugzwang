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

import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;


/**
 * Taken from Ding.
 */
public class RoundedRectangleNodeShape extends RendererNodeShape 
{

	public RoundedRectangleNodeShape() 
	{
		super(RendererNodeShape.SHAPE_ROUNDED_RECTANGLE);
	}

	public Shape getShape(float xMin, float yMin, float xMax, float yMax) 
	{
		RoundRectangle2D.Float rect = new RoundRectangle2D.Float(0.0f,0.0f,1.0f,1.0f,0.3f,0.3f);
		
		float w = xMax - xMin;
		float h = yMax - yMin;
		float arcSize = Math.min(w, h) * 0.25f;
		rect.setRoundRect(xMin, yMin, w, h, arcSize, arcSize);
		
		return rect;
	}
}
