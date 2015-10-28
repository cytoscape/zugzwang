package org.cytoscape.zugzwang.internal.algebra;

import com.jogamp.opengl.math.FloatUtil;

public class Quaternion 
{
	public Vector3 xyz;
    public float w;
    
    public Quaternion(Vector3 v, float w)
    {
        this.xyz = v;
        this.w = w;
    }
    
    public Quaternion(float x, float y, float z, float w)
    {
    	this.xyz = new Vector3(x, y, z);
    	this.w = w;
    }
    
    public Quaternion(float pitch, float yaw, float roll)
    {
        yaw *= 0.5f;
        pitch *= 0.5f;
        roll *= 0.5f;

        float c1 = FloatUtil.cos(yaw);
        float c2 = FloatUtil.cos(pitch);
        float c3 = FloatUtil.cos(roll);
        float s1 = FloatUtil.sin(yaw);
        float s2 = FloatUtil.sin(pitch);
        float s3 = FloatUtil.sin(roll);

        this.w = c1 * c2 * c3 - s1 * s2 * s3;
        this.xyz.x = s1 * s2 * c3 + c1 * c2 * s3;
        this.xyz.y = s1 * c2 * c3 + c1 * s2 * s3;
        this.xyz.z = c1 * s2 * c3 - s1 * c2 * s3;
    }
    
    public Quaternion copy()
    {
    	return new Quaternion(xyz.x, xyz.y, xyz.z, w);
    }
    
    public float length()
    {
    	return FloatUtil.sqrt(w * w + xyz.lengthSquared());
    }
    
    public float lengthSquared()
    {
    	return w * w + xyz.lengthSquared();
    }
    
    public Quaternion normalize()
    {
        float scale = 1.0f / length();
        
        return new Quaternion(Vector3.scalarMult(scale, xyz), w * scale);
    }
    
    public Quaternion invert()
    {
    	Quaternion q = this.copy();
    	q.w = -q.w;
    	
    	return q;
    }
    
    public Quaternion conjugate()
    {
    	Quaternion q = this.copy();
    	q.xyz = q.xyz.negate();
    	
    	return q;
    }
    
    @Override
    public String toString()
    {
    	return "V: " + xyz.toString() + ", W: " + w;
    }
    
    /**
     * Converts the quaternion to a rotation angle around a rotated axis.
     *  
     * @return Rotated axis; its length equals the rotation angle around it
     */
    public Vector3 toAxisAngle()
    {
        Quaternion q = this.copy();
        if (Math.abs(q.w) > 1.0f)
            q = q.normalize();

        Vector3 axis = new Vector3();

        float angle = 2.0f * FloatUtil.acos(q.w);
        
        float den = FloatUtil.sqrt(1.0f - q.w * q.w);
        if (den > 0.0001f)
        	axis = Vector3.scalarMult(1.0f / den, q.xyz);
        else
            axis = new Vector3(1.0f, 0.0f, 0.0f);

        axis = Vector3.scalarMult(angle, axis.normalize());

        return axis;
    }
    
    public static Quaternion fromAxisAngle(Vector3 axis, float angle)
    {
        if (axis.lengthSquared() == 0.0f)
            return identity();

        Quaternion result = identity();

        angle *= 0.5f;
        axis = axis.normalize();
        
        result.xyz = Vector3.scalarMult(FloatUtil.sin(angle), axis);
        result.w = FloatUtil.cos(angle);

        return result.normalize();
    }
    
    public static Quaternion identity()
    {
    	return new Quaternion(new Vector3(0, 0, 0), 1);
    }
    
    public static Quaternion add(Quaternion l, Quaternion r)
    {
    	return new Quaternion(Vector3.add(l.xyz, r.xyz), l.w + r.w);
    }
    
    public static Quaternion subtract(Quaternion l, Quaternion r)
    {
    	return new Quaternion(Vector3.subtract(l.xyz, r.xyz), l.w - r.w);
    }
    
    public static Quaternion multiply(Quaternion l, Quaternion r)
    {
    	return new Quaternion(Vector3.add(Vector3.add(Vector3.scalarMult(r.w, l.xyz), Vector3.scalarMult(l.w, r.xyz)), Vector3.cross(l.xyz, r.xyz)),
    						  l.w * r.w - Vector3.dot(l.xyz, r.xyz));
    }
    
    public static boolean equals(Quaternion l, Quaternion r)
    {
    	return Vector3.equals(l.xyz, r.xyz) && l.w == r.w;
    }
}