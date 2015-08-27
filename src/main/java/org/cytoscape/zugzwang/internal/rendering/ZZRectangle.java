package org.cytoscape.zugzwang.internal.rendering;

import java.nio.ByteBuffer;

import org.cytoscape.zugzwang.internal.algebra.*;
import org.cytoscape.zugzwang.internal.tools.GLMemoryLimit;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.math.FloatUtil;

public class ZZRectangle 
{
	private final Object m_sync = new Object();
	
	private final ZZRectangleManager manager;
	public final int index;
	private Vector3 center = new Vector3();
	
	private short width, height;
	
	private short textureWidth = 1, textureHeight = 1;
	private byte[] textureHost = new byte[4];
	private ZZBindlessTexture texture;
	
	public ZZRectangle(ZZRectangleManager manager, int index, Vector3 center, short width, short height)
	{
		this.manager = manager;
		this.index = index;
		
		setSize(width, height);
		setCenter(center);
		manager.setTextureToDefault(index);
	}
	
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
	
	public short getTextureSizeU()
	{
		return this.textureWidth;
	}
	
	public short getTextureSizeV()
	{
		return this.textureHeight;
	}
	
	public byte getTextureElement(int i)
	{
		return textureHost[i];
	}
	
	public void setTextureElement(int i, byte value)
	{
		textureHost[i] = value;
	}
	
	public void setTexture(byte[] data, short width, short height)
	{
		synchronized (m_sync)
		{
			GLMemoryLimit.freeMemory(textureWidth * textureHeight * (long)4);
			this.textureWidth = width;
			this.textureHeight = height;
			this.textureHost = data;
			
			manager.setTextureSizeU(index, width);
			manager.setTextureSizeV(index, height);
		}
	}
	
	public boolean isOnDevice()
	{
		return texture != null;
	}
	
	public void putOnDevice(GL4 gl)
	{
		synchronized (m_sync)
		{
			if (isOnDevice())
			{
				texture.dispose();
			}
			
			texture = new ZZBindlessTexture(gl, textureHost, textureWidth, textureHeight, GL4.GL_RGBA8, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE);
			
			manager.setTexture(index, texture.getID());
		}
	}
	
	public void discardOnDevice(GL4 gl)
	{
		synchronized (m_sync)
		{
			if (!isOnDevice())
				return;
			
			texture.dispose();
			texture = null;
			GLMemoryLimit.freeMemory(textureWidth * textureHeight * (long)4);
			
			manager.setTextureToDefault(index);
		}
	}
	
	public Vector4[] getCorners(Matrix4 viewMatrix)
	{
		Vector4[] corners = new Vector4[4];
		
		synchronized (m_sync)
		{
			Vector4 viewCenter = Vector4.MatrixMult(viewMatrix, new Vector4(center, 1.0f));
			
			corners[0] = new Vector4(viewCenter.x - (float)(width / 2),
									 viewCenter.y - (float)(height / 2),
									 viewCenter.z,
									 1.0f);
			
			corners[1] = new Vector4(viewCenter.x - (float)(width / 2),
									 viewCenter.y + (float)((height + 1) / 2),
									 viewCenter.z,
									 1.0f);
			
			corners[2] = new Vector4(viewCenter.x + (float)((width + 1) / 2),
									 viewCenter.y - (float)(height / 2),
									 viewCenter.z,
									 1.0f);
			
			corners[3] = new Vector4(viewCenter.x + (float)((width + 1) / 2),
									 viewCenter.y + (float)((height + 1) / 2),
									 viewCenter.z,
									 1.0f);
		}
		
		return corners;
	}
	
	public boolean isInFrustum(Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen, Vector2 outExtent)
	{
		Vector4 center4 = new Vector4(center, 1.0f);
		center4 = Vector4.MatrixMult(viewMatrix, center4);
		float leftWidth = (float)(width / 2), topHeight = (float)(height / 2);
		float rightWidth = (float)((width + 1) / 2), bottomHeight = (float)((height + 1) / 2);
		
		boolean result = false;
		
		Vector4 leftTop = Vector4.MatrixMult(projMatrix, new Vector4(center4.x - leftWidth, center4.y - bottomHeight, center4.z, center4.w)).HomogeneousToCartesian();
		if (FloatUtil.abs(leftTop.x) <= 1.0f && FloatUtil.abs(leftTop.y) <= 1.0f)
			result = true;
		
		Vector4 rightBottom = Vector4.MatrixMult(projMatrix, new Vector4(center4.x + rightWidth, center4.y + topHeight, center4.z, center4.w)).HomogeneousToCartesian();
		if (FloatUtil.abs(rightBottom.x) <= 1.0f && FloatUtil.abs(rightBottom.y) <= 1.0f)
			result = true;

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
	
	public void dispose(GL4 gl)
	{
		if (!isOnDevice())
			return;
		
		texture.dispose();
		texture = null;
		GLMemoryLimit.freeMemory(textureWidth * textureHeight * (long)4);
	}
}