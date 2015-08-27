package org.cytoscape.zugzwang.internal.algebra;

import com.jogamp.opengl.math.FloatUtil;

public class Matrix3 
{
	public float e11, e12, e13,
				 e21, e22, e23,
				 e31, e32, e33;
	
	public Matrix3()
	{
		this.e11 = 1;	this.e12 = 0;	this.e13 = 0;		
		this.e21 = 0;	this.e22 = 1;	this.e23 = 0;		
		this.e31 = 0;	this.e32 = 0;	this.e33 = 1;
	}
	
	public Matrix3(float e11, float e12, float e13, 
				   float e21, float e22, float e23, 
				   float e31, float e32, float e33)
	{
		this.e11 = e11;	this.e12 = e12;	this.e13 = e13;		
		this.e21 = e21;	this.e22 = e22;	this.e23 = e23;		
		this.e31 = e31;	this.e32 = e32;	this.e33 = e33;
	}
	
	public Matrix3(float[] values)
	{
		this.e11 = values[0]; this.e12 = values[3]; this.e13 = values[6];
		this.e21 = values[1]; this.e22 = values[4]; this.e23 = values[7];		
		this.e31 = values[2]; this.e32 = values[5]; this.e33 = values[8];
	}
	
	public float[] AsArrayRM()
	{
		return new float[] { e11, e12, e13,
							 e21, e22, e23,
							 e31, e32, e33};
	}
	
	public float[] AsArrayCM()
	{
		return new float[] { e11, e21, e31,
							 e12, e22, e32,
							 e12, e23, e33};
	}
	
	public Matrix3 Transpose()
	{
		return new Matrix3(e11, e21, e31,
						   e12, e22, e32,
						   e13, e23, e33);		
	}
	
	public static Matrix3 Mult(Matrix3 l, Matrix3 r)
	{
		return new Matrix3(l.e11 * r.e11 + l.e12 * r.e21 + l.e13 * r.e31,   l.e11 * r.e12 + l.e12 * r.e22 + l.e13 * r.e32,   l.e11 * r.e13 + l.e12 * r.e23 + l.e13 * r.e33,
						   l.e21 * r.e11 + l.e22 * r.e21 + l.e23 * r.e31,   l.e21 * r.e12 + l.e22 * r.e22 + l.e23 * r.e32,   l.e21 * r.e13 + l.e22 * r.e23 + l.e23 * r.e33,
						   l.e31 * r.e11 + l.e32 * r.e21 + l.e33 * r.e31,   l.e31 * r.e12 + l.e32 * r.e22 + l.e33 * r.e32,   l.e31 * r.e13 + l.e32 * r.e23 + l.e33 * r.e33);
	}
	
	public static Matrix3 Add(Matrix3 l, Matrix3 r)
	{
		return new Matrix3(l.e11 + r.e11,   l.e12 + r.e12,   l.e13 + r.e13,
						   l.e21 + r.e21,   l.e22 + r.e22,   l.e23 + r.e23,
						   l.e31 + r.e31,   l.e32 + r.e32,   l.e33 + r.e33);
	}
	
	public static Matrix3 Subtract(Matrix3 l, Matrix3 r)
	{
		return new Matrix3(l.e11 - r.e11,   l.e12 - r.e12,   l.e13 - r.e13,
						   l.e21 - r.e21,   l.e22 - r.e22,   l.e23 - r.e23,
						   l.e31 - r.e31,   l.e32 - r.e32,   l.e33 - r.e33);
	}
	
	public static Matrix3 Identity()
	{
		return new Matrix3(1, 0, 0,
						   0, 1, 0,
						   0, 0, 1);
	}
	
	public static Matrix3 RotationX(float radians)
	{
		float cos = FloatUtil.cos(radians);
		float sin = FloatUtil.sin(radians);
		
		return new Matrix3(1,   0,    0,
						   0, cos, -sin,
						   0, sin,  cos);
	}
	
	public static Matrix3 RotationY(float radians)
	{
		float cos = FloatUtil.cos(radians);
		float sin = FloatUtil.sin(radians);
		
		return new Matrix3( cos,   0, sin,
						      0,   1,   0,
						   -sin,   0, cos);
	}
	
	public static Matrix3 RotationZ(float radians)
	{
		float cos = FloatUtil.cos(radians);
		float sin = FloatUtil.sin(radians);
		
		return new Matrix3(cos, -sin, 0,
						   sin,  cos, 0,
						     0,    0, 1);
	}
	
	public static Matrix3 Translation(Vector2 v)
	{
		return new Matrix3(1, 0, v.x,
						   0, 1, v.y,
						   0, 0,   1);
	}
	
	public static Matrix3 Scale(Vector3 v)
	{
		return new Matrix3(v.x,   0,   0,
						     0, v.y,   0,
						     0,   0, v.z);
	}
}