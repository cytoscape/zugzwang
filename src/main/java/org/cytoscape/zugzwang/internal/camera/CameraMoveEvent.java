package org.cytoscape.zugzwang.internal.camera;

import org.cytoscape.zugzwang.internal.algebra.Matrix4;
import org.cytoscape.zugzwang.internal.algebra.Vector3;

public class CameraMoveEvent 
{
	public final CameraConfiguration oldConfiguration;
	public final CameraConfiguration newConfiguration;
	public final Matrix4 matrixViewProj;
	public final boolean isWithinPlane;	// Only pan movement
	

	public CameraMoveEvent (CameraConfiguration oldConfiguration, CameraConfiguration newConfiguration, Matrix4 matrixViewProj, boolean isWithinPlane) 
	{
		this.oldConfiguration = oldConfiguration;
		this.newConfiguration = newConfiguration;
		this.matrixViewProj = matrixViewProj;
		this.isWithinPlane = isWithinPlane;
	}
}