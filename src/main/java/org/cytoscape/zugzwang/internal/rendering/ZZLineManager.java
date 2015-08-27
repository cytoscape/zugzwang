package org.cytoscape.zugzwang.internal.rendering;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import org.cytoscape.zugzwang.internal.algebra.*;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;


public class ZZLineManager
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
	
	public ZZLineManager(GL4 gl, int initialCapacity, long defaultTexture, short defaultTextureWidth, short defaultTextureHeight)
	{
		this.gl = gl;
		this.defaultTexture = defaultTexture;
		this.defaultTextureWidth = defaultTextureWidth;
		this.defaultTextureHeight = defaultTextureHeight;
		this.capacity = initialCapacity;
		
		hostPosition = new float[initialCapacity * 3 * 2];	// Source & target vec3
		hostSize = new short[initialCapacity * 3 * 2];			// Width & textureUV
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
		
		float[] newHostPosition = new float[newCapacity * 3 * 2];
		short[] newHostSize = new short[newCapacity * 3 * 2];
		long[] newHostTexture = new long[newCapacity];
		int[] newReverseMap = new int[newCapacity];
		
		for (int i = 0; i < Math.min(newCapacity, oldCapacity) * 3 * 2; i++)
			newHostPosition[i] = hostPosition[i];
		for (int i = 0; i < Math.min(newCapacity, oldCapacity) * 3 * 2; i++)
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
			gl.glBufferStorage(GL4.GL_ARRAY_BUFFER, capacity * 3 * 2 * GLBuffers.SIZEOF_FLOAT, null, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT);
			devicePosition = gl.glMapBufferRange(GL4.GL_ARRAY_BUFFER, 0, capacity * 3 * 2 * GLBuffers.SIZEOF_FLOAT, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT | GL4.GL_MAP_FLUSH_EXPLICIT_BIT);
			
			for (int i = 0; i < elements * 3 * 2; i++)
				devicePosition.putFloat(hostPosition[i]);
			devicePosition.rewind();
			gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 3 * 2 * GLBuffers.SIZEOF_FLOAT);
			
			gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 3 * GLBuffers.SIZEOF_FLOAT, 0);
			gl.glEnableVertexAttribArray(0);
		}
		
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[1]);
		{
			gl.glBufferStorage(GL4.GL_ARRAY_BUFFER, capacity * 3 * 2 * GLBuffers.SIZEOF_INT, null, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT);
			deviceSize = gl.glMapBufferRange(GL4.GL_ARRAY_BUFFER, 0, capacity * 3 * 2 * GLBuffers.SIZEOF_INT, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT | GL4.GL_MAP_FLUSH_EXPLICIT_BIT);
			
			for (int i = 0; i < elements * 3 * 2; i++)
				deviceSize.putInt((int)hostSize[i]);
			deviceSize.rewind();
			gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 3 * 2 * GLBuffers.SIZEOF_INT);
			
			gl.glVertexAttribIPointer(1, 3, GL4.GL_UNSIGNED_INT, 3 * GLBuffers.SIZEOF_INT, 0);
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
	
	public ZZLine createLine(Vector3 source, Vector3 target, short width)
	{
		ZZLine newLine;
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
		
		newLine = new ZZLine(this, index, source, target, width);	// This also sets the texture to default for this rect
		
		return newLine;
	}
	
	public void deleteLine(ZZLine line)
	{
		synchronized (m_sync) 
		{
			int oldIndex = line.index;
			availableIndices.add(oldIndex);
			
			int denseToFill = indicesMap[oldIndex];		// Position in buffer that has become vacant
			int lastIndex = reverseMap[elements - 1];	// Mapping index of last element in buffer
			reverseMap[elements - 1] = -1;				// Last element doesn't exist anymore
			reverseMap[denseToFill] = lastIndex;		// Move it to newly vacant position
			indicesMap[lastIndex] = denseToFill;			
			elements--;

			// Move data from last to vacant position
			
			hostPosition[denseToFill * 6] = hostPosition[elements * 6];
			hostPosition[denseToFill * 6 + 1] = hostPosition[elements * 6 + 1];
			hostPosition[denseToFill * 6 + 2] = hostPosition[elements * 6 + 2];
			hostPosition[denseToFill * 6 + 3] = hostPosition[elements * 6 + 3];
			hostPosition[denseToFill * 6 + 4] = hostPosition[elements * 6 + 4];
			hostPosition[denseToFill * 6 + 5] = hostPosition[elements * 6 + 5];
			needsUpdatePosition = true;

			hostSize[denseToFill * 6] = hostSize[elements * 6];
			hostSize[denseToFill * 6 + 1] = hostSize[elements * 6 + 1];
			hostSize[denseToFill * 6 + 2] = hostSize[elements * 6 + 2];
			hostSize[denseToFill * 6 + 3] = hostSize[elements * 6 + 3];
			hostSize[denseToFill * 6 + 4] = hostSize[elements * 6 + 4];
			hostSize[denseToFill * 6 + 5] = hostSize[elements * 6 + 5];
			needsUpdateSize = true;
			
			hostTexture[denseToFill] = hostTexture[elements];
			needsUpdateTexture = true;
			
			if (elements < capacity / 2)
				resize(Math.max(10, capacity * 2 / 3));
		}
			
		line.dispose(gl);
	}
	
	public void setSourceX(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 6;
			hostPosition[address] = value;
			devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
			needsUpdatePosition = true;
		}
	}
	
	public void setSourceY(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 6 + 1;
			hostPosition[address] = value;
			devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
			needsUpdatePosition = true;
		}
	}
	
	public void setSourceZ(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 6 + 2;
			hostPosition[address] = value;
			devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
			needsUpdatePosition = true;
		}
	}
	
	public void setTargetX(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 6 + 3;
			hostPosition[address] = value;
			devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
			needsUpdatePosition = true;
		}
	}
	
	public void setTargetY(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 6 + 4;
			hostPosition[address] = value;
			devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
			needsUpdatePosition = true;
		}
	}
	
	public void setTargetZ(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 6 + 5;
			hostPosition[address] = value;
			devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
			needsUpdatePosition = true;
		}
	}
	
	public void setWidth(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 6;
			hostSize[address] = value;
			deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
			hostSize[address + 3] = value;
			deviceSize.putInt((address + 3) * GLBuffers.SIZEOF_INT, value);
			needsUpdateSize = true;
		}
	}
	
	public void setTextureSizeU(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 6 + 1;
			hostSize[address] = value;
			deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
			hostSize[address + 3] = value;
			deviceSize.putInt((address + 3) * GLBuffers.SIZEOF_INT, value);
			needsUpdateSize = true;
		}
	}
	
	public void setTextureSizeV(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 6 + 2;
			hostSize[address] = value;
			deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
			hostSize[address + 3] = value;
			deviceSize.putInt((address + 3) * GLBuffers.SIZEOF_INT, value);
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
			
			hostSize[address * 6 + 1] = defaultTextureWidth;
			hostSize[address * 6 + 2] = defaultTextureHeight;
			hostSize[address * 6 + 4] = defaultTextureWidth;
			hostSize[address * 6 + 5] = defaultTextureHeight;
			deviceSize.putInt((address * 6 + 1) * GLBuffers.SIZEOF_INT, defaultTextureWidth);
			deviceSize.putInt((address * 6 + 2) * GLBuffers.SIZEOF_INT, defaultTextureHeight);
			deviceSize.putInt((address * 6 + 4) * GLBuffers.SIZEOF_INT, defaultTextureWidth);
			deviceSize.putInt((address * 6 + 5) * GLBuffers.SIZEOF_INT, defaultTextureHeight);
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
					
					hostSize[i * 6 + 1] = newWidth;
					hostSize[i * 6 + 2] = newHeight;
					hostSize[i * 6 + 4] = newWidth;
					hostSize[i * 6 + 5] = newHeight;
					deviceSize.putInt((i * 6 + 1) * GLBuffers.SIZEOF_INT, newWidth);
					deviceSize.putInt((i * 6 + 2) * GLBuffers.SIZEOF_INT, newHeight);
					deviceSize.putInt((i * 6 + 4) * GLBuffers.SIZEOF_INT, newWidth);
					deviceSize.putInt((i * 6 + 5) * GLBuffers.SIZEOF_INT, newHeight);
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
				gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[0]);
				gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 3 * 2 * GLBuffers.SIZEOF_FLOAT);
				needsUpdatePosition = false;
			}
			
			if (needsUpdateSize)
			{
				gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[1]);
				gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 3 * 2 * GLBuffers.SIZEOF_INT);
				needsUpdateSize = false;
			}
	
			if (needsUpdateTexture)
			{				
				gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, attributeBuffers[2]);
				{
					deviceTexture = gl.glMapBufferRange(GL4.GL_SHADER_STORAGE_BUFFER, 0, elements * 2 * GLBuffers.SIZEOF_LONG, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_INVALIDATE_RANGE_BIT);	// * 2 because of std140 layout in shader
					
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