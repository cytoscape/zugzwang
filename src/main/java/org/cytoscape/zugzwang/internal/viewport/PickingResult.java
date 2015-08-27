package org.cytoscape.zugzwang.internal.viewport;

import org.cytoscape.zugzwang.internal.algebra.Ray3;
import org.cytoscape.zugzwang.internal.algebra.Vector3;

public class PickingResult 
{
	public final Pickable source;
	public final Vector3 pointFromCamera;
	
	public PickingResult(Pickable source, Vector3 pointFromCamera)
	{
		this.source = source;
		this.pointFromCamera = pointFromCamera;
	}
}