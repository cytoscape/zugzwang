package org.cytoscape.zugzwang.internal.viewport;

import org.cytoscape.zugzwang.internal.algebra.*;

/**
 * An object that can be picked with a mouse cursor by intersecting it with a ray in 3D space.
 */
public interface Pickable 
{
	/**
	 * Prompts the object to test for intersections with a ray.
	 * 
	 * @param ray Ray, typically going from the camera through the mouse pointer
	 * @param viewMatrix Current view matrix 
	 * @return PickingResult object if there is an intersection, null otherwise
	 */
	public PickingResult intersectsWith(Ray3 ray, Matrix4 viewMatrix);
}