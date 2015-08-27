package org.cytoscape.zugzwang.internal.algebra;

import com.jogamp.opengl.math.FloatUtil;

public class Vector4 
{
	public float x, y, z, w;
	
	public Vector4()
	{
		x = 0;
		y = 0;
		z = 0;
		w = 0;
	}
	
	public Vector4(float x, float y, float z, float w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public Vector4(float[] values)
	{
		this.x = values[0];
		this.y = values[1];
		this.z = values[2];
		this.w = values[3];
	}
	
	public Vector4(Vector3 xyz, float w)
	{
		this.x = xyz.x;
		this.y = xyz.y;
		this.z = xyz.z;
		this.w = w;
	}
	
	public float[] AsArray()
	{
		return new float[] { x, y, z, w };
	}
	
	public float Length()
	{
		return FloatUtil.sqrt(x * x + y * y + z * z + w * w);
	}
	
	public Vector4 Normalize()
	{
		float length = Length();
		if (length > 0)
			length = 1.0f / length;
		return new Vector4(x * length, y * length, z * length, w * length);
	}
	
	public Vector4 HomogeneousToCartesian()
	{
		return new Vector4(x / w, y / w, z / w, 1.0f);
	}
	
	public static Vector4 Add(Vector4 l, Vector4 r)
	{
		return new Vector4(l.x + r.x, l.y + r.y, l.z + r.z, l.w + r.w);
	}
	
	public static Vector4 Subtract(Vector4 l, Vector4 r)
	{
		return new Vector4(l.x - r.x, l.y - r.y, l.z - r.z, l.w - r.w);
	}
	
	public static Vector4 ScalarMult(float s, Vector4 v)
	{
		return new Vector4(v.x * s, v.y * s, v.z * s, v.w * s);
	}
	
	public static Vector4 MatrixMult(Matrix4 m, Vector4 v)
	{
		return new Vector4(m.e11 * v.x + m.e12 * v.y + m.e13 * v.z + m.e14 * v.w,
						   m.e21 * v.x + m.e22 * v.y + m.e23 * v.z + m.e24 * v.w,
						   m.e31 * v.x + m.e32 * v.y + m.e33 * v.z + m.e34 * v.w,
						   m.e41 * v.x + m.e42 * v.y + m.e43 * v.z + m.e44 * v.w);
	}
	
	public static float Dot(Vector4 l, Vector4 r)
	{
		return l.x * r.x + l.y * r.y + l.z * r.z + l.w * r.w;
	}
	
	public static boolean Equals(Vector4 l, Vector4 r)
	{
		return l.x == r.x && l.y == r.y && l.z == r.z && l.w == r.w;
	}
	
	@Override
	public String toString()
	{
		return "{" + x + ", " + y + ", " + z + ", " + w + "}";
	}
}