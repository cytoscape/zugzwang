package org.cytoscape.zugzwang.internal.rendering;

import org.cytoscape.zugzwang.internal.algebra.*;

import com.jogamp.opengl.GL4;

/**
 * Drawing daemon goes through 3 stages on each drawing iteration.
 * These are the methods for each respective stage.
 */
public interface ZZDrawingDaemonPrimitive
{
	/**
	 * Checks visibility, size, and other relevant parameters to decide
	 * whether any textures need to be redrawn or resources updated.
	 * The implementation must be thread-safe, as primitive processing
	 * is distributed across multiple threads.
	 * 
	 * @param updateVisualProperties Indicates that VP values changed and their internal copies should be updated
	 * @param gl Current GL context
	 * @param viewMatrix Camera view matrix
	 * @param projMatrix Camera projection matrix
	 * @param halfScreen Screen dimensions divided by 2
	 * @return True if textures need to be redrawn or other resources updated, false otherwise
	 */
	public boolean updateState(boolean updateVisualProperties, GL4 gl, Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen);
	
	/**
	 * Redraws textures that have been determined to need a redraw
	 * in the previous stage.
	 * The implementation must be thread-safe, as primitive processing
	 * is distributed across multiple threads.
	 * 
	 * @param viewMatrix Camera view matrix
	 * @param projMatrix Camera projection matrix
	 * @return True if textures or other resources need to be uploaded to the device, false otherwise
	 */
	public boolean redrawTextures(Matrix4 viewMatrix, Matrix4 projMatrix);
	
	/**
	 * Pushes resource updates committed in the previous 
	 * stages to the device.
	 * 
	 * @param gl Current GL context
	 */
	public void updateResources(GL4 gl);
}