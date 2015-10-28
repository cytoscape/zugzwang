package org.cytoscape.zugzwang.internal.camera;


public interface CameraEventListener 
{
	/**
	 * Event raised upon a change in camera parameters (most likely movement).
	 */
	public void cameraMoved(CameraMoveEvent e);
}