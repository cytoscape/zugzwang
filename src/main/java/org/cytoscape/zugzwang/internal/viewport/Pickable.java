package org.cytoscape.zugzwang.internal.viewport;

import org.cytoscape.zugzwang.internal.algebra.*;

public interface Pickable 
{
	public PickingResult intersectsWith(Ray3 ray, Matrix4 viewMatrix);
}