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


public class ZZBirdsEyeRenderingEngineFactory implements RenderingEngineFactory<CyNetwork> 
{	
	private final RenderingEngineManager renderingEngineManager;
	private final VisualLexicon visualLexicon;
	private final TaskFactoryListener taskFactoryListener;
	private final DialogTaskManager taskManager;
	
	
	public ZZBirdsEyeRenderingEngineFactory(RenderingEngineManager renderingEngineManager, 
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
	public RenderingEngine<CyNetwork> createRenderingEngine(Object container, View<CyNetwork> viewModel) 
	{		
		ZZBirdsEyeRenderingEngine engine = new ZZBirdsEyeRenderingEngine((JComponent)container, (ZZNetworkView)viewModel, visualLexicon, taskFactoryListener, taskManager);
		
		renderingEngineManager.addRenderingEngine(engine);
		return engine;
	}
	
	@Override
	public VisualLexicon getVisualLexicon()
	{
		return visualLexicon;
	}
}
