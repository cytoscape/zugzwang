package org.cytoscape.zugzwang.internal;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.zugzwang.internal.viewmodel.ZZNetworkView;

/**
 * Factory responsible for instantiation of {@link ZZNetworkView}, which 
 * implements both the network view model, and the network rendering engine 
 * in Zugzwang. The rendering engine part is initialized later by sending
 * the new instance to {@link ZZMainRenderingEngineFactory}. 
 * A factory is created on startup, and a handle to the factory is stored 
 * in {@link ZZNetworkViewRenderer}, which is the central rendering service 
 * registered on startup and visible to Cytoscape.
 */
public class ZZNetworkViewFactory implements CyNetworkViewFactory 
{
	private final VisualLexicon visualLexicon;
	private final VisualMappingManager visualMappingManager;
	private final CyServiceRegistrar registrar;
	private final CyEventHelper eventHelper;
	
	public ZZNetworkViewFactory(VisualLexicon visualLexicon, VisualMappingManager visualMappingManager, CyEventHelper eventHelper, CyServiceRegistrar registrar) 
	{
		this.visualLexicon = visualLexicon;
		this.visualMappingManager = visualMappingManager;
		this.registrar = registrar;
		this.eventHelper = eventHelper;
	}
	
	/**
	 * Creates a new instance of {@link ZZNetworkView}, wrapping 
	 * the provided network data model.
	 * 
	 * @param network Network data model to be visualized by the new view model
	 * @return New network view instance
	 */
	@Override
	public CyNetworkView createNetworkView(CyNetwork network) 
	{
		return new ZZNetworkView(network, visualLexicon, visualMappingManager, eventHelper, registrar);
	}

}
