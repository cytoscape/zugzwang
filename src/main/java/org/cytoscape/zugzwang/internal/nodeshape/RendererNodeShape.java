package org.cytoscape.zugzwang.internal.nodeshape;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.HashMap;
import java.util.Map;



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


/**
 * An abstract implementation of NodeShape that contains an
 * enumeration of all available node shapes.
 */
public abstract class RendererNodeShape
{
	/**
	 * Node shape constants
	 */
	public static final byte SHAPE_RECTANGLE = 0;
	public static final byte SHAPE_DIAMOND = 1;
	public static final byte SHAPE_ELLIPSE = 2;
	public static final byte SHAPE_HEXAGON = 3;
	public static final byte SHAPE_OCTAGON = 4;
	public static final byte SHAPE_PARALLELOGRAM = 5;
	public static final byte SHAPE_ROUNDED_RECTANGLE = 6;
	public static final byte SHAPE_TRIANGLE = 7;
	public static final byte SHAPE_VEE = 8;

	private static final float DEF_SHAPE_SIZE = 32;
	
	public static final Map<Byte, RendererNodeShape> nodeShapes;
	
	static 
	{		
		nodeShapes = new HashMap<Byte, RendererNodeShape>();

		nodeShapes.put(SHAPE_RECTANGLE, new RectangleNodeShape()); 
		nodeShapes.put(SHAPE_ELLIPSE, new EllipseNodeShape()); 
		nodeShapes.put(SHAPE_ROUNDED_RECTANGLE, new RoundedRectangleNodeShape()); 
		nodeShapes.put(SHAPE_DIAMOND, new DiamondNodeShape()); 
		nodeShapes.put(SHAPE_HEXAGON, new HexagonNodeShape()); 
		nodeShapes.put(SHAPE_OCTAGON, new OctagonNodeShape()); 
		nodeShapes.put(SHAPE_PARALLELOGRAM, new ParallelogramNodeShape()); 
		nodeShapes.put(SHAPE_TRIANGLE, new TriangleNodeShape()); 
		nodeShapes.put(SHAPE_VEE, new VeeNodeShape());
	}
		
	private final byte type;
	
	RendererNodeShape(byte type) 
	{
		this.type = type;	
	}
	
	/**
	 * A legacy method to interact cleanly with the current implementation of
	 * GraphGraphics.  
	 * @return the byte associated with this node shape.
	 */
	public byte getType() 
	{
		return type;
	}

	/**
	 * Returns a Shape object scaled to fit within the bounding box defined by the
	 * input parameters.
	 */
	public abstract Shape getShape(final float xMin,final float yMin, final float xMax, final float yMax);
	
	/**
	 * get list of node shapes.
	 * 
	 * @return A map of node shape bytes to Shape objects.
	 */
	public static Map<Byte, Shape> getNodeShapes() 
	{
		final Map<Byte, Shape> shapeMap = new HashMap<Byte, Shape>();

		for (RendererNodeShape ns : nodeShapes.values()) 
			shapeMap.put(ns.getType(), new GeneralPath(ns.getShape(0f, 0f, DEF_SHAPE_SIZE, DEF_SHAPE_SIZE)));

		return shapeMap;
	}
}
