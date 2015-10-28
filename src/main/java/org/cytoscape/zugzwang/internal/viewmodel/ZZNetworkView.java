package org.cytoscape.zugzwang.internal.viewmodel;

import java.awt.Component;
import java.awt.Image;
import java.awt.print.Printable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.*;
import org.cytoscape.model.events.*;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.*;
import org.cytoscape.view.model.events.*;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.events.*;
import org.cytoscape.zugzwang.internal.ZZNetworkViewRenderer;
import org.cytoscape.zugzwang.internal.algebra.*;
import org.cytoscape.zugzwang.internal.rendering.*;
import org.cytoscape.zugzwang.internal.tools.*;
import org.cytoscape.zugzwang.internal.viewport.*;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.GLBuffers;

/**
 * Implements both a network view and its renderer. This class listens
 * to changes in the data model (node/edge addition/removal, visual prop 
 * changes) and user actions in the viewport (mouse input). When the
 * viewport needs to be redrawn, ZZNetworkView executes the entire update/
 * drawing pipeline directed by ZZDrawingDaemon.
 */
public class ZZNetworkView extends ZZView<CyNetwork> implements CyNetworkView, 
																RenderingEngine<CyNetwork>, 
																ViewportEventListener, 
																ViewportMouseEventListener,
																AddedEdgesListener, 
																AddedNodesListener, 
																AboutToRemoveEdgesListener, 
																AboutToRemoveNodesListener, 
																RowsSetListener,
																VisualStyleChangedListener, 
																VisualMappingFunctionChangedListener 
{	
	Object m_sync = new Object();
	
	CyEventHelper eventHelper;
	CyServiceRegistrar registrar;
	
	GL4 gl;						// Current GL context
	boolean doBindless = false;	// Device support for bindless textures
	
	private Viewport viewport;	// Viewport that hosts the GLJPanel and registers user interaction with it
	
	public float globalDownsampling = 1.0f;	// Downsampling factor for all node and edge textures, used to regulate overall consumption
	
	private final CyNetwork network;	// Underlying data model
	
	private final DefaultValueVault defaultVault;				// Vault with default values for visual props
	private final VisualLexicon visualLexicon;					// Lexicon of visual props supported by this view model
	private final VisualMappingManager visualMappingManager;	// This is needed for custom graphics VP dependencies
	private Set<VisualPropertyDependency<?>> visualDependencies; // The said VP dependencies, updated at the beginning of each frame
	
	// Internal node/edge view collections
	private Map<CyNode, ZZNodeView> nodeViews = new HashMap<>();
	private Map<CyEdge, ZZEdgeView> edgeViews = new HashMap<>();

	// Current selection
	private List<CyNode> nodeSelectionList;
	private List<CyEdge> edgeSelectionList;
	
	// Central drawing primitive managers maintain information about primitives in device buffers
	private ZZRectangleManager managerNodeShapes, managerNodeLabels;
	private ZZRectangleManager[] managersNodeCG = new ZZRectangleManager[ZZVisualLexicon.getCGVisualProperties().size()];
	private ZZLineManager managerEdgeLines;
	
	// Signals that default VP values changed and view models should sync
	private boolean defaultVisualPropertiesChanged = false;
	
	// GLSL programs
	int[] programs = new int[2];
	
	// Default textures
	ZZBindlessTexture shapeDefaultTex, labelDefaultTex;
	
	public ZZNetworkView(CyNetwork network, 
						 VisualLexicon visualLexicon, 
						 VisualMappingManager visualMappingManager,
						 CyEventHelper eventHelper,
						 CyServiceRegistrar registrar) 
	{
		super(visualLexicon, eventHelper);
		
		this.eventHelper = eventHelper;
		this.registrar = registrar;

		registrar.registerAllServices(this, new Properties());
		
		this.network = network;
		this.visualLexicon = visualLexicon;
		this.defaultVault = new DefaultValueVault(visualLexicon);
		this.visualMappingManager = visualMappingManager;
	}
	
	// ***********************
	// Initialization methods:
	// ***********************
	
	/**
	 * Initializes the viewport and lets it put itself
	 * into the provided container.
	 * 
	 * @param component Container to put the viewport into
	 */
	public void putIntoContainer(JComponent component)
	{		
		this.viewport = new Viewport(component);
		viewport.addViewportEventListener(this);
	}
	
	/**
	 * Corrects discrepancies between Cytoscape's node and edge data,
	 * and the internal view models for them.
	 */
	private void syncWithModel()
	{
		synchronized (m_sync)
		{
			Set<CyNode> modelNodes = new HashSet<>(network.getNodeList().size());
			for (CyNode node : network.getNodeList())
				modelNodes.add(node);
			Set<CyEdge> modelEdges = new HashSet<>(network.getEdgeList().size());
			for (CyEdge edge : network.getEdgeList())
				modelEdges.add(edge);
	
			List<CyNode> notInModelNodes = new ArrayList<>(nodeViews.size());
			for (CyNode node : nodeViews.keySet())
				if (!modelNodes.contains(node))
					notInModelNodes.add(node);
			List<CyEdge> notInModelEdges = new ArrayList<>(edgeViews.size());
			for (CyEdge edge : edgeViews.keySet())
				if (!modelEdges.contains(edge))
					notInModelEdges.add(edge);
			
			for (CyNode node : notInModelNodes)
				removeNodeView(node);
			for (CyEdge edge : notInModelEdges)
				removeEdgeView(edge);
			
			for (CyNode node : network.getNodeList()) 
				addNodeView(node);
			
			for (CyEdge edge : network.getEdgeList()) 
				addEdgeView(edge);
		}
	}
	
	// ****************
	// View add/remove:
	// ****************
	
	/**
	 * Adds a node view that wraps the provided node data model.
	 * 
	 * @param node Node data model to add
	 */
	private void addNodeView(CyNode node)
	{
		synchronized (m_sync)
		{
			if (nodeViews.containsKey(node))
				return;
			
			ZZNodeView nodeView = new ZZNodeView(this, defaultVault, visualLexicon, eventHelper, node, managerNodeShapes, managerNodeLabels, managersNodeCG);
			nodeViews.put(node, nodeView);
			
			for (CyEdge edge : network.getAdjacentEdgeIterable(node, CyEdge.Type.OUTGOING))
			{
				ZZEdgeView edgeView = (ZZEdgeView)edgeViews.get(edge);
				if (edgeView != null)
					nodeView.addOutgoingEdgeView(edgeView);
			}
			
			for (CyEdge edge : network.getAdjacentEdgeIterable(node, CyEdge.Type.INCOMING))
			{
				ZZEdgeView edgeView = (ZZEdgeView)edgeViews.get(edge);
				if (edgeView != null)
					nodeView.addIncomingEdgeView(edgeView);
			}
			
			eventHelper.addEventPayload((CyNetworkView)this, (View<CyNode>)nodeView, AddedNodeViewsEvent.class);
		}
	}
	
	/**
	 * Removes the node view associated with the provided 
	 * node data model, if present.
	 * 
	 * @param node Node data model to remove
	 */
	private void removeNodeView(CyNode node)
	{
		synchronized (m_sync)
		{
			ZZNodeView nodeView = nodeViews.get(node);
			if (nodeView == null)
				return;
			
			nodeView.dispose(gl);
			nodeViews.remove(node);
	
			eventHelper.addEventPayload((CyNetworkView)this, (View<CyNode>)nodeView, AboutToRemoveNodeViewsEvent.class);
		}
	}
	
	/**
	 * Adds an edge view that wraps the provided edge data model.
	 * 
	 * @param edge Edge data model to add
	 */
	private void addEdgeView(CyEdge edge)
	{
		synchronized (m_sync)
		{
			if (edgeViews.containsKey(edge))
				return;
			
			ZZEdgeView edgeView = new ZZEdgeView(this, defaultVault, visualLexicon, eventHelper, edge, managerEdgeLines);
			edgeViews.put(edge, edgeView);
			
			ZZNodeView sourceView = (ZZNodeView)nodeViews.get(edge.getSource());
			if (sourceView != null)
				sourceView.addOutgoingEdgeView(edgeView);
			
			ZZNodeView targetView = (ZZNodeView)nodeViews.get(edge.getTarget());
			if (targetView != null)
				targetView.addIncomingEdgeView(edgeView);
				
			eventHelper.addEventPayload((CyNetworkView)this, (View<CyEdge>)edgeView, AddedEdgeViewsEvent.class);
		}
	}
	
	/**
	 * Removes the edge view associated with the provided
	 * edge data model, if present.
	 * 
	 * @param edge Edge data model to remove
	 */
	private void removeEdgeView(CyEdge edge)
	{
		synchronized (m_sync)
		{
			ZZEdgeView edgeView = edgeViews.get(edge);
			if (edgeView == null)
				return;
			
			ZZNodeView sourceView = nodeViews.get(edge.getSource());
			if (sourceView != null)
				sourceView.removeOutgoingEdgeView(edgeView);
			
			ZZNodeView targetView = nodeViews.get(edge.getTarget());
			if (targetView != null)
				targetView.removeIncomingEdgeView(edgeView);
			
			edgeView.dispose(gl);
			nodeViews.remove(edge);
	
			eventHelper.addEventPayload((CyNetworkView)this, (View<CyEdge>)edgeView, AboutToRemoveEdgeViewsEvent.class);
		}
	}
	
	// ************************
	// CyNetworkView interface:
	// ************************
	
	/**
	 * Gets the underlying CyNetwork data model.
	 * 
	 * @return Data model
	 */
	@Override
	public CyNetwork getModel() 
	{
		return network;
	}

	/**
	 * Gets the network view's view model for the provided CyNode data model.
	 * 
	 * @return Node view model
	 */
	@Override
	public View<CyNode> getNodeView(CyNode node) 
	{
		return nodeViews.get(node.getSUID());
	}

	/**
	 * Gets the network view's view models for the provided CyNode data models.
	 * 
	 * @return Collection of node view models
	 */
	@Override
	public Collection<View<CyNode>> getNodeViews() 
	{
		List<View<CyNode>> result = new ArrayList<>(nodeViews.size());
		for (ZZNodeView view : nodeViews.values())
			result.add((View<CyNode>)view);
		
		return result;
	}

	/**
	 * Gets the network view's view model for the provided CyEdge data model.
	 * 
	 * @return Edge view model
	 */
	@Override
	public View<CyEdge> getEdgeView(CyEdge edge) 
	{
		return edgeViews.get(edge.getSUID());
	}

	/**
	 * Gets the network view's view models for the provided CyEdge data models.
	 * 
	 * @return Collection of edge view models
	 */
	@Override
	public Collection<View<CyEdge>> getEdgeViews() 
	{
		List<View<CyEdge>> result = new ArrayList<>(edgeViews.size());
		for (ZZEdgeView view : edgeViews.values())
			result.add((View<CyEdge>)view);
		
		return result;
	}

	/**
	 * Gets all of the network view's view models for nodes and edges.
	 * 
	 * @return Collection of all node and edge models (in that order)
	 */
	@Override
	public Collection<View<? extends CyIdentifiable>> getAllViews() 
	{
		Collection<View<? extends CyIdentifiable>> views = new HashSet<>();
		
		synchronized (m_sync)
		{
			views.addAll(getNodeViews());
			views.addAll(getEdgeViews());
			views.add(this);
		}
		
		return views;
	}

	/**
	 * Positions the viewport's camera so that all nodes and edges are visible.
	 */
	@Override
	public void fitContent() 
	{
		synchronized (m_sync)
		{
			List<ZZNodeView> fitNodes = new ArrayList<ZZNodeView>(nodeViews.size());
			for (ZZNodeView view : nodeViews.values())
				fitNodes.add(view);
			
			fitContent(fitNodes);
		}
	}
	
	private void fitContent(List<ZZNodeView> fitNodes)
	{		
		Vector3 minCoords = new Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		Vector3 maxCoords = new Vector3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		Vector2 minScreen = new Vector2(Float.MAX_VALUE, Float.MAX_VALUE);
		Vector2 maxScreen = new Vector2(-Float.MAX_VALUE, -Float.MAX_VALUE);
		
		for (ZZNodeView nodeView : fitNodes)
		{
			Vector3 pos = nodeView.getPosition();
			minCoords = Vector3.min(minCoords, pos);
			maxCoords = Vector3.max(maxCoords, pos);
		}
		
		// Center camera on nodes.
		Vector3 center = Vector3.scalarMult(0.5f, Vector3.add(minCoords, maxCoords));
		viewport.getCamera().moveBy(Vector3.subtract(center, viewport.getCamera().getTargetPosition()));
		
		// Move away far enough to have all nodes in front of the camera
		Vector3 cameraDir = viewport.getCamera().getDirection();
		float minDistance = Float.MAX_VALUE;
		for (ZZNodeView nodeView : fitNodes)
		{
			Vector3 centeredPos = Vector3.subtract(nodeView.getPosition(), center);
			float distance = Vector3.dot(centeredPos, cameraDir);
			minDistance = Math.min(minDistance, distance);
		}
		viewport.getCamera().setDistance(Math.max(-minDistance, 1.0f) * 10.0f);
		
		// Find out how nodes are fitting into the camera frustum
		Matrix4 view = viewport.getCamera().getViewMatrix();
		Matrix4 proj = viewport.getCamera().getProjectionMatrix();
		for (ZZNodeView nodeView : fitNodes)
		{
			Vector3[] corners = nodeView.getCorners(view);
			
			Vector4 screenLU = Vector4.matrixMult(proj, new Vector4(corners[0], 1.0f)).homogeneousToCartesian();
			Vector4 screenRB = Vector4.matrixMult(proj, new Vector4(corners[1], 1.0f)).homogeneousToCartesian();
			minScreen.x = Math.min(minScreen.x, screenLU.x);
			minScreen.y = Math.min(minScreen.y, screenLU.y);
			maxScreen.x = Math.max(maxScreen.x, screenRB.x);
			maxScreen.y = Math.max(maxScreen.y, screenRB.y);
		}
		float maxOffset = Math.max(Math.max(Math.max(Math.abs(minScreen.x), Math.abs(minScreen.y)), Math.abs(maxScreen.x)), Math.abs(maxScreen.y));
		viewport.getCamera().setDistance(viewport.getCamera().getDistance() * maxOffset);
		
		// Something went terribly wrong because of some NaNs upstream :-(
		if (!Float.isFinite(viewport.getCamera().getDistance()))
			viewport.getCamera().reset();
		
		this.updateView();
	}

	/**
	 * Forces the viewport to be redrawn.
	 */
	@Override
	public void updateView() 
	{
		if (viewport != null)
			viewport.redraw();
	}

	/**
	 * Positions the viewport's camera so that all selected nodes and edges are visible.
	 */
	@Override
	public void fitSelected() 
	{
		synchronized (m_sync)
		{
			// Obtain selected nodes
			List<ZZNodeView> selectedNodeViews = new ArrayList<ZZNodeView>(nodeViews.size());
			
			for (ZZNodeView view : nodeViews.values())
				if (view.getVisualProperty(BasicVisualLexicon.NODE_SELECTED))
					selectedNodeViews.add(view);
			
			if (selectedNodeViews.isEmpty())
				return;
			else
				fitContent(selectedNodeViews);
		}
	}
	
	/**
	 * Sets the network view's default value for the provided visual prop
	 * 
	 * @param visualProperty Visual property to be set
	 * @param defaultValue New default value
	 */
	@Override
	public <T, V extends T> void setViewDefault(VisualProperty<? extends T> visualProperty, V defaultValue) 
	{
		synchronized (m_sync)
		{
			if (defaultVault.getDefaultValue(visualProperty) == null || !defaultVault.getDefaultValue(visualProperty).equals(defaultValue))
			{
				defaultVisualPropertiesChanged = true;
				defaultVault.modifyDefaultValue(visualProperty, defaultValue);
			}
		}
	}

	/**
	 * Gets the network view's default value for the provided visual prop
	 * 
	 * @param vp Visual property to get the default value for
	 * @return Default value
	 */
	@Override
	protected <T, V extends T> V getDefaultValue(VisualProperty<T> vp) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Gets the network view's value for the provided visual prop
	 * 
	 * @param visualProperty Visual property to get the default value for
	 * @return Visual property value
	 */
	@Override
	public <T> T getVisualProperty(VisualProperty<T> visualProperty) 
	{
		T value = super.getVisualProperty(visualProperty);
		
		if (value != null)
			return value;
		else
			return defaultVault.getDefaultValue(visualProperty);
	}

	/**
	 * Sets the value for a visual property.
	 * 
	 * @param vp Visual property to be set
	 * @param value New value
	 */
	@Override
	protected <T, V extends T> void applyVisualProperty(VisualProperty<? extends T> vp, V value) 
	{
		// TODO Auto-generated method stub
		
	}

	/**
	 * Gets the renderer's name.
	 * 
	 * @return Renderer name
	 */
	@Override
	public String getRendererId() 
	{
		return ZZNetworkViewRenderer.ID;
	}

	/**
	 * Frees all resources associated with the network view
	 */
	@Override
	public void dispose() 
	{
		synchronized (m_sync)
		{
			registrar.unregisterAllServices(this);
			
			for (ZZNodeView view : nodeViews.values())
				view.dispose(gl);
			for (ZZEdgeView view : edgeViews.values())
				view.dispose(gl);
			
			managerNodeShapes.dispose();
			managerNodeLabels.dispose();
			managerEdgeLines.dispose();
		}
	}
	
	
	// **************************
	// RenderingEngine interface:
	// **************************
	
	/**
	 * Gets the network view model associated with this renderer.
	 * 
	 * @return Network view model
	 */
	@Override
	public View<CyNetwork> getViewModel() 
	{
		return this;
	}

	//TODO: Not implemented
	@Override
	public Properties getProperties() 
	{
		return null;
	}

	/**
	 * Gets a lexicon of visual props supported by this renderer.
	 * 
	 * @return Supported visual lexicon
	 */
	@Override
	public VisualLexicon getVisualLexicon() 
	{
		return visualLexicon;
	}
	
	/**
	 * Gets the VP dependencies of the current visual style, updated for each frame.
	 *  
	 * @return Set of visual property dependencies
	 */
	public Set<VisualPropertyDependency<?>> getVisualDependencies()
	{
		return visualDependencies;
	}

	//TODO: Not implemented
	@Override
	public Printable createPrintable() 
	{
		return null;
	}

	//TODO: Not implemented
	@Override
	public Image createImage(int width, int height) 
	{
		return null;
	}

	//TODO: Not implemented
	@Override
	public <V> Icon createIcon(VisualProperty<V> vp, V value, int width, int height) 
	{
		return null;
	}

	//TODO: Not implemented
	@Override
	public void printCanvas(java.awt.Graphics printCanvas) 
	{
		
	}
	
	
	// ********************************
	// ViewportEventListener interface:
	// ********************************

	/**
	 * Callback method invoked upon viewport and GL context initialization.
	 * Compiles default shaders, creates default node and edge textures,
	 * initializes rectangle and line managers.
	 * 
	 * @param drawable The viewport's GLJPanel being initialized
	 */
	@Override
	public void viewportInitialize(GLAutoDrawable drawable) 
	{	
		synchronized (m_sync)
		{
			try
			{
				gl = drawable.getGL().getGL4();
				
				System.out.println(gl.glGetString(GL4.GL_VERSION));
				if (gl.isExtensionAvailable("GL_ARB_bindless_texture"))
				{
					doBindless = true;
					System.out.println("Supports bindless textures.");
				}
				
				programs[0] = GLSLProgram.CompileProgram(gl, 
														 getClass().getResource("/glsl/Rectangle.vert"), 
														 null, null, 
														 getClass().getResource("/glsl/Rectangle.geom"), 
														 getClass().getResource("/glsl/Rectangle.frag"));
				
				programs[1] = GLSLProgram.CompileProgram(gl, 
														 getClass().getResource("/glsl/Line.vert"), 
														 null, null, 
														 getClass().getResource("/glsl/Line.geom"), 
														 getClass().getResource("/glsl/Line.frag"));
				
				/*gl.glGenBuffers(1, buffers, 0);
				gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, buffers[0]);
				{
					float[] vertices = new float[] {  0.5f,  0.5f, 0f, 1.0f, 
												      0.5f, -0.5f, 0f, 1.0f,
												     -0.5f,  0.5f, 0f, 1.0f};
					
					
					gl.glBufferStorage(GL4.GL_ARRAY_BUFFER, vertices.length * GLBuffers.SIZEOF_FLOAT, null, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT);
					ByteBuffer vertexBuffer = gl.glMapBufferRange(GL4.GL_ARRAY_BUFFER, 0, vertices.length * GLBuffers.SIZEOF_FLOAT, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT | GL4.GL_MAP_FLUSH_EXPLICIT_BIT);
					
					for (int i = 0; i < vertices.length; i++)
						vertexBuffer.putFloat(vertices[i]);
					vertexBuffer.rewind();
	
					gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, vertices.length * GLBuffers.SIZEOF_FLOAT);
				}
				gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
				
				gl.glGenVertexArrays(1, arrays, 0);
				gl.glBindVertexArray(arrays[0]);
				{
					gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, buffers[0]);
					{
						gl.glEnableVertexAttribArray(0);
						gl.glVertexAttribPointer(0, 4, GL4.GL_FLOAT, false, 4 * GLBuffers.SIZEOF_FLOAT, 0 * GLBuffers.SIZEOF_FLOAT);
					}
					gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
				}
				gl.glBindVertexArray(0);*/
				
				// Default texture:
				int blubbWidth = 16, blubbHeight = 16;
				byte[] defaultTextureData = new byte[blubbWidth * blubbHeight * 4];
				for (int y = 0; y < blubbHeight; y++)
					for (int x = 0; x < blubbWidth; x++)
					{
						byte val = (byte)(x % 2 == (y % 2 == 0 ? 0 : 1) ? 0 : -1);
						defaultTextureData[(y * blubbWidth + x) * 4 + 0] = 80;
						defaultTextureData[(y * blubbWidth + x) * 4 + 1] = 80;
						defaultTextureData[(y * blubbWidth + x) * 4 + 2] = 80;
						defaultTextureData[(y * blubbWidth + x) * 4 + 3] = -1;	// -1 = FF unsigned
					}
				shapeDefaultTex = new ZZBindlessTexture(gl, defaultTextureData, (short)blubbWidth, (short)16, GL4.GL_RGBA8, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE);
				labelDefaultTex = new ZZBindlessTexture(gl, new byte[4], (short)1, (short)1, GL4.GL_RGBA8, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE);
				
				managerNodeShapes = new ZZRectangleManager(gl, 10, shapeDefaultTex.getID(), (short)blubbWidth, (short)blubbHeight);
				managerNodeLabels = new ZZRectangleManager(gl, 10, labelDefaultTex.getID(), (short)1, (short)1);
				for (int i = 0; i < managersNodeCG.length; i++)
					managersNodeCG[i] = new ZZRectangleManager(gl, 10, labelDefaultTex.getID(), (short)1, (short)1);
				
				managerEdgeLines = new ZZLineManager(gl, 10, shapeDefaultTex.getID(), (short)blubbWidth, (short)blubbHeight);
			}
			catch (GLException exc)
			{
				System.out.println(exc.toString());
			}		
	
			syncWithModel();
		}
	}

	/**
	 * Callback method invoked when the viewport needs to be redrawn.
	 * This is the main rendering pipeline for all standard components.
	 * For more details on how it is structured, refer to ZZDrawingDaemon.
	 * 
	 * @param drawable The viewport's GLJPanel
	 */
	@Override
	public void viewportDisplay(GLAutoDrawable drawable) 
	{ 
		synchronized (m_sync)
		{
			gl = drawable.getGL().getGL4();
			
			visualDependencies = visualMappingManager.getVisualStyle(this).getAllVisualPropertyDependencies();
			
			Matrix4 viewMatrix = viewport.getCamera().getViewMatrix();
			Vector3 cameraPos = viewport.getCamera().getCameraPosition();
			Vector3 viewDirection = viewport.getCamera().getDirection();
			Vector2 halfScreen = new Vector2(viewport.getPanel().getWidth(), viewport.getPanel().getHeight());
			
			Queue<ZZDrawingDaemonPrimitive> forDrawUpdate = new LinkedBlockingQueue<>();
			
			// Determine optimal clipping range to make better use of the depth buffer:
			Vector2 optimalClip = new Vector2(Float.MAX_VALUE, 0.2f);
			int nodesInFront = 0;
			for (ZZNodeView view : nodeViews.values())
			{
				Vector3 toNode = Vector3.subtract(view.getPosition(), cameraPos);
				float distance = Vector3.dot(toNode, viewDirection);
				if (distance <= 0.1f)
					continue;
				optimalClip.x = Math.min(optimalClip.x, distance);
				optimalClip.y = Math.max(optimalClip.y, distance);
				nodesInFront++;
			}
			if (nodesInFront > 0)
			{
				optimalClip.y += 2.0f;
				optimalClip.x = Math.max(0.1f, optimalClip.x - 2.0f);
				viewport.getCamera().setClippingRange(optimalClip);
			}
			
			// Projection matrix with the updated clipping range:
			Matrix4 projMatrix = viewport.getCamera().getProjectionMatrix();
			
			for (ZZNodeView view : nodeViews.values())
			{
				ZZDrawingDaemonPrimitive primitive = (ZZDrawingDaemonPrimitive)view;
				if (primitive.updateState(defaultVisualPropertiesChanged, gl, viewMatrix, projMatrix, halfScreen))
				{
					forDrawUpdate.add(primitive);
					//System.out.println("Node wants to draw.");
				}
			}
			defaultVisualPropertiesChanged = false;	// Everyone is in sync now
			
			Queue<ZZDrawingDaemonPrimitive> forResourceUpdate = new LinkedBlockingQueue<>();
			
			for (ZZDrawingDaemonPrimitive primitive : forDrawUpdate)
				if (primitive.redrawTextures(viewMatrix, projMatrix))
				{
					forResourceUpdate.add(primitive);
					//System.out.println("Node wants to update resources.");
				}
			
			for (ZZDrawingDaemonPrimitive primitive : forResourceUpdate)
				primitive.updateResources(gl);
			
			managerNodeShapes.flush();
			managerNodeLabels.flush();
			for (int i = 0; i < managersNodeCG.length; i++)
				if (managersNodeCG[i].size() > 0)
					managersNodeCG[i].flush();			
			
			managerEdgeLines.flush();
			
			gl.glUseProgram(programs[1]);
			{
				gl.glUniformMatrix4fv(gl.glGetUniformLocation(programs[1], "viewMatrix"), 1, false, viewport.getCamera().getViewMatrix().asArrayCM(), 0);
				gl.glUniformMatrix4fv(gl.glGetUniformLocation(programs[1], "projMatrix"), 1, false, viewport.getCamera().getProjectionMatrix().asArrayCM(), 0);
				
				if (managerEdgeLines.size() > 0)		
				{
					managerEdgeLines.bind();
					gl.glLineWidth(10.0f);
					gl.glDrawArrays(GL4.GL_LINES, 0, managerEdgeLines.size() * 2);
					gl.glFinish();
				}
	
				gl.glBindVertexArray(0);
			}
			gl.glUseProgram(0);
			
			gl.glUseProgram(programs[0]);
			{
				gl.glUniformMatrix4fv(gl.glGetUniformLocation(programs[0], "viewMatrix"), 1, false, viewport.getCamera().getViewMatrix().asArrayCM(), 0);
				gl.glUniformMatrix4fv(gl.glGetUniformLocation(programs[0], "projMatrix"), 1, false, viewport.getCamera().getProjectionMatrix().asArrayCM(), 0);
				
				if (managerNodeShapes.size() > 0)
				{
					gl.glUniform1f(gl.glGetUniformLocation(programs[0], "depthOffset"), -0.5e-6f);
					
					managerNodeShapes.bind();
					gl.glDrawArrays(GL4.GL_POINTS, 0, managerNodeShapes.size());	
				}
				
				for (int i = 0; i < managersNodeCG.length; i++) 
				{
					if (managersNodeCG[i].size() > 0)
					{
						// CG go on top of node shapes
						gl.glUniform1f(gl.glGetUniformLocation(programs[0], "depthOffset"), -1.0e-6f - (float)i * 1.0e-6f);
						
						managersNodeCG[i].bind();
						gl.glDrawArrays(GL4.GL_POINTS, 0, managersNodeCG[i].size());
					}
				}
				
				if (managerNodeLabels.size() > 0)
				{
					// Labels go on top of everything
					gl.glUniform1f(gl.glGetUniformLocation(programs[0], "depthOffset"), -1.0e-6f - (float)ZZVisualLexicon.numCustomGraphics * 1.0e-6f);
				
					managerNodeLabels.bind();
					gl.glDrawArrays(GL4.GL_POINTS, 0, managerNodeLabels.size());
				}
				gl.glBindVertexArray(0);
			}
			gl.glUseProgram(0);
			
			gl.glFinish();
			
			float memoryConsumption = (float)GLMemoryLimit.getCurrentMemory() / (float)GLMemoryLimit.getMaxMemory();
			float newDownsampling = globalDownsampling;
			if (memoryConsumption > 0.9f)
				newDownsampling = globalDownsampling * 0.8f;
			else if (globalDownsampling < 1.0f && memoryConsumption < 0.88f * 0.8f * 0.8f)
				newDownsampling = globalDownsampling / 0.8f;
			newDownsampling = Math.min(1.0f, newDownsampling);
			if (newDownsampling != globalDownsampling)
			{
				globalDownsampling = newDownsampling;
				System.out.println(newDownsampling);
				//eventHelper.fireEvent(new UpdateNetworkPresentationEvent(this));
			}
		}
	}

	/**
	 * Callback method invoked when the viewport is resized.
	 * 
	 * @param drawable The viewport's GLJPanel
	 * @param e Information about the viewport's new shape
	 */
	@Override
	public void viewportReshape(GLAutoDrawable drawable, ViewportResizedEvent e) { }

	/**
	 * Callback method invoked when the viewport is being disposed.
	 * 
	 * @param drawable The viewport's GLJPanel
	 */
	@Override
	public void viewportDispose(GLAutoDrawable drawable) { }

	
	// *************************************
	// ViewportMouseEventListener interface:
	// *************************************
	
	@Override
	public void viewportMouseDown(ViewportMouseEvent e) { }


	@Override
	public void viewportMouseUp(ViewportMouseEvent e) { }


	@Override
	public void viewportMouseClick(ViewportMouseEvent e) { }


	@Override
	public void viewportMouseMove(ViewportMouseEvent e) { }


	@Override
	public void viewportMouseDrag(ViewportMouseEvent e) { }


	@Override
	public void viewportMouseEnter(ViewportMouseEvent e) { }


	@Override
	public void viewportMouseLeave(ViewportMouseEvent e) { }


	@Override
	public void viewportMouseScroll(ViewportMouseEvent e) { }
	
	
	// **************************
	// RowsSetListener interface:
	// **************************

	/**
	 * Listens to changes in the SELECTED property column of nodes and edges,
	 * and alters their internal selection state accordingly.
	 * 
	 * @param e Row set event
	 */
	@Override
	public void handleEvent(RowsSetEvent e) 
	{
		if (!e.containsColumn(CyNetwork.SELECTED))
			return;

		nodeSelectionList.clear();
		edgeSelectionList.clear();
		if (e.getSource() == getModel().getDefaultNodeTable()) 
		{
			for (RowSetRecord record: e.getColumnRecords(CyNetwork.SELECTED)) 
			{
				// Get the SUID
				Long suid = record.getRow().get(CyNetwork.SUID, Long.class);
				CyNode node = getModel().getNode(suid);
				if (node == null) 
					continue;
				
				ZZNodeView nv = (ZZNodeView)getNodeView(node);
				if (nv == null) 
					continue;
				
				boolean value = record.getRow().get(CyNetwork.SELECTED, Boolean.class);
				/*if (value)
					nv.selectInternal();
				else
					nv.unselectInternal();*/

				nodeSelectionList.add(node);
			}
		} 
		else if (e.getSource() == getModel().getDefaultEdgeTable()) 
		{
			for (RowSetRecord record: e.getColumnRecords(CyNetwork.SELECTED))
			{
				Long suid = record.getRow().get(CyNetwork.SUID, Long.class);
				CyEdge edge = getModel().getEdge(suid);
				if (edge == null) 
					continue;

				ZZEdgeView ev = (ZZEdgeView)getEdgeView(edge);
				if (ev == null) 
					continue;
				
				boolean value = record.getRow().get(CyNetwork.SELECTED, Boolean.class);
				/*if (value)
					ev.selectInternal(false);
				else
					ev.unselectInternal();*/

				edgeSelectionList.add(edge);
			}
		} 
		else 
		{
			return;
		}

		if (nodeSelectionList.size() > 0 || edgeSelectionList.size() > 0) 
		{
			// Update renderings
		}
	}

	
	// *************************************
	// AboutToRemoveNodesListener interface:
	// *************************************
	
	/**
	 * Callback method invoked when node data models 
	 * are about to be removed from the network.
	 * 
	 * @param e Information about the deleted nodes
	 */
	@Override
	public void handleEvent(AboutToRemoveNodesEvent e) 
	{
		if (network != e.getSource())
			return;

		List<View<CyNode>> nvs = new ArrayList<>(e.getNodes().size());
		for (CyNode n : e.getNodes()) 
		{
			View<CyNode> v = this.getNodeView(n);
			if (v != null)
				nvs.add(v);
		}
		
		if (nvs.size() == 0)
			return;

		for (CyNode n : e.getNodes()) 
			this.removeNodeView(n);
	}
	
	
	// *************************************
	// AboutToRemoveEdgesListener interface:
	// *************************************

	/**
	 * Callback method invoked when edge data models
	 * are about to be removed from the network.
	 * 
	 * @param e Information about the deleted edges
	 */
	@Override
	public void handleEvent(AboutToRemoveEdgesEvent e) 
	{
		if (network != e.getSource())
			return;

		List<View<CyEdge>> evs = new ArrayList<>(e.getEdges().size());
		for (CyEdge edge : e.getEdges()) 
		{
			View<CyEdge> v = getEdgeView(edge);
			if ( v != null)
				evs.add(v);
		}

		if (evs.size() == 0)
			return;

		for (CyEdge edge : e.getEdges())
			this.removeEdgeView(edge);
	}
	
	
	// *****************************
	// AddedNodesListener interface:
	// *****************************

	/**
	 * Callback method invoked when node data models
	 * are added to the network.
	 * 
	 * @param e Information about the added nodes
	 */
	@Override
	public void handleEvent(AddedNodesEvent e) 
	{
		// Respond to the event only if the source is equal to the network model
		// associated with this view.
		if (network != e.getSource())
			return;

		for (CyNode node : e.getPayloadCollection())
			this.addNodeView(node);
	}
	
	
	// *****************************
	// AddedEdgesListener interface:
	// *****************************

	/**
	 * Callback method invoked when edge data models
	 * are added to the network.
	 * 
	 * @param e Information about the added edges
	 */
	@Override
	public void handleEvent(AddedEdgesEvent e) 
	{
		if (network != e.getSource())
			return;

		for ( CyEdge edge : e.getPayloadCollection())
			addEdgeView(edge);
	}
	
	
	// ***********************************************
	// VisualMappingFunctionChangedListener interface:
	// ***********************************************

	/**
	 * Callback method invoked when a visual mapping function
	 * has been changed, causing a different interpretation of
	 * values, thus possibly changing the visual appearance 
	 * of nodes/edges.
	 * 
	 * @param e Information about the changed mapping function 
	 */
	@Override
	public void handleEvent(VisualMappingFunctionChangedEvent e) 
	{
	}
	
	
	// *************************************
	// VisualStyleChangedListener interface:
	// *************************************
	
	@Override
	public void handleEvent(VisualStyleChangedEvent e) 
	{
		
	}

	@Override
	protected ZZNetworkView getZZNetworkView() 
	{
		return this;
	}
}
