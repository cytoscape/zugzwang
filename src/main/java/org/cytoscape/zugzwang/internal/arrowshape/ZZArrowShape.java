package org.cytoscape.zugzwang.internal.arrowshape;

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


import java.awt.Shape;
import java.util.Hashtable;
import java.util.Map;

import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.ArrowShape;


/**
 * Defines arrow shapes.<br>
 * This replaces constants defined in Arrow.java.
 *
 *
 */
public enum ZZArrowShape 
{
	NONE("None", "NONE", ArrowShapeVisualProperty.NONE, RendererArrowShape.ARROW_NONE),
	DIAMOND("Diamond", "DIAMOND", ArrowShapeVisualProperty.DIAMOND, RendererArrowShape.ARROW_DIAMOND),
	DELTA("Delta", "DELTA", ArrowShapeVisualProperty.DELTA, RendererArrowShape.ARROW_DELTA),
	ARROW("Arrow", "ARROW", ArrowShapeVisualProperty.ARROW, RendererArrowShape.ARROW_ARROWHEAD),
	T("T", "T", ArrowShapeVisualProperty.T, RendererArrowShape.ARROW_TEE),
	CIRCLE("Circle", "CIRCLE", ArrowShapeVisualProperty.CIRCLE, RendererArrowShape.ARROW_DISC),
	HALF_TOP("Half Top", "HALF_TOP", ArrowShapeVisualProperty.HALF_TOP, RendererArrowShape.ARROW_HALF_TOP),
	HALF_BOTTOM("Half Top", "HALF_BOTTOM", ArrowShapeVisualProperty.HALF_BOTTOM, RendererArrowShape.ARROW_HALF_BOTTOM),
	DELTA_SHORT_1("Delta Short 1", "DELTA_SHORT_1", ArrowShapeVisualProperty.DELTA_SHORT_1, RendererArrowShape.ARROW_DELTA_SHORT_1),
	DELTA_SHORT_2("Delta Short 2", "DELTA_SHORT_2", ArrowShapeVisualProperty.DELTA_SHORT_2, RendererArrowShape.ARROW_DELTA_SHORT_2),
	ARROW_SHORT("Arrow Short", "ARROW_SHORT", ArrowShapeVisualProperty.ARROW_SHORT, RendererArrowShape.ARROW_ARROWHEAD_SHORT),
	DIAMOND_SHORT_1("Diamond Short 1", "DIAMOND_SHORT_1", ArrowShapeVisualProperty.DIAMOND_SHORT_1, RendererArrowShape.ARROW_DIAMOND_SHORT_1),
	DIAMOND_SHORT_2("Diamond Short 2", "DIAMOND_SHORT_2", ArrowShapeVisualProperty.DIAMOND_SHORT_2, RendererArrowShape.ARROW_DIAMOND_SHORT_2);
	

	private final String displayName;
	private final String serializableString;
	private final ArrowShape presentationShape;
	private final byte rendererTypeID;
	
	private static final Map<Byte, Shape> ARROW_SHAPES;
	/** old_key -> ArrowShape */
	private static final Map<String, ZZArrowShape> legacyShapes = new Hashtable<String, ZZArrowShape>();
	static {
		// We have to support Cytoscape 2.8 XGMML shapes!
		legacyShapes.put("0", NONE);
		legacyShapes.put("3", DELTA);
		legacyShapes.put("6", ARROW);
		legacyShapes.put("9", DIAMOND);
		legacyShapes.put("12", CIRCLE);
		legacyShapes.put("15", T);
		legacyShapes.put("16", HALF_TOP);
		legacyShapes.put("17", HALF_BOTTOM);
		ARROW_SHAPES = RendererArrowShape.getArrowShapes();
	}
	
	
	private ZZArrowShape(final String displayName, final String serializableString, 
	                    final ArrowShape presentationShape, final byte rendererTypeID) {
		this.displayName = displayName;
		this.rendererTypeID = rendererTypeID;
		this.serializableString = serializableString;
		this.presentationShape = presentationShape;
	}

	/**
	 * Returns arrow type ID used in renderer code.
	 *
	 * @return
	 */
	public byte getRendererTypeID() {
		return rendererTypeID;
	}


	/**
	 * Returns human-readable name of this object.  This will be used in labels.
	 *
	 * @return DOCUMENT ME!
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	public Shape getShape() {
		return ARROW_SHAPES.get((Byte) rendererTypeID);
	}

	/**
	 *
	 * @param text
	 * @return
	 */
	public static ZZArrowShape parseArrowText(final String text) {
		ZZArrowShape shape = null;
		
		if (text != null) {
			String key = text.trim().toUpperCase();
			
			try {
				shape = valueOf(key);
			} catch (IllegalArgumentException e) {
				// brilliant flow control--this isn't a problem, we just don't match
				
				// maybe it is an old 2.x key
				shape = legacyShapes.get(key);
				
				if (shape == null) {
					// if string doesn't match, then try other possible GINY names 
					for (ZZArrowShape val : values())  {
						if (val.displayName.equalsIgnoreCase(text)) {
							shape = val;
							break;
						}
					}
				}
			}
		}
		
		if (shape == null) shape = NONE;

		return shape;
	}

	public static ArrowShape getArrowShape(final Byte rendererTypeID) {
		for (ZZArrowShape shape : values()) {
			if (shape.rendererTypeID == rendererTypeID) {
				return shape.presentationShape;
			}
		}
		return ArrowShapeVisualProperty.NONE;
	}
	
	public static ZZArrowShape getArrowShape(final ArrowShape arrowShape) {
		final String serializedString = arrowShape.getSerializableString();
		// first try for an exact match
		for (ZZArrowShape shape : values()) {
			if (shape.serializableString.equals(serializedString))
				return shape;
		}

		// if we can't match anything, just return NONE.
		return NONE;
	}
}
