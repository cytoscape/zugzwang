package org.cytoscape.zugzwang.internal.cytoscape.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.zugzwang.internal.algebra.*;
import org.cytoscape.zugzwang.internal.rendering.*;
import org.cytoscape.zugzwang.internal.tools.GLMemoryLimit;
import org.cytoscape.zugzwang.internal.viewport.*;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.math.FloatUtil;

public class ZZNodeView extends ZZView<CyNode> implements Pickable, ZZDrawingDaemonPrimitive
{
	//**********************************
	// Default values, copied from Ding:
	//**********************************
	
	// Default size of node.
	static final float DEFAULT_WIDTH = 30.0f;
	static final float DEFAULT_HEIGHT = 30.0f;

	// Default border color
	static final Paint DEFAULT_BORDER_PAINT = Color.DARK_GRAY;
	static final Paint DEFAULT_NODE_PAINT = Color.BLUE;
	static final Paint DEFAULT_NODE_SELECTED_PAINT = Color.YELLOW;

	static final String DEFAULT_LABEL_TEXT = "";
	static final Font DEFAULT_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
	static final Paint DEFAULT_LABEL_PAINT = Color.DARK_GRAY;
	static final double DEFAULT_LABEL_WIDTH = 100.0;

	// Default opacity
	static final int DEFAULT_TRANSPARENCY = 255;
	
	private final Object m_sync = new Object();
	
	private final ZZNetworkView networkView;
	private final DefaultValueVault defaultVault;
	private final CyNode node;

	private final List<ZZEdgeView> edgesOutgoing = new ArrayList<>();
	private final List<ZZEdgeView> edgesIncoming = new ArrayList<>(); 
	
	private final ZZRectangleManager managerShape, managerLabel;
	
	private ZZRectangle rectShape;
	private ZZRectangle rectLabel;
	
	private boolean isOnScreen = true;
	private boolean needsShapeRedraw = false, needsLabelRedraw = false;
	
	// Store on-screen texture size at current position
	private short optimumShapeWidth = 1, optimumShapeHeight = 1;
	private short optimumLabelWidth = 1, optimumLabelHeight = 1;
	
	// VizProps describing the current state
	private boolean localVisible;
	private boolean localSelected;
	private Vector3 localPosition;
	private short localWidth;
	private short localHeight;
	private Paint localFillColor;
	private Paint localSelectedFillColor;
	private String localLabel;
	private Color localLabelTextColor;
	
	public ZZNodeView(ZZNetworkView networkView, 
					  DefaultValueVault defaultVault, 
					  VisualLexicon lexicon, 
					  CyEventHelper eventHelper, 
					  CyNode node, 
					  ZZRectangleManager managerShape, 
					  ZZRectangleManager managerLabel) 
	{
		super(lexicon, eventHelper);
		this.networkView = networkView;
		this.defaultVault = defaultVault;
		this.node = node;
		
		this.managerShape = managerShape;
		this.managerLabel = managerLabel;
		
		syncProperties();
		setupRectangles();
	}
	
	public void addOutgoingEdgeView(ZZEdgeView view)
	{
		CyEdge addedEdge = view.getModel();
		for (ZZEdgeView existing : edgesOutgoing)
			if (existing.getModel().getSUID() == addedEdge.getSUID())
				return;
		
		edgesOutgoing.add(view);
	}
	
	public void addIncomingEdgeView(ZZEdgeView view)
	{
		CyEdge addedEdge = view.getModel();
		for (ZZEdgeView existing : edgesIncoming)
			if (existing.getModel().getSUID() == addedEdge.getSUID())
				return;
		
		edgesIncoming.add(view);
	}
	
	public void removeOutgoingEdgeView(ZZEdgeView view)
	{
		edgesOutgoing.remove(view);
	}
	
	public void removeIncomingEdgeView(ZZEdgeView view)
	{
		edgesIncoming.remove(view);
	}
	
	private void syncProperties()
	{
		localVisible = ((Boolean)getVisualProperty(BasicVisualLexicon.NODE_VISIBLE)).booleanValue();
		localSelected = ((Boolean)getVisualProperty(BasicVisualLexicon.NODE_SELECTED)).booleanValue();
		
		localPosition = new Vector3(((Number)getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION)).floatValue(),
									((Number)getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION)).floatValue(),
									((Number)getVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION)).floatValue());
		
		localWidth = ((Number)getVisualProperty(BasicVisualLexicon.NODE_WIDTH)).shortValue();
		localHeight = ((Number)getVisualProperty(BasicVisualLexicon.NODE_HEIGHT)).shortValue();

		localFillColor = (Paint)getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR);
		localSelectedFillColor = (Paint)getVisualProperty(BasicVisualLexicon.NODE_SELECTED_PAINT);
		
		localLabel = getVisualProperty(BasicVisualLexicon.NODE_LABEL).toString();
		localLabelTextColor = (Color)getVisualProperty(BasicVisualLexicon.NODE_LABEL_COLOR);
	}
	
	private void setupRectangles()
	{
		if (localVisible)
		{
			if (rectShape == null)
			{
				rectShape = managerShape.createRectangle(localPosition, localWidth, localHeight);
				needsShapeRedraw = true;
				//System.out.println("Created shape.");
			}
		}
		else if (rectShape != null)
		{
			managerShape.deleteRectangle(rectShape);
			rectShape = null;
			//System.out.println("Deleted shape.");
		}
		
		/*if (localLabel.length() > 0)
		{
			if (rectLabel == null)
			{
				rectLabel = managerLabel.createRectangle(localPosition, localWidth, localHeight);
				needsLabelRedraw = true;
				System.out.println("Created label.");
			}
		}
		else if (rectLabel != null)
		{
			managerLabel.deleteRectangle(rectLabel);
			rectLabel = null;
			System.out.println("Deleted label.");
		}*/
	}
	
	@Override
	public CyNode getModel() 
	{
		return node;
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

		if (vp == ZZVisualLexicon.NODE_SHAPE) 
		{
			//setShape(((NodeShape) value));
		} 
		else if (vp == ZZVisualLexicon.NODE_SELECTED_PAINT) 
		{
			//setSelectedPaint((Paint) value);
		} 
		else if (vp == BasicVisualLexicon.NODE_SELECTED) 
		{
			//setSelected((Boolean) value);
		} 
		else if (vp == BasicVisualLexicon.NODE_VISIBLE) 
		{
			setVisible(((Boolean)value).booleanValue());
		} 
		else if (vp == BasicVisualLexicon.NODE_FILL_COLOR) 
		{
			//setUnselectedPaint((Paint) value);
		} 
		else if (vp == ZZVisualLexicon.NODE_BORDER_PAINT) 
		{
			//setBorderPaint((Color)value);
		} 
		else if (vp == ZZVisualLexicon.NODE_BORDER_WIDTH) 
		{
			//setBorderWidth(((Number) value).floatValue());
		} 
		else if (vp == BasicVisualLexicon.NODE_WIDTH) 
		{
			setWidth(((Number)value).shortValue());
		} 
		else if (vp == BasicVisualLexicon.NODE_HEIGHT) 
		{
			setHeight(((Number)value).shortValue());
		} 
		else if (vp == BasicVisualLexicon.NODE_LABEL) 
		{
			setText(value.toString());
		}  
		else if (vp == BasicVisualLexicon.NODE_LABEL_COLOR) 
		{
			setTextPaint((Color)value);
		}
		else if (vp == BasicVisualLexicon.NODE_LABEL_WIDTH) 
		{
			//setLabelWidth(((Number) value).doubleValue());
		} 
		else if (vp == BasicVisualLexicon.NODE_X_LOCATION) 
		{
			setXPosition(((Number)value).floatValue());
		} 
		else if (vp == BasicVisualLexicon.NODE_Y_LOCATION) 
		{
			setYPosition(((Number)value).floatValue());
		} 
		else if (vp == BasicVisualLexicon.NODE_Z_LOCATION) 
		{
			setZPosition(((Number)value).floatValue());
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

	private void setXPosition(float value) 
	{
		synchronized (m_sync)
		{
			if (localPosition.x == value)
				return;
			localPosition.x = value;
			
			if (rectShape != null)
				rectShape.setCenter(localPosition);
		}
	}

	private void setYPosition(float value) 
	{
		synchronized (m_sync)
		{
			if (localPosition.y == value)
				return;
			localPosition.y = value;
			
			if (rectShape != null)
				rectShape.setCenter(localPosition);
		}
	}
	
	private void setZPosition(float value) 
	{
		synchronized (m_sync)
		{
			if (localPosition.z == value)
				return;
			localPosition.z = value;
			
			if (rectShape != null)
				rectShape.setCenter(localPosition);
		}
	}

	private void setText(String value) 
	{
		synchronized (m_sync)
		{
			if (localLabel == value)
				return;
			localLabel = value;
			
			setupRectangles();
			needsLabelRedraw = true;
		}
	}

	private void setTextPaint(Color value) 
	{
		synchronized (m_sync)
		{
			if (localLabelTextColor.equals(value))
				return;
			localLabelTextColor = value;
			
			needsLabelRedraw = true;
		}
	}

	private void setWidth(short value) 
	{
		synchronized (m_sync)
		{
			if (localWidth == value)
				return;
			localWidth = value;
			
			if (rectShape != null)
				rectShape.setSize(localWidth, localHeight);
		}
	}

	private void setHeight(short value) 
	{
		synchronized (m_sync)
		{
			if (localHeight == value)
				return;
			localHeight = value;
			
			if (rectShape != null)
				rectShape.setSize(localWidth, localHeight);
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
			for (ZZEdgeView edge : edgesOutgoing)
			{
				edge.setSourceX(localPosition.x);
				edge.setSourceY(localPosition.y);
				edge.setSourceZ(localPosition.z);
			}
			for (ZZEdgeView edge : edgesIncoming)
			{
				edge.setTargetX(localPosition.x);
				edge.setTargetY(localPosition.y);
				edge.setTargetZ(localPosition.z);
			}
			
			Vector4 center = new Vector4(localPosition, 1.0f);
			center = Vector4.MatrixMult(viewMatrix, center);
			
			isOnScreen = false;
			Vector2 optimumSize = new Vector2();
			if (rectShape != null)
			{
				isOnScreen = rectShape.isInFrustum(viewMatrix, projMatrix, halfScreen, optimumSize);
				optimumShapeWidth = (short)Math.max(1, Math.min(2048.0f, optimumSize.x));
				optimumShapeHeight = (short)Math.max(1, Math.min(2048.0f, optimumSize.y));
			}
			if (rectLabel != null)
			{
				isOnScreen = isOnScreen || rectLabel.isInFrustum(viewMatrix, projMatrix, halfScreen, optimumSize);
				optimumLabelWidth = (short)Math.max(1, Math.min(2048.0f, optimumSize.x));
				optimumLabelHeight = (short)Math.max(1, Math.min(2048.0f, optimumSize.y));
			}
			
			//System.out.println("Is on screen: " + isOnScreen);
			
			setupRectangles();

			if (!isOnScreen)	// Exiting frustum
			{
				if (rectShape != null && rectShape.isOnDevice())	// Discard custom texture on host, device will be discarded later
					rectShape.setTexture(new byte[4], (short)1, (short)1);
				
				if (rectLabel != null && rectLabel.isOnDevice())	// Discard custom texture on host, device will be discarded later
					rectLabel.setTexture(new byte[4], (short)1, (short)1);
			}
			else	// Entering frustum: 
			{
				if (needsShapeRedraw || needsLabelRedraw)
					return true;
				
				boolean result = false;
				
				if (rectShape != null)
				{
					if (!rectShape.isOnDevice())
					{
						needsShapeRedraw = true;
						result = true;
					}
					
					if (Math.abs(rectShape.getTextureSizeU() - optimumShapeWidth) > 1 || Math.abs(rectShape.getTextureSizeV() - optimumShapeHeight) > 1)	// Needs update if texture size is not optimal
					{
						needsShapeRedraw = true;
						result = true;
					}
				}
	
				if (rectLabel != null)
				{
					if (!rectLabel.isOnDevice())
					{
						needsLabelRedraw = true;
						result = true;
					}
					
					if (Math.abs(rectLabel.getTextureSizeU() - optimumLabelWidth) > 1 || Math.abs(rectLabel.getTextureSizeV() - optimumLabelHeight) > 1)	// Needs update if texture size is not optimal
					{
						needsLabelRedraw = true;
						result = true;
					}
				}
				
				if (result)
					return true;
			}
	
			return false;
		}
	}

	@Override
	public boolean redrawTextures(Matrix4 viewMatrix, Matrix4 projMatrix)
	{
		synchronized (m_sync)
		{
			if (!isOnScreen)	// Exiting frustum, this is for pass-through to resource update only
			{
				if (rectShape != null && rectShape.isOnDevice())
					return true;
				
				if (rectLabel != null && rectLabel.isOnDevice())
					return true;
			}
			else if (localVisible && (needsShapeRedraw || needsLabelRedraw))
			{
				if (!GLMemoryLimit.tryGetMemory(optimumShapeWidth * optimumShapeHeight * (long)4))
					return false;
				
				byte keyR = (byte)(localSelected ? 127 : 0);
				byte keyG = (byte)(localSelected ? 0 : 0);
				byte keyB = (byte)(localSelected ? 0 : 127);
				keyG += (byte)((node.getSUID() % 16) * 8);
				
				byte[] texture = new byte[optimumShapeWidth * optimumShapeHeight * 4];
				
				for (int y = 0; y < optimumShapeHeight; y++)
					for (int x = 0; x < optimumShapeWidth; x++) 
					{
						boolean check = (y % 32 < 16) ? (x % 32 < 16) : (x % 32 > 15);
						texture[(y * optimumShapeWidth + x) * 4 + 0] = check ? keyR : -1;
						texture[(y * optimumShapeWidth + x) * 4 + 1] = check ? keyG : -1;
						texture[(y * optimumShapeWidth + x) * 4 + 2] = check ? keyB : -1;
						texture[(y * optimumShapeWidth + x) * 4 + 3] = -1;
					}
				
				if (rectShape != null)
					rectShape.setTexture(texture, optimumShapeWidth, optimumShapeHeight);
				if (rectLabel != null)
					rectLabel.setTexture(texture, optimumLabelWidth, optimumLabelHeight);
				
				return true;
			}
			
			return false;
		}
	}

	@Override
	public void updateResources(GL4 gl) 
	{
		if (!isOnScreen)	// Exiting frustum
		{
			if (rectShape != null && rectShape.isOnDevice())	// Discard custom texture on device, set to default
			{
				rectShape.discardOnDevice(gl);					// This calls manager.setTextureToDefault as well
				//System.out.println("Set shape texture to default.");
			}
			
			if (rectLabel != null && rectLabel.isOnDevice())	// Discard custom texture on device, set to default
				rectLabel.discardOnDevice(gl);					// This calls manager.setTextureToDefault as well
		}
		else				// Entering frustum
		{
			if (rectShape != null && needsShapeRedraw)
			{
				rectShape.putOnDevice(gl);
				needsShapeRedraw = false;
				//System.out.println("Updated texture on device.");
			}
			
			if (rectLabel != null && needsLabelRedraw)
			{
				rectLabel.putOnDevice(gl);
				needsLabelRedraw = false;
			}
		}
	}
}
