package org.cytoscape.zugzwang.internal.tools;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

import java.util.List;
import java.util.ArrayList;


/**
 * A class that takes a String and a number of rendering parameters and then 
 * splits the text into MeasuredLines based on newline characters and whether 
 * line length is otherwise greater than the specified label width limit.
 */
public class MeasuredLineCreator 
{
	private float maxLineWidth;
	private float totalHeight;

	private final float labelWidth;
	private final String[] rawLines;
	private final List<MeasuredLine> measuredLines;
	
	public MeasuredLineCreator(final String text, final Font font, final FontRenderContext fontContext, final float labelWidth) 
	{
		this.labelWidth = labelWidth;
		this.measuredLines = new ArrayList<MeasuredLine>(1);
		
		if (text.length() > 0)
		{
			this.rawLines = text.split("\n"); 
	
			createMeasuredLines(font, fontContext);
		}
		else
		{
			maxLineWidth = 0;
			totalHeight = 0;
			rawLines = new String[0];
		}
	}

	/**
	 * Calculates the bounds of a single string.
	 */
	private Rectangle2D calcBounds(final String s, final Font font, final FontRenderContext fontContext) 
	{
		return font.getStringBounds(s, fontContext);
	}

	/**
	 * Splits the raw lines according to how many lines are present and if any of 
	 * the lines are too long.  Recalculates the maxLineWidth and totalHeight based
	 * on the new lines created.
	 */
	private void createMeasuredLines(final Font font, final FontRenderContext fontContext) 
	{

		// There's only one line, let's see if it's short enough.
		if (rawLines.length == 1) 
		{
			Rectangle2D bounds = calcBounds(rawLines[0], font, fontContext);
			float width = (float)bounds.getWidth();
			
			// Single line, and either short enough to fit into labelWidth, or a single word.
			if (width <= labelWidth || rawLines[0].indexOf(' ') < 0)
			{
				maxLineWidth = width;
				totalHeight = (float)bounds.getHeight();
				
				measuredLines.add(new MeasuredLine(rawLines[0], maxLineWidth, totalHeight, (float)bounds.getY()));
				return;
			}
		}

		// There are multiple and/or longer-than-allowed lines.   
		// Process each of them. Also update overall widths and heights 
		// as those may change. 
		totalHeight = 0;
		maxLineWidth = 0;
		float spaceWidth = (float)font.getStringBounds(" ", fontContext).getWidth();
	
		for (String line : rawLines) 
		{
			float currentWidth = 0;
			float wordWidth = 0; 
			float wordHeight = 0; 
			float wordOffsetY = 0;
			StringBuilder currentLine = new StringBuilder();

			// Split each line based on the space char and then build up
			// new lines by concatenating the words together into a new
			// new line that is within the specified length. 
			String[] words = line.split(" ");
			for (String w : words) 
			{
				String word = w;	
				Rectangle2D bounds = calcBounds(word, font, fontContext);
				wordWidth = (float)bounds.getWidth();
				wordHeight = (float)bounds.getHeight();
				wordOffsetY = Math.min(wordOffsetY, (float)bounds.getY());

				// If the current line width plus the new word
				// width is >= than the label width save the line
				if (currentWidth + wordWidth >= labelWidth) 
				{
					// only write the string if something is there
					if (currentWidth > 0) 
					{
						// - spaceWidth because there's an extra space at the end
						measuredLines.add(new MeasuredLine(currentLine.toString(), currentWidth - spaceWidth, wordHeight, wordOffsetY));
						
						maxLineWidth = Math.max(maxLineWidth, currentWidth - spaceWidth);
						totalHeight += wordHeight;

						currentLine.delete(0, currentLine.length());	// Start building a new line.
					}
					
					// If the word itself is >= labelWidth, make the word itself a new line.
					if (wordWidth >= labelWidth)
					{
						measuredLines.add(new MeasuredLine(word, wordWidth, wordHeight, (float)bounds.getY()));
						
						maxLineWidth = Math.max(maxLineWidth, wordWidth);
						totalHeight += wordHeight;

						currentWidth = 0;
						wordOffsetY = 0;
					} 
					else // Otherwise use the word as the beginning of a new line.
					{
						currentLine.append(word + " ");
						currentWidth = wordWidth + spaceWidth;
						wordOffsetY = (float)bounds.getY();
					}				
				} 
				else // Otherwise append the word to the line.
				{
					currentLine.append(word + " ");
					currentWidth += wordWidth + spaceWidth;
				}
			}

			// Add the last line if there's anything there.
			if (currentWidth > 0) 
			{
				measuredLines.add(new MeasuredLine(currentLine.toString(), currentWidth - spaceWidth, wordHeight, wordOffsetY));

				maxLineWidth = Math.max(maxLineWidth, currentWidth - spaceWidth);
				totalHeight += wordHeight;
			}
		}
	}

	/**
	 * @return the maximum line width among the lines found in the input text.
	 */
	public float getMaxLineWidth() 
	{
		return maxLineWidth;
	}

	/**
	 * @return the total combined height of all of the lines found in the input text.
	 */
	public float getTotalHeight() 
	{
		return totalHeight;
	}

	/**
	 * @return a list of MeasuredLine objects created from the input text.
	 */
	public List<MeasuredLine> getMeasuredLines() 
	{
		return measuredLines;
	}
	
	public static MeasuredLineCreator Empty()
	{
		return new MeasuredLineCreator("", null, null, 1);
	}
}
