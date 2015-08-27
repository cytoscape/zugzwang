package org.cytoscape.zugzwang.internal.cytoscape.view;

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
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.AboutToRemoveEdgesEvent;
import org.cytoscape.model.events.AboutToRemoveEdgesListener;
import org.cytoscape.model.events.AboutToRemoveNodesEvent;
import org.cytoscape.model.events.AboutToRemoveNodesListener;
import org.cytoscape.model.events.AddedEdgesEvent;
import org.cytoscape.model.events.AddedEdgesListener;
import org.cytoscape.model.events.AddedNodesEvent;
import org.cytoscape.model.events.AddedNodesListener;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.AboutToRemoveEdgeViewsEvent;
import org.cytoscape.view.model.events.AboutToRemoveNodeViewsEvent;
import org.cytoscape.view.model.events.AddedEdgeViewsEvent;
import org.cytoscape.view.model.events.AddedNodeViewsEvent;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.events.VisualMappingFunctionChangedEvent;
import org.cytoscape.view.vizmap.events.VisualMappingFunctionChangedListener;
import org.cytoscape.view.vizmap.events.VisualStyleChangedEvent;
import org.cytoscape.view.vizmap.events.VisualStyleChangedListener;
import org.cytoscape.zugzwang.internal.ZZNetworkViewRenderer;
import org.cytoscape.zugzwang.internal.algebra.*;
import org.cytoscape.zugzwang.internal.rendering.*;
import org.cytoscape.zugzwang.internal.tools.*;
import org.cytoscape.zugzwang.internal.viewport.*;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.GLBuffers;

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
	Object m_lock = new Object();
	boolean m_needsUpdate = false;
	
	CyEventHelper eventHelper;
	CyServiceRegistrar registrar;
	
	GL4 gl;
	boolean doBindless = false;
	private Viewport viewport;
	
	private final CyNetwork network;
	
	private final DefaultValueVault defaultVault;
	private final VisualLexicon visualLexicon;
	private final VisualMappingManager visualMappingManager;
	
	private Map<CyNode, View<CyNode>> nodeViews = new HashMap<>();
	private Map<CyEdge, View<CyEdge>> edgeViews = new HashMap<>();

	private List<CyNode> nodeSelectionList;
	private List<CyEdge> edgeSelectionList;
	
	private ZZRectangleManager managerNodeShapes, managerNodeLabels;
	private ZZLineManager managerEdgeLines;
	
	// Testing:	
	int[] programs = new int[2];
	ZZRectangle rectTest;
	ZZBindlessTexture rectDefaultTex;
	
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
	
	public void putIntoContainer(JComponent component)
	{		
		this.viewport = new Viewport(component);
		viewport.addViewportEventListener(this);
	}
	
	private void syncWithModel()
	{
		synchronized (m_lock)
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
	
	private void addNodeView(CyNode node)
	{
		if (nodeViews.containsKey(node))
			return;
		
		ZZNodeView nodeView = new ZZNodeView(this, defaultVault, visualLexicon, eventHelper, node, managerNodeShapes, managerNodeLabels);
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
		m_needsUpdate = true;
	}
	
	private void removeNodeView(CyNode node)
	{
		View<CyNode> nodeView = nodeViews.get(node);
		if (nodeView == null)
			return;
		
		((ZZNodeView)nodeView).dispose();
		nodeViews.remove(node);

		eventHelper.addEventPayload((CyNetworkView)this, nodeView, AboutToRemoveNodeViewsEvent.class);
		m_needsUpdate = true;
	}
	
	private void addEdgeView(CyEdge edge)
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
		m_needsUpdate = true;
	}
	
	private void removeEdgeView(CyEdge edge)
	{
		ZZEdgeView edgeView = (ZZEdgeView)edgeViews.get(edge);
		if (edgeView == null)
			return;
		
		ZZNodeView sourceView = (ZZNodeView)nodeViews.get(edge.getSource());
		if (sourceView != null)
			sourceView.removeOutgoingEdgeView(edgeView);
		
		ZZNodeView targetView = (ZZNodeView)nodeViews.get(edge.getTarget());
		if (targetView != null)
			targetView.removeIncomingEdgeView(edgeView);
		
		edgeView.dispose();
		nodeViews.remove(edge);

		eventHelper.addEventPayload((CyNetworkView)this, (View<CyEdge>)edgeView, AboutToRemoveEdgeViewsEvent.class);
		m_needsUpdate = true;
	}
	
	// ************************
	// CyNetworkView interface:
	// ************************
	
	@Override
	public CyNetwork getModel() 
	{
		return network;
	}

	@Override
	public View<CyNode> getNodeView(CyNode node) 
	{
		return nodeViews.get(node.getSUID());
	}

	@Override
	public Collection<View<CyNode>> getNodeViews() 
	{
		return nodeViews.values();
	}

	@Override
	public View<CyEdge> getEdgeView(CyEdge edge) 
	{
		return edgeViews.get(edge.getSUID());
	}

	@Override
	public Collection<View<CyEdge>> getEdgeViews() 
	{
		return edgeViews.values();
	}

	@Override
	public Collection<View<? extends CyIdentifiable>> getAllViews() 
	{
		Collection<View<? extends CyIdentifiable>> views = new HashSet<>();
		
		synchronized (m_lock)
		{
			views.addAll(getNodeViews());
			views.addAll(getEdgeViews());
			views.add(this);
		}
		
		return views;
	}

	@Override
	public void fitContent() 
	{
		//fitNodesInView();
		updateView();
	}

	@Override
	public void updateView() 
	{
		
	}

	//TODO: Not implemented
	@Override
	public void fitSelected() 
	{
		// Obtain selected nodes
		Set<View<CyNode>> selectedNodeViews = new HashSet<View<CyNode>>();
		
		for (View<CyNode> nodeView : getNodeViews())
			if (nodeView.getVisualProperty(BasicVisualLexicon.NODE_SELECTED))
				selectedNodeViews.add(nodeView);
		
		if (selectedNodeViews.isEmpty())
			return;
	}
	
	@Override
	public <T, V extends T> void setViewDefault(VisualProperty<? extends T> visualProperty, V defaultValue) 
	{
		defaultVault.modifyDefaultValue(visualProperty, defaultValue);
	}
	
	@Override
	public <T> T getVisualProperty(VisualProperty<T> visualProperty) 
	{
		T value = super.getVisualProperty(visualProperty);
		
		if (value != null)
			return value;
		else
			return defaultVault.getDefaultValue(visualProperty);
	}

	@Override
	public String getRendererId() 
	{
		return ZZNetworkViewRenderer.ID;
	}

	@Override
	public void dispose() 
	{
		registrar.unregisterAllServices(this);
	}
	
	
	// **************************
	// RenderingEngine interface:
	// **************************
		
	@Override
	public View<CyNetwork> getViewModel() 
	{
		return this;
	}

	@Override
	public Properties getProperties() 
	{
		return null;
	}

	@Override
	public VisualLexicon getVisualLexicon() 
	{
		return null;
	}
	
	@Override
	public Printable createPrintable() 
	{
		return null;
	}

	@Override
	public Image createImage(int width, int height) 
	{
		return null;
	}

	@Override
	public <V> Icon createIcon(VisualProperty<V> vp, V value, int width, int height) 
	{
		return null;
	}

	@Override
	public void printCanvas(java.awt.Graphics printCanvas) 
	{
		
	}
	
	
	// ********************************
	// ViewportEventListener interface:
	// ********************************

	@Override
	public void viewportInitialize(GLAutoDrawable drawable) 
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
													 null, 
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
			
			int blubbWidth = 16, blubbHeight = 16;
			byte[] defaultTextureData = new byte[blubbWidth * blubbHeight * 4];
			for (int y = 0; y < blubbHeight; y++)
				for (int x = 0; x < blubbWidth; x++)
				{
					byte val = (byte)(x % 2 == (y % 2 == 0 ? 0 : 1) ? 0 : -1);
					defaultTextureData[(y * blubbWidth + x) * 4 + 0] = 80;
					defaultTextureData[(y * blubbWidth + x) * 4 + 1] = 80;
					defaultTextureData[(y * blubbWidth + x) * 4 + 2] = 80;
					defaultTextureData[(y * blubbWidth + x) * 4 + 3] = -1;	// -1 = FF bitwise
				}
			rectDefaultTex = new ZZBindlessTexture(gl, defaultTextureData, (short)blubbWidth, (short)16, GL4.GL_RGBA8, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE);
			
			managerNodeShapes = new ZZRectangleManager(gl, 10, rectDefaultTex.getID(), (short)blubbWidth, (short)blubbHeight);
			managerNodeLabels = new ZZRectangleManager(gl, 10, rectDefaultTex.getID(), (short)blubbWidth, (short)blubbHeight);
			
			managerEdgeLines = new ZZLineManager(gl, 10, rectDefaultTex.getID(), (short)blubbWidth, (short)blubbHeight);
		}
		catch (GLException exc)
		{
			System.out.println(exc.toString());
		}		

		syncWithModel();
	}

	@Override
	public void viewportDisplay(GLAutoDrawable drawable) 
	{ 
		gl = drawable.getGL().getGL4();
		
		Matrix4 viewMatrix = viewport.getCamera().getViewMatrix();
		Matrix4 projMatrix = viewport.getCamera().getProjectionMatrix();
		Vector2 halfScreen = new Vector2(viewport.getPanel().getWidth(), viewport.getPanel().getHeight());
		
		Queue<ZZDrawingDaemonPrimitive> forDrawUpdate = new LinkedBlockingQueue<>();
		
		for (View<CyNode> view : nodeViews.values())
		{
			ZZDrawingDaemonPrimitive primitive = (ZZDrawingDaemonPrimitive)view;
			if (primitive.updateState(gl, viewMatrix, projMatrix, halfScreen))
			{
				forDrawUpdate.add(primitive);
				//System.out.println("Node wants to draw.");
			}
		}
		
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
		
		managerEdgeLines.flush();
		
		gl.glUseProgram(programs[1]);
		{
			gl.glUniformMatrix4fv(0, 1, false, viewport.getCamera().getViewProjectionMatrix().AsArrayCM(), 0);
			
			if (managerEdgeLines.size() > 0)		
			{
				managerEdgeLines.bind(programs[0]);
				gl.glLineWidth(2.0f);
				gl.glDrawArrays(GL4.GL_LINES, 0, managerEdgeLines.size() * 2);
			}

			gl.glBindVertexArray(0);
		}
		gl.glUseProgram(0);
		
		gl.glUseProgram(programs[0]);
		{
			gl.glUniformMatrix4fv(0, 1, false, viewport.getCamera().getViewMatrix().AsArrayCM(), 0);
			gl.glUniformMatrix4fv(1, 1, false, viewport.getCamera().getProjectionMatrix().AsArrayCM(), 0);
			
			if (managerNodeShapes.size() > 0)
			{
				managerNodeShapes.bind(programs[0]);
				gl.glDrawArrays(GL4.GL_POINTS, 0, managerNodeShapes.size());				
			}
			
			if (managerNodeLabels.size() > 0)		
			{
				managerNodeLabels.bind(programs[0]);
				gl.glDrawArrays(GL4.GL_POINTS, 0, managerNodeLabels.size());
			}

			gl.glBindVertexArray(0);
		}
		gl.glUseProgram(0);
		
		gl.glFinish();
	}

	@Override
	public void viewportReshape(GLAutoDrawable drawable, ViewportResizedEvent e) { }

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

	@Override
	protected <T, V extends T> void applyVisualProperty(VisualProperty<? extends T> vp, V value) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected <T, V extends T> V getDefaultValue(VisualProperty<T> vp) 
	{
		// TODO Auto-generated method stub
		return null;
	}
}
