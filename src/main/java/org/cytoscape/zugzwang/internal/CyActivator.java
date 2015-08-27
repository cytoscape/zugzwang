package org.cytoscape.zugzwang.internal;

import static org.cytoscape.work.ServiceProperties.*;

import java.io.IOException;
import java.util.Properties;

import org.cytoscape.application.NetworkViewRenderer;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.EdgeViewTaskFactory;
import org.cytoscape.task.NetworkViewLocationTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.zugzwang.internal.cytoscape.view.ZZVisualLexicon;
import org.cytoscape.zugzwang.internal.task.TaskFactoryListener;
import org.osgi.framework.BundleContext;

/**
 * CyActivator object used to import and export services from and to Cytoscape, 
 * such as manager and factory objects.
 */
public class CyActivator extends AbstractCyActivator 
{
	public void start(BundleContext bc) 
	{
		RenderingEngineManager renderingEngineManager = getService(bc, RenderingEngineManager.class);
		VisualMappingManager visualMappingManagerService = getService(bc, VisualMappingManager.class);
		CyEventHelper eventHelper = getService(bc, CyEventHelper.class);
		CyServiceRegistrar registrar = getService(bc, CyServiceRegistrar.class);
		
		// TaskManager object used to execute tasks
		DialogTaskManager dialogTaskManager = getService(bc, DialogTaskManager.class);
		
		// Register service to collect references to relevant task factories for the right-click context menu
		TaskFactoryListener taskFactoryListener = new TaskFactoryListener();
		registerServiceListener(bc, taskFactoryListener, "addNodeViewTaskFactory", "removeNodeViewTaskFactory", NodeViewTaskFactory.class);
		registerServiceListener(bc, taskFactoryListener, "addEdgeViewTaskFactory", "removeEdgeViewTaskFactory", EdgeViewTaskFactory.class);
		registerServiceListener(bc, taskFactoryListener, "addNetworkViewTaskFactory", "removeNetworkViewTaskFactory", NetworkViewTaskFactory.class);
		registerServiceListener(bc, taskFactoryListener, "addNetworkViewLocationTaskFactory", "removeNetworkViewLocationTaskFactory", NetworkViewLocationTaskFactory.class);
		
		// Cy3D Visual Lexicon
		VisualLexicon zzVisualLexicon = new ZZVisualLexicon();
		Properties zzVisualLexiconProps = new Properties();
		zzVisualLexiconProps.setProperty("serviceType", "visualLexicon");
		zzVisualLexiconProps.setProperty("id", "zugzwang");
		registerService(bc, zzVisualLexicon, VisualLexicon.class, zzVisualLexiconProps);

		// Cy3D NetworkView factory
		ZZNetworkViewFactory zzNetworkViewFactory = new ZZNetworkViewFactory(zzVisualLexicon, visualMappingManagerService, eventHelper, registrar);
		Properties zzNetworkViewFactoryProps = new Properties();
		zzNetworkViewFactoryProps.setProperty("serviceType", "factory");
		registerService(bc, zzNetworkViewFactory, CyNetworkViewFactory.class, zzNetworkViewFactoryProps);
		
		// Main RenderingEngine factory
		ZZMainRenderingEngineFactory zzMainRenderingEngineFactory = new ZZMainRenderingEngineFactory(renderingEngineManager, zzVisualLexicon, taskFactoryListener, dialogTaskManager);
		
		// Main RenderingEngine factory
		ZZBirdsEyeRenderingEngineFactory zzBirdsEyeRenderingEngineFactory = new ZZBirdsEyeRenderingEngineFactory(renderingEngineManager, zzVisualLexicon, taskFactoryListener, dialogTaskManager);
				
		// NetworkViewRenderer, this is the main entry point that Cytoscape will call into
		ZZNetworkViewRenderer networkViewRenderer = new ZZNetworkViewRenderer(zzNetworkViewFactory, zzMainRenderingEngineFactory, zzBirdsEyeRenderingEngineFactory);
		registerService(bc, networkViewRenderer, NetworkViewRenderer.class, new Properties());
		
		// Still need to register the rendering engine factory directly
		Properties renderingEngineProps = new Properties();
		renderingEngineProps.setProperty(ID, ZZNetworkViewRenderer.ID);
		registerAllServices(bc, zzMainRenderingEngineFactory, renderingEngineProps);
				
		
		// Special handling for JOGL library
		try 
		{
			JoglInitializer.unpackNativeLibrariesForJOGL(bc);
		} 
		catch (IOException e) 
		{
			// This App will be useless if JOGL can't find its libraries, so best throw an exception to OSGi to shut it down.
			throw new RuntimeException(e);
 		}
	}	
}