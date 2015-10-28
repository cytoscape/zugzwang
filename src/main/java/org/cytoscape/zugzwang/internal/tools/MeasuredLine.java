package org.cytoscape.zugzwang.internal.tools;


/**
 * A simple class to hold the width and height of a given string in terms
 * of specific fonts, rendering contexts, etc.. 
 */
class MeasuredLine 
{
	private final String line;
	private final float width;
	private final float height;
	private final float offsetY;

	public MeasuredLine(final String line, final float width, final float height, final float offsetY) 
	{
		this.line = line;
		this.width = width;
		this.height = height;
		this.offsetY = offsetY;
	}

	public String getLine() 
	{
		return line;
	}

	public float getWidth() 
	{
		return width;
	}

	public float getHeight() 
	{
		return height;
	}
	
	public float getOffsetY()
	{
		return offsetY;
	}

	public String toString() 
	{
		return "'" + line + "'  w:" + width + " h:" + height;
	}
}
