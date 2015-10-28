package org.cytoscape.zugzwang.internal.tools;


import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.StringTokenizer;

import org.cytoscape.zugzwang.internal.algebra.Vector2;
import org.cytoscape.zugzwang.internal.visualproperties.Anchor;
import org.cytoscape.zugzwang.internal.visualproperties.Justification;


public class TextRenderingUtils 
{
	public static void renderHorizontalText(final Graphics2D g, 
                                            final MeasuredLineCreator measuredText,
                                            final Font font, final FontRenderContext fontContext,
                                            final Vector2 textCenter, final Vector2 scale,
                                            final Justification textJustify, final Paint paint) 
	{
		float currHeight = 0.0f;
		float overallWidth =  measuredText.getMaxLineWidth();

		for (MeasuredLine line : measuredText.getMeasuredLines())
		{
			Vector2 center = new Vector2(0.0f, currHeight - line.getOffsetY() * scale.y);

			if (textJustify == Justification.JUSTIFY_CENTER)
				center.x = textCenter.x;
			else if (textJustify == Justification.JUSTIFY_LEFT)
				center.x = textCenter.x - 0.5f * (overallWidth - line.getWidth()) * scale.x;
			else if (textJustify == Justification.JUSTIFY_RIGHT)
				center.x = textCenter.x + 0.5f * (overallWidth - line.getWidth()) * scale.x;
			else
				throw new IllegalStateException("textJustify value unrecognized");
			center.x -= line.getWidth() * 0.5f * scale.x;

			drawTextFull(g, font, fontContext, line.getLine(), center, scale, paint);
			
			currHeight += line.getHeight() * scale.y;
		}
	}
	
	private static void drawTextFull(final Graphics2D g, 
									 final Font font, final FontRenderContext fontContext, 
									 final String text, 
									 final Vector2 position, final Vector2 scale, 
									 final Paint paint) 
	{
		g.setPaint(paint);
		g.setFont(font);
		g.drawString(text, position.x, position.y);
	}
}



