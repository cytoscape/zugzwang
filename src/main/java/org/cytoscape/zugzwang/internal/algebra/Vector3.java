package org.cytoscape.zugzwang.internal.algebra;

import com.jogamp.opengl.math.FloatUtil;

public class Vector3 
{
	public float x, y, z;
	
	public Vector3()
	{
		x = 0;
		y = 0;
		z = 0;
	}
	
	public Vector3(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3(float[] values)
	{
		this.x = values[0];
		this.y = values[1];
		this.z = values[2];
	}
	
	public Vector3 copy()
	{
		return new Vector3(x, y, z);
	}
	
	public float[] asArray()
	{
		return new float[] { x, y, z };
	}
	
	public float length()
	{
		return FloatUtil.sqrt(x * x + y * y + z * z);
	}
	
	public float lengthSquared()
	{
		return x * x + y * y + z * z;
	}
	
	public Vector3 normalize()
	{
		float length = length();
		if (length > 0)
			length = 1.0f / length;
		return new Vector3(x * length, y * length, z * length);
	}
	
	public Vector3 negate()
	{
		return new Vector3(-x, -y, -z);
	}
	
	public static Vector3 add(Vector3 l, Vector3 r)
	{
		return new Vector3(l.x + r.x, l.y + r.y, l.z + r.z);
	}
	
	public static Vector3 subtract(Vector3 l, Vector3 r)
	{
		return new Vector3(l.x - r.x, l.y - r.y, l.z - r.z);
	}
	
	public static Vector3 scalarMult(float s, Vector3 v)
	{
		return new Vector3(v.x * s, v.y * s, v.z * s);
	}
	
	public static Vector3 matrixMult(Matrix3 m, Vector3 v)
	{
		return new Vector3(m.e11 * v.x + m.e12 * v.y + m.e13 * v.z,
						   m.e21 * v.x + m.e22 * v.y + m.e23 * v.z,
						   m.e31 * v.x + m.e32 * v.y + m.e33 * v.z);
	}
	
	public static Vector3 quaternionMult(Quaternion q, Vector3 v)
	{
		return Vector3.matrixMult(Matrix3.fromQuaternion(q), v);
	}
	
	public static float dot(Vector3 l, Vector3 r)
	{
		return l.x * r.x + l.y * r.y + l.z * r.z;
	}
	
	public static Vector3 cross(Vector3 l, Vector3 r)
	{
		return new Vector3(l.y * r.z - l.z * r.y,
						   l.z * r.x - l.x * r.z,
						   l.x * r.y - l.y * r.x);
	}
	
	public static Vector3 min(Vector3 l, Vector3 r)
	{
		return new Vector3(Math.min(l.x, r.x), Math.min(l.y, r.y), Math.min(l.z, r.z));
	}
	
	public static Vector3 max(Vector3 l, Vector3 r)
	{
		return new Vector3(Math.max(l.x, r.x), Math.max(l.y, r.y), Math.max(l.z, r.z));
	}
	
	public static boolean equals(Vector3 l, Vector3 r)
	{
		return l.x == r.x && l.y == r.y && l.z == r.z;
	}
	
	@Override
	public String toString()
	{
		return "{" + x + ", " + y + ", " + z + "}";
	}
}