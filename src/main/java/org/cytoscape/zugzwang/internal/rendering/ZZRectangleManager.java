package org.cytoscape.zugzwang.internal.rendering;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import org.cytoscape.zugzwang.internal.algebra.*;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;


public class ZZRectangleManager
{
	private final Object m_sync = new Object();
	
	private final GL4 gl;
	private long defaultTexture;
	private short defaultTextureWidth, defaultTextureHeight;
	
	private int elements = 0, capacity = 0;
	
	private int[] indicesMap, reverseMap;
	private final Queue<Integer> availableIndices = new LinkedList<>();
	
	private float[] hostPosition;
	private short[] hostSize;
	private long[] hostTexture;
	
	private ByteBuffer devicePosition;
	private ByteBuffer deviceSize;
	private ByteBuffer deviceTexture;
	
	private final int[] attributeBuffers = new int[3];
	private final int[] vertexArray = new int[1];
	
	private boolean needsUpdatePosition = false;
	private boolean needsUpdateSize = false;
	private boolean needsUpdateTexture = false;
	
	public ZZRectangleManager(GL4 gl, int initialCapacity, long defaultTexture, short defaultTextureWidth, short defaultTextureHeight)
	{
		this.gl = gl;
		this.defaultTexture = defaultTexture;
		this.defaultTextureWidth = defaultTextureWidth;
		this.defaultTextureHeight = defaultTextureHeight;
		this.capacity = initialCapacity;
		
		hostPosition = new float[initialCapacity * 3];
		hostSize = new short[initialCapacity * 4];
		hostTexture = new long[initialCapacity];
		indicesMap = new int[initialCapacity];
		reverseMap = new int[initialCapacity];
		
		for (int i = 0; i < initialCapacity; i++)
			availableIndices.add(i);
		
		createBuffers();
	}
	
	public void resize(int newCapacity)
	{
		int oldCapacity = capacity;
		if (oldCapacity == newCapacity)
			return;
		
		deleteBuffers();
		
		float[] newHostPosition = new float[newCapacity * 3];
		short[] newHostSize = new short[newCapacity * 4];
		long[] newHostTexture = new long[newCapacity];
		int[] newReverseMap = new int[newCapacity];
		
		for (int i = 0; i < Math.min(newCapacity, oldCapacity) * 3; i++)
			newHostPosition[i] = hostPosition[i];
		for (int i = 0; i < Math.min(newCapacity, oldCapacity) * 4; i++)
			newHostSize[i] = hostSize[i];
		for (int i = 0; i < Math.min(newCapacity, oldCapacity); i++)
			newHostTexture[i] = hostTexture[i];
		for (int i = 0; i < Math.min(newCapacity, oldCapacity); i++)
			newReverseMap[i] = reverseMap[i];
				
		hostPosition = newHostPosition;
		hostSize = newHostSize;
		hostTexture = newHostTexture;
		reverseMap = newReverseMap;
		
		capacity = newCapacity;
		
		// Can't safely decrease map size without reindexing everything, so only increase
		if (newCapacity > indicesMap.length)
		{
			// Add indices for the newly available range
			for (int i = indicesMap.length; i < newCapacity; i++)
				availableIndices.add(i);
			
			// Copy map into a bigger one
			int[] newIndicesMap = new int[newCapacity];
			for (int i = 0; i < indicesMap.length; i++)
				newIndicesMap[i] = indicesMap[i];
			indicesMap = newIndicesMap;
		}
		
		createBuffers();
	}
	
	private void createBuffers()
	{
		gl.glGenBuffers(3, attributeBuffers, 0);
		gl.glGenVertexArrays(1, vertexArray, 0);
		gl.glBindVertexArray(vertexArray[0]);
		
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[0]);
		{
			gl.glBufferStorage(GL4.GL_ARRAY_BUFFER, capacity * 3 * GLBuffers.SIZEOF_FLOAT, null, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT);
			devicePosition = gl.glMapBufferRange(GL4.GL_ARRAY_BUFFER, 0, capacity * 3 * GLBuffers.SIZEOF_FLOAT, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT | GL4.GL_MAP_FLUSH_EXPLICIT_BIT);
			
			for (int i = 0; i < elements * 3; i++)
				devicePosition.putFloat(hostPosition[i]);
			devicePosition.rewind();
			gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 3 * GLBuffers.SIZEOF_FLOAT);
			
			gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 3 * GLBuffers.SIZEOF_FLOAT, 0);
			gl.glEnableVertexAttribArray(0);
		}
		
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[1]);
		{
			gl.glBufferStorage(GL4.GL_ARRAY_BUFFER, capacity * 4 * GLBuffers.SIZEOF_INT, null, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT);
			deviceSize = gl.glMapBufferRange(GL4.GL_ARRAY_BUFFER, 0, capacity * 4 * GLBuffers.SIZEOF_INT, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT | GL4.GL_MAP_FLUSH_EXPLICIT_BIT);
			
			for (int i = 0; i < elements * 4; i++)
				deviceSize.putInt((int)hostSize[i]);
			deviceSize.rewind();
			gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 4 * GLBuffers.SIZEOF_INT);
			
			gl.glVertexAttribIPointer(1, 4, GL4.GL_UNSIGNED_INT, 4 * GLBuffers.SIZEOF_INT, 0);
			gl.glEnableVertexAttribArray(1);
		}
		
		gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, attributeBuffers[2]);
		{
			gl.glBufferStorage(GL4.GL_SHADER_STORAGE_BUFFER, capacity * 2 * GLBuffers.SIZEOF_LONG, null, GL4.GL_MAP_WRITE_BIT);		// Capacity * 2 because of std140 alignment in shader
			deviceTexture = gl.glMapBufferRange(GL4.GL_SHADER_STORAGE_BUFFER, 0, capacity * 2 * GLBuffers.SIZEOF_LONG, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_INVALIDATE_BUFFER_BIT);
			
			for (int i = 0; i < elements; i++)
			{
				deviceTexture.putLong(hostTexture[i]);
				deviceTexture.putLong(0);
			}
			deviceTexture.rewind();
			gl.glUnmapBuffer(GL4.GL_SHADER_STORAGE_BUFFER);
		}
	}
	
	private void deleteBuffers()
	{
		gl.glDeleteBuffers(3, attributeBuffers, 0);
		gl.glDeleteVertexArrays(1, vertexArray, 0);
		
		devicePosition = null;
		deviceSize = null;
		deviceTexture = null;
	}
	
	public ZZRectangle createRectangle(Vector3 center, short width, short height)
	{
		ZZRectangle newRectangle;
		int index;
		
		synchronized (m_sync) 
		{
			if (elements >= capacity)
				resize(capacity * 3 / 2);
			
			index = availableIndices.poll();
			indicesMap[index] = elements;
			reverseMap[elements] = index;
						
			elements++;
		}
		
		newRectangle = new ZZRectangle(this, index, center, width, height);	// This also sets the texture to default for this rect
		
		return newRectangle;
	}
	
	public void deleteRectangle(ZZRectangle rect)
	{
		synchronized (m_sync) 
		{
			int oldIndex = rect.index;
			availableIndices.add(oldIndex);
			
			int denseToFill = indicesMap[oldIndex];		// Position in buffer that has become vacant
			int lastIndex = reverseMap[elements - 1];	// Mapping index of last element in buffer
			reverseMap[elements - 1] = -1;				// Last element doesn't exist anymore
			reverseMap[denseToFill] = lastIndex;		// Move it to newly vacant position
			indicesMap[lastIndex] = denseToFill;			
			elements--;

			// Move data from last to vacant position
			
			hostPosition[denseToFill * 3] = hostPosition[elements * 3];
			hostPosition[denseToFill * 3 + 1] = hostPosition[elements * 3 + 1];
			hostPosition[denseToFill * 3 + 2] = hostPosition[elements * 3 + 2];
			needsUpdatePosition = true;

			hostSize[denseToFill * 4] = hostSize[elements * 4];
			hostSize[denseToFill * 4 + 1] = hostSize[elements * 4 + 1];
			hostSize[denseToFill * 4 + 2] = hostSize[elements * 4 + 2];
			hostSize[denseToFill * 4 + 3] = hostSize[elements * 4 + 3];
			needsUpdateSize = true;
			
			hostTexture[denseToFill] = hostTexture[elements];
			needsUpdateTexture = true;
			
			if (elements < capacity / 2)
				resize(Math.max(10, capacity * 2 / 3));
			
			rect.dispose(gl);
		}
	}
	
	public void setPositionX(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 3;
			hostPosition[address] = value;
			devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
			needsUpdatePosition = true;
		}
	}
	
	public void setPositionY(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 3 + 1;
			hostPosition[address] = value;
			devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
			needsUpdatePosition = true;
		}
	}
	
	public void setPositionZ(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 3 + 2;
			hostPosition[address] = value;
			devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
			needsUpdatePosition = true;
		}
	}
	
	public void setSizeX(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 4;
			hostSize[address] = value;
			deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
			needsUpdateSize = true;
		}
	}
	
	public void setSizeY(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 4 + 1;
			hostSize[address] = value;
			deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
			needsUpdateSize = true;
		}
	}
	
	public void setTextureSizeU(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 4 + 2;
			hostSize[address] = value;
			deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
			needsUpdateSize = true;
		}
	}
	
	public void setTextureSizeV(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 4 + 3;
			hostSize[address] = value;
			deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
			needsUpdateSize = true;
		}
	}
	
	public void setTexture(int id, long value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id];
			hostTexture[address] = value;
			needsUpdateTexture = true;
		}
	}
	
	public void setTextureToDefault(int id)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id];
			
			hostTexture[address] = defaultTexture;
			needsUpdateTexture = true;
			
			hostSize[address * 4 + 2] = defaultTextureWidth;
			hostSize[address * 4 + 3] = defaultTextureHeight;
			deviceSize.putInt((address * 4 + 2) * GLBuffers.SIZEOF_INT, defaultTextureWidth);
			deviceSize.putInt((address * 4 + 3) * GLBuffers.SIZEOF_INT, defaultTextureHeight);
			needsUpdateSize = true;
		}
	}
	
	public void switchDefaultTexture(long newDefault, short newWidth, short newHeight)
	{
		synchronized (m_sync)
		{
			for (int i = 0; i < elements; i++)
				if (hostTexture[i] == defaultTexture)
				{
					hostTexture[i] = newDefault;
					
					hostSize[i * 4 + 2] = newWidth;
					hostSize[i * 4 + 3] = newHeight;
					deviceSize.putInt((i * 4 + 2) * GLBuffers.SIZEOF_INT, newWidth);
					deviceSize.putInt((i * 4 + 3) * GLBuffers.SIZEOF_INT, newHeight);
				}
			needsUpdateTexture = true;
			needsUpdateSize = true;
			
			defaultTexture = newDefault;
			defaultTextureWidth = newWidth;
			defaultTextureHeight = newHeight;
		}
	}
	
	public void flush()
	{
		synchronized (m_sync) 
		{		
			if (elements == 0)
				return;
			
			if (needsUpdatePosition)
			{
				/*for (int i = 0; i < elements * 3; i++)
					devicePosition.putFloat(hostPosition[i]);
				devicePosition.rewind();*/
				
				gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[0]);
				gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 3 * GLBuffers.SIZEOF_FLOAT);
				needsUpdatePosition = false;
			}
			
			if (needsUpdateSize)
			{
				/*for (int i = 0; i < elements * 4; i++)
					deviceSize.putInt(hostSize[i]);
				deviceSize.rewind();*/
				
				gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[1]);
				gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 4 * GLBuffers.SIZEOF_INT);
				needsUpdateSize = false;
			}
	
			if (needsUpdateTexture)
			{				
				gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, attributeBuffers[2]);
				{
					deviceTexture = gl.glMapBufferRange(GL4.GL_SHADER_STORAGE_BUFFER, 0, elements * 2 * GLBuffers.SIZEOF_LONG, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_INVALIDATE_RANGE_BIT);
					
					for (int i = 0; i < elements; i++)
					{
						deviceTexture.putLong(hostTexture[i]);
						deviceTexture.putLong(0);
					}
					gl.glUnmapBuffer(GL4.GL_SHADER_STORAGE_BUFFER);
				}
				
				needsUpdateTexture = false;
			}
		}
	}
	
	public int getVertexArray()
	{
		return vertexArray[0];
	}
	
	public int size()
	{
		return elements;
	}
	
	public int capacity()
	{
		return capacity;
	}
	
	public void bind(int program)
	{
		gl.glBindVertexArray(getVertexArray());
		
		gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, attributeBuffers[2]);
		gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 0, attributeBuffers[2]);
		//gl.glShaderStorageBlockBinding(program, 0, 0);
	}
}