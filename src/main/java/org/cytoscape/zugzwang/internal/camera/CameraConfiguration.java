package org.cytoscape.zugzwang.internal.camera;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.zugzwang.internal.algebra.Matrix3;
import org.cytoscape.zugzwang.internal.algebra.Matrix4;
import org.cytoscape.zugzwang.internal.algebra.Vector2;
import org.cytoscape.zugzwang.internal.algebra.Vector3;
import org.cytoscape.zugzwang.internal.algebra.Vector4;

import com.jogamp.opengl.math.FloatUtil;

public class CameraConfiguration
{
	public String name;
	
	public Vector3 targetPos, cameraPos;	
	public float FOV;
	public Vector2 clippingRange;
	
	public CameraConfiguration(String name, Vector3 targetPos, Vector3 cameraPos, float FOV, Vector2 clippingRange)
	{
		this.name = name;
		
		this.targetPos = targetPos;
		this.cameraPos = cameraPos;
		this.FOV = FOV;
		this.clippingRange = clippingRange;
	}
}