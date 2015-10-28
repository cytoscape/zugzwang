package org.cytoscape.zugzwang.internal.algebra;

import com.jogamp.opengl.math.FloatUtil;

public class Matrix2 
{
	/** Elements: first 1-based index is row, second is column. **/
	public float e11, e12, 
				 e21, e22;
	
	public Matrix2()
	{
		this.e11 = 1; this.e12 = 0;
		this.e21 = 0; this.e22 = 1;
	}
	
	public Matrix2(float e11, float e12, 
				   float e21, float e22)
	{
		this.e11 = e11; this.e12 = e12;
		this.e21 = e21; this.e22 = e22;
	}
	
	public Matrix2(float[] values)
	{
		this.e11 = values[0]; this.e12 = values[2];
		this.e21 = values[1]; this.e22 = values[3];
	}
	
	public float[] asArrayRM()
	{
		return new float[] { e11, e12, 
							 e21, e22 };
	}
	
	public float[] asArrayCM()
	{
		return new float[] { e11, e21, 
							 e12, e22 };
	}
	
	public Matrix2 transpose()
	{
		return new Matrix2(e11, e21,
						   e12, e22);		
	}
	
	public static Matrix2 mult(Matrix2 left, Matrix2 right)
	{
		return new Matrix2(left.e11 * right.e11 + left.e12 * right.e21, left.e11 * right.e12 + left.e12 * right.e22,
						   left.e21 * right.e11 + left.e22 * right.e21, left.e21 * right.e12 + left.e22 * right.e22);
	}
	
	public static Matrix2 add(Matrix2 left, Matrix2 right)
	{
		return new Matrix2(left.e11 + right.e11, left.e12 + right.e12,
						   left.e21 + right.e21, left.e22 + right.e22);
	}
	
	public static Matrix2 subtract(Matrix2 l, Matrix2 r)
	{
		return new Matrix2(l.e11 - r.e11,   l.e12 - r.e12,
						   l.e21 - r.e21,   l.e22 - r.e22);
	}
	
	public static Matrix2 identity()
	{
		return new Matrix2(1, 0,
						   0, 1);
	}
	
	public static Matrix2 rotation(float radians)
	{
		float cos = FloatUtil.cos(radians);
		float sin = FloatUtil.sin(radians);
		
		return new Matrix2(cos, -sin,
						   sin,  cos);
	}
	
	public static Matrix2 scale(Vector2 v)
	{
		return new Matrix2(v.x,   0,
						     0, v.y);
	}
}