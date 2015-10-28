package org.cytoscape.zugzwang.internal.viewport;

import com.jogamp.opengl.GLAutoDrawable;

/**
 * OpenGL-related events raised by a viewport.
 */
public interface ViewportEventListener 
{
	/**
	 * The viewport's GLJPanel and thus a new GL context has been initialized.
	 * 
	 * @param drawable GLJPanel control
	 */
	public void viewportInitialize(GLAutoDrawable drawable);
	
	/**
	 * The viewport has been resized.
	 * 
	 * @param drawable GLJPanel control
	 * @param e Information about the new viewport size
	 */
	public void viewportReshape(GLAutoDrawable drawable, ViewportResizedEvent e);
	
	/**
	 * The viewport has started drawing a new frame, this is where all rendering should occur.
	 * 
	 * @param drawable GLJPanel control
	 */
	public void viewportDisplay(GLAutoDrawable drawable);
	
	/**
	 * The viewport is being destroyed and needs to free all resources associated with its GL context.
	 * 
	 * @param drawable
	 */
	public void viewportDispose(GLAutoDrawable drawable);
}