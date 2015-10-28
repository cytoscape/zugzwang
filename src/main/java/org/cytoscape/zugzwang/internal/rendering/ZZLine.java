package org.cytoscape.zugzwang.internal.rendering;

import java.nio.ByteBuffer;

import org.cytoscape.zugzwang.internal.algebra.*;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.math.FloatUtil;
/**
 * Represents a textured line that is always oriented
 * in parallel to the focal plane in its width, but not
 * necessarily in its length.
 *
 */
public class ZZLine 
{
	// Thread synchronization
	private final Object m_sync = new Object();

	// Sets of lines are managed centrally. The manager maintains
	// an index for each primitive that determines its position in
	// continuous data buffers.
	private final ZZLineManager manager;
	public final int index;

	// Geometry description
	private Vector3 source = new Vector3(), target = new Vector3();	
	private short width;

	// Texture description
	private short textureWidth = 1, textureHeight = 1;
	private byte[] textureHost = new byte[4];
	private ZZBindlessTexture texture;
	
	public ZZLine(ZZLineManager manager, int index, Vector3 source, Vector3 target, short width)
	{
		this.manager = manager;
		this.index = index;
		
		setWidth(width);
		setSource(source);
		setTarget(target);
		manager.setTextureToDefault(index);
	}

	/**
	 * Sets the line's width.
	 * 
	 * @param width New width
	 */
	public void setWidth(short width)
	{
		synchronized (m_sync)
		{
			this.width = width;
			
			manager.setWidth(index, width);
		}
	}
	
	/**
	 * Sets the line's source position
	 * 
	 * @param source New source position
	 */
	public void setSource(Vector3 source)
	{
		synchronized (m_sync)
		{
			this.source = source;

			manager.setSourceX(index, source.x);
			manager.setSourceY(index, source.y);
			manager.setSourceZ(index, source.z);
		}
	}

	/**
	 * Sets the line's target position
	 * 
	 * @param target New target position
	 */
	public void setTarget(Vector3 target)
	{
		synchronized (m_sync)
		{
			this.target = target;

			manager.setTargetX(index, target.x);
			manager.setTargetY(index, target.y);
			manager.setTargetZ(index, target.z);
		}
	}
	
	/**
	 * Sets the line's texture size.
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
	 * the line manager with the new bindless texture ID.
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
	 * Gets the line's 4 corners transformed into camera space.
	 * 
	 * @param viewMatrix Current camera view matrix
	 * @return An array with the corners, ordered as LU, LB, RB, RU
	 */
	public Vector4[] getCorners(Matrix4 viewMatrix)
	{
		Vector4[] corners = new Vector4[1];

		corners[0] = new Vector4();
		
		return corners;
	}

	/**
	 * Checks if the line intersects with the camera frustum.
	 * 
	 * @param viewMatrix Camera view matrix
	 * @param projMatrix Camera projection matrix
	 * @param halfScreen Screen dimensions divided by 2
	 * @param outExtent Projected line width and length in pixels will be written here
	 * @return True if line and frustum intersect, false otherwise
	 */
	public boolean isInFrustum(Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen, Vector2 outExtent)
	{
		return true;
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