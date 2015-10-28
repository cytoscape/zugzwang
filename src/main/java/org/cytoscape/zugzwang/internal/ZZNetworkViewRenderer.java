package org.cytoscape.zugzwang.internal;

import org.cytoscape.application.NetworkViewRenderer;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.presentation.RenderingEngineFactory;

/**
 * This is the main service that is registered with Cytoscape so that it
 * picks up the 3D renderer. Despite its suggestive name, this class doesn't
 * render anything itself. Instead, it contains factory objects to instantiate
 * network view models (which are also network rendering engines), and bird's
 * eye view rendering engines (which are camera control panels in Zugzwang).
 */
public class ZZNetworkViewRenderer implements NetworkViewRenderer 
{
	public static final String ID = "org.cytoscape.zugzwang";
	public static final String DISPLAY_NAME = "Zugzwang";
	
	private final CyNetworkViewFactory networkViewFactory;
	private final RenderingEngineFactory<CyNetwork> mainFactory;
	private final RenderingEngineFactory<CyNetwork> birdsEyeFactory;
	
	public ZZNetworkViewRenderer(CyNetworkViewFactory networkViewFactory, RenderingEngineFactory<CyNetwork> mainFactory, RenderingEngineFactory<CyNetwork> birdsEyeFactory) 
	{
		this.networkViewFactory = networkViewFactory;
		this.mainFactory = mainFactory;
		this.birdsEyeFactory = birdsEyeFactory;
	}	
	
	@Override
	public String getId() 
	{
		return ID;
	}

	/**
	 * Gets a factory that instantiates network view models, 
	 * which are also their own main rendering engines.
	 * 
	 * @return Network view factory
	 */
	@Override
	public CyNetworkViewFactory getNetworkViewFactory() 
	{
		return networkViewFactory;
	}

	/**
	 * Gets a rendering engine factory for the provided context.
	 * 
	 * @return Rendering engine factory of the specified type
	 */
	@Override
	public RenderingEngineFactory<CyNetwork> getRenderingEngineFactory(String context) 
	{
		switch (context) 
		{
			default:
			case DEFAULT_CONTEXT: 
				return mainFactory;
			case BIRDS_EYE_CONTEXT: 
				return birdsEyeFactory;
			case VISUAL_STYLE_PREVIEW_CONTEXT: 
				return null;
		}
	}

	@Override
	public String toString() 
	{
		return DISPLAY_NAME;
	}
}
