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
	
	public Vector4 copy()
	{
		return new Vector4(x, y, z, w);
	}
	
	public float[] asArray()
	{
		return new float[] { x, y, z, w };
	}
	
	public float length()
	{
		return FloatUtil.sqrt(x * x + y * y + z * z + w * w);
	}
	
	public float lengthSquared()
	{
		return x * x + y * y + z * z + w * w;
	}
	
	public Vector4 normalize()
	{
		float length = length();
		if (length > 0)
			length = 1.0f / length;
		return new Vector4(x * length, y * length, z * length, w * length);
	}
	
	public Vector4 negate()
	{
		return new Vector4(-x, -y, -z, -w);
	}
	
	public Vector4 homogeneousToCartesian()
	{
		return new Vector4(x / w, y / w, z / w, 1.0f);
	}
	
	public static Vector4 add(Vector4 l, Vector4 r)
	{
		return new Vector4(l.x + r.x, l.y + r.y, l.z + r.z, l.w + r.w);
	}
	
	public static Vector4 subtract(Vector4 l, Vector4 r)
	{
		return new Vector4(l.x - r.x, l.y - r.y, l.z - r.z, l.w - r.w);
	}
	
	public static Vector4 scalarMult(float s, Vector4 v)
	{
		return new Vector4(v.x * s, v.y * s, v.z * s, v.w * s);
	}
	
	public static Vector4 matrixMult(Matrix4 m, Vector4 v)
	{
		return new Vector4(m.e11 * v.x + m.e12 * v.y + m.e13 * v.z + m.e14 * v.w,
						   m.e21 * v.x + m.e22 * v.y + m.e23 * v.z + m.e24 * v.w,
						   m.e31 * v.x + m.e32 * v.y + m.e33 * v.z + m.e34 * v.w,
						   m.e41 * v.x + m.e42 * v.y + m.e43 * v.z + m.e44 * v.w);
	}
	
	public static Vector4 quaternionMult(Quaternion q, Vector4 v)
	{
		return Vector4.matrixMult(Matrix4.fromQuaternion(q), v);
	}
	
	public static float dot(Vector4 l, Vector4 r)
	{
		return l.x * r.x + l.y * r.y + l.z * r.z + l.w * r.w;
	}
	
	public static Vector4 min(Vector4 l, Vector4 r)
	{
		return new Vector4(Math.min(l.x, r.x), Math.min(l.y, r.y), Math.min(l.z, r.z), Math.min(l.w, r.w));
	}
	
	public static Vector4 max(Vector4 l, Vector4 r)
	{
		return new Vector4(Math.max(l.x, r.x), Math.max(l.y, r.y), Math.max(l.z, r.z), Math.max(l.w, r.w));
	}
	
	public static boolean equals(Vector4 l, Vector4 r)
	{
		return l.x == r.x && l.y == r.y && l.z == r.z && l.w == r.w;
	}
	
	@Override
	public String toString()
	{
		return "{" + x + ", " + y + ", " + z + ", " + w + "}";
	}
}