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
import java.awt.Shape;

/**
 * Taken from Ding.
 */
public final class VeeNodeShape extends RendererNodeShape 
{
	public VeeNodeShape() 
	{
		super(RendererNodeShape.SHAPE_VEE);
	}
	
	@Override
	public Shape getShape(float xMin, float yMin, float xMax, float yMax) 
	{
		GeneralPath path = new GeneralPath(); 

		path.reset();

		path.moveTo(xMin, yMin);
		path.lineTo((xMin + xMax) / 2.0f, ((2.0f * yMin) + yMax) / 3.0f);
		path.lineTo(xMax, yMin);
		path.lineTo((xMin + xMax) / 2.0f, yMax);

		path.closePath();

		return path;
	}
}