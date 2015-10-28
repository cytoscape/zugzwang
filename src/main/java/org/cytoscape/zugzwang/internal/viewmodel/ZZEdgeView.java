package org.cytoscape.zugzwang.internal.viewmodel;

import java.awt.Color;
import java.awt.Paint;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.zugzwang.internal.algebra.*;
import org.cytoscape.zugzwang.internal.rendering.*;
import org.cytoscape.zugzwang.internal.tools.GLMemoryLimit;
import org.cytoscape.zugzwang.internal.viewport.*;

import com.jogamp.opengl.GL4;

/**
 * Provides the visual representation of a CyEdge,
 * guides its interaction with user input,
 * draws edge textures on the CPU,
 * manages OpenGL resources needed to render it,
 * and performs the rendering on the GPU.
 */
public class ZZEdgeView extends ZZView<CyEdge> implements Pickable, ZZDrawingDaemonPrimitive
{
	// Thread synchronization
	private final Object m_sync = new Object();	
	
	private final DefaultValueVault defaultVault;	// Central vault for default visual property values	
	private final ZZNetworkView networkView;		// Parent network view this edge view is associated with
	private final CyEdge edge;						// Underlying data model of the edge
	
	private final ZZLineManager managerLine;		// Central line object manager used by all ZZEdgeViews 
	
	private ZZLine lineLine;						// The line drawing primitive that represents the edge line
	
	// State and update flags
	private boolean isOnScreen = true;
	private boolean needsShapeRedraw = false, needsLabelRedraw = false;
	
	// Store on-screen texture size at current position
	private short optimumShapeWidth = 1, optimumShapeHeight = 1;
	private short optimumLabelWidth = 1, optimumLabelHeight = 1;
	
	// VizProps describing the current state
	private boolean localVisible;
	private boolean localSelected;
	private short localWidth;
	
	// Internal copies of the position-related visual props allow for faster access
	private Vector3 positionSource = new Vector3(), positionTarget = new Vector3();
	
	public ZZEdgeView(ZZNetworkView networkView, 
					  DefaultValueVault defaultVault, 
					  VisualLexicon lexicon, 
					  CyEventHelper eventHelper, 
					  CyEdge edge, 
					  ZZLineManager managerLine) 
	{
		super(lexicon, eventHelper);
		this.networkView = networkView;
		this.defaultVault = defaultVault;
		this.edge = edge;
		
		this.managerLine = managerLine;
		
		syncProperties();
		setupRectangles();
	}
	
	/**
	 * Updates internal visual prop copies with the values managed by Cytoscape.
	 */
	private void syncProperties()
	{
		localVisible = ((Boolean)getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE)).booleanValue();
		localSelected = ((Boolean)getVisualProperty(BasicVisualLexicon.EDGE_SELECTED)).booleanValue();
		
		localWidth = ((Number)getVisualProperty(BasicVisualLexicon.EDGE_WIDTH)).shortValue();
	}
	
	/**
	 * Initializes lines and rectangles using the respective central manager objects.
	 */
	private void setupRectangles()
	{
		if (localVisible)
		{
			if (lineLine == null)
			{
				lineLine = managerLine.createLine(positionSource, positionTarget, localWidth);
				needsShapeRedraw = true;
				//System.out.println("Created line.");
			}
		}
		else if (lineLine != null)
		{
			managerLine.deleteLine(lineLine);
			lineLine = null;
			//System.out.println("Deleted line.");
		}
	}
	
	/**
	 * Gets the underlying CyEdge data model.
	 * 
	 * @return CyEdge data model
	 */
	@Override
	public CyEdge getModel() 
	{
		return edge;
	}

	/**
	 * Gets the network view this edge view is associated with.
	 * 
	 * @return Associated network view
	 */
	@Override
	protected ZZNetworkView getZZNetworkView() 
	{
		return networkView;
	}

	/**
	 * Gets the value that this view associates with the given visual property.
	 * 
	 * @param visualProperty Visual property to obtain the value for
	 * @return Value associated with the visual property
	 */
	@Override
	public <T> T getVisualProperty(VisualProperty<T> visualProperty) 
	{
		T value = super.getVisualProperty(visualProperty);
				
		return value;
	}

	/**
	 * Gets the default value for the given visual property.
	 * 
	 * @param visualProperty Visual property to get the default value for
	 * @return Default value
	 */
	@Override
	protected <T, V extends T> V getDefaultValue(VisualProperty<T> visualProperty) 
	{
		return (V)defaultVault.getDefaultValue(visualProperty);
	}

	/**
	 * Sets the value this view associates with the given visual property.
	 * 
	 * @param vp Visual property to set the value for
	 * @param value New visual property value
	 */
	@Override
	protected <T, V extends T> void applyVisualProperty(VisualProperty<? extends T> vp, V value) 
	{
		if (value == null)
			value = (V)vp.getDefault();

		if (vp == BasicVisualLexicon.EDGE_SELECTED) 
		{
			//setSelected((Boolean) value);
		} 
		else if (vp == BasicVisualLexicon.NODE_VISIBLE) 
		{
			setVisible(((Boolean)value).booleanValue());
		} 
		else if (vp == BasicVisualLexicon.EDGE_WIDTH) 
		{
			setWidth(((Number)value).shortValue());
		}
	}
	
	/**
	 * Sets the view's visibility.
	 * 
	 * @param value New visibility value
	 */
	private void setVisible(boolean value)
	{
		synchronized (m_sync)
		{
			if (localVisible == value)
				return;
			localVisible = value;
			
			setupRectangles();
		}
	}

	/**
	 * Sets the X coordinate for the line's source.
	 * 
	 * @param value New X coordinate
	 */
	public void setSourceX(float value) 
	{
		synchronized (m_sync)
		{
			if (positionSource.x == value)
				return;
			positionSource.x = value;
			
			if (lineLine != null)
				lineLine.setSource(positionSource);
		}
	}

	/**
	 * Sets the Y coordinate for the line's source.
	 * 
	 * @param value New Y coordinate
	 */
	public void setSourceY(float value) 
	{
		synchronized (m_sync)
		{
			if (positionSource.y == value)
				return;
			positionSource.y = value;
			
			if (lineLine != null)
				lineLine.setSource(positionSource);
		}
	}

	/**
	 * Sets the Z coordinate for the line's source.
	 * 
	 * @param value New Z coordinate
	 */
	public void setSourceZ(float value) 
	{
		synchronized (m_sync)
		{
			if (positionSource.z == value)
				return;
			positionSource.z = value;
			
			if (lineLine != null)
				lineLine.setSource(positionSource);
		}
	}
	
	public void setSourcePosition(Vector3 value)
	{
		synchronized (m_sync)
		{
			if (positionSource.equals(value))
				return;
			positionSource = value;
			
			if (lineLine != null)
				lineLine.setSource(positionSource);
		}
	}

	/**
	 * Sets the X coordinate for the line's target.
	 * 
	 * @param value New X coordinate
	 */
	public void setTargetX(float value) 
	{
		synchronized (m_sync)
		{
			if (positionTarget.x == value)
				return;
			positionTarget.x = value;
			
			if (lineLine != null)
				lineLine.setTarget(positionTarget);
		}
	}

	/**
	 * Sets the Y coordinate for the line's target.
	 * 
	 * @param value New Y coordinate
	 */
	public void setTargetY(float value) 
	{
		synchronized (m_sync)
		{
			if (positionTarget.y == value)
				return;
			positionTarget.y = value;
			
			if (lineLine != null)
				lineLine.setTarget(positionTarget);
		}
	}

	/**
	 * Sets the Z coordinate for the line's target.
	 * 
	 * @param value New Z coordinate
	 */
	public void setTargetZ(float value) 
	{
		synchronized (m_sync)
		{
			if (positionTarget.z == value)
				return;
			positionTarget.z = value;
			
			if (lineLine != null)
				lineLine.setTarget(positionTarget);
		}
	}
	
	public void setTargetPosition(Vector3 value)
	{
		synchronized (m_sync)
		{
			if (positionTarget.equals(value))
				return;
			positionTarget = value;
			
			if (lineLine != null)
				lineLine.setTarget(positionTarget);
		}
	}

	/**
	 * Sets the line's width.
	 * 
	 * @param value New width
	 */
	private void setWidth(short value) 
	{
		synchronized (m_sync)
		{
			if (localWidth == value)
				return;
			localWidth = value;
			
			if (lineLine != null)
				lineLine.setWidth(localWidth);
		}
	}

	/**
	 * Part of the Pickable interface; performs an intersection with the line.
	 * 
	 * @param ray Ray to intersect with
	 * @param viewMatrix Current view matrix
	 * @return PickingResult object if an intersection is found, null otherwise
	 */
	@Override
	public PickingResult intersectsWith(Ray3 ray, Matrix4 viewMatrix) 
	{
		return null;
	}	
	
	/**
	 * Frees associated device resources.
	 */
	public void dispose(GL4 gl)
	{
		if (lineLine != null && lineLine.isOnDevice())
		{
			GLMemoryLimit.freeMemory(lineLine.getOccupiedTextureMemory());
			lineLine.discardOnDevice(gl);
		}
	}
	

	//************************************
	// ZZDrawingDaemonPrimitive interface:
	//************************************
	
	@Override
	public boolean updateState(boolean updateVisualProperties, GL4 gl, Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen) 
	{
		synchronized (m_sync)
		{
			return false;
		}
	}

	@Override
	public boolean redrawTextures(Matrix4 viewMatrix, Matrix4 projMatrix)
	{
		synchronized (m_sync)
		{			
			return false;
		}
	}

	@Override
	public void updateResources(GL4 gl) 
	{
	}
}
