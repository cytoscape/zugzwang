package org.cytoscape.zugzwang.internal.rendering;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.nio.ByteBuffer;

import org.cytoscape.zugzwang.internal.algebra.*;
import org.cytoscape.zugzwang.internal.tools.GLMemoryLimit;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.math.FloatUtil;

/**
 * Represents a textured rectangle that is always oriented
 * in parallel to the focal plane.
 *
 */
public class ZZRectangle 
{
	// Thread synchronization
	private final Object m_sync = new Object();
	
	// Sets of rectangles are managed centrally. The manager maintains
	// an index for each primitive that determines its position in
	// continuous data buffers.
	private final ZZRectangleManager manager;
	public final int index;
	
	// Geometry description
	private Vector3 center = new Vector3();	
	private short width, height;
	private short offsetX, offsetY;
	
	// Texture description
	private short textureWidth = 1, textureHeight = 1;
	private byte[] textureHost = new byte[4];
	private ZZBindlessTexture texture;
	
	public ZZRectangle(ZZRectangleManager manager, int index, 
					   Vector3 center, 
					   short width, short height, 
					   short offsetX, short offsetY)
	{
		this.manager = manager;
		this.index = index;
		
		setSize(width, height);
		setCenter(center);
		manager.setTextureToDefault(index);
	}
	
	/**
	 * Sets the rectangle's size.
	 * 
	 * @param width New width
	 * @param height New height
	 */
	public void setSize(short width, short height)
	{
		synchronized (m_sync)
		{
			this.width = width;
			this.height = height;
			
			manager.setSizeX(index, width);
			manager.setSizeY(index, height);
		}
	}
	
	/**
	 * Sets the rectangle's position.
	 * 
	 * @param center New center
	 */
	public void setCenter(Vector3 center)
	{
		synchronized (m_sync)
		{
			this.center = center;

			manager.setPositionX(index, center.x);
			manager.setPositionY(index, center.y);
			manager.setPositionZ(index, center.z);
		}
	}
	
	/**
	 * Sets the rectangle's offset from its center within the focal plane.
	 * 
	 * @param offsetX Offset along the X axis
	 * @param offsetY Offset along the Y axis
	 */
	public void setOffset(short offsetX, short offsetY)
	{
		synchronized (m_sync)
		{
			this.offsetX = offsetX;
			this.offsetY = offsetY;

			manager.setOffsetX(index, offsetX);
			manager.setOffsetY(index, offsetY);
		}
	}
	
	/**
	 * Sets the rectangle's texture size.
	 * 
	 * @param width New texture width
	 * @param height New texture height
	 */
	public void setTextureSize(short width, short height)
	{
		synchronized (m_sync)
		{
			this.textureWidth = width;
			this.textureHeight = height;
			textureHost = new byte[width * height * 4];
			
			manager.setTextureSizeU(index, width);
			manager.setTextureSizeV(index, height);
		}
	}
	
	/**
	 * Gets current texture width.
	 * 
	 * @return Texture width
	 */
	public short getTextureSizeU()
	{
		return this.textureWidth;
	}
	
	/**
	 * Gets current texture height.
	 * 
	 * @return Texture height
	 */
	public short getTextureSizeV()
	{
		return this.textureHeight;
	}
	
	/**
	 * Gets texture pixel component value at the given position.
	 * 
	 * @param i Pixel component position
	 * @return Pixel component value at position i
	 */
	public byte getTextureElement(int i)
	{
		return textureHost[i];
	}
	
	/**
	 * Sets texture pixel component value at the given position.
	 * 
	 * @param i Pixel component position
	 * @param value New pixel component value
	 */
	public void setTextureElement(int i, byte value)
	{
		textureHost[i] = value;
	}
	
	/**
	 * Sets texture data and parameters.
	 * 
	 * @param data New texture data
	 * @param width	New texture width
	 * @param height New texture height
	 */
	public void setTexture(byte[] data, short width, short height)
	{
		synchronized (m_sync)
		{
			this.textureWidth = width;
			this.textureHeight = height;
			this.textureHost = data;
			
			manager.setTextureSizeU(index, width);
			manager.setTextureSizeV(index, height);
		}
	}
	
	/**
	 * Sets texture data and parameters from host image.
	 * 
	 * @param img Java's BufferedImage containing the texture
	 */
	public void setTexture(BufferedImage img)
	{
		// Remember pixel format is ABGR, rectangle shaders already consider that.
		Raster r = img.getData();					
		byte[] texture = ((DataBufferByte)r.getDataBuffer()).getData();
		setTexture(texture, (short)img.getWidth(), (short)img.getHeight());
	}
	
	/**
	 * Checks if the current texture has been uploaded to the device.
	 * 
	 * @return True if a device handle exists for the texture, false otherwise
	 */
	public boolean isOnDevice()
	{
		return texture != null;
	}
	
	/**
	 * Uploads the current texture to the device, and updates
	 * the rectangle manager with the new bindless texture ID.
	 * 
	 * @param gl Current GL context
	 */
	public void putOnDevice(GL4 gl)
	{
		synchronized (m_sync)
		{
			if (isOnDevice())
				texture.dispose();
			
			texture = new ZZBindlessTexture(gl, textureHost, textureWidth, textureHeight, GL4.GL_RGBA8, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE);
			
			manager.setTexture(index, texture.getID());
		}
	}
	
	/**
	 * Discards the current texture on the device if it exists.
	 * 
	 * @param gl Current GL context
	 */
	public void discardOnDevice(GL4 gl)
	{
		synchronized (m_sync)
		{
			if (!isOnDevice())
				return;
			
			texture.dispose();
			texture = null;
			
			manager.setTextureToDefault(index);
		}
	}
	
	/**
	 * Gets the rectangle's 4 corners transformed into camera space.
	 * 
	 * @param viewMatrix Current camera view matrix
	 * @return An array with the corners, ordered as LU, LB, RU, RB
	 */
	public Vector4[] getCorners(Matrix4 viewMatrix)
	{
		Vector4[] corners = new Vector4[4];
		
		synchronized (m_sync)
		{
			Vector4 viewCenter = Vector4.matrixMult(viewMatrix, new Vector4(center, 1.0f));
			
			corners[0] = new Vector4(viewCenter.x - (float)(width / 2) + (float)offsetX,
									 viewCenter.y - (float)(height / 2) + (float)offsetY,
									 viewCenter.z,
									 1.0f);
			
			corners[1] = new Vector4(viewCenter.x - (float)(width / 2) + (float)offsetX,
									 viewCenter.y + (float)((height + 1) / 2) + (float)offsetY,
									 viewCenter.z,
									 1.0f);
			
			corners[2] = new Vector4(viewCenter.x + (float)((width + 1) / 2) + (float)offsetX,
									 viewCenter.y - (float)(height / 2) + (float)offsetY,
									 viewCenter.z,
									 1.0f);
			
			corners[3] = new Vector4(viewCenter.x + (float)((width + 1) / 2) + (float)offsetX,
									 viewCenter.y + (float)((height + 1) / 2) + (float)offsetY,
									 viewCenter.z,
									 1.0f);
		}
		
		return corners;
	}
	
	public Vector4 getBounds2D()
	{
		return new Vector4(-(float)(width / 2),
						   -(float)(height / 2),
						    (float)((width + 1) / 2),
						    (float)((height + 1) / 2));
	}
	
	/**
	 * Checks if the rectangle intersects with the camera frustum.
	 * 
	 * @param viewMatrix Camera view matrix
	 * @param projMatrix Camera projection matrix
	 * @param halfScreen Screen dimensions divided by 2
	 * @param outExtent Projected rectangle size in pixels will be written here
	 * @return True if rectangle and frustum intersect, false otherwise
	 */
	public boolean isInFrustum(Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen, Vector2 outExtent)
	{
		Vector4 center4 = new Vector4(center, 1.0f);
		center4 = Vector4.matrixMult(viewMatrix, center4);
		float leftWidth = (float)(width / 2), topHeight = (float)(height / 2);
		float rightWidth = (float)((width + 1) / 2), bottomHeight = (float)((height + 1) / 2);
		
		boolean result = false;
		
		Vector4 leftTop = Vector4.matrixMult(projMatrix, new Vector4(center4.x - leftWidth + offsetX, center4.y - bottomHeight + offsetY, center4.z, center4.w)).homogeneousToCartesian();
		if (FloatUtil.abs(leftTop.x) <= 1.0f && FloatUtil.abs(leftTop.y) <= 1.0f)
			result = true;
		
		Vector4 rightBottom = Vector4.matrixMult(projMatrix, new Vector4(center4.x + rightWidth + offsetX, center4.y + topHeight + offsetY, center4.z, center4.w)).homogeneousToCartesian();
		if (FloatUtil.abs(rightBottom.x) <= 1.0f && FloatUtil.abs(rightBottom.y) <= 1.0f)
			result = true;

		// Store projected size in pixels
		outExtent.x = FloatUtil.abs((float)Math.floor(rightBottom.x * halfScreen.x) - (float)Math.floor(leftTop.x * halfScreen.x));
		outExtent.y = FloatUtil.abs((float)Math.floor(rightBottom.y * halfScreen.y) - (float)Math.floor(leftTop.y * halfScreen.y));
		
		// Calculated the extent, good to exit if screen presence has been established
		if (result)
			return true;
		
		// Other two corners can be constructed from the first two
		if (FloatUtil.abs(rightBottom.x) <= 1.0f && FloatUtil.abs(leftTop.y) <= 1.0f)
			return true;
		
		if (FloatUtil.abs(leftTop.x) <= 1.0f && FloatUtil.abs(rightBottom.y) <= 1.0f)
			return true;
		
		if (Math.signum(leftTop.x) == -Math.signum(rightBottom.x))
		{
			if (Math.signum(leftTop.y) == -Math.signum(rightBottom.y))	// Everyone is on opposite outsides
				return true;
			else if (FloatUtil.abs(leftTop.y) <= 1.0f || FloatUtil.abs(rightBottom.y) <= 1.0f)	// Horizontally on opposite outsides, vertically inside
				return true;
		}
		else if (Math.signum(leftTop.y) == -Math.signum(rightBottom.y))
		{
			if (FloatUtil.abs(leftTop.x) <= 1.0f || FloatUtil.abs(rightBottom.x) <= 1.0f)	// Vertically on opposite outsides, horizontally inside
				return true;
		}
		
		return result;
	}
	
	/**
	 * Gets the amount of device memory in bytes occupied by the current texture.
	 * @return Device texture size in bytes, 0 if no texture has been uploaded
	 */
	public long getOccupiedTextureMemory()
	{
		synchronized (m_sync)
		{
			if (texture != null)
				return texture.getWidth() * texture.getHeight() * (long)4;
			else
				return 0;
		}
	}
	
	/**
	 * Frees all device resources associated with this rectangle.
	 * 
	 * @param gl Current GL context
	 */
	public void dispose(GL4 gl)
	{
		if (!isOnDevice())
			return;
		
		texture.dispose();
		texture = null;
	}
}