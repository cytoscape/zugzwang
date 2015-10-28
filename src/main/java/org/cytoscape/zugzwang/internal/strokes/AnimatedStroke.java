package org.cytoscape.zugzwang.internal.strokes;

import java.awt.Stroke;

/**
 * A marker interface for strokes that are meant to be animated (e.g. marquees).
 */
public interface AnimatedStroke extends Stroke 
{	
	/**
	 * @return A new instance of this AnimatedStroke with the next step in the animation
	 */
	public AnimatedStroke newInstanceForNextOffset();

	/**
	 * @return the current offset for this stroke
	 */
	public float getOffset();
}
