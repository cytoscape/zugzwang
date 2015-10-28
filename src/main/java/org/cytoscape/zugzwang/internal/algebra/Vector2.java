package org.cytoscape.zugzwang.internal.algebra;

import com.jogamp.opengl.math.FloatUtil;

public class Vector2 
{
	public float x, y;
	
	public Vector2()
	{
		x = 0;
		y = 0;
	}
	
	public Vector2(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public Vector2(float[] values)
	{
		this.x = values[0];
		this.y = values[1];
	}
	
	public Vector2 copy()
	{
		return new Vector2(x, y);
	}
	
	public float[] asArray()
	{
		return new float[] { x, y };
	}
	
	public float length()
	{
		return FloatUtil.sqrt(x * x + y * y);
	}
	
	public float lengthSquared()
	{
		return x * x + y * y;
	}
	
	public Vector2 normalize()
	{
		float length = length();
		if (length > 0)
			length = 1.0f / length;
		return new Vector2(x * length, y * length);
	}
	
	public Vector2 negate()
	{
		return new Vector2(-x, -y);
	}
	
	public static Vector2 add(Vector2 l, Vector2 r)
	{
		return new Vector2(l.x + r.x, l.y + r.y);
	}
	
	public static Vector2 subtract(Vector2 l, Vector2 r)
	{
		return new Vector2(l.x - r.x, l.y - r.y);
	}
	
	public static Vector2 scalarMult(float s, Vector2 v)
	{
		return new Vector2(v.x * s, v.y * s);
	}
	
	public static Vector2 matrixMult(Matrix2 m, Vector2 v)
	{
		return new Vector2(m.e11 * v.x + m.e12 * v.y,
						   m.e21 * v.x + m.e22 * v.y);
	}
	
	public static float dot(Vector2 l, Vector2 r)
	{
		return l.x * r.x + l.y * r.y;
	}
	
	public static Vector2 min(Vector2 l, Vector2 r)
	{
		return new Vector2(Math.min(l.x, r.x), Math.min(l.y, r.y));
	}
	
	public static Vector2 max(Vector2 l, Vector2 r)
	{
		return new Vector2(Math.max(l.x, r.x), Math.max(l.y, r.y));
	}
	
	public static boolean equals(Vector2 l, Vector2 r)
	{
		return l.x == r.x && l.y == r.y;
	}
	
	@Override
	public String toString()
	{
		return "{" + x + ", " + y + "}";
	}
}