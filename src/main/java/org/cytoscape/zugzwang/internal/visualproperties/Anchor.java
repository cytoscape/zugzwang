package org.cytoscape.zugzwang.internal.visualproperties;

import org.cytoscape.zugzwang.internal.algebra.Vector2;
import org.cytoscape.zugzwang.internal.algebra.Vector4;

public class Anchor 
{
	/**
	 * Specifies that an anchor point lies at the center of a bounding box.
	 */
	public static final byte ANCHOR_CENTER = 0;

	/**
	 * Specifies that an anchor point lies on the north edge of a bounding box,
	 * halfway between the east and west edges.
	 */
	public static final byte ANCHOR_NORTH = 1;

	/**
	 * Specifies that an anchor point lies on the northeast corner of a bounding
	 * box.
	 */
	public static final byte ANCHOR_NORTHEAST = 2;

	/**
	 * Specifies that an anchor point lies on the east edge of a bounding box,
	 * halfway between the north and south edges.
	 */
	public static final byte ANCHOR_EAST = 3;

	/**
	 * Specifies that an anchor point lies on the southeast corner of a bounding
	 * box.
	 */
	public static final byte ANCHOR_SOUTHEAST = 4;

	/**
	 * Specifies that an anchor point lies on the south edge of a bounding box,
	 * halfway between the east and west edges.
	 */
	public static final byte ANCHOR_SOUTH = 5;

	/**
	 * Specifies that an anchor point lies on the southwest corner of a bounding
	 * box.
	 */
	public static final byte ANCHOR_SOUTHWEST = 6;

	/**
	 * Specifies that an anchor point lies on the west edge of a bounding box,
	 * halfway between the north and south edges.
	 */
	public static final byte ANCHOR_WEST = 7;

	/**
	 * Specifies that an anchor point lies on the northwest corner of a bounding
	 * box.
	 */
	public static final byte ANCHOR_NORTHWEST = 8;

	/**
	 * Used for range checking the anchor values.
	 */
	// Seems like these values should really be an enum...:
	public static final byte MAX_ANCHOR_VAL = 8;

	/**
	 * Specifies that the lines in a multi-line node label should each have a
	 * center point with similar X coordinate.
	 */
	public static final byte LABEL_WRAP_JUSTIFY_CENTER = 64;

	/**
	 * Specifies that the lines of a multi-line node label should each have a
	 * leftmost point with similar X coordinate.
	 */
	public static final byte LABEL_WRAP_JUSTIFY_LEFT = 65;

	/**
	 * Specifies that the lines of a multi-line node label should each have a
	 * rightmost point with similar X coordinate.
	 */
	public static final byte LABEL_WRAP_JUSTIFY_RIGHT = 66;
	
	
	private final static Vector2 lemma_computeAnchor(final int anchor, final Vector4 input) 
	{
		Vector2 result = new Vector2();
		
		switch (anchor) 
		{
			case ANCHOR_CENTER:
				result.x = (input.x + input.z) * 0.5f;
				result.y = (input.y + input.w) * 0.5f;			
			break;
			
			case ANCHOR_SOUTH:
				result.x = (input.x + input.z) * 0.5f;
				result.y = input.w;			
			break;
			
			case ANCHOR_SOUTHEAST:
				result.x = input.z;
				result.y = input.w;
			break;
			
			case ANCHOR_EAST:
				result.x = input.w;
				result.y = (input.y + input.w) * 0.5f;			
			break;
			
			case ANCHOR_NORTHEAST:
				result.x = input.z;
				result.y = input.y;			
			break;
			
			case ANCHOR_NORTH:
				result.x = (input.x + input.z) * 0.5f;
				result.y = input.y;			
			break;
			
			case ANCHOR_NORTHWEST:
				result.x = input.x;
				result.y = input.y;			
			break;
			
			case ANCHOR_WEST:
				result.x = input.x;
				result.y = (input.y + input.w) * 0.5f;			
			break;
			
			case ANCHOR_SOUTHWEST:
				result.x = input.x;
				result.y = input.w;			
			break;
			
			default:
				throw new IllegalStateException("encoutered an invalid ANCHOR_* constant: " + anchor);
		}
		
		return result;
	}
}
