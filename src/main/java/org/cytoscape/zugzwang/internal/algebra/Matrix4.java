package org.cytoscape.zugzwang.internal.algebra;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.FovHVHalves;

public class Matrix4 
{
	/** Elements: first 1-based index is row, second is column. **/
	public float e11, e12, e13, e14,
				 e21, e22, e23, e24,
				 e31, e32, e33, e34,
				 e41, e42, e43, e44;
	
	public Matrix4()
	{
		this.e11 = 1;	this.e12 = 0;	this.e13 = 0;	this.e14 = 0;
		this.e21 = 0;	this.e22 = 1;	this.e23 = 0;	this.e24 = 0;
		this.e31 = 0;	this.e32 = 0;	this.e33 = 1; 	this.e34 = 0;
		this.e41 = 0;	this.e42 = 0;	this.e43 = 0; 	this.e44 = 1;
	}

	public Matrix4(float e11, float e12, float e13, float e14, 
				   float e21, float e22, float e23, float e24,
				   float e31, float e32, float e33, float e34,
				   float e41, float e42, float e43, float e44)
	{
		this.e11 = e11;	this.e12 = e12;	this.e13 = e13;	this.e14 = e14;
		this.e21 = e21;	this.e22 = e22;	this.e23 = e23;	this.e24 = e24;
		this.e31 = e31;	this.e32 = e32;	this.e33 = e33; this.e34 = e34;
		this.e41 = e41;	this.e42 = e42;	this.e43 = e43; this.e44 = e44;
	}
	
	public Matrix4(float[] values)
	{
		this.e11 = values[0]; this.e12 = values[4]; this.e13 =  values[8]; this.e14 = values[12];
		this.e21 = values[1]; this.e22 = values[5]; this.e23 =  values[9]; this.e24 = values[13];
		this.e31 = values[2]; this.e32 = values[6]; this.e33 = values[10]; this.e34 = values[14];
		this.e41 = values[3]; this.e42 = values[7]; this.e43 = values[11]; this.e44 = values[15];
	}
	
	public float[] asArrayRM()
	{
		return new float[] { e11, e12, e13, e14,
							 e21, e22, e23, e24,
							 e31, e32, e33, e34,
							 e41, e42, e43, e44};
	}
	
	public float[] asArrayCM()
	{
		return new float[] { e11, e21, e31, e41,
							 e12, e22, e32, e42,
							 e13, e23, e33, e43,
							 e14, e24, e34, e44};
	}
	
	public Matrix4 transpose()
	{
		return new Matrix4(e11, e21, e31, e41,
						   e12, e22, e32, e42,
						   e13, e23, e33, e43,
						   e14, e24, e34, e44);		
	}
	
	public static Matrix4 mult(Matrix4 l, Matrix4 r)
	{
		return new Matrix4(l.e11 * r.e11 + l.e12 * r.e21 + l.e13 * r.e31 + l.e14 * r.e41,   l.e11 * r.e12 + l.e12 * r.e22 + l.e13 * r.e32 + l.e14 * r.e42,   l.e11 * r.e13 + l.e12 * r.e23 + l.e13 * r.e33 + l.e14 * r.e43,   l.e11 * r.e14 + l.e12 * r.e24 + l.e13 * r.e34 + l.e14 * r.e44,
						   l.e21 * r.e11 + l.e22 * r.e21 + l.e23 * r.e31 + l.e24 * r.e41,   l.e21 * r.e12 + l.e22 * r.e22 + l.e23 * r.e32 + l.e24 * r.e42,   l.e21 * r.e13 + l.e22 * r.e23 + l.e23 * r.e33 + l.e24 * r.e43,   l.e21 * r.e14 + l.e22 * r.e24 + l.e23 * r.e34 + l.e24 * r.e44,
						   l.e31 * r.e11 + l.e32 * r.e21 + l.e33 * r.e31 + l.e34 * r.e41,   l.e31 * r.e12 + l.e32 * r.e22 + l.e33 * r.e32 + l.e34 * r.e42,   l.e31 * r.e13 + l.e32 * r.e23 + l.e33 * r.e33 + l.e34 * r.e43,   l.e31 * r.e14 + l.e32 * r.e24 + l.e33 * r.e34 + l.e34 * r.e44,
						   l.e41 * r.e11 + l.e42 * r.e21 + l.e43 * r.e31 + l.e44 * r.e41,   l.e41 * r.e12 + l.e42 * r.e22 + l.e43 * r.e32 + l.e44 * r.e42,   l.e41 * r.e13 + l.e42 * r.e23 + l.e43 * r.e33 + l.e44 * r.e43,   l.e41 * r.e14 + l.e42 * r.e24 + l.e43 * r.e34 + l.e44 * r.e44);
	}
	
	public static Matrix4 add(Matrix4 l, Matrix4 r)
	{
		return new Matrix4(l.e11 + r.e11,   l.e12 + r.e12,   l.e13 + r.e13,   l.e14 + r.e14,
						   l.e21 + r.e21,   l.e22 + r.e22,   l.e23 + r.e23,   l.e24 + r.e24,
						   l.e31 + r.e31,   l.e32 + r.e32,   l.e33 + r.e33,   l.e34 + r.e34,
						   l.e41 + r.e41,   l.e42 + r.e42,   l.e43 + r.e43,   l.e44 + r.e44);
	}
	
	public static Matrix4 subtract(Matrix4 l, Matrix4 r)
	{
		return new Matrix4(l.e11 - r.e11,   l.e12 - r.e12,   l.e13 - r.e13,   l.e14 - r.e14,
						   l.e21 - r.e21,   l.e22 - r.e22,   l.e23 - r.e23,   l.e24 - r.e24,
						   l.e31 - r.e31,   l.e32 - r.e32,   l.e33 - r.e33,   l.e34 - r.e34,
						   l.e41 - r.e41,   l.e42 - r.e42,   l.e43 - r.e43,   l.e44 - r.e44);
	}
	
	public static Matrix4 identity()
	{
		return new Matrix4(1, 0, 0, 0,
						   0, 1, 0, 0,
						   0, 0, 1, 0,
						   0, 0, 0, 1);
	}
	
	public static Matrix4 rotationX(float radians)
	{
		float cos = FloatUtil.cos(radians);
		float sin = FloatUtil.sin(radians);
		
		return new Matrix4(1,   0,    0, 0,
						   0, cos, -sin, 0,
						   0, sin,  cos, 0,
						   0,   0,    0, 1);
	}
	
	public static Matrix4 rotationY(float radians)
	{
		float cos = FloatUtil.cos(radians);
		float sin = FloatUtil.sin(radians);
		
		return new Matrix4( cos,   0, sin, 0,
						      0,   1,   0, 0,
						   -sin,   0, cos, 0,
						      0,   0,   0, 1);
	}
	
	public static Matrix4 rotationZ(float radians)
	{
		float cos = FloatUtil.cos(radians);
		float sin = FloatUtil.sin(radians);
		
		return new Matrix4(cos, -sin, 0, 0,
						   sin,  cos, 0, 0,
						     0,    0, 1, 0,
						     0,    0, 0, 1);
	}
	
	/**
	 * Creates a matrix that represents a rotation around a rotated axis.
	 * Copied from OpenTK.
	 * 
	 * @param axis Rotated axis
	 * @param angle Rotation angle around the axis
	 * @return Rotation matrix
	 */
	public static Matrix4 fromAxisAngle(Vector3 axis, float angle)
    {
		Matrix4 result = identity();
		
        // Normalize and create a local copy of the vector.
        axis = axis.normalize();
        float axisX = axis.x, axisY = axis.y, axisZ = axis.z;

        // Calculate angles
        float cos = FloatUtil.cos(-angle);
        float sin = FloatUtil.sin(-angle);
        float t = 1.0f - cos;

        // Do the conversion math once
        float tXX = t * axisX * axisX,
			  tXY = t * axisX * axisY,
			  tXZ = t * axisX * axisZ,
			  tYY = t * axisY * axisY,
			  tYZ = t * axisY * axisZ,
			  tZZ = t * axisZ * axisZ;

        float sinX = sin * axisX,
              sinY = sin * axisY,
              sinZ = sin * axisZ;

        result.e11 = tXX + cos;
        result.e12 = tXY - sinZ;
        result.e13 = tXZ + sinY;

        result.e21 = tXY + sinZ;
        result.e22 = tYY + cos;
        result.e23 = tYZ - sinX;

        result.e31 = tXZ - sinY;
        result.e32 = tYZ + sinX;
        result.e33 = tZZ + cos;

        return result;
    }
	
	public static Matrix4 fromQuaternion(Quaternion q)
	{
		Vector3 axis = q.toAxisAngle();
		float angle = axis.length();
		
		return fromAxisAngle(axis, angle);
	}
	
	public static Matrix4 translation(Vector3 v)
	{
		return new Matrix4(1, 0, 0, v.x,
						   0, 1, 0, v.y,
						   0, 0, 1, v.z,
						   0, 0, 0,   1);
	}
	
	public static Matrix4 scale(Vector3 v)
	{
		return new Matrix4(v.x,   0,   0, 0,
						     0, v.y,   0, 0,
						     0,   0, v.z, 0,
						     0,   0,   0, 1);
	}
	
	public static Matrix4 lookAtRH(Vector3 camera, Vector3 target, Vector3 up)
	{
		/*Vector3 forward = Vector3.Subtract(target, camera).Normalize();
	    Vector3 right = Vector3.Cross(forward, up).Normalize();
	    up = Vector3.Cross(right, forward);
	 
	    return new Matrix4(right.x, up.x, forward.x, -camera.x,
	    				   right.y, up.y, forward.y, -camera.y,
	    				   right.z, up.z, forward.z, -camera.z,
	    				         0,    0,         0,         1);*/
	    
	    return new Matrix4(FloatUtil.makeLookAt(new float[16], 0, camera.asArray(), 0, target.asArray(), 0, up.asArray(), 0, new float[16]));
	}
	
	public static Matrix4 projectionPerspective(float aspectRatio, float fieldOfView, float near, float far)
	{
		float tan = FloatUtil.tan(fieldOfView * 0.5f);
		float range = near - far;
		
		/*return new Matrix4(1.0f / tan,                          0,                    0,                         0,
						            0, 1.0f / (tan / aspectRatio),                    0,                         0,
						            0,                          0, (near + far) / range, 2.0f * near * far / range,
						            0,                          0,                   -1,                         0);*/
		
		return new Matrix4(FloatUtil.makePerspective(new float[16], 0, true, new FovHVHalves(fieldOfView * 0.5f, fieldOfView * 0.5f, fieldOfView * 0.5f / aspectRatio, fieldOfView * 0.5f / aspectRatio, true), near, far));
	}
	
	public static Matrix4 projectionOrthogonal(int width, int height, float near, float far)
	{
		float range = far - near;
		
		return new Matrix4(2.0f / (float)width,                    0,             0,                    0,
						                     0, 2.0f / (float)height,             0,                    0,
						                     0,                    0, -2.0f / range, (far + near) / range,
						                     0,                    0,             0,                    1);
	}
}