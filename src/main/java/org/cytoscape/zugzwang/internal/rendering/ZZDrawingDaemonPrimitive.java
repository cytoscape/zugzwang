package org.cytoscape.zugzwang.internal.rendering;

import org.cytoscape.zugzwang.internal.algebra.*;

import com.jogamp.opengl.GL4;

public interface ZZDrawingDaemonPrimitive
{
	public boolean updateState(GL4 gl, Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen);
	public boolean redrawTextures(Matrix4 viewMatrix, Matrix4 projMatrix);
	public void updateResources(GL4 gl);
}