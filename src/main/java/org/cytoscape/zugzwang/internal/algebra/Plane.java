package org.cytoscape.zugzwang.internal.algebra;


public class Plane 
{
	public Vector3 origin, normal;
	
	public Plane(Vector3 origin, Vector3 normal)
	{
		this.origin = origin;
		this.normal = normal;
	}
	
	public Vector3 intersect(Ray3 ray)
	{
		float denominator = Vector3.Dot(normal, ray.direction);
		float numerator = Vector3.Dot(Vector3.Subtract(origin, ray.origin), normal);
		
		if (denominator == 0.0f)
			if (numerator == 0.0f)
				return ray.origin;	// Parallel, in plane
			else
				return null;		// Parallel, not in plane
		
		float d = numerator / denominator;
		
		return Vector3.Add(ray.origin, Vector3.ScalarMult(d, ray.direction));
	}
	
	@Override
	public String toString()
	{
		return origin.toString() + " -> " + normal.toString();
	}
}