package org.cytoscape.zugzwang.internal;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.zugzwang.internal.cytoscape.view.ZZNetworkView;

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
	
	@Override
	public CyNetworkView createNetworkView(CyNetwork network) 
	{
		return new ZZNetworkView(network, visualLexicon, visualMappingManager, eventHelper, registrar);
	}

}
