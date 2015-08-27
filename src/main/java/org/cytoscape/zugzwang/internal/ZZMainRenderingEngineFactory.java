package org.cytoscape.zugzwang.internal;

import javax.swing.JComponent;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.RenderingEngineFactory;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.zugzwang.internal.cytoscape.view.ZZNetworkView;
import org.cytoscape.zugzwang.internal.task.TaskFactoryListener;


public class ZZMainRenderingEngineFactory implements RenderingEngineFactory<CyNetwork> 
{	
	private final RenderingEngineManager renderingEngineManager;
	private final VisualLexicon visualLexicon;
	private final TaskFactoryListener taskFactoryListener;
	private final DialogTaskManager taskManager;
	
	
	public ZZMainRenderingEngineFactory(RenderingEngineManager renderingEngineManager, 
									VisualLexicon lexicon,
									TaskFactoryListener taskFactoryListener,
									DialogTaskManager taskManager) 
	{		
		this.renderingEngineManager = renderingEngineManager;
		this.visualLexicon = lexicon;
		this.taskFactoryListener = taskFactoryListener;
		this.taskManager = taskManager;
	}
	
	
	/**
	 */
	@Override
	public RenderingEngine<CyNetwork> createRenderingEngine(Object container, View<CyNetwork> view) 
	{		
		if(view instanceof ZZNetworkView) // ZZNetworkView implements both NetworkView and RenderingEngine
		{
			ZZNetworkView zzView = (ZZNetworkView)view;
			zzView.putIntoContainer((JComponent)container);
			
			RenderingEngine<CyNetwork> engine = (RenderingEngine<CyNetwork>)zzView;
			renderingEngineManager.addRenderingEngine(engine);
			return engine;
		}
		else
			return null;	// Wrong type of NetworkView
	}
	
	@Override
	public VisualLexicon getVisualLexicon()
	{
		return visualLexicon;
	}
}
