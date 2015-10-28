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
 * Factory responsible for instantiation of {@link ZZBirdsEyeRenderingEngine},
 * which is a camera control panel in Zugzwang. Upon creation, the new instance
 * is registered in {@link RenderingEngineManager} and given the {@link VisualLexicon}
 * defined for this factory. A factory is created on startup, and a handle to the factory 
 * is stored in {@link ZZNetworkViewRenderer}, which is the central rendering service 
 * registered on startup and visible to Cytoscape.
 */
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
	 * Creates a new instance of {@link ZZBirdsEyeRenderingEngine} and registers
	 * it in the {@link RenderingEngineManager} defined during the factory's creation.
	 * 
	 * @return New engine instance
	 */
	@Override
	public RenderingEngine<CyNetwork> createRenderingEngine(Object container, View<CyNetwork> viewModel) 
	{		
		ZZBirdsEyeRenderingEngine engine = new ZZBirdsEyeRenderingEngine((JComponent)container, (ZZNetworkView)viewModel, visualLexicon, taskFactoryListener, taskManager);
		
		renderingEngineManager.addRenderingEngine(engine);
		return engine;
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
