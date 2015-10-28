package org.cytoscape.zugzwang.internal.viewmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.NullDataType;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.Visualizable;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.DefaultVisualizableVisualProperty;
import org.cytoscape.view.presentation.property.DoubleVisualProperty;
import org.cytoscape.view.presentation.property.NullVisualProperty;
import org.cytoscape.zugzwang.internal.customgraphics.CustomGraphicsManager;
import org.cytoscape.zugzwang.internal.customgraphics.CustomGraphicsRange;
import org.cytoscape.zugzwang.internal.customgraphics.NullCustomGraphics;
import org.cytoscape.zugzwang.internal.lines.ZZLineType;
import org.cytoscape.zugzwang.internal.strokes.*;
import org.cytoscape.zugzwang.internal.visualproperties.CustomGraphicsVisualProperty;
import org.cytoscape.zugzwang.internal.visualproperties.ObjectPosition;
import org.cytoscape.zugzwang.internal.visualproperties.ObjectPositionImpl;
import org.cytoscape.zugzwang.internal.visualproperties.ObjectPositionVisualProperty;


public class ZZVisualLexicon extends BasicVisualLexicon 
{
	// Set of custom graphics positions.
	private static final List<VisualProperty<ObjectPosition>> CG_POSITIONS = new ArrayList<>();
	private static final List<VisualProperty<CyCustomGraphics>> CG = new ArrayList<>();
	private static final List<VisualProperty<Double>> CG_SIZE = new ArrayList<>();
	
	private static final Map<VisualProperty<CyCustomGraphics>, VisualProperty<Double>> CG_TO_SIZE = new HashMap<>();
	private static final Map<VisualProperty<CyCustomGraphics>, VisualProperty<ObjectPosition>> CG_TO_POSITION = new HashMap<>();
	private static final Map<VisualProperty<Double>, VisualProperty<CyCustomGraphics>> SIZE_TO_CG = new HashMap<>();

	public static final VisualProperty<ObjectPosition> NODE_LABEL_POSITION = new ObjectPositionVisualProperty(ObjectPositionImpl.DEFAULT_POSITION, "NODE_LABEL_POSITION", "Node Label Position", CyNode.class);

	// Range object for custom graphics.
	private static final CustomGraphicsRange CG_RANGE = new CustomGraphicsRange();
	
	public static final int numCustomGraphics = 9;
	
	public static final VisualProperty<Visualizable> NODE_CUSTOMPAINT_1 = new DefaultVisualizableVisualProperty("NODE_CUSTOMPAINT_1", "Node Custom Paint 1", CyNode.class);
	public static final VisualProperty<Visualizable> NODE_CUSTOMPAINT_2 = new DefaultVisualizableVisualProperty("NODE_CUSTOMPAINT_2", "Node Custom Paint 2", CyNode.class);
	public static final VisualProperty<Visualizable> NODE_CUSTOMPAINT_3 = new DefaultVisualizableVisualProperty("NODE_CUSTOMPAINT_3", "Node Custom Paint 3", CyNode.class);
	public static final VisualProperty<Visualizable> NODE_CUSTOMPAINT_4 = new DefaultVisualizableVisualProperty("NODE_CUSTOMPAINT_4", "Node Custom Paint 4", CyNode.class);
	public static final VisualProperty<Visualizable> NODE_CUSTOMPAINT_5 = new DefaultVisualizableVisualProperty("NODE_CUSTOMPAINT_5", "Node Custom Paint 5", CyNode.class);
	public static final VisualProperty<Visualizable> NODE_CUSTOMPAINT_6 = new DefaultVisualizableVisualProperty("NODE_CUSTOMPAINT_6", "Node Custom Paint 6", CyNode.class);
	public static final VisualProperty<Visualizable> NODE_CUSTOMPAINT_7 = new DefaultVisualizableVisualProperty("NODE_CUSTOMPAINT_7", "Node Custom Paint 7", CyNode.class);
	public static final VisualProperty<Visualizable> NODE_CUSTOMPAINT_8 = new DefaultVisualizableVisualProperty("NODE_CUSTOMPAINT_8", "Node Custom Paint 8", CyNode.class);
	public static final VisualProperty<Visualizable> NODE_CUSTOMPAINT_9 = new DefaultVisualizableVisualProperty("NODE_CUSTOMPAINT_9", "Node Custom Paint 9", CyNode.class);

	public static final VisualProperty<Double> NODE_CUSTOMGRAPHICS_SIZE_1 = new DoubleVisualProperty(0.0, NONE_ZERO_POSITIVE_DOUBLE_RANGE, "NODE_CUSTOMGRAPHICS_SIZE_1", "Node Image/Chart Size 1", CyNode.class);
	public static final VisualProperty<Double> NODE_CUSTOMGRAPHICS_SIZE_2 = new DoubleVisualProperty(0.0, NONE_ZERO_POSITIVE_DOUBLE_RANGE, "NODE_CUSTOMGRAPHICS_SIZE_2", "Node Image/Chart Size 2", CyNode.class);
	public static final VisualProperty<Double> NODE_CUSTOMGRAPHICS_SIZE_3 = new DoubleVisualProperty(0.0, NONE_ZERO_POSITIVE_DOUBLE_RANGE, "NODE_CUSTOMGRAPHICS_SIZE_3", "Node Image/Chart Size 3", CyNode.class);
	public static final VisualProperty<Double> NODE_CUSTOMGRAPHICS_SIZE_4 = new DoubleVisualProperty(0.0, NONE_ZERO_POSITIVE_DOUBLE_RANGE, "NODE_CUSTOMGRAPHICS_SIZE_4", "Node Image/Chart Size 4", CyNode.class);
	public static final VisualProperty<Double> NODE_CUSTOMGRAPHICS_SIZE_5 = new DoubleVisualProperty(0.0, NONE_ZERO_POSITIVE_DOUBLE_RANGE, "NODE_CUSTOMGRAPHICS_SIZE_5", "Node Image/Chart Size 5", CyNode.class);
	public static final VisualProperty<Double> NODE_CUSTOMGRAPHICS_SIZE_6 = new DoubleVisualProperty(0.0, NONE_ZERO_POSITIVE_DOUBLE_RANGE, "NODE_CUSTOMGRAPHICS_SIZE_6", "Node Image/Chart Size 6", CyNode.class);
	public static final VisualProperty<Double> NODE_CUSTOMGRAPHICS_SIZE_7 = new DoubleVisualProperty(0.0, NONE_ZERO_POSITIVE_DOUBLE_RANGE, "NODE_CUSTOMGRAPHICS_SIZE_7", "Node Image/Chart Size 7", CyNode.class);
	public static final VisualProperty<Double> NODE_CUSTOMGRAPHICS_SIZE_8 = new DoubleVisualProperty(0.0, NONE_ZERO_POSITIVE_DOUBLE_RANGE, "NODE_CUSTOMGRAPHICS_SIZE_8", "Node Image/Chart Size 8", CyNode.class);
	public static final VisualProperty<Double> NODE_CUSTOMGRAPHICS_SIZE_9 = new DoubleVisualProperty(0.0, NONE_ZERO_POSITIVE_DOUBLE_RANGE, "NODE_CUSTOMGRAPHICS_SIZE_9", "Node Image/Chart Size 9", CyNode.class);

	public static final VisualProperty<CyCustomGraphics> NODE_CUSTOMGRAPHICS_1 = new CustomGraphicsVisualProperty(NullCustomGraphics.getNullObject(), CG_RANGE, "NODE_CUSTOMGRAPHICS_1", "Node Image/Chart 1", CyNode.class);
	public static final VisualProperty<CyCustomGraphics> NODE_CUSTOMGRAPHICS_2 = new CustomGraphicsVisualProperty(NullCustomGraphics.getNullObject(), CG_RANGE, "NODE_CUSTOMGRAPHICS_2", "Node Image/Chart 2", CyNode.class);
	public static final VisualProperty<CyCustomGraphics> NODE_CUSTOMGRAPHICS_3 = new CustomGraphicsVisualProperty(NullCustomGraphics.getNullObject(), CG_RANGE, "NODE_CUSTOMGRAPHICS_3", "Node Image/Chart 3", CyNode.class);
	public static final VisualProperty<CyCustomGraphics> NODE_CUSTOMGRAPHICS_4 = new CustomGraphicsVisualProperty(NullCustomGraphics.getNullObject(), CG_RANGE, "NODE_CUSTOMGRAPHICS_4", "Node Image/Chart 4", CyNode.class);
	public static final VisualProperty<CyCustomGraphics> NODE_CUSTOMGRAPHICS_5 = new CustomGraphicsVisualProperty(NullCustomGraphics.getNullObject(), CG_RANGE, "NODE_CUSTOMGRAPHICS_5", "Node Image/Chart 5", CyNode.class);
	public static final VisualProperty<CyCustomGraphics> NODE_CUSTOMGRAPHICS_6 = new CustomGraphicsVisualProperty(NullCustomGraphics.getNullObject(), CG_RANGE, "NODE_CUSTOMGRAPHICS_6", "Node Image/Chart 6", CyNode.class);
	public static final VisualProperty<CyCustomGraphics> NODE_CUSTOMGRAPHICS_7 = new CustomGraphicsVisualProperty(NullCustomGraphics.getNullObject(), CG_RANGE, "NODE_CUSTOMGRAPHICS_7", "Node Image/Chart 7", CyNode.class);
	public static final VisualProperty<CyCustomGraphics> NODE_CUSTOMGRAPHICS_8 = new CustomGraphicsVisualProperty(NullCustomGraphics.getNullObject(), CG_RANGE, "NODE_CUSTOMGRAPHICS_8", "Node Image/Chart 8", CyNode.class);
	public static final VisualProperty<CyCustomGraphics> NODE_CUSTOMGRAPHICS_9 = new CustomGraphicsVisualProperty(NullCustomGraphics.getNullObject(), CG_RANGE, "NODE_CUSTOMGRAPHICS_9", "Node Image/Chart 9", CyNode.class);

	// Location of custom graphics
	public static final VisualProperty<ObjectPosition> NODE_CUSTOMGRAPHICS_POSITION_1 = new ObjectPositionVisualProperty(ObjectPositionImpl.DEFAULT_POSITION, "NODE_CUSTOMGRAPHICS_POSITION_1", "Node Image/Chart Position 1", CyNode.class);
	public static final VisualProperty<ObjectPosition> NODE_CUSTOMGRAPHICS_POSITION_2 = new ObjectPositionVisualProperty(ObjectPositionImpl.DEFAULT_POSITION, "NODE_CUSTOMGRAPHICS_POSITION_2", "Node Image/Chart Position 2", CyNode.class);
	public static final VisualProperty<ObjectPosition> NODE_CUSTOMGRAPHICS_POSITION_3 = new ObjectPositionVisualProperty(ObjectPositionImpl.DEFAULT_POSITION, "NODE_CUSTOMGRAPHICS_POSITION_3", "Node Image/Chart Position 3", CyNode.class);
	public static final VisualProperty<ObjectPosition> NODE_CUSTOMGRAPHICS_POSITION_4 = new ObjectPositionVisualProperty(ObjectPositionImpl.DEFAULT_POSITION, "NODE_CUSTOMGRAPHICS_POSITION_4", "Node Image/Chart Position 4", CyNode.class);
	public static final VisualProperty<ObjectPosition> NODE_CUSTOMGRAPHICS_POSITION_5 = new ObjectPositionVisualProperty(ObjectPositionImpl.DEFAULT_POSITION, "NODE_CUSTOMGRAPHICS_POSITION_5", "Node Image/Chart Position 5", CyNode.class);
	public static final VisualProperty<ObjectPosition> NODE_CUSTOMGRAPHICS_POSITION_6 = new ObjectPositionVisualProperty(ObjectPositionImpl.DEFAULT_POSITION, "NODE_CUSTOMGRAPHICS_POSITION_6", "Node Image/Chart Position 6", CyNode.class);
	public static final VisualProperty<ObjectPosition> NODE_CUSTOMGRAPHICS_POSITION_7 = new ObjectPositionVisualProperty(ObjectPositionImpl.DEFAULT_POSITION, "NODE_CUSTOMGRAPHICS_POSITION_7", "Node Image/Chart Position 7", CyNode.class);
	public static final VisualProperty<ObjectPosition> NODE_CUSTOMGRAPHICS_POSITION_8 = new ObjectPositionVisualProperty(ObjectPositionImpl.DEFAULT_POSITION, "NODE_CUSTOMGRAPHICS_POSITION_8", "Node Image/Chart Position 8", CyNode.class);
	public static final VisualProperty<ObjectPosition> NODE_CUSTOMGRAPHICS_POSITION_9 = new ObjectPositionVisualProperty(ObjectPositionImpl.DEFAULT_POSITION, "NODE_CUSTOMGRAPHICS_POSITION_9", "Node Image/Chart Position 9", CyNode.class);
	
	// Ding-local line types
	private static final ZZLineType ZIGZAG = new ZZLineType("Zigzag", "ZIGZAG", new ZigzagStroke(1.0f));
	private static final ZZLineType SINEWAVE = new ZZLineType("Sinewave", "SINEWAVE", new SineWaveStroke(1.0f));
	private static final ZZLineType VERTICAL_SLASH = new ZZLineType("Vertical Slash", "VERTICAL_SLASH", new VerticalSlashStroke(1.0f, PipeStroke.Type.VERTICAL));
	private static final ZZLineType FORWARD_SLASH = new ZZLineType("Forward Slash", "FORWARD_SLASH", new ForwardSlashStroke(1.0f, PipeStroke.Type.FORWARD));
	private static final ZZLineType BACKWARD_SLASH = new ZZLineType("Backward Slash", "BACKWARD_SLASH", new BackwardSlashStroke(1.0f, PipeStroke.Type.BACKWARD));
	private static final ZZLineType PARALLEL_LINES = new ZZLineType("Parallel Lines", "PARALLEL_LINES", new ParallelStroke(1.0f));
	private static final ZZLineType CONTIGUOUS_ARROW = new ZZLineType("Contiguous Arrow", "CONTIGUOUS_ARROW", new ContiguousArrowStroke(1.0f));
	private static final ZZLineType SEPARATE_ARROW = new ZZLineType("Separate Arrow", "SEPARATE_ARROW", new SeparateArrowStroke(1.0f));

	// For marquee or marching ants animations.  Not sure what the
	// right number of these
	private static final ZZLineType MARQUEE_DASH = new ZZLineType("Marquee Dash", "MARQUEE_DASH", new AnimatedLongDashStroke(1.0f,0.0f));
	private static final ZZLineType MARQUEE_EQUAL = new ZZLineType("Marquee Equal Dash", "MARQUEE_EQUAL", new AnimatedEqualDashStroke(1.0f,0.0f));
	private static final ZZLineType MARQUEE_DASH_DOT = new ZZLineType("Marquee Dash Dot", "MARQUEE_DASH_DOT", new AnimatedDashDotStroke(1.0f, 0.0f));
	
	public static final VisualProperty<NullDataType> ROOT = new NullVisualProperty( "ZZ_ROOT", "ZZ Rendering Engine Root Visual Property");
	
	private final Set<VisualProperty<?>> unsupportedProps = new HashSet<>();
	
	static 
	{
		CG.add(NODE_CUSTOMGRAPHICS_1);
		CG.add(NODE_CUSTOMGRAPHICS_2);
		CG.add(NODE_CUSTOMGRAPHICS_3);
		CG.add(NODE_CUSTOMGRAPHICS_4);
		CG.add(NODE_CUSTOMGRAPHICS_5);
		CG.add(NODE_CUSTOMGRAPHICS_6);
		CG.add(NODE_CUSTOMGRAPHICS_7);
		CG.add(NODE_CUSTOMGRAPHICS_8);
		CG.add(NODE_CUSTOMGRAPHICS_9);
		
		CG_POSITIONS.add(NODE_CUSTOMGRAPHICS_POSITION_1);
		CG_POSITIONS.add(NODE_CUSTOMGRAPHICS_POSITION_2);
		CG_POSITIONS.add(NODE_CUSTOMGRAPHICS_POSITION_3);
		CG_POSITIONS.add(NODE_CUSTOMGRAPHICS_POSITION_4);
		CG_POSITIONS.add(NODE_CUSTOMGRAPHICS_POSITION_5);
		CG_POSITIONS.add(NODE_CUSTOMGRAPHICS_POSITION_6);
		CG_POSITIONS.add(NODE_CUSTOMGRAPHICS_POSITION_7);
		CG_POSITIONS.add(NODE_CUSTOMGRAPHICS_POSITION_8);
		CG_POSITIONS.add(NODE_CUSTOMGRAPHICS_POSITION_9);

		CG_SIZE.add(NODE_CUSTOMGRAPHICS_SIZE_1);
		CG_SIZE.add(NODE_CUSTOMGRAPHICS_SIZE_2);
		CG_SIZE.add(NODE_CUSTOMGRAPHICS_SIZE_3);
		CG_SIZE.add(NODE_CUSTOMGRAPHICS_SIZE_4);
		CG_SIZE.add(NODE_CUSTOMGRAPHICS_SIZE_5);
		CG_SIZE.add(NODE_CUSTOMGRAPHICS_SIZE_6);
		CG_SIZE.add(NODE_CUSTOMGRAPHICS_SIZE_7);
		CG_SIZE.add(NODE_CUSTOMGRAPHICS_SIZE_8);
		CG_SIZE.add(NODE_CUSTOMGRAPHICS_SIZE_9);

		CG_TO_SIZE.put(NODE_CUSTOMGRAPHICS_1, NODE_CUSTOMGRAPHICS_SIZE_1);
		CG_TO_SIZE.put(NODE_CUSTOMGRAPHICS_2, NODE_CUSTOMGRAPHICS_SIZE_2);
		CG_TO_SIZE.put(NODE_CUSTOMGRAPHICS_3, NODE_CUSTOMGRAPHICS_SIZE_3);
		CG_TO_SIZE.put(NODE_CUSTOMGRAPHICS_4, NODE_CUSTOMGRAPHICS_SIZE_4);
		CG_TO_SIZE.put(NODE_CUSTOMGRAPHICS_5, NODE_CUSTOMGRAPHICS_SIZE_5);
		CG_TO_SIZE.put(NODE_CUSTOMGRAPHICS_6, NODE_CUSTOMGRAPHICS_SIZE_6);
		CG_TO_SIZE.put(NODE_CUSTOMGRAPHICS_7, NODE_CUSTOMGRAPHICS_SIZE_7);
		CG_TO_SIZE.put(NODE_CUSTOMGRAPHICS_8, NODE_CUSTOMGRAPHICS_SIZE_8);
		CG_TO_SIZE.put(NODE_CUSTOMGRAPHICS_9, NODE_CUSTOMGRAPHICS_SIZE_9);

		CG_TO_POSITION.put(NODE_CUSTOMGRAPHICS_1, NODE_CUSTOMGRAPHICS_POSITION_1);
		CG_TO_POSITION.put(NODE_CUSTOMGRAPHICS_2, NODE_CUSTOMGRAPHICS_POSITION_2);
		CG_TO_POSITION.put(NODE_CUSTOMGRAPHICS_3, NODE_CUSTOMGRAPHICS_POSITION_3);
		CG_TO_POSITION.put(NODE_CUSTOMGRAPHICS_4, NODE_CUSTOMGRAPHICS_POSITION_4);
		CG_TO_POSITION.put(NODE_CUSTOMGRAPHICS_5, NODE_CUSTOMGRAPHICS_POSITION_5);
		CG_TO_POSITION.put(NODE_CUSTOMGRAPHICS_6, NODE_CUSTOMGRAPHICS_POSITION_6);
		CG_TO_POSITION.put(NODE_CUSTOMGRAPHICS_7, NODE_CUSTOMGRAPHICS_POSITION_7);
		CG_TO_POSITION.put(NODE_CUSTOMGRAPHICS_8, NODE_CUSTOMGRAPHICS_POSITION_8);
		CG_TO_POSITION.put(NODE_CUSTOMGRAPHICS_9, NODE_CUSTOMGRAPHICS_POSITION_9);
		
		SIZE_TO_CG.put(NODE_CUSTOMGRAPHICS_SIZE_1, NODE_CUSTOMGRAPHICS_1);
		SIZE_TO_CG.put(NODE_CUSTOMGRAPHICS_SIZE_2, NODE_CUSTOMGRAPHICS_2);
		SIZE_TO_CG.put(NODE_CUSTOMGRAPHICS_SIZE_3, NODE_CUSTOMGRAPHICS_3);
		SIZE_TO_CG.put(NODE_CUSTOMGRAPHICS_SIZE_4, NODE_CUSTOMGRAPHICS_4);
		SIZE_TO_CG.put(NODE_CUSTOMGRAPHICS_SIZE_5, NODE_CUSTOMGRAPHICS_5);
		SIZE_TO_CG.put(NODE_CUSTOMGRAPHICS_SIZE_6, NODE_CUSTOMGRAPHICS_6);
		SIZE_TO_CG.put(NODE_CUSTOMGRAPHICS_SIZE_7, NODE_CUSTOMGRAPHICS_7);
		SIZE_TO_CG.put(NODE_CUSTOMGRAPHICS_SIZE_8, NODE_CUSTOMGRAPHICS_8);
		SIZE_TO_CG.put(NODE_CUSTOMGRAPHICS_SIZE_9, NODE_CUSTOMGRAPHICS_9);
	}
	
	public ZZVisualLexicon(final CustomGraphicsManager manager) 
	{
		super(ROOT);
		
		// Add line types.
		/*((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(ZIGZAG);
		((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(BACKWARD_SLASH);
		((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(CONTIGUOUS_ARROW);
		((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(FORWARD_SLASH);
		((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(PARALLEL_LINES);
		((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(SEPARATE_ARROW);
		((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(SINEWAVE);
		((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(VERTICAL_SLASH);

		((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(MARQUEE_DASH);
		((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(MARQUEE_EQUAL);
		((DiscreteRange<LineType>) EDGE_LINE_TYPE.getRange()).addRangeValue(MARQUEE_DASH_DOT);*/
		
		CG_RANGE.setManager(manager);
		
		addVisualPropertyNodes();
		initUnsupportedProps();
		createLookupMap();
	}
	
	private void initUnsupportedProps() 
	{
		unsupportedProps.add(BasicVisualLexicon.NODE_TRANSPARENCY);
		unsupportedProps.add(BasicVisualLexicon.NODE_BORDER_TRANSPARENCY);
		unsupportedProps.add(BasicVisualLexicon.NODE_LABEL_TRANSPARENCY);
		unsupportedProps.add(BasicVisualLexicon.NODE_DEPTH);
		
		
		unsupportedProps.add(BasicVisualLexicon.EDGE_TRANSPARENCY);
		unsupportedProps.add(BasicVisualLexicon.EDGE_LABEL_TRANSPARENCY);
		unsupportedProps.add(BasicVisualLexicon.EDGE_BEND);
	}
	
	private void addVisualPropertyNodes()
	{		
		addVisualProperty(NODE_LABEL_POSITION, NODE);
		
		// Parent of Custom Graphics related
		addVisualProperty(NODE_CUSTOMPAINT_1, NODE_PAINT);
		addVisualProperty(NODE_CUSTOMPAINT_2, NODE_PAINT);
		addVisualProperty(NODE_CUSTOMPAINT_3, NODE_PAINT);
		addVisualProperty(NODE_CUSTOMPAINT_4, NODE_PAINT);
		addVisualProperty(NODE_CUSTOMPAINT_5, NODE_PAINT);
		addVisualProperty(NODE_CUSTOMPAINT_6, NODE_PAINT);
		addVisualProperty(NODE_CUSTOMPAINT_7, NODE_PAINT);
		addVisualProperty(NODE_CUSTOMPAINT_8, NODE_PAINT);
		addVisualProperty(NODE_CUSTOMPAINT_9, NODE_PAINT);

		// Custom Graphics. Currently Cytoscape supports 9 objects/node.
		addVisualProperty(NODE_CUSTOMGRAPHICS_1, NODE_CUSTOMPAINT_1);
		addVisualProperty(NODE_CUSTOMGRAPHICS_2, NODE_CUSTOMPAINT_2);
		addVisualProperty(NODE_CUSTOMGRAPHICS_3, NODE_CUSTOMPAINT_3);
		addVisualProperty(NODE_CUSTOMGRAPHICS_4, NODE_CUSTOMPAINT_4);
		addVisualProperty(NODE_CUSTOMGRAPHICS_5, NODE_CUSTOMPAINT_5);
		addVisualProperty(NODE_CUSTOMGRAPHICS_6, NODE_CUSTOMPAINT_6);
		addVisualProperty(NODE_CUSTOMGRAPHICS_7, NODE_CUSTOMPAINT_7);
		addVisualProperty(NODE_CUSTOMGRAPHICS_8, NODE_CUSTOMPAINT_8);
		addVisualProperty(NODE_CUSTOMGRAPHICS_9, NODE_CUSTOMPAINT_9);

		addVisualProperty(NODE_CUSTOMGRAPHICS_SIZE_1, NODE_SIZE);
		addVisualProperty(NODE_CUSTOMGRAPHICS_SIZE_2, NODE_SIZE);
		addVisualProperty(NODE_CUSTOMGRAPHICS_SIZE_3, NODE_SIZE);
		addVisualProperty(NODE_CUSTOMGRAPHICS_SIZE_4, NODE_SIZE);
		addVisualProperty(NODE_CUSTOMGRAPHICS_SIZE_5, NODE_SIZE);
		addVisualProperty(NODE_CUSTOMGRAPHICS_SIZE_6, NODE_SIZE);
		addVisualProperty(NODE_CUSTOMGRAPHICS_SIZE_7, NODE_SIZE);
		addVisualProperty(NODE_CUSTOMGRAPHICS_SIZE_8, NODE_SIZE);
		addVisualProperty(NODE_CUSTOMGRAPHICS_SIZE_9, NODE_SIZE);

		// These are children of NODE_CUSTOMGRAPHICS.
		addVisualProperty(NODE_CUSTOMGRAPHICS_POSITION_1, NODE_CUSTOMPAINT_1);
		addVisualProperty(NODE_CUSTOMGRAPHICS_POSITION_2, NODE_CUSTOMPAINT_2);
		addVisualProperty(NODE_CUSTOMGRAPHICS_POSITION_3, NODE_CUSTOMPAINT_3);
		addVisualProperty(NODE_CUSTOMGRAPHICS_POSITION_4, NODE_CUSTOMPAINT_4);
		addVisualProperty(NODE_CUSTOMGRAPHICS_POSITION_5, NODE_CUSTOMPAINT_5);
		addVisualProperty(NODE_CUSTOMGRAPHICS_POSITION_6, NODE_CUSTOMPAINT_6);
		addVisualProperty(NODE_CUSTOMGRAPHICS_POSITION_7, NODE_CUSTOMPAINT_7);
		addVisualProperty(NODE_CUSTOMGRAPHICS_POSITION_8, NODE_CUSTOMPAINT_8);
		addVisualProperty(NODE_CUSTOMGRAPHICS_POSITION_9, NODE_CUSTOMPAINT_9);
	}
	
	@Override
	public boolean isSupported(VisualProperty<?> vp) 
	{
		return !unsupportedProps.contains(vp) && super.isSupported(vp);
	}

	private void createLookupMap() 
	{
		// 2.x VizMap Properties:
		addIdentifierMapping(CyNode.class, "nodeCustomGraphics1", NODE_CUSTOMGRAPHICS_1);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphics2", NODE_CUSTOMGRAPHICS_2);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphics3", NODE_CUSTOMGRAPHICS_3);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphics4", NODE_CUSTOMGRAPHICS_4);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphics5", NODE_CUSTOMGRAPHICS_5);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphics6", NODE_CUSTOMGRAPHICS_6);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphics7", NODE_CUSTOMGRAPHICS_7);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphics8", NODE_CUSTOMGRAPHICS_8);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphics9", NODE_CUSTOMGRAPHICS_9);
		
		addIdentifierMapping(CyNode.class, "nodeCustomGraphicsPosition1", NODE_CUSTOMGRAPHICS_POSITION_1);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphicsPosition2", NODE_CUSTOMGRAPHICS_POSITION_2);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphicsPosition3", NODE_CUSTOMGRAPHICS_POSITION_3);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphicsPosition4", NODE_CUSTOMGRAPHICS_POSITION_4);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphicsPosition5", NODE_CUSTOMGRAPHICS_POSITION_5);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphicsPosition6", NODE_CUSTOMGRAPHICS_POSITION_6);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphicsPosition7", NODE_CUSTOMGRAPHICS_POSITION_7);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphicsPosition8", NODE_CUSTOMGRAPHICS_POSITION_8);
		addIdentifierMapping(CyNode.class, "nodeCustomGraphicsPosition9", NODE_CUSTOMGRAPHICS_POSITION_9);
		
		addIdentifierMapping(CyNode.class, "nodeLabelPosition", NODE_LABEL_POSITION);
	}

	// Related to custom graphics:
	
	static List<VisualProperty<ObjectPosition>> getCGPositionVP() 
	{
		return CG_POSITIONS;
	}

	static List<VisualProperty<Double>> getCGSizeVP() 
	{
		return CG_SIZE;
	}

	public static VisualProperty<Double> getAssociatedCGSizeVP(VisualProperty<?> cgVP) 
	{
		return CG_TO_SIZE.get(cgVP);
	}

	public static VisualProperty<ObjectPosition> getAssociatedCGPositionVP(VisualProperty<?> cgVP) 
	{
		return CG_TO_POSITION.get(cgVP);
	}
	
	@SuppressWarnings("rawtypes")
	public static VisualProperty<CyCustomGraphics> getAssociatedCGVisualProperties(VisualProperty<Double> cgSizeVP) 
	{
		return SIZE_TO_CG.get(cgSizeVP);
	}
	
	@SuppressWarnings("rawtypes")
	public static List<VisualProperty<CyCustomGraphics>> getCGVisualProperties() 
	{
		return CG;
	}
}
