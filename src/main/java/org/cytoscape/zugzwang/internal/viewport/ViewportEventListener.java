package org.cytoscape.zugzwang.internal.viewport;

import com.jogamp.opengl.GLAutoDrawable;

public interface ViewportEventListener 
{
	public void viewportInitialize(GLAutoDrawable drawable);
	public void viewportReshape(GLAutoDrawable drawable, ViewportResizedEvent e);
	public void viewportDisplay(GLAutoDrawable drawable);
	public void viewportDispose(GLAutoDrawable drawable);
}