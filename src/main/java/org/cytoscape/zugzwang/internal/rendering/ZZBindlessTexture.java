package org.cytoscape.zugzwang.internal.rendering;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL4;

public class ZZBindlessTexture 
{
	private final GL4 gl;
	private final long id;
	private final int[] glTexture = new int[1];
	
	private final short width, height;
	
	public ZZBindlessTexture(GL4 gl, byte[] data, short width, short height, int storageFormat, int pixelFormat, int componentFormat)
	{
		this.gl = gl;
		this.width = width;
		this.height = height;
		
		gl.glGenTextures(1, glTexture, 0);
		gl.glBindTexture(GL4.GL_TEXTURE_2D, glTexture[0]);
		{
			gl.glTexStorage2D(GL4.GL_TEXTURE_2D, 1, storageFormat, width, height);
			
			gl.glTexSubImage2D(GL4.GL_TEXTURE_2D, 0, 
							   0, 0, width, height, 
							   pixelFormat, componentFormat, 
							   ByteBuffer.wrap(data));
			
			this.id = gl.glGetTextureHandleARB(glTexture[0]);
			gl.glMakeTextureHandleResidentARB(this.id);
		}
		gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
	}
	
	public long getID()
	{
		return id;
	}
	
	public short getWidth()
	{
		return width;
	}
	
	public short getHeight()
	{
		return height;
	}
	
	public void dispose()
	{
		gl.glMakeTextureHandleNonResidentARB(this.id);
		gl.glDeleteTextures(1, glTexture, 0);
	}
}