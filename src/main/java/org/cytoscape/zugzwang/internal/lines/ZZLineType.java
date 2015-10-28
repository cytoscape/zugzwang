package org.cytoscape.zugzwang.internal.lines;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
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

import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.zugzwang.internal.strokes.AnimatedDashDotStroke;
import org.cytoscape.zugzwang.internal.strokes.AnimatedEqualDashStroke;
import org.cytoscape.zugzwang.internal.strokes.AnimatedLongDashStroke;
import org.cytoscape.zugzwang.internal.strokes.BackwardSlashStroke;
import org.cytoscape.zugzwang.internal.strokes.ContiguousArrowStroke;
import org.cytoscape.zugzwang.internal.strokes.DashDotStroke;
import org.cytoscape.zugzwang.internal.strokes.DotStroke;
import org.cytoscape.zugzwang.internal.strokes.EqualDashStroke;
import org.cytoscape.zugzwang.internal.strokes.ForwardSlashStroke;
import org.cytoscape.zugzwang.internal.strokes.LongDashStroke;
import org.cytoscape.zugzwang.internal.strokes.ParallelStroke;
import org.cytoscape.zugzwang.internal.strokes.PipeStroke;
import org.cytoscape.zugzwang.internal.strokes.SeparateArrowStroke;
import org.cytoscape.zugzwang.internal.strokes.SineWaveStroke;
import org.cytoscape.zugzwang.internal.strokes.SolidStroke;
import org.cytoscape.zugzwang.internal.strokes.VerticalSlashStroke;
import org.cytoscape.zugzwang.internal.strokes.WidthStroke;
import org.cytoscape.zugzwang.internal.strokes.ZeroStroke;
import org.cytoscape.zugzwang.internal.strokes.ZigzagStroke;

public class ZZLineType implements LineType {

	private static final ZZLineType SOLID = new ZZLineType(LineTypeVisualProperty.SOLID.getDisplayName(), LineTypeVisualProperty.SOLID.getSerializableString(), new SolidStroke(1.0f));
	
	private static final ZZLineType ZIGZAG = new ZZLineType("Zigzag", "ZIGZAG", new ZigzagStroke(1.0f));
	private static final ZZLineType SINEWAVE = new ZZLineType("Sinewave", "SINEWAVE", new SineWaveStroke(1.0f));
	private static final ZZLineType VERTICAL_SLASH = new ZZLineType("Vertical Slash", "VERTICAL_SLASH", new VerticalSlashStroke(1.0f, PipeStroke.Type.VERTICAL));
	private static final ZZLineType FORWARD_SLASH = new ZZLineType("Forward Slash", "FORWARD_SLASH", new ForwardSlashStroke(1.0f, PipeStroke.Type.FORWARD));
	private static final ZZLineType BACKWARD_SLASH = new ZZLineType("Backward Slash", "BACKWARD_SLASH", new BackwardSlashStroke(1.0f, PipeStroke.Type.BACKWARD));
	private static final ZZLineType PARALLEL_LINES = new ZZLineType("Parallel Lines", "PARALLEL_LINES", new ParallelStroke(1.0f));
	private static final ZZLineType CONTIGUOUS_ARROW = new ZZLineType("Contiguous Arrow", "CONTIGUOUS_ARROW", new ContiguousArrowStroke(1.0f));
	private static final ZZLineType SEPARATE_ARROW = new ZZLineType("Separate Arrow", "SEPARATE_ARROW", new SeparateArrowStroke(1.0f));

	// For marquee or marching ants animations.  Not sure what the
	// right number of these
	private static final ZZLineType MARQUEE_DASH = new ZZLineType("Marquee Dash", "MARQUEE_DASH", new AnimatedLongDashStroke(1.0f,0.0f));
	private static final ZZLineType MARQUEE_EQUAL = new ZZLineType("Marquee Equal Dash", "MARQUEE_EQUAL", new AnimatedEqualDashStroke(1.0f,0.0f));
	private static final ZZLineType MARQUEE_DASH_DOT = new ZZLineType("Marquee Dash Dot", "MARQUEE_DASH_DOT", new AnimatedDashDotStroke(1.0f, 0.0f));
	
	private static final Map<String, ZZLineType> DEF_LINE_TYPE_MAP;

	static 
	{
		DEF_LINE_TYPE_MAP = new HashMap<String, ZZLineType>();
		DEF_LINE_TYPE_MAP.put(LineTypeVisualProperty.SOLID.getDisplayName(), SOLID);
		
		DEF_LINE_TYPE_MAP.put("Sinewave", SINEWAVE);
		DEF_LINE_TYPE_MAP.put("Vertical Slash", VERTICAL_SLASH);
		DEF_LINE_TYPE_MAP.put("Forward Slash", FORWARD_SLASH);
		DEF_LINE_TYPE_MAP.put("Backward Slash", BACKWARD_SLASH);
		DEF_LINE_TYPE_MAP.put("Parallel Lines", PARALLEL_LINES);
		DEF_LINE_TYPE_MAP.put("Contiguous Arrow", CONTIGUOUS_ARROW);
		DEF_LINE_TYPE_MAP.put("Separate Arrow", SEPARATE_ARROW);

		DEF_LINE_TYPE_MAP.put("Marquee Dash", MARQUEE_DASH);
		DEF_LINE_TYPE_MAP.put("Marquee Equal Dash", MARQUEE_EQUAL);
		DEF_LINE_TYPE_MAP.put("Marquee Dash Dot", MARQUEE_DASH_DOT);
	}

	/**
	 * Current problem: Ding's static method already adds extra line 
	 * types of Ding's own DLineType method, no matter what renderer 
	 * is active. ZZ then adds the same, causing duplicates.
	 * 
	 * Current DIRTY solution: rely on the fact that Ding's types will
	 * have the same display names as ZZ's, and match them by that name.
	 * **/
	public static final ZZLineType getZZLineType(final LineType lineType) 
	{
		if(lineType instanceof ZZLineType)
			return (ZZLineType) lineType;
		else 
		{
			final ZZLineType zl = DEF_LINE_TYPE_MAP.get(lineType.getDisplayName());
			if(zl == null)
				return ZZLineType.SOLID;
			else
				return zl;
		}
	}

	
	private final String displayName;
	private final String serializableString;
	
	private final WidthStroke stroke;
	
	public ZZLineType(final String displayName, final String serializableString, final WidthStroke stroke) 
	{
		this.displayName = displayName;
		this.serializableString = serializableString;
		this.stroke = stroke;
	}

	@Override
	public String getDisplayName() 
	{
		return displayName;
	}
	
	@Override public String toString() 
	{
		return displayName;
	}

	@Override
	public String getSerializableString() 
	{
		return serializableString;
	}
	
	/**
	 * Creates a new stroke of this LineStyle with the specified width.
	 */
	public Stroke getStroke(final float width)
	{
		if ( width <= 0 )
			return new ZeroStroke(stroke);
		else
			return stroke.newInstanceForWidth(width);
	}
}
