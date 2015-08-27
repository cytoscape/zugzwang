package org.cytoscape.zugzwang.internal.cytoscape.view;

import java.awt.Color;
import java.awt.Paint;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.zugzwang.internal.algebra.*;
import org.cytoscape.zugzwang.internal.rendering.*;
import org.cytoscape.zugzwang.internal.viewport.*;

import com.jogamp.opengl.GL4;

public class ZZEdgeView extends ZZView<CyEdge> implements Pickable, ZZDrawingDaemonPrimitive
{
	private final Object m_sync = new Object();
	
	private final ZZNetworkView networkView;
	private final DefaultValueVault defaultVault;
	private final CyEdge edge;
	
	private final ZZLineManager managerLine;
	
	private ZZLine lineLine;
	
	private boolean isOnScreen = true;
	private boolean needsShapeRedraw = false, needsLabelRedraw = false;
	
	// Store on-screen texture size at current position
	private short optimumShapeWidth = 1, optimumShapeHeight = 1;
	private short optimumLabelWidth = 1, optimumLabelHeight = 1;
	
	// VizProps describing the current state
	private boolean localVisible;
	private boolean localSelected;
	private short localWidth;
	
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
	
	private void syncProperties()
	{
		localVisible = ((Boolean)getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE)).booleanValue();
		localSelected = ((Boolean)getVisualProperty(BasicVisualLexicon.EDGE_SELECTED)).booleanValue();
		
		localWidth = ((Number)getVisualProperty(BasicVisualLexicon.EDGE_WIDTH)).shortValue();
	}
	
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
	
	@Override
	public CyEdge getModel() 
	{
		return edge;
	}

	@Override
	protected ZZNetworkView getZZNetworkView() 
	{
		return networkView;
	}

	@Override
	public <T> T getVisualProperty(VisualProperty<T> visualProperty) 
	{
		T value = super.getVisualProperty(visualProperty);
				
		return value;
	}

	@Override
	protected <T, V extends T> V getDefaultValue(VisualProperty<T> visualProperty) 
	{
		return (V)defaultVault.getDefaultValue(visualProperty);
	}

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

	@Override
	public PickingResult intersectsWith(Ray3 ray, Matrix4 viewMatrix) 
	{
		return null;
	}	
	
	public void dispose()
	{
		
	}
	

	//************************************
	// ZZDrawingDaemonPrimitive interface:
	//************************************
	
	@Override
	public boolean updateState(GL4 gl, Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen) 
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
