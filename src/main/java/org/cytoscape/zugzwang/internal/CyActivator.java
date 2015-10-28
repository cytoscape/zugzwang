package org.cytoscape.zugzwang.internal;

import static org.cytoscape.work.ServiceProperties.*;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.NetworkViewRenderer;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.EdgeViewTaskFactory;
import org.cytoscape.task.NetworkViewLocationTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphicsFactory;
import org.cytoscape.view.presentation.property.values.CyColumnIdentifierFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependencyFactory;
import org.cytoscape.view.vizmap.gui.editor.ContinuousMappingCellRendererFactory;
import org.cytoscape.view.vizmap.gui.editor.ValueEditor;
import org.cytoscape.view.vizmap.gui.editor.VisualPropertyEditor;
import org.cytoscape.view.vizmap.mappings.ValueTranslator;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.zugzwang.internal.customgraphics.CustomGraphicsManager;
import org.cytoscape.zugzwang.internal.customgraphics.CustomGraphicsTranslator;
import org.cytoscape.zugzwang.internal.customgraphics.CyCustomGraphics2Manager;
import org.cytoscape.zugzwang.internal.customgraphics.CyCustomGraphics2ManagerImpl;
import org.cytoscape.zugzwang.internal.customgraphics.bitmap.URLImageCustomGraphicsFactory;
import org.cytoscape.zugzwang.internal.customgraphics.charts.bar.BarChartFactory;
import org.cytoscape.zugzwang.internal.customgraphics.charts.box.BoxChartFactory;
import org.cytoscape.zugzwang.internal.customgraphics.charts.heatmap.HeatMapChartFactory;
import org.cytoscape.zugzwang.internal.customgraphics.charts.line.LineChartFactory;
import org.cytoscape.zugzwang.internal.customgraphics.charts.pie.PieChartFactory;
import org.cytoscape.zugzwang.internal.customgraphics.charts.ring.RingChartFactory;
import org.cytoscape.zugzwang.internal.customgraphics.vector.GradientOvalFactory;
import org.cytoscape.zugzwang.internal.customgraphics.vector.GradientRoundRectangleFactory;
import org.cytoscape.zugzwang.internal.customgraphicsmgr.CustomGraphicsManagerImpl;
import org.cytoscape.zugzwang.internal.customgraphicsmgr.action.CustomGraphicsManagerAction;
import org.cytoscape.zugzwang.internal.customgraphicsmgr.ui.CustomGraphicsBrowser;
import org.cytoscape.zugzwang.internal.dependency.CustomGraphicsSizeDependencyFactory;
import org.cytoscape.zugzwang.internal.editors.CustomGraphicsVisualPropertyEditor;
import org.cytoscape.zugzwang.internal.editors.CyCustomGraphicsValueEditor;
import org.cytoscape.zugzwang.internal.editors.ObjectPositionEditor;
import org.cytoscape.zugzwang.internal.editors.ObjectPositionValueEditor;
import org.cytoscape.zugzwang.internal.gradients.LinearGradientFactory;
import org.cytoscape.zugzwang.internal.gradients.RadialGradientFactory;
import org.cytoscape.zugzwang.internal.task.TaskFactoryListener;
import org.cytoscape.zugzwang.internal.viewmodel.ZZVisualLexicon;
import org.osgi.framework.BundleContext;

/**
 * CyActivator object used to import and export services from and to Cytoscape, 
 * such as manager and factory objects.
 */
public class CyActivator extends AbstractCyActivator 
{
	public void start(BundleContext bc) 
	{
		CustomGraphicsBrowser cgbBrowser = startCustomGraphicsMgr(bc);
		startCharts(bc);
		startGradients(bc);

		startPresentationImpl(bc, cgbBrowser);		
	}
	
	private void startPresentationImpl(BundleContext bc, CustomGraphicsBrowser cgbBrowser) 
	{
		VisualMappingManager vmmServiceRef = getService(bc, VisualMappingManager.class);
		CyServiceRegistrar cyServiceRegistrarServiceRef = getService(bc, CyServiceRegistrar.class);
		CyApplicationManager cyApplicationManagerServiceRef = getService(bc, CyApplicationManager.class);
		CustomGraphicsManager customGraphicsManagerServiceRef = getService(bc, CustomGraphicsManager.class);
		CyCustomGraphics2Manager cyCustomGraphics2ManagerServiceRef = getService(bc, CyCustomGraphics2Manager.class);
		RenderingEngineManager renderingEngineManagerServiceRef = getService(bc, RenderingEngineManager.class);
		CyRootNetworkManager cyRootNetworkFactoryServiceRef = getService(bc, CyRootNetworkManager.class);
		UndoSupport undoSupportServiceRef = getService(bc, UndoSupport.class);
		CyTableFactory cyDataTableFactoryServiceRef = getService(bc, CyTableFactory.class);
		CyServiceRegistrar cyServiceRegistrarRef = getService(bc, CyServiceRegistrar.class);
		CyEventHelper cyEventHelperServiceRef = getService(bc, CyEventHelper.class);
		CyProperty cyPropertyServiceRef = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		CyNetworkTableManager cyNetworkTableManagerServiceRef = getService(bc, CyNetworkTableManager.class);
		CyNetworkViewManager cyNetworkViewManagerServiceRef = getService(bc, CyNetworkViewManager.class);
		CyNetworkFactory cyNetworkFactory = getService(bc, CyNetworkFactory.class);
		IconManager iconManagerServiceRef = getService(bc, IconManager.class);
		
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
		
		// ZZ Visual Lexicon
		VisualLexicon zzVisualLexicon = new ZZVisualLexicon(customGraphicsManagerServiceRef);
		Properties zzVisualLexiconProps = new Properties();
		zzVisualLexiconProps.setProperty("serviceType", "visualLexicon");
		zzVisualLexiconProps.setProperty("id", "zugzwang");
		registerService(bc, zzVisualLexicon, VisualLexicon.class, zzVisualLexiconProps);

		// ZZ NetworkView factory
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
		

		ContinuousMappingCellRendererFactory continuousMappingCellRendererFactory = getService(bc, ContinuousMappingCellRendererFactory.class);

		// Object Position Editor
		ObjectPositionValueEditor objectPositionValueEditor = new ObjectPositionValueEditor();
		final Properties positionEditorProp = new Properties();
		positionEditorProp.setProperty(ID, "objectPositionValueEditor");
		registerService(bc, objectPositionValueEditor, ValueEditor.class, positionEditorProp);
		
		ObjectPositionEditor objectPositionEditor = new ObjectPositionEditor(objectPositionValueEditor, continuousMappingCellRendererFactory, iconManagerServiceRef);
		final Properties objectPositionEditorProp = new Properties();
		objectPositionEditorProp.setProperty(ID, "objectPositionEditor");
		registerService(bc, objectPositionEditor, VisualPropertyEditor.class, objectPositionEditorProp);	
		
		// Translators for Passthrough:
		
		final CustomGraphicsTranslator cgTranslator = new CustomGraphicsTranslator(customGraphicsManagerServiceRef, cyCustomGraphics2ManagerServiceRef);
		registerService(bc, cgTranslator, ValueTranslator.class, new Properties());
		
		// Factories for Visual Property Dependency:

		final CustomGraphicsSizeDependencyFactory customGraphicsSizeDependencyFactory = new CustomGraphicsSizeDependencyFactory(zzVisualLexicon);
		registerService(bc, customGraphicsSizeDependencyFactory, VisualPropertyDependencyFactory.class, new Properties());
		
		// Custom Graphics Editors
		final CyCustomGraphicsValueEditor customGraphicsValueEditor = new CyCustomGraphicsValueEditor(customGraphicsManagerServiceRef, cyCustomGraphics2ManagerServiceRef, cgbBrowser, cyServiceRegistrarRef);
		registerAllServices(bc, customGraphicsValueEditor, new Properties());
		
		final CustomGraphicsVisualPropertyEditor customGraphicsVisualPropertyEditor = new CustomGraphicsVisualPropertyEditor(CyCustomGraphics.class, customGraphicsValueEditor, continuousMappingCellRendererFactory, iconManagerServiceRef);
		registerService(bc, customGraphicsVisualPropertyEditor, VisualPropertyEditor.class, new Properties());	
	}

	private CustomGraphicsBrowser startCustomGraphicsMgr(BundleContext bc) 
	{
		CyServiceRegistrar cyServiceRegistrarServiceRef = getService(bc, CyServiceRegistrar.class);
		DialogTaskManager dialogTaskManagerServiceRef = getService(bc, DialogTaskManager.class);
		SynchronousTaskManager<?> syncTaskManagerServiceRef = getService(bc, SynchronousTaskManager.class);
		CyProperty coreCyPropertyServiceRef = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		CyApplicationManager cyApplicationManagerServiceRef = getService(bc, CyApplicationManager.class);
		CyApplicationConfiguration cyApplicationConfigurationServiceRef = getService(bc,
				CyApplicationConfiguration.class);
		CyEventHelper eventHelperServiceRef = getService(bc, CyEventHelper.class);

		VisualMappingManager vmmServiceRef = getService(bc, VisualMappingManager.class);
		
		CustomGraphicsManagerImpl customGraphicsManager = new CustomGraphicsManagerImpl(coreCyPropertyServiceRef,
																						dialogTaskManagerServiceRef, 
																						syncTaskManagerServiceRef, 
																						cyApplicationConfigurationServiceRef, 
																						eventHelperServiceRef, 
																						vmmServiceRef, 
																						cyApplicationManagerServiceRef, 
																						getdefaultImageURLs(bc));
		CustomGraphicsBrowser cgBrowser = new CustomGraphicsBrowser(customGraphicsManager);
		registerAllServices(bc, cgBrowser, new Properties());

		CustomGraphicsManagerAction customGraphicsManagerAction = new CustomGraphicsManagerAction(customGraphicsManager, cgBrowser, cyServiceRegistrarServiceRef);

		registerAllServices(bc, customGraphicsManager, new Properties());
		registerService(bc, customGraphicsManagerAction, CyAction.class, new Properties());

		// Create and register our built-in factories.
		// TODO:  When the CustomGraphicsFactory service stuff is set up, just
		// register these as services
		URLImageCustomGraphicsFactory imageFactory = new URLImageCustomGraphicsFactory(customGraphicsManager);
		customGraphicsManager.addCustomGraphicsFactory(imageFactory, new Properties());

		GradientOvalFactory ovalFactory = new GradientOvalFactory(customGraphicsManager);
		customGraphicsManager.addCustomGraphicsFactory(ovalFactory, new Properties());

		GradientRoundRectangleFactory rectangleFactory = new GradientRoundRectangleFactory(customGraphicsManager);
		customGraphicsManager.addCustomGraphicsFactory(rectangleFactory, new Properties());

		// Register this service listener so that app writers can provide their own CustomGraphics factories
		registerServiceListener(bc, customGraphicsManager, 
		                        "addCustomGraphicsFactory", "removeCustomGraphicsFactory", 
		                        CyCustomGraphicsFactory.class);
		
		// Register this service listener so that app writers can provide their own CyCustomGraphics2 factories
		final CyCustomGraphics2Manager chartFactoryManager = CyCustomGraphics2ManagerImpl.getInstance();
		registerAllServices(bc, chartFactoryManager, new Properties());
		registerServiceListener(bc, chartFactoryManager, "addFactory", "removeFactory", CyCustomGraphics2Factory.class);
		
		return cgBrowser;
	}
	
	private void startCharts(BundleContext bc) 
	{
		// Register Chart Factories
		final CyApplicationManager cyApplicationManagerServiceRef = getService(bc, CyApplicationManager.class);
		final CyColumnIdentifierFactory colIdFactory = getService(bc, CyColumnIdentifierFactory.class);
		final IconManager iconManagerServiceRef = getService(bc, IconManager.class);
		
		final Properties factoryProps = new Properties();
		factoryProps.setProperty(CyCustomGraphics2Factory.GROUP, CyCustomGraphics2Manager.GROUP_CHARTS);

		final BarChartFactory barFactory = new BarChartFactory(cyApplicationManagerServiceRef, iconManagerServiceRef, colIdFactory);
		registerService(bc, barFactory, CyCustomGraphics2Factory.class, factoryProps);

		final BoxChartFactory boxFactory = new BoxChartFactory(cyApplicationManagerServiceRef, iconManagerServiceRef, colIdFactory);
		registerService(bc, boxFactory, CyCustomGraphics2Factory.class, factoryProps);

		final PieChartFactory pieFactory = new PieChartFactory(cyApplicationManagerServiceRef, iconManagerServiceRef, colIdFactory);
		registerService(bc, pieFactory, CyCustomGraphics2Factory.class, factoryProps);

		final RingChartFactory ringFactory = new RingChartFactory(cyApplicationManagerServiceRef, iconManagerServiceRef, colIdFactory);
		registerService(bc, ringFactory, CyCustomGraphics2Factory.class, factoryProps);

		final LineChartFactory lineFactory = new LineChartFactory(cyApplicationManagerServiceRef, iconManagerServiceRef, colIdFactory);
		registerService(bc, lineFactory, CyCustomGraphics2Factory.class, factoryProps);

		final HeatMapChartFactory heatFactory = new HeatMapChartFactory(cyApplicationManagerServiceRef, iconManagerServiceRef, colIdFactory);
		registerService(bc, heatFactory, CyCustomGraphics2Factory.class, factoryProps);
	}
	
	private void startGradients(BundleContext bc) 
	{
		// Register Gradient Factories
		final Properties factoryProps = new Properties();
		factoryProps.setProperty(CyCustomGraphics2Factory.GROUP, CyCustomGraphics2Manager.GROUP_GRADIENTS);

		final LinearGradientFactory linearFactory = new LinearGradientFactory();
		registerService(bc, linearFactory, CyCustomGraphics2Factory.class, factoryProps);

		final RadialGradientFactory radialFactory = new RadialGradientFactory();
		registerService(bc, radialFactory, CyCustomGraphics2Factory.class, factoryProps);
	}
	
	/**
	 * Get list of default images from resource.
	 * 
	 * @param bc
	 * @return Set of default image files in the bundle
	 */
	private Set<URL> getdefaultImageURLs(BundleContext bc) 
	{
		Enumeration<URL> e = bc.getBundle().findEntries("images/sampleCustomGraphics", "*.png", true);
		final Set<URL> defaultImageUrls = new HashSet<URL>();
		while (e.hasMoreElements())
			defaultImageUrls.add(e.nextElement());
		
		return defaultImageUrls;
	}
}