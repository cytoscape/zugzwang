package org.cytoscape.zugzwang.internal.viewport;

import org.cytoscape.zugzwang.internal.algebra.Vector2;

public class ViewportResizedEvent 
{
	public final Vector2 newRawSize;
	public final Vector2 newScaledSize;
	

	public ViewportResizedEvent (Vector2 newRawSize, Vector2 newScaledSize) 
	{
		this.newRawSize = newRawSize;
		this.newScaledSize = newScaledSize;
	}
}