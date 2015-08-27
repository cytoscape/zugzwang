package org.cytoscape.zugzwang.internal.rendering;

import java.nio.ByteBuffer;

import org.cytoscape.zugzwang.internal.algebra.*;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.math.FloatUtil;

public class ZZLine 
{
	private final Object m_sync = new Object();
	
	private final ZZLineManager manager;
	public final int index;
	private Vector3 source = new Vector3(), target = new Vector3();
	
	private short width;
	
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
	
	public void setWidth(short width)
	{
		synchronized (m_sync)
		{
			this.width = width;
			
			manager.setWidth(index, width);
		}
	}
	
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
				texture.dispose();
			
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
			
			manager.setTextureToDefault(index);
		}
	}
	
	public Vector4[] getCorners(Matrix4 viewMatrix)
	{
		Vector4[] corners = new Vector4[1];

		corners[0] = new Vector4();
		
		return corners;
	}
	
	public boolean isInFrustum(Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen, Vector2 outExtent)
	{
		return true;
	}
	
	public void dispose(GL4 gl)
	{
		if (!isOnDevice())
			return;
		
		texture.dispose();
		texture = null;
	}
}