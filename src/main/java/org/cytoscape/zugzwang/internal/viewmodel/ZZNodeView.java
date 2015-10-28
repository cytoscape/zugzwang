package org.cytoscape.zugzwang.internal.viewmodel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.Cy2DGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.ImageCustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.PaintedShape;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.zugzwang.internal.algebra.*;
import org.cytoscape.zugzwang.internal.customgraphics.CustomGraphicsInfo;
import org.cytoscape.zugzwang.internal.customgraphics.NullCustomGraphics;
import org.cytoscape.zugzwang.internal.lines.ZZLineType;
import org.cytoscape.zugzwang.internal.nodeshape.RendererNodeShape;
import org.cytoscape.zugzwang.internal.nodeshape.ZZNodeShape;
import org.cytoscape.zugzwang.internal.rendering.*;
import org.cytoscape.zugzwang.internal.tools.GLMemoryLimit;
import org.cytoscape.zugzwang.internal.tools.MeasuredLineCreator;
import org.cytoscape.zugzwang.internal.tools.TextRenderingUtils;
import org.cytoscape.zugzwang.internal.viewport.*;
import org.cytoscape.zugzwang.internal.visualproperties.Justification;
import org.cytoscape.zugzwang.internal.visualproperties.ObjectPosition;
import org.cytoscape.zugzwang.internal.visualproperties.ObjectPositionImpl;

import com.jogamp.opengl.GL4;

public class ZZNodeView extends ZZView<CyNode> implements Pickable, ZZDrawingDaemonPrimitive
{
	private enum RedrawState
	{
		KEEP,
		REDRAW,
		DELETE
	}
	
	private static final Paint defaultFillColor = new Color(127, 127, 127, 255);
	private static final Paint defaultSelectedFillColor = new Color(255, 127, 127, 255);
	private static final NodeShape defaultShapeType = NodeShapeVisualProperty.ROUND_RECTANGLE;
	private static final Shape defaultShapeShape = RendererNodeShape.nodeShapes.get(ZZNodeShape.getZZShape(NodeShapeVisualProperty.ROUND_RECTANGLE).getNativeShape()).getShape(0, 0, 1, 1);
	private static final LineType defaultBorderLineType = LineTypeVisualProperty.SOLID;
	private static final Stroke defaultBorderStroke = ZZLineType.getZZLineType(LineTypeVisualProperty.SOLID).getStroke(1);
	private static final Paint defaultBorderColor = new Color(0, 0, 0, 255);
	private static final Paint defaultLabelColor = new Color(0, 0, 0, 255);
	private static final Font defaultLabelFont = Font.decode("");
	private static final MeasuredLineCreator defaultLabelMeasuredLines = MeasuredLineCreator.Empty();
	private static final ObjectPosition defaultLabelPosition = ObjectPositionImpl.DEFAULT_POSITION;
	
	private final Object m_sync = new Object();
	
	private final ZZNetworkView networkView;
	private final DefaultValueVault defaultVault;
	private final CyNode node;

	private final List<ZZEdgeView> edgesOutgoing = new ArrayList<>();
	private final List<ZZEdgeView> edgesIncoming = new ArrayList<>(); 
	
	private final ZZRectangleManager managerShape, managerLabel;
	private final ZZRectangleManager[] managersCG;
	
	private ZZRectangle rectShape;
	private ZZRectangle rectLabel;
	private ZZRectangle[] rectCG = new ZZRectangle[9];
	
	private boolean isOnScreen = true;
	private RedrawState shapeRedraw = RedrawState.KEEP, labelRedraw = RedrawState.KEEP;
	private RedrawState[] cgRedraw = new RedrawState[ZZVisualLexicon.numCustomGraphics];
	
	// Store on-screen texture size at current position.
	private short optimumShapeWidth = 1, optimumShapeHeight = 1;
	private short optimumLabelWidth = 1, optimumLabelHeight = 1;
	private short[] optimumCGWidth = new short[ZZVisualLexicon.numCustomGraphics],
				 	optimumCGHeight = new short[ZZVisualLexicon.numCustomGraphics];
	
	// Local copies of VP values to avoid getting them through official channels for each frame.
	private boolean localVisible = true;
	private boolean localSelected;	
	private Vector3 localPosition = new Vector3();
	// Shape:
	private short localWidth = 1;
	private short localHeight = 1;
	private Paint localFillColor = defaultFillColor;
	private Paint localSelectedFillColor = defaultSelectedFillColor;
	private NodeShape localShapeType = defaultShapeType;
	private Shape localShapeShape = defaultShapeShape;		// Not a direct VP, but calculated from ShapeType.
	// Shape border:
	private short localBorderWidth;
	private LineType localBorderLineType = defaultBorderLineType;
	private Stroke localBorderStroke = defaultBorderStroke;	// Not a direct VP, but calculated from BorderWidth and BorderLineType.
	private Paint localBorderColor = defaultBorderColor;
	// Label:
	private String localLabel = "";
	private Paint localLabelColor = defaultLabelColor;
	private short localLabelWidth = 1;
	private Font localLabelFont = defaultLabelFont;
	private short localLabelFontSize = 12;
	private MeasuredLineCreator localLabelMeasuredLines = defaultLabelMeasuredLines;	// Not a direct VP, but calculated from Label, LabelWidth, LabelFont and LabelFontSize.
	private short localLabelRectWidth = 1, localLabelRectHeight = 1;	// Calculated based on all size-related label properties.
	// Label position:
	private ObjectPosition localLabelPosition = defaultLabelPosition;
	private short localLabelOffsetX = 0, localLabelOffsetY = 0;	// Calculated based on localLabelPosition
	// CustomGraphics:
	private CustomGraphicsInfo[] localCGInfo = new CustomGraphicsInfo[ZZVisualLexicon.getCGVisualProperties().size()];
	private List<CustomGraphicLayer>[] localCGLayers = new List[ZZVisualLexicon.numCustomGraphics]; 
	private short[] localCGWidth = new short[ZZVisualLexicon.numCustomGraphics], localCGHeight = new short[ZZVisualLexicon.numCustomGraphics];
	private short[] localCGOffsetX = new short[ZZVisualLexicon.numCustomGraphics], localCGOffsetY = new short[ZZVisualLexicon.numCustomGraphics];
	private float[] localCGCenterX = new float[ZZVisualLexicon.numCustomGraphics], localCGCenterY = new float[ZZVisualLexicon.numCustomGraphics];
	
	public ZZNodeView(ZZNetworkView networkView, 
					  DefaultValueVault defaultVault, 
					  VisualLexicon lexicon, 
					  CyEventHelper eventHelper, 
					  CyNode node, 
					  ZZRectangleManager managerShape, 
					  ZZRectangleManager managerLabel,
					  ZZRectangleManager[] managersCG) 
	{
		super(lexicon, eventHelper);
		this.networkView = networkView;
		this.defaultVault = defaultVault;
		this.node = node;
		
		this.managerShape = managerShape;
		this.managerLabel = managerLabel;
		this.managersCG = managersCG;
		
		for (int i = 0; i < cgRedraw.length; i++)
			cgRedraw[i] = RedrawState.KEEP;
		
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
	
	/**
	 * Synchronizes internal copies of VP values with the currently set defaults/bypass (locked) values. 
	 */
	private void syncProperties()
	{
		setVisible(getVisualProperty(BasicVisualLexicon.NODE_VISIBLE));
		setSelected(getVisualProperty(BasicVisualLexicon.NODE_SELECTED));
		
		setPosition(getPosition());
		
		setWidth(getVisualProperty(BasicVisualLexicon.NODE_WIDTH).shortValue());
		setHeight(getVisualProperty(BasicVisualLexicon.NODE_HEIGHT).shortValue());

		setUnselectedColor(getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR));		
		setSelectedColor(getVisualProperty(BasicVisualLexicon.NODE_SELECTED_PAINT));
		
		setShapeType(getVisualProperty(BasicVisualLexicon.NODE_SHAPE));
		setBorderLineType(getVisualProperty(BasicVisualLexicon.NODE_BORDER_LINE_TYPE));
		setBorderWidth(getVisualProperty(BasicVisualLexicon.NODE_BORDER_WIDTH).shortValue());
		setBorderColor(getVisualProperty(BasicVisualLexicon.NODE_BORDER_PAINT));
		
		setLabelPosition(getVisualProperty(ZZVisualLexicon.NODE_LABEL_POSITION));
		setLabel(getVisualProperty(BasicVisualLexicon.NODE_LABEL));
		setLabelWidth(((Number)getVisualProperty(BasicVisualLexicon.NODE_LABEL_WIDTH)).shortValue());
		setLabelColor(getVisualProperty(BasicVisualLexicon.NODE_LABEL_COLOR));
		setLabelFont(getVisualProperty(BasicVisualLexicon.NODE_LABEL_FONT_FACE));
		setLabelFontSize(((Number)getVisualProperty(BasicVisualLexicon.NODE_LABEL_FONT_SIZE)).shortValue());
		
		List<VisualProperty<CyCustomGraphics>> propsCG = ZZVisualLexicon.getCGVisualProperties();
		List<VisualProperty<Double>> propsCGSize = ZZVisualLexicon.getCGSizeVP();
		List<VisualProperty<ObjectPosition>> propsCGPosition = ZZVisualLexicon.getCGPositionVP();
		for (int i = 0; i < localCGInfo.length; i++) 
		{
			setCustomGraphics(getVisualProperty(propsCG.get(i)), i);
			setCustomGraphicsSize(((Number)getVisualProperty(propsCGSize.get(i))).floatValue(), i);
			setCustomGraphicsPosition(getVisualProperty(propsCGPosition.get(i)), i);
		}
	}
	
	/**
	 * Creates or destroys shape/label/etc. rectangles depending 
	 * on whether the node view is visible or not.
	 */
	private void setupRectangles()
	{
		synchronized (m_sync)
		{
			if (localVisible)
			{
				if (rectShape == null)
				{
					rectShape = managerShape.createRectangle(localPosition, 
															 localWidth, localHeight, 
															 (short)0, (short)0);
					shapeRedraw = RedrawState.REDRAW;
					//System.out.println("Created shape.");
				}
			}
			else if (rectShape != null)
			{
				GLMemoryLimit.freeMemory(rectShape.getOccupiedTextureMemory());
				managerShape.deleteRectangle(rectShape);	// Also deletes the texture.
				shapeRedraw = RedrawState.KEEP;				// Texture already deleted, no need to do anything
				rectShape = null;
				//System.out.println("Deleted shape.");
			}
			
			if (localVisible)
			{
				if (rectLabel == null)
				{
					rectLabel = managerLabel.createRectangle(localPosition, 
															 localLabelRectWidth, localLabelRectHeight, 
															 localLabelOffsetX, localLabelOffsetY);
					labelRedraw = RedrawState.REDRAW;
					//System.out.println("Created label.");
				}
			}
			else if (rectLabel != null)
			{
				GLMemoryLimit.freeMemory(rectLabel.getOccupiedTextureMemory());
				managerLabel.deleteRectangle(rectLabel);
				labelRedraw = RedrawState.KEEP;
				rectLabel = null;
				//System.out.println("Deleted label.");
			}
			
			if (localVisible)
			{
				for (int i = 0; i < localCGInfo.length; i++) 
					if (localCGInfo[i] != null && rectCG[i] == null)
					{
						rectCG[i] = managersCG[i].createRectangle(localPosition, 
																  localCGWidth[i], localCGHeight[i], 
																  localCGOffsetX[i], localCGOffsetY[i]);
						cgRedraw[i] = RedrawState.REDRAW;
					}
					else if (localCGInfo[i] == null && rectCG[i] != null)	// CG has been reset to null.
					{
						GLMemoryLimit.freeMemory(rectCG[i].getOccupiedTextureMemory());
						managersCG[i].deleteRectangle(rectCG[i]);
						cgRedraw[i] = RedrawState.KEEP;
						rectCG[i] = null;
					}
			}
			else
			{
				for (int i = 0; i < rectCG.length; i++)
					if (rectCG[i] != null)
					{
						GLMemoryLimit.freeMemory(rectCG[i].getOccupiedTextureMemory());
						managersCG[i].deleteRectangle(rectCG[i]);
						cgRedraw[i] = RedrawState.KEEP;
						rectCG[i] = null;
					}
			}
		}
	}
	
	/**
	 * Gets a list of all rectangle primitives that are currently not null.
	 * 
	 * @return List of rectangles
	 */
	private List<ZZRectangle> getAllRectangles()
	{
		List<ZZRectangle> rects = new ArrayList<ZZRectangle>(2);
		if (rectShape != null)
			rects.add(rectShape);
		if (rectLabel != null)
			rects.add(rectLabel);
		
		return rects;
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
			setUnselectedColor((Paint)value);
		} 
		else if (vp == ZZVisualLexicon.NODE_BORDER_LINE_TYPE) 
		{
			setBorderLineType((LineType)value);
		} 
		else if (vp == ZZVisualLexicon.NODE_BORDER_PAINT) 
		{
			setBorderColor((Paint)value);
		} 
		else if (vp == ZZVisualLexicon.NODE_BORDER_WIDTH) 
		{
			setBorderWidth(((Number)value).shortValue());
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
			setLabel(value.toString());
		}  
		else if (vp == BasicVisualLexicon.NODE_LABEL_COLOR) 
		{
			setLabelColor((Color)value);
		}
		else if (vp == BasicVisualLexicon.NODE_LABEL_WIDTH) 
		{
			setLabelWidth(((Number)value).shortValue());
		}
		else if (vp == BasicVisualLexicon.NODE_LABEL_FONT_FACE) 
		{
			setLabelFont((Font)value);
		}
		else if (vp == BasicVisualLexicon.NODE_LABEL_FONT_SIZE) 
		{
			setLabelFontSize(((Number)value).shortValue());
		}
		else if (vp == ZZVisualLexicon.NODE_LABEL_POSITION) 
		{
			setLabelPosition((ObjectPosition)value);
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
		else
		{
			List<VisualProperty<CyCustomGraphics>> propsCG = ZZVisualLexicon.getCGVisualProperties();
			List<VisualProperty<Double>> propsCGSize = ZZVisualLexicon.getCGSizeVP();
			List<VisualProperty<ObjectPosition>> propsCGPosition = ZZVisualLexicon.getCGPositionVP();
			
			for (int i = 0; i < localCGInfo.length; i++) 
				if (vp == propsCG.get(i))					
					setCustomGraphics(getVisualProperty(propsCG.get(i)), i);
				else if (vp == propsCGSize.get(i))
					setCustomGraphicsSize(((Number)getVisualProperty(propsCGSize.get(i))).floatValue(), i);
				else if (vp == propsCGPosition.get(i))
					setCustomGraphicsPosition(getVisualProperty(propsCGPosition.get(i)), i);
		}
	}

	/**
	 * Tests all primitives within the node view for intersections.
	 * 
	 * @param ray 3D ray to intersect with
	 * @param viewMatrix Current view matrix, required to construct the primitive sprites
	 * @return If one or multiple intersections are found, the closest to the ray's source is returned; otherwise null
	 */
	@Override
	public PickingResult intersectsWith(final Ray3 ray, final Matrix4 viewMatrix) 
	{
		return null;
	}
	
	/**
	 * Gets the node view's position that is in sync with its location VPs.
	 * 
	 * @return Node view position
	 */
	public Vector3 getPosition()
	{
		return new Vector3(((Number)getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION)).floatValue(),
						   ((Number)getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION)).floatValue(),
						   ((Number)getVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION)).floatValue());
	}
	
	/**
	 * Gets the left upper, and right bottom corners of a rectangle 
	 * that contains all primitives within the node view.
	 * 
	 * @param viewMatrix Current view matrix to construct the sprites
	 * @return LU and RB corners, i. e. min and max coordinates
	 */
	public Vector3[] getCorners(Matrix4 viewMatrix)
	{
		Vector3 minCorner = new Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		Vector3 maxCorner = new Vector3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		
		// Go through all rectangles and determine the min and max coordinates.
		List<ZZRectangle> rects = getAllRectangles();
		for (ZZRectangle rect : rects)
		{
			// Need only 0th and 3rd corners (LU and RB).
			Vector4[] rectCorners = rect.getCorners(viewMatrix);

			minCorner.x = Math.min(minCorner.x, rectCorners[0].x);
			minCorner.y = Math.min(minCorner.y, rectCorners[0].y);
			minCorner.z = Math.min(minCorner.z, rectCorners[0].z);

			maxCorner.x = Math.max(maxCorner.x, rectCorners[3].x);
			maxCorner.y = Math.max(maxCorner.y, rectCorners[3].y);
			maxCorner.z = Math.max(maxCorner.z, rectCorners[3].z);
		}
		
		return new Vector3[] { minCorner, maxCorner };
	}
	
	/**
	 * Frees all associated device resources.
	 * 
	 * @param gl Current GL context
	 */
	public void dispose(GL4 gl)
	{
		if (rectShape != null && rectShape.isOnDevice())
		{
			GLMemoryLimit.freeMemory(rectShape.getOccupiedTextureMemory());
			rectShape.discardOnDevice(gl);
		}
		
		if (rectLabel != null && rectLabel.isOnDevice())
		{
			GLMemoryLimit.freeMemory(rectLabel.getOccupiedTextureMemory());
			rectLabel.discardOnDevice(gl);
		}
		
		for (int i = 0; i < rectCG.length; i++) 
			if (rectCG[i] != null && rectCG[i].isOnDevice())
			{
				GLMemoryLimit.freeMemory(rectCG[i].getOccupiedTextureMemory());
				rectCG[i].discardOnDevice(gl);
			}
	}
	

	// ***********************************
	// ZZDrawingDaemonPrimitive interface:
	// ***********************************
	
	@Override
	public boolean updateState(boolean updateVisualProperties, GL4 gl, Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen) 
	{
		synchronized (m_sync)
		{
			//if (updateVisualProperties)
				syncProperties();
			
			// Check if shape or label are within the camera frustum.
			// Also calculate on-screen rectangles sizes for optimal texturing.
			// Texture size is capped at 2048 px in each dimension.
			
			isOnScreen = false;
			Vector2 optimumSize = new Vector2();
			float maxSize = 2048.0f * networkView.globalDownsampling;
			if (rectShape != null)
			{
				isOnScreen = rectShape.isInFrustum(viewMatrix, projMatrix, halfScreen, optimumSize);
				optimumShapeWidth = (short)Math.min(maxSize, Math.max(optimumSize.x * networkView.globalDownsampling, 4.0f));
				optimumShapeHeight = (short)Math.min(maxSize, Math.max(optimumSize.y * networkView.globalDownsampling, 4.0f));
			}
			if (rectLabel != null)
			{
				isOnScreen = rectLabel.isInFrustum(viewMatrix, projMatrix, halfScreen, optimumSize) || isOnScreen;
				optimumLabelWidth = (short)Math.min(maxSize, Math.max(optimumSize.x * networkView.globalDownsampling, 4.0f));
				optimumLabelHeight = (short)Math.min(maxSize, Math.max(optimumSize.y * networkView.globalDownsampling, 4.0f));
			}
			for (int i = 0; i < rectCG.length; i++)
				if (rectCG[i] != null)
				{
					isOnScreen = rectCG[i].isInFrustum(viewMatrix, projMatrix, halfScreen, optimumSize) || isOnScreen;
					optimumCGWidth[i] = (short)Math.min(maxSize, Math.max(optimumSize.x * networkView.globalDownsampling, 4.0f));
					optimumCGHeight[i] = (short)Math.min(maxSize, Math.max(optimumSize.y * networkView.globalDownsampling, 4.0f));
				}
			
			//System.out.println("Is on screen: " + isOnScreen);
			
			setupRectangles();	// Considers visibility status to create, keep, or remove shape and label rectangles.

			if (!isOnScreen)	// Exiting frustum
			{
				if (rectShape != null && rectShape.isOnDevice())
					shapeRedraw = RedrawState.DELETE;	// Mark for deletion later in updateResources
				
				if (rectLabel != null && rectLabel.isOnDevice())
					labelRedraw = RedrawState.DELETE;	// Mark for deletion later in updateResources
				
				for (int i = 0; i < rectCG.length; i++)
					if (rectCG[i] != null && rectCG[i].isOnDevice())
						cgRedraw[i] = RedrawState.DELETE;	// Mark for deletion later in updateResources
			}
			else	// Entering frustum
			{
				// Check if on-screen shape size deviates from the currently available texture.
				if (rectShape != null)
					if (!rectShape.isOnDevice() ||	// Texture doesn't exist, but should -> redraw.					
					    Math.abs(rectShape.getTextureSizeU() - optimumShapeWidth) > 1 || 
						Math.abs(rectShape.getTextureSizeV() - optimumShapeHeight) > 1)	// Needs update because texture size is not optimal
					{
						shapeRedraw = RedrawState.REDRAW;
					}
	
				// Check if on-screen label size deviates from the currently available texture.
				if (rectLabel != null)
					if ((!rectLabel.isOnDevice() && localLabel != null && localLabel.length() > 0) ||	// Texture doesn't exist, but should -> redraw.
						Math.abs(rectLabel.getTextureSizeU() - optimumLabelWidth) > 1 || 
						Math.abs(rectLabel.getTextureSizeV() - optimumLabelHeight) > 1)	// Needs update because texture size is not optimal
					{
						labelRedraw = RedrawState.REDRAW;
					}
				
				// Check if on-screen CG size deviates from the currently available texture.
				for (int i = 0; i < rectCG.length; i++)
					if (rectCG[i] != null)
						if (!rectCG[i].isOnDevice() ||	// Texture doesn't exist, but should -> redraw.
							Math.abs(rectCG[i].getTextureSizeU() - optimumCGWidth[i]) > 1 ||
							Math.abs(rectCG[i].getTextureSizeV() - optimumCGHeight[i]) > 1)	// Needs update because texture size is not optimal
						{
							cgRedraw[i] = RedrawState.REDRAW;
						}
			}
	
			if (shapeRedraw == RedrawState.KEEP && labelRedraw == RedrawState.KEEP)
				return false;
			else
				return true;
		}
	}

	@Override
	public boolean redrawTextures(Matrix4 viewMatrix, Matrix4 projMatrix)
	{
		synchronized (m_sync)
		{
			if (!isOnScreen)	// Exiting frustum, this is for pass-through to resource update only.
			{
				if (rectShape != null && rectShape.isOnDevice())
					shapeRedraw = RedrawState.DELETE;
				
				if (rectLabel != null && rectLabel.isOnDevice())
					labelRedraw = RedrawState.DELETE;
				
				for (int i = 0; i < rectCG.length; i++) 
					if (rectCG[i] != null && rectCG[i].isOnDevice())
						cgRedraw[i] = RedrawState.DELETE;
			}
			else if (localVisible) // On screen, visible, and needs redraw.
			{
				if (shapeRedraw == RedrawState.REDRAW)
				{
					boolean redrawn = false;
					
					if (rectShape != null)
					{
						// Check if there is enough memory.
						long requested = optimumShapeWidth * optimumShapeHeight * (long)4;
						long freed = rectShape.getOccupiedTextureMemory();
						if (GLMemoryLimit.tryGetMemory(requested, freed))
						{						
							BufferedImage img = new BufferedImage(optimumShapeWidth, optimumShapeHeight, BufferedImage.TYPE_4BYTE_ABGR);
							Graphics2D g = (Graphics2D)img.getGraphics();
							setQualityOptions(g);
							
							float scaleX = (float)optimumShapeWidth / (float)localWidth;
							float scaleY = (float)optimumShapeHeight / (float)localHeight;
							// This is a static method because network view needs it too.
							drawShapeTexture(g, scaleX, scaleY, localWidth, localHeight, localFillColor, localShapeShape, localBorderColor, localBorderStroke, localBorderWidth);
							
							rectShape.setTexture(img);
							redrawn = true;
						}
					}
					
					if (redrawn)
						shapeRedraw = RedrawState.REDRAW;
					else if (!redrawn && rectShape != null && rectShape.isOnDevice())	// Texture invalid but still on device
						shapeRedraw = RedrawState.DELETE;
					else																// Invalid and not on device
						shapeRedraw = RedrawState.KEEP;
				}
				
				if (labelRedraw == RedrawState.REDRAW)
				{
					boolean redrawn = false;
					
					if (rectLabel != null && 
						localLabel != null && localLabel.length() > 0 && 
						localLabelColor != null && localLabelFont != null && 
						localLabelMeasuredLines != null &&
						localLabelRectWidth >= 1 && localLabelRectHeight >= 1)	// Check width & height in case the label is too small.
					{
						// Check if there is enough memory.
						long requested = optimumLabelWidth * optimumLabelHeight * (long)4;
						long freed = rectLabel.getOccupiedTextureMemory();
						if (GLMemoryLimit.tryGetMemory(requested, freed))
						{
							BufferedImage img = new BufferedImage(optimumLabelWidth, optimumLabelHeight, BufferedImage.TYPE_4BYTE_ABGR);
							Graphics2D g = (Graphics2D)img.getGraphics();
							setQualityOptions(g);
							
							FontRenderContext fontContext = g.getFontRenderContext();
							Vector2 textCenter = new Vector2(optimumLabelWidth * 0.5f, optimumLabelHeight * 0.5f);					
							Vector2 scale = new Vector2((float)optimumLabelWidth / (float)localLabelRectWidth, (float)optimumLabelHeight / (float)localLabelRectHeight);
							
							Font sizedFont = localLabelFont.deriveFont((float)localLabelFontSize * scale.x);
							
							TextRenderingUtils.renderHorizontalText(g,
																	localLabelMeasuredLines, 
																	sizedFont, 
																	fontContext, 
																	textCenter, 
																	scale, 
																	localLabelPosition == null ? Justification.JUSTIFY_CENTER : localLabelPosition.getJustify(), 
																	localLabelColor);
							
							rectLabel.setTexture(img);
							redrawn = true;
						}
					}
					
					if (redrawn)
						labelRedraw = RedrawState.REDRAW;
					else if (!redrawn && rectLabel != null && rectLabel.isOnDevice())	// Texture invalid but still on device
						labelRedraw = RedrawState.DELETE;
					else																// Invalid and not on device
						labelRedraw = RedrawState.KEEP;
				}
				
				for (int i = 0; i < cgRedraw.length; i++) 
					if (cgRedraw[i] == RedrawState.REDRAW)
					{
						boolean redrawn = false;
						
						if (rectCG[i] != null &&
							localCGInfo[i] != null &&
							localCGLayers[i] != null &&
							optimumCGWidth[i] >= 1 && optimumCGHeight[i] >= 1)
						{
							long requested = optimumCGWidth[i] * optimumCGHeight[i] * (long)4;
							long freed = rectCG[i].getOccupiedTextureMemory();
							if (GLMemoryLimit.tryGetMemory(requested, freed))
							{
								BufferedImage img = new BufferedImage(optimumCGWidth[i], optimumCGHeight[i], BufferedImage.TYPE_4BYTE_ABGR);
								Graphics2D g = (Graphics2D)img.getGraphics();
								setQualityOptions(g);

								Vector2 scale = new Vector2((float)optimumCGWidth[i] / (float)localCGWidth[i], (float)optimumCGHeight[i] / (float)localCGHeight[i]);
								g.translate(localCGCenterX[i] * scale.x, localCGCenterY[i] * scale.y);
								g.scale(scale.x, scale.y);
								
								for (CustomGraphicLayer layer : localCGLayers[i])
									drawCustomGraphicLayer(g, networkView, this, localShapeShape, layer);
								
								rectCG[i].setTexture(img);
								redrawn = true;
							}
						}
						
						if (redrawn)
							cgRedraw[i] = RedrawState.REDRAW;
						else if (!redrawn && rectCG[i] != null && rectCG[i].isOnDevice())	// Texture invalid but still on device
							cgRedraw[i] = RedrawState.DELETE;
						else																// Invalid and not on device
							cgRedraw[i] = RedrawState.KEEP;
					}
			}
			
			if (shapeRedraw == RedrawState.KEEP && labelRedraw == RedrawState.KEEP)
				return false;
			else
				return true;
		}
	}

	@Override
	public void updateResources(GL4 gl) 
	{
		synchronized (m_sync)
		{
			if (rectShape != null)
			{
				if (shapeRedraw == RedrawState.DELETE)
				{
					GLMemoryLimit.freeMemory(rectShape.getOccupiedTextureMemory());
					rectShape.discardOnDevice(gl);	// This calls manager.setTextureToDefault as well.
					rectShape.setTexture( new byte[] { 0, 0, 0, 0 }, (short)1, (short)1);
					//System.out.println("Set shape texture to default.");
				}
				else if (shapeRedraw == RedrawState.REDRAW)
				{
					rectShape.putOnDevice(gl);	// New texture is already in host buffer, now get it to device.
					//System.out.println("Updated shape texture on device.");
				}
			}
				
			shapeRedraw = RedrawState.KEEP;
			
			if (rectLabel != null)
			{
				if (labelRedraw == RedrawState.DELETE)
				{
					GLMemoryLimit.freeMemory(rectLabel.getOccupiedTextureMemory());
					rectLabel.discardOnDevice(gl);					// This calls manager.setTextureToDefault as well.
					rectLabel.setTexture( new byte[] { 0, 0, 0, 0 }, (short)1, (short)1);
					//System.out.println("Set label texture to default.");
				}				
				else if (labelRedraw == RedrawState.REDRAW)
				{
					rectLabel.putOnDevice(gl);	// New texture is already in host buffer, now get it to device.
					//System.out.println("Updated label texture on device.");
				}
			}
				
			labelRedraw = RedrawState.KEEP;
			
			for (int i = 0; i < rectCG.length; i++) 
			{
				if (rectCG[i] != null)
				{
					if (cgRedraw[i] == RedrawState.DELETE)
					{
						GLMemoryLimit.freeMemory(rectCG[i].getOccupiedTextureMemory());
						rectCG[i].discardOnDevice(gl);
						rectCG[i].setTexture(new byte[] { 0, 0, 0, 0 }, (short)1, (short)1);						
					}
					else if (cgRedraw[i] == RedrawState.REDRAW)
					{
						rectCG[i].putOnDevice(gl);
					}
				}
				
				cgRedraw[i] = RedrawState.KEEP;
			}
		}
	}
	
	public static void drawShapeTexture(Graphics2D g, float scaleX, float scaleY,
										short width, short height,
										Paint fillColor, Shape shape,
										Paint borderColor, Stroke stroke, short strokeWidth)
	{
		AffineTransform transform = new AffineTransform();
		//transform.translate((float)width * 0.5f, (float)height * 0.5f);
		transform.scale(scaleX, scaleY);
		g.transform(transform);
		
		if (strokeWidth > 0)
		{
			g.setPaint(borderColor);
			g.setStroke(stroke);
			g.draw(shape);
		}

		g.setPaint(fillColor);
		g.fill(shape);
	}
	
	public static void drawCustomGraphicLayer(Graphics2D g,
											  CyNetworkView netView, View<CyNode> nodeView,
											  Shape nodeShape, CustomGraphicLayer cg) 
	{
		if (cg instanceof PaintedShape) 
		{
			PaintedShape ps = (PaintedShape)cg;
			Shape shape = ps.getShape();
			
			if (ps.getStroke() != null) 
			{
				Paint strokePaint = ps.getStrokePaint();
				if (strokePaint == null) strokePaint = Color.BLACK;
					g.setPaint(strokePaint);
				g.setStroke(ps.getStroke());
				g.draw(shape);
			}
			
			g.setPaint(ps.getPaint());
			g.fill(shape);
		} 
		else if (cg instanceof Cy2DGraphicLayer) 
		{
			Cy2DGraphicLayer layer = (Cy2DGraphicLayer)cg;
			layer.draw(g, nodeShape, netView, nodeView);
		} 
		else if (cg instanceof ImageCustomGraphicLayer) 
		{
			Rectangle bounds = cg.getBounds2D().getBounds();
			BufferedImage bImg = ((ImageCustomGraphicLayer)cg).getPaint(bounds).getImage();
			g.drawImage(bImg, bounds.x, bounds.y, bounds.width, bounds.height, null);
		} 
		else 
		{
			Rectangle2D bounds = nodeShape.getBounds2D();
			g.setPaint(cg.getPaint(bounds));
			g.fill(nodeShape);
		}
	}
	
	public static void setQualityOptions(Graphics2D g)
	{
		// Antialiasing is ON
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// Rendering quality is HIGH.
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		
		// High quality alpha blending is ON.
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		
		// High quality color rendering is ON.
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		
		g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
		
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		// Text antialiasing is ON.
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		
		g.setStroke(new BasicStroke(0.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f));
	}
	
	
	// React to VP value changes:
	
	private void setVisible(final boolean value)
	{
		synchronized (m_sync)
		{
			if (localVisible == value)
				return;
			localVisible = value;
			
			setupRectangles();
		}
	}
	
	private void setSelected(final boolean value)
	{
		synchronized (m_sync)
		{
			if (localSelected == value)
				return;
			localSelected = value;
			
			shapeRedraw = RedrawState.REDRAW;
		}
	}
	
	private void setSelectedColor(Paint value)
	{
		synchronized (m_sync)
		{
			if (localSelectedFillColor != null && value.equals(localSelectedFillColor))
				return;
			
			localSelectedFillColor = value;
			shapeRedraw = RedrawState.REDRAW;
		}
	}
	
	private void setUnselectedColor(Paint value)
	{
		synchronized (m_sync)
		{
			if (localFillColor != null && value.equals(localFillColor))
				return;
			
			localFillColor = value;
			shapeRedraw = RedrawState.REDRAW;
		}
	}
	
	private void setShapeType(NodeShape value)
	{
		synchronized (m_sync)
		{
			if (localShapeType != null && value.equals(localShapeType))
				return;
			
			localShapeType = value;
			setShapeShape();
			shapeRedraw = RedrawState.REDRAW;
		}
	}
	
	private void setShapeShape()
	{
		if (localShapeType == null)
			return;
		
		RendererNodeShape rendererShape = RendererNodeShape.nodeShapes.get(ZZNodeShape.getZZShape(localShapeType).getNativeShape());
			
		localShapeShape = rendererShape.getShape(0.0f, 0.0f, 
												 (float)localWidth, (float)localHeight);
		
		if (localBorderWidth > 0.0f && localBorderStroke != null)
		{
			Shape borderShape = localBorderStroke.createStrokedShape(localShapeShape);
			Rectangle2D bounds = borderShape.getBounds2D();
			float boundsWidth = (float)bounds.getWidth();
			float borderHalf = (boundsWidth - (float)localWidth) * 0.5f;
			// Don't let the entire node get consumed by the border:
			borderHalf = Math.min(borderHalf, Math.min(localWidth, localHeight) * 0.5f - 2.0f);
			localShapeShape = rendererShape.getShape(borderHalf, borderHalf, 
													 (float)localWidth - borderHalf, (float)localHeight - borderHalf);
		}
	}
	
	private void setBorderWidth(short value)
	{
		synchronized (m_sync)
		{
			if (localBorderWidth == value)
				return;
			
			localBorderWidth = value;
			setBorderStroke();
			setShapeShape();
			shapeRedraw = RedrawState.REDRAW;
		}
	}
	
	private void setBorderLineType(LineType value)
	{
		synchronized (m_sync)
		{
			if (localBorderLineType != null && localBorderLineType.equals(value))
				return;
			
			localBorderLineType = value;
			setBorderStroke();
			setShapeShape();
			shapeRedraw = RedrawState.REDRAW;
		}
	}
	
	private void setBorderStroke()
	{
		if (localBorderLineType == null)
			return;
		
		localBorderStroke = ZZLineType.getZZLineType(localBorderLineType).getStroke(localBorderWidth);
	}
	
	private void setBorderColor(Paint value)
	{
		synchronized (m_sync)
		{
			if (localBorderColor != null && value.equals(localBorderColor))
				return;
			
			localBorderColor = value;
			shapeRedraw = RedrawState.REDRAW;
		}
	}

	private void setWidth(final short value)
	{
		synchronized (m_sync)
		{
			if (localWidth == value)
				return;
			localWidth = value;
			
			if (rectShape != null)
				rectShape.setSize(localWidth, localHeight);
			setShapeShape();
			setLabelOffset();
			for (int i = 0; i < rectCG.length; i++)
				if (rectCG[i] != null)
					setCustomGraphicsBounds(i);
			shapeRedraw = RedrawState.REDRAW;
		}
	}

	private void setHeight(final short value)
	{
		synchronized (m_sync)
		{
			if (localHeight == value)
				return;
			localHeight = value;
			
			if (rectShape != null)
				rectShape.setSize(localWidth, localHeight);
			setShapeShape();
			setLabelOffset();
			for (int i = 0; i < rectCG.length; i++)
				if (rectCG[i] != null)
					setCustomGraphicsBounds(i);
			shapeRedraw = RedrawState.REDRAW;
		}
	}

	private void setXPosition(final float value)
	{
		synchronized (m_sync)
		{
			if (localPosition.x == value)
				return;
			localPosition.x = value;
			
			// Position edge sources and targets.
			for (ZZEdgeView edge : edgesOutgoing)
				edge.setSourceX(value);
			for (ZZEdgeView edge : edgesIncoming)
				edge.setTargetX(value);
			
			if (rectShape != null)
				rectShape.setCenter(localPosition);
			if (rectLabel != null)
				rectLabel.setCenter(localPosition);
			for (int i = 0; i < rectCG.length; i++)
				if (rectCG[i] != null)
					rectCG[i].setCenter(localPosition);
		}
	}

	private void setYPosition(final float value)
	{
		synchronized (m_sync)
		{
			if (localPosition.y == value)
				return;
			localPosition.y = value;
			
			// Position edge sources and targets.
			for (ZZEdgeView edge : edgesOutgoing)
				edge.setSourceY(value);
			for (ZZEdgeView edge : edgesIncoming)
				edge.setTargetY(value);
			
			if (rectShape != null)
				rectShape.setCenter(localPosition);
			if (rectLabel != null)
				rectLabel.setCenter(localPosition);
			for (int i = 0; i < rectCG.length; i++)
				if (rectCG[i] != null)
					rectCG[i].setCenter(localPosition);
		}
	}
	
	private void setZPosition(final float value)
	{
		synchronized (m_sync)
		{
			if (localPosition.z == value)
				return;
			localPosition.z = value;
			
			// Position edge sources and targets.
			for (ZZEdgeView edge : edgesOutgoing)
				edge.setSourceZ(value);
			for (ZZEdgeView edge : edgesIncoming)
				edge.setTargetZ(value);
			
			if (rectShape != null)
				rectShape.setCenter(localPosition);
			if (rectLabel != null)
				rectLabel.setCenter(localPosition);
			for (int i = 0; i < rectCG.length; i++)
				if (rectCG[i] != null)
					rectCG[i].setCenter(localPosition);
		}
	}
	
	private void setPosition(final Vector3 value)
	{
		synchronized (m_sync)
		{
			if (localPosition != null && localPosition.equals(value))
				return;
			localPosition = value;
			
			// Position edge sources and targets.
			for (ZZEdgeView edge : edgesOutgoing)
				edge.setSourcePosition(value);
			for (ZZEdgeView edge : edgesIncoming)
				edge.setTargetPosition(value);
			
			if (rectShape != null)
				rectShape.setCenter(localPosition);
			if (rectLabel != null)
				rectLabel.setCenter(localPosition);
			for (int i = 0; i < rectCG.length; i++)
				if (rectCG[i] != null)
					rectCG[i].setCenter(localPosition);
		}
	}

	private void setLabel(final String value)
	{
		synchronized (m_sync)
		{
			if (localLabel != null && localLabel.equals(value))
				return;
			localLabel = value;
			
			setLabelBounds();
			setLabelOffset();
			labelRedraw = RedrawState.REDRAW;
		}
	}

	private void setLabelColor(final Paint value)
	{
		synchronized (m_sync)
		{
			if (localLabelColor != null && localLabelColor.equals(value))
				return;
			localLabelColor = value;
			
			labelRedraw = RedrawState.REDRAW;
		}
	}

	private void setLabelWidth(final short value)
	{
		synchronized (m_sync)
		{
			if (localLabelWidth == value)
				return;
			localLabelWidth = value;

			setLabelBounds();
			setLabelOffset();
			labelRedraw = RedrawState.REDRAW;
		}
	}
	
	private void setLabelFont(final Font value)
	{
		synchronized (m_sync)
		{
			if (localLabelFont != null && localLabelFont.equals(value))
				return;
			localLabelFont = value;

			setLabelBounds();
			setLabelOffset();
			labelRedraw = RedrawState.REDRAW;
		}
	}

	private void setLabelFontSize(final short value)
	{
		synchronized (m_sync)
		{
			if (localLabelFontSize == value)
				return;
			localLabelFontSize = value;
			
			setLabelBounds();
			setLabelOffset();
			labelRedraw = RedrawState.REDRAW;
		}
	}
	
	private void setLabelBounds()
	{
		if (rectLabel == null || localLabelFont == null || localLabel == null)
			return;
		
		Font sizedFont = localLabelFont.deriveFont((float)localLabelFontSize);
		
		// Create temporary graphics object with the usual quality settings to obtain a FontRenderContext:
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = (Graphics2D)img.getGraphics();
		setQualityOptions(g);
		
		FontRenderContext fontContext = g.getFontRenderContext();
		
		localLabelMeasuredLines = new MeasuredLineCreator(localLabel, sizedFont, fontContext, localLabelWidth);

		Vector2 bounds = new Vector2(localLabelMeasuredLines.getMaxLineWidth(), localLabelMeasuredLines.getTotalHeight());
		localLabelRectWidth = (short)Math.ceil(bounds.x);
		localLabelRectHeight = (short)Math.ceil(bounds.y);
		rectLabel.setSize(localLabelRectWidth, localLabelRectHeight);
	}
	
	private void setLabelPosition(ObjectPosition value)
	{
		synchronized (m_sync)
		{
			if (localLabelPosition != null && localLabelPosition.equals(value))
				return;
			localLabelPosition = value;

			setLabelOffset();
		}
	}
	
	private void setLabelOffset()
	{
		if (rectShape == null || rectLabel == null || localLabelPosition == null)
			return;
		
		Vector4 boundsObject = rectLabel.getBounds2D();
		Vector4 boundsTarget = rectShape.getBounds2D();
		
		Vector2 offset = ((ObjectPositionImpl)localLabelPosition).calcOffset(boundsObject, boundsTarget);
		localLabelOffsetX = (short)offset.x;
		localLabelOffsetY = (short)offset.y;
		
		rectLabel.setOffset(localLabelOffsetX, localLabelOffsetY);
	}
	
	private void setCustomGraphics(CyCustomGraphics value, int index)
	{
		// CG equals NullCustomGraphics and has thus not been set, or reset.
		if (value == NullCustomGraphics.getNullObject())
		{
			// Reset.
			if (localCGInfo[index] != null)
			{
				localCGInfo[index] = null;
				setupRectangles();	// This will delete the corresponding rectangle.
			}
			return;
		}
		
		if (localCGInfo[index] == null)
		{
			localCGInfo[index] = new CustomGraphicsInfo(ZZVisualLexicon.getCGVisualProperties().get(index));
			setupRectangles();	// Create new rectangle for this CG.
		}
		
		if (localCGInfo[index].getCustomGraphics() != null && 
			localCGInfo[index].getCustomGraphics().equals(value))
			return;
		localCGInfo[index].setCustomGraphics(value);
		
		setCustomGraphicsBounds(index);
		setCustomGraphicsOffset(index);
		
		cgRedraw[index] = RedrawState.REDRAW;
	}
	
	private void setCustomGraphicsSize(float value, int index)
	{
		CustomGraphicsInfo info = localCGInfo[index];
		if (info == null || (info.getSize() != null && info.getSize() == value))
			return;		
		info.setSize(value);
		
		setCustomGraphicsBounds(index);
		setCustomGraphicsOffset(index);
		
		cgRedraw[index] = RedrawState.REDRAW;
	}
	
	private void setCustomGraphicsBounds(int index)
	{
		if (localCGInfo[index] == null)
			return;
		
		Vector4 bounds = new Vector4(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		List<CustomGraphicLayer> layers = localCGInfo[index].createLayers(networkView, this, networkView.getVisualDependencies(), localWidth, localHeight, localBorderWidth);
		localCGLayers[index] = layers;
		
		for (CustomGraphicLayer layer : layers)
		{
			Rectangle2D r = layer.getBounds2D();
			bounds.x = Math.min(bounds.x, (float)r.getMinX());
			bounds.y = Math.min(bounds.y, (float)r.getMinY());
			bounds.z = Math.max(bounds.z, (float)r.getMaxX());
			bounds.w = Math.max(bounds.w, (float)r.getMaxY());
		}

		localCGWidth[index] = (short)Math.ceil(bounds.z - bounds.x);
		localCGHeight[index] = (short)Math.ceil(bounds.w - bounds.y);
		localCGCenterX[index] = (float)Math.ceil(-bounds.x);
		localCGCenterY[index] = (float)Math.ceil(-bounds.y);
		
		if (rectCG[index] != null)
			rectCG[index].setSize(localCGWidth[index], localCGHeight[index]);
	}
	
	private void setCustomGraphicsPosition(ObjectPosition value, int index)
	{
		CustomGraphicsInfo info = localCGInfo[index];
		if (info == null || info.getPosition().equals(value))
			return;
		
		info.setPosition(value);
		setCustomGraphicsOffset(index);
	}
	
	private void setCustomGraphicsOffset(int index)
	{
		if (rectShape == null || rectCG[index] == null)
			return;
		
		Vector4 boundsObject = rectCG[index].getBounds2D();
		Vector4 boundsTarget = rectShape.getBounds2D();
		
		Vector2 offset = ((ObjectPositionImpl)localCGInfo[index].getPosition()).calcOffset(boundsObject, boundsTarget);
		localCGOffsetX[index] = (short)offset.x;
		localCGOffsetY[index] = (short)offset.y;
		
		rectCG[index].setOffset(localCGOffsetX[index], localCGOffsetY[index]);
	}
}
