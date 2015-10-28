package org.cytoscape.zugzwang.internal.customgraphics;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.ImageCustomGraphicLayer;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.zugzwang.internal.viewmodel.ZZNodeView;
import org.cytoscape.zugzwang.internal.viewmodel.ZZVisualLexicon;
import org.cytoscape.zugzwang.internal.visualproperties.ObjectPosition;
import org.cytoscape.zugzwang.internal.visualproperties.ObjectPositionImpl;

@SuppressWarnings("rawtypes")
public class CustomGraphicsInfo 
{	
	private final VisualProperty<CyCustomGraphics> visualProperty;
	private CyCustomGraphics<? extends CustomGraphicLayer> customGraphics;
	private ObjectPosition position;
	private Float size;
	
	public CustomGraphicsInfo(final VisualProperty<CyCustomGraphics> visualProperty) 
	{
		this.visualProperty = visualProperty;
	}

	public VisualProperty<CyCustomGraphics> getVisualProperty() 
	{
		return visualProperty;
	}
	
	public CyCustomGraphics<? extends CustomGraphicLayer> getCustomGraphics() 
	{
		return customGraphics;
	}

	public void setCustomGraphics(CyCustomGraphics<? extends CustomGraphicLayer> customGraphics) 
	{
		this.customGraphics = customGraphics;
	}

	public ObjectPosition getPosition() 
	{
		return position == null ? ObjectPositionImpl.DEFAULT_POSITION : position;
	}

	public void setPosition(ObjectPosition position) 
	{
		this.position = position;
	}

	public Float getSize() 
	{
		return size;
	}

	public void setSize(Float size) 
	{
		this.size = size;
	}
	
	public List<CustomGraphicLayer> createLayers(CyNetworkView netView, ZZNodeView nodeView, Set<VisualPropertyDependency<?>> dependencies,
												 short nodeWidth, short nodeHeight, short borderWidth) 
	{
		final List<CustomGraphicLayer> transformedLayers = new ArrayList<>();
		
		if (customGraphics == null)
			return transformedLayers;
		
		final List<? extends CustomGraphicLayer> originalLayers = customGraphics.getLayers(netView, nodeView);

		if (originalLayers == null || originalLayers.isEmpty())
			return transformedLayers;

		float fitRatio = customGraphics.getFitRatio();
		
		// Check dependency. Sync size or not.
		boolean sync = syncToNode(dependencies);
		Float cgSize = size;
		ObjectPosition cgPos = position;
		float nw = nodeWidth;
		float nh = nodeHeight;
		
		for (CustomGraphicLayer layer : originalLayers) 
		{
			// Assume it's a Ding layer
			CustomGraphicLayer finalLayer = layer;

			// Resize the layer
			float cgw = 0.0f;
			float cgh = 0.0f;
			
			if (sync) 
			{
				// Size is locked to node size.
				float bw = borderWidth;
				cgw = nw - bw;
				cgh = nh - bw;
			} 
			else 
			{
				// Width and height should be set to custom size
				if (cgSize == null) 
				{
					VisualProperty<Double> sizeVP = ZZVisualLexicon.getAssociatedCGSizeVP(visualProperty);
					cgSize = nodeView.getVisualProperty(sizeVP).floatValue();
				}
				
				if (cgSize != null)
					cgw = cgh = cgSize;
			}
			
			if (cgw > 0.0 && cgh > 0.0)
				finalLayer = syncSize(layer, cgw, cgh, fitRatio);
			
			// Move the layer to the correct position
			if (cgPos == null)
				cgPos = ObjectPositionImpl.DEFAULT_POSITION;
			
			finalLayer = moveLayer(finalLayer, cgPos, nw, nh);
			
			transformedLayers.add(finalLayer);
		}
		
		return transformedLayers;
	}
	
	private boolean syncToNode(final Set<VisualPropertyDependency<?>> dependencies) 
	{
		boolean sync = false;
		
		if (dependencies != null)
			for (VisualPropertyDependency<?> dep:dependencies)
				if (dep.getIdString().equals("nodeCustomGraphicsSizeSync")) 
				{
					sync = dep.isDependencyEnabled();
					break;
				}
		
		return sync;
	}

	private CustomGraphicLayer syncSize(CustomGraphicLayer layer, float width, float height, float fitRatio)
	{
		Rectangle2D originalBounds = layer.getBounds2D();
		
		// If this is just a paint, getBounds2D will return null and we can use our own width and height
		if (originalBounds == null) 
			return layer;

		if (width == 0.0 || height == 0.0) 
			return layer;

		float cgW = (float)originalBounds.getWidth();
		float cgH = (float)originalBounds.getHeight();

		// In case size is same, return the original.
		if (width == cgW && height == cgH)
			return layer;

		AffineTransform xform;

		if (layer instanceof ImageCustomGraphicLayer) 
		{
			// Case 1: Images - Find the maximum scale to which the graphics can be scaled while
			//         fitting within the node's rectangle and maintaining its original aspect ratio
			double scale = Math.min(width / cgW, height / cgH);
			xform = AffineTransform.getScaleInstance(scale * fitRatio, scale * fitRatio);
		} 
		else 
		{
			// Case 2: If custom graphic is a vector or other implementation, fit to node's width and height
			xform = AffineTransform.getScaleInstance(fitRatio * width / cgW, fitRatio * height / cgH);
		}
		
		return layer.transform(xform);
	}
	
	private CustomGraphicLayer moveLayer(CustomGraphicLayer layer, ObjectPosition position, double nodeWidth, double nodeHeight) 
	{
		return position != null ? CustomGraphicsPositionCalculator.transform(position, nodeWidth, nodeHeight, layer) : layer;
	}
	
	@Override
	public int hashCode() 
	{
		int prime = 37;
		int result = 5;
		result = prime * result + ((visualProperty == null) ? 0 : visualProperty.hashCode());
		
		return result;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomGraphicsInfo other = (CustomGraphicsInfo) obj;
		if (visualProperty == null) 
		{
			if (other.visualProperty != null)
				return false;
		} 
		else if (!visualProperty.equals(other.visualProperty))
			return false;
		
		return true;
	}

	@Override
	public String toString() 
	{
		return "CGInfo [customGraphics=" + customGraphics + ", position=" + position + ", size=" + size + "]";
	}
}
