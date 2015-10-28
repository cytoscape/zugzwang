package org.cytoscape.zugzwang.internal.camera;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.zugzwang.internal.algebra.*;

import com.jogamp.opengl.math.FloatUtil;

/**
 * A set of camera parameters that can be stored by the camera control, 
 * to be swapped (possibly with an animated transition) quickly.
 */
public class CameraConfiguration
{
	public String name;
	
	public Vector3 targetPos;		// Target position
	public Vector3 cameraPos;		// Camera position
	public Quaternion rotation;		// Camera rotation around target
	public float distance;			// Distance from camera to target
	public float FOV;				// Field of view in radians
	public Vector2 clippingRange;	// Near and far clipping plane distances
	
	public CameraConfiguration(String name, Vector3 targetPos, Quaternion rotation, float distance, float FOV, Vector2 clippingRange)
	{
		this.name = name;
		
		this.targetPos = targetPos;
		this.rotation = rotation;
		this.distance = distance;
		this.FOV = FOV;
		this.clippingRange = clippingRange;
		
		this.cameraPos = Vector3.quaternionMult(rotation, new Vector3(0, 0, distance));
	}
}