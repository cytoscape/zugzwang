package org.cytoscape.zugzwang.internal.arrowshape;

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
import java.util.HashMap;
import java.util.Map;


/**
 * Abstract arrow.
 */
abstract class RendererArrowShape
{	
	//
	// Arrow shape constants. 
	//
	public static final byte ARROW_NONE = -1;
	public static final byte ARROW_DELTA = -2;
	public static final byte ARROW_DIAMOND = -3;
	public static final byte ARROW_DISC = -4;
	public static final byte ARROW_TEE = -5;
	public static final byte ARROW_HALF_TOP = -6;
	public static final byte ARROW_HALF_BOTTOM = -7;
	public static final byte ARROW_ARROWHEAD = -8;
	public static final byte ARROW_DELTA_SHORT_1 = -9;
	public static final byte ARROW_DELTA_SHORT_2 = -10;
	public static final byte ARROW_ARROWHEAD_SHORT = -11;
	public static final byte ARROW_DIAMOND_SHORT_1 = -12;
	public static final byte ARROW_DIAMOND_SHORT_2 = -13;

	// The way to access all Arrow objects.
	private static final Map<Byte, RendererArrowShape> arrows;

	static 
	{
		arrows = new HashMap<Byte, RendererArrowShape>();

		arrows.put(ARROW_NONE, new NoArrow() );
		arrows.put(ARROW_DELTA, new DeltaArrow() );
		arrows.put(ARROW_DISC, new DiscArrow() );
		arrows.put(ARROW_DIAMOND, new DiamondArrow() );
		arrows.put(ARROW_TEE, new TeeArrow() );
		arrows.put(ARROW_ARROWHEAD, new ArrowheadArrow() );
		arrows.put(ARROW_HALF_TOP, new HalfTopArrow() );
		arrows.put(ARROW_HALF_BOTTOM, new HalfBottomArrow() );
		arrows.put(ARROW_DELTA_SHORT_1, new DeltaArrowShort1() );
		arrows.put(ARROW_DELTA_SHORT_2, new DeltaArrowShort2() );
		arrows.put(ARROW_ARROWHEAD_SHORT, new ArrowheadArrowShort() );
		arrows.put(ARROW_DIAMOND_SHORT_1, new DiamondArrowShort1() );
		arrows.put(ARROW_DIAMOND_SHORT_2, new DiamondArrowShort2() );
	}
	
	
	protected final double tOffset;

	protected Shape arrow;
	protected Shape cap;

	RendererArrowShape(final double tOffset) 
	{
		this.tOffset = tOffset;
		this.arrow = null;
		this.cap = null;
	}
	
	public Shape getArrowShape() 
	{
		return arrow;
	}

	public Shape getCapShape(final double ratio) 
	{
		// ignore the ratio by default
		return cap;
	}

	public double getTOffset() 
	{
		return tOffset;
	}

	/**
	 * Get list of arrow heads.
	 * 
	 * @return A map of arrow shape bytes to Shape objects.
	 */
	public static Map<Byte, Shape> getArrowShapes() 
	{
		final Map<Byte, Shape> shapeMap = new HashMap<Byte, Shape>();
		for (final Byte key : arrows.keySet())
			shapeMap.put(key, arrows.get(key).getArrowShape());

		return shapeMap;
	}
}

