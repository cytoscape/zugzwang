package org.cytoscape.zugzwang.internal.algebra;


public class Ray3 
{
	public Vector3 origin, direction;
	
	public Ray3(Vector3 origin, Vector3 direction)
	{
		this.origin = origin;
		this.direction = direction;
	}
	
	@Override
	public String toString()
	{
		return origin.toString() + " -> " + direction.toString();
	}
}