package org.cytoscape.zugzwang.internal;

import javax.swing.JComponent;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.RenderingEngineFactory;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.zugzwang.internal.task.TaskFactoryListener;
import org.cytoscape.zugzwang.internal.viewmodel.ZZNetworkView;

/**
 * Factory responsible for initialization of {@link ZZNetworkView}, which 
 * implements both the network view model, and the network rendering engine 
 * in Zugzwang. Note that the {@link ZZNetworkView} has already been created
 * before by {@link ZZNetworkViewFactory}. Upon initialization, the instance 
 * is registered in {@link RenderingEngineManager}, given the {@link VisualLexicon} 
 * defined for this factory, and put into the provided container panel. 
 * A factory is created on startup, and a handle to the factory is stored 
 * in {@link ZZNetworkViewRenderer}, which is the central rendering service 
 * registered on startup and visible to Cytoscape.
 */
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
	 * Creates a new instance of {@link ZZNetworkView} and registers
	 * it in the {@link RenderingEngineManager} defined during the factory's creation.
	 * 
	 * @return New engine instance
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

	/**
	 * Gets the {@link VisualLexicon} defined for this factory during its creation.
	 * This is the lexicon used by all instantiated renderer objects.
	 * 
	 * @return Visual lexicon
	 */
	@Override
	public VisualLexicon getVisualLexicon()
	{
		return visualLexicon;
	}
}
