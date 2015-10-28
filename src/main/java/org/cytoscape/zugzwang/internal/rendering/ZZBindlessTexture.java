package org.cytoscape.zugzwang.internal.rendering;

import java.nio.ByteBuffer;

import org.cytoscape.zugzwang.internal.tools.GLMemoryLimit;

import com.jogamp.opengl.GL4;

/**
 * Manages an OpenGL bindless texture object. This requires at least GL 4.4 to work.
 * The texture will be kept resident throughout the existence of this object,
 * until the dispose method is called.
 */
public class ZZBindlessTexture 
{
	private static Object m_sync = new Object();
	
	private final GL4 gl;						// Current GL context
	private long id = -1;						// 64 bit texture ID used in shaders
	private final int[] glTexture = new int[1];	// Texture handle
	
	private final short width, height;
	
	public ZZBindlessTexture(GL4 gl, byte[] data, short width, short height, int storageFormat, int pixelFormat, int componentFormat)
	{
		synchronized (m_sync)
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
			
			//if (width == 1)
				//System.out.println("Created texture: " + width + " x " + height);
		}
	}
	
	/**
	 * Gets the texture ID used to refer to it in shaders.
	 * 
	 * @return 64 bit texture ID
	 */
	public long getID()
	{
		return id;
	}
	
	/**
	 * Gets the texture width.
	 * 
	 * @return Texture width
	 */
	public short getWidth()
	{
		return width;
	}
	
	/**
	 * Gets the texture height.
	 * 
	 * @return Texture height
	 */
	public short getHeight()
	{
		return height;
	}
	
	/**
	 * Makes the bindless texture non-resident and deleted the corresponding object.
	 */
	public void dispose()
	{
		synchronized (m_sync)
		{
			if (this.id == -1)
				return;
			
			gl.glMakeTextureHandleNonResidentARB(this.id);
			gl.glDeleteTextures(1, glTexture, 0);
			this.id = -1;
			//GLMemoryLimit.freeMemory((long)width * (long)height * (long)4);
			
			//System.out.println("Deleted texture: " + width + " x " + height);
		}
	}
	
	@Override
	public void finalize()
	{		
		try
		{
			dispose();
			super.finalize();
		}
		catch (Throwable exc) {}
	}
}