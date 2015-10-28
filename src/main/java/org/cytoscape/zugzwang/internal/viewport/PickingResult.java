package org.cytoscape.zugzwang.internal.viewport;

import org.cytoscape.zugzwang.internal.algebra.Ray3;
import org.cytoscape.zugzwang.internal.algebra.Vector3;

/**
 * Description of an intersection between a pickable object and a ray in 3D space.
 */
public class PickingResult 
{
	public final Pickable source;	// Picked object
	public final Vector3 position;	// Intersection point coordinates
	public final float distance;	// Intersection distance from the point's origin
	public Ray3 ray;				// Original ray the picking has been performed with
	
	public PickingResult(Pickable source, Vector3 position, float distance, Ray3 ray)
	{
		this.source = source;
		this.position = position;
		this.distance = distance;
		this.ray = ray;
	}
}