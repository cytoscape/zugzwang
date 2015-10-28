package org.cytoscape.zugzwang.internal.rendering;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import org.cytoscape.zugzwang.internal.algebra.*;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;


/**
 * Rectangle manager maintains buffers for rectangle parameters 
 * both on host and device, and keeps them in sync in case of changes.
 *
 */
public class ZZRectangleManager
{
	// For thread synchronization
	private final Object m_sync = new Object();
	
	private final GL4 gl;
	
	// Parameters for the default texture that is used when
	// a rectangle doesn't specify a texture, e. g. because
	// it hasn't been drawn yet.
	private long defaultTexture;
	private short defaultTextureWidth, defaultTextureHeight;
	
	// Number of currently managed rectangles, and currently available buffer capacity
	private int elements = 0, capacity = 0;
	
	// Information in device buffers can't have gaps. Those would
	// occur if any rectangle but the last was removed. Rectangle manager
	// keeps track of the available positions in the allocated buffers, and
	// distributes them so no gaps occur.
	private int[] indicesMap, reverseMap;
	private final Queue<Integer> availableIndices = new LinkedList<>();
	
	// Host buffers
	private float[] hostPosition;
	private short[] hostSize;
	private short[] hostOffset;
	private long[] hostTexture;
	
	// Mapped device buffers
	private ByteBuffer devicePosition;
	private ByteBuffer deviceSize;
	private ByteBuffer deviceOffset;
	private ByteBuffer deviceTexture;
	
	// Device buffer handles
	private final int[] attributeBuffers = new int[4];
	private final int[] vertexArray = new int[1];
	
	// Update flags, prevent unnecessary updates of buffers
	// that haven't been altered.
	private boolean needsUpdatePosition = false;
	private boolean needsUpdateSize = false;
	private boolean needsUpdateOffset = false;
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
		hostOffset = new short[initialCapacity * 2];
		hostTexture = new long[initialCapacity];
		indicesMap = new int[initialCapacity];
		reverseMap = new int[initialCapacity];
		
		for (int i = 0; i < initialCapacity; i++)
			availableIndices.add(i);
		
		createBuffers();
	}
	
	/**
	 * Deletes old device buffers, allocates new ones, and copies
	 * data from host buffers to them.
	 * 
	 * @param newCapacity Desired new buffer capacity
	 */
	public void resize(int newCapacity)
	{
		int oldCapacity = capacity;
		if (oldCapacity == newCapacity)
			return;
		
		deleteBuffers();
		
		float[] newHostPosition = new float[newCapacity * 3];
		short[] newHostSize = new short[newCapacity * 4];
		short[] newHostOffset = new short[newCapacity * 2];
		long[] newHostTexture = new long[newCapacity];
		int[] newReverseMap = new int[newCapacity];
		
		for (int i = 0; i < Math.min(newCapacity, oldCapacity) * 3; i++)
			newHostPosition[i] = hostPosition[i];
		for (int i = 0; i < Math.min(newCapacity, oldCapacity) * 4; i++)
			newHostSize[i] = hostSize[i];
		for (int i = 0; i < Math.min(newCapacity, oldCapacity) * 2; i++)
			newHostOffset[i] = hostOffset[i];
		for (int i = 0; i < Math.min(newCapacity, oldCapacity); i++)
			newHostTexture[i] = hostTexture[i];
		for (int i = 0; i < Math.min(newCapacity, oldCapacity); i++)
			newReverseMap[i] = reverseMap[i];
				
		hostPosition = newHostPosition;
		hostSize = newHostSize;
		hostOffset = newHostOffset;
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
	
	/**
	 * Allocates device buffers of the currently set capacity.
	 */
	private void createBuffers()
	{
		gl.glGenBuffers(4, attributeBuffers, 0);
		gl.glGenVertexArrays(1, vertexArray, 0);
		gl.glBindVertexArray(vertexArray[0]);
		
		// Rectangle center position
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
		
		// Rectangle size, texture size
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
		
		// Rectangle offset within the focal plane
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[2]);
		{
			gl.glBufferStorage(GL4.GL_ARRAY_BUFFER, capacity * 2 * GLBuffers.SIZEOF_INT, null, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT);
			deviceOffset = gl.glMapBufferRange(GL4.GL_ARRAY_BUFFER, 0, capacity * 2 * GLBuffers.SIZEOF_INT, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_PERSISTENT_BIT | GL4.GL_MAP_FLUSH_EXPLICIT_BIT);
			
			for (int i = 0; i < elements * 2; i++)
				deviceOffset.putInt((int)hostOffset[i]);
			deviceOffset.rewind();
			gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 2 * GLBuffers.SIZEOF_INT);
			
			gl.glVertexAttribIPointer(2, 2, GL4.GL_UNSIGNED_INT, 2 * GLBuffers.SIZEOF_INT, 0);
			gl.glEnableVertexAttribArray(2);
		}
		
		// Bindless texture ID
		gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, attributeBuffers[3]);
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
	
	/**
	 * Deletes device buffers.
	 */
	private void deleteBuffers()
	{
		gl.glDeleteBuffers(4, attributeBuffers, 0);
		gl.glDeleteVertexArrays(1, vertexArray, 0);
		
		devicePosition = null;
		deviceSize = null;
		deviceOffset = null;
		deviceTexture = null;
	}
	
	/**
	 * Creates a new rectangle and assigns it the next vacant
	 * position in the buffers. The rectangle will use the 
	 * default texture. If current buffer capacity is
	 * insufficient, the capacity will be increased by 50 %.
	 * 
	 * @param center Rectangle center position
	 * @param width Rectangle width
	 * @param height Rectangle height
	 * @return Created rectangle
	 */
	public ZZRectangle createRectangle(Vector3 center, short width, short height, short offsetX, short offsetY)
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
		
		newRectangle = new ZZRectangle(this, index, center, width, height, offsetX, offsetY);	// This also sets the texture to default for this rect
		
		return newRectangle;
	}
	
	/**
	 * Deletes a rectangle and moves the last rectangle in 
	 * the buffer up to its position to fill the gap, making 
	 * the last position vacant.
	 * 
	 * @param rect Rectangle to be deleted
	 */
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
			devicePosition.putFloat((denseToFill * 3) * GLBuffers.SIZEOF_FLOAT, hostPosition[elements * 3]);
			hostPosition[denseToFill * 3 + 1] = hostPosition[elements * 3 + 1];
			devicePosition.putFloat((denseToFill * 3 + 1) * GLBuffers.SIZEOF_FLOAT, hostPosition[elements * 3 + 1]);
			hostPosition[denseToFill * 3 + 2] = hostPosition[elements * 3 + 2];
			devicePosition.putFloat((denseToFill * 3 + 2) * GLBuffers.SIZEOF_FLOAT, hostPosition[elements * 3 + 2]);
			needsUpdatePosition = true;

			hostSize[denseToFill * 4] = hostSize[elements * 4];
			deviceSize.putInt((denseToFill * 4) * GLBuffers.SIZEOF_INT, hostSize[elements * 4]);
			hostSize[denseToFill * 4 + 1] = hostSize[elements * 4 + 1];
			deviceSize.putInt((denseToFill * 4 + 1) * GLBuffers.SIZEOF_INT, hostSize[elements * 4 + 1]);
			hostSize[denseToFill * 4 + 2] = hostSize[elements * 4 + 2];
			deviceSize.putInt((denseToFill * 4 + 2) * GLBuffers.SIZEOF_INT, hostSize[elements * 4 + 2]);
			hostSize[denseToFill * 4 + 3] = hostSize[elements * 4 + 3];
			deviceSize.putInt((denseToFill * 4 + 3) * GLBuffers.SIZEOF_INT, hostSize[elements * 4 + 3]);
			needsUpdateSize = true;

			hostOffset[denseToFill * 2] = hostOffset[elements * 2];
			deviceOffset.putInt((denseToFill * 2) * GLBuffers.SIZEOF_INT, hostOffset[elements * 2]);
			hostOffset[denseToFill * 2 + 1] = hostOffset[elements * 2 + 1];
			deviceOffset.putInt((denseToFill * 2 + 1) * GLBuffers.SIZEOF_INT, hostOffset[elements * 2 + 1]);
			needsUpdateOffset = true;
			
			hostTexture[denseToFill] = hostTexture[elements];
			needsUpdateTexture = true;
			
			if (elements < capacity / 2)
				resize(Math.max(10, capacity * 2 / 3));
			
			rect.dispose(gl);	// This deletes the texture if it exists.
		}
	}
	
	/**
	 * Sets a rectangle's X coordinate.
	 * 
	 * @param id Rectangle ID
	 * @param value New X coordinate
	 */
	public void setPositionX(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 3;
			if (hostPosition[address] != value)
			{
				hostPosition[address] = value;
				devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
				needsUpdatePosition = true;
			}
		}
	}
	
	/**
	 * Sets a rectangle's Y coordinate.
	 * 
	 * @param id Rectangle ID
	 * @param value New Y coordinate
	 */
	public void setPositionY(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 3 + 1;
			if (hostPosition[address] != value)
			{
				hostPosition[address] = value;
				devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
				needsUpdatePosition = true;
			}
		}
	}
	
	/**
	 * Sets a rectangle's Z coordinate.
	 * 
	 * @param id Rectangle ID
	 * @param value New Z coordinate
	 */
	public void setPositionZ(int id, float value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 3 + 2;
			if (hostPosition[address] != value)
			{
				hostPosition[address] = value;
				devicePosition.putFloat(address * GLBuffers.SIZEOF_FLOAT, value);
				needsUpdatePosition = true;
			}
		}
	}
	
	/**
	 * Sets a rectangle's width.
	 * 
	 * @param id Rectangle ID
	 * @param value New width
	 */
	public void setSizeX(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 4;
			if (hostSize[address] != value)
			{
				hostSize[address] = value;
				deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
				needsUpdateSize = true;
			}
		}
	}

	/**
	 * Sets a rectangle's height.
	 * 
	 * @param id Rectangle ID
	 * @param value New height
	 */
	public void setSizeY(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 4 + 1;
			if (hostSize[address] != value)
			{
				hostSize[address] = value;
				deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
				needsUpdateSize = true;
			}
		}
	}

	/**
	 * Sets a rectangle's texture width.
	 * 
	 * @param id Rectangle ID
	 * @param value New texture width
	 */
	public void setTextureSizeU(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 4 + 2;
			if (hostSize[address] != value)
			{
				hostSize[address] = value;
				deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
				needsUpdateSize = true;
			}
		}
	}

	/**
	 * Sets a rectangle's texture height.
	 * 
	 * @param id Rectangle ID
	 * @param value New texture height
	 */
	public void setTextureSizeV(int id, short value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id] * 4 + 3;
			if (hostSize[address] != value)
			{
				hostSize[address] = value;
				deviceSize.putInt(address * GLBuffers.SIZEOF_INT, value);
				needsUpdateSize = true;
			}
		}
	}
	
	/**
	 * Sets a rectangle's offset along the X axis within the focal plane.
	 * 
	 * @param id Rectangle ID
	 * @param value New offset along X
	 */
	public void setOffsetX(int id, short value)
	{
		synchronized (m_sync)
		{
			int address = indicesMap[id] * 2;
			if (hostOffset[address] != value)
			{
				hostOffset[address] = value;
				deviceOffset.putInt(address * GLBuffers.SIZEOF_INT, value);
				needsUpdateOffset = true;
			}
		}
	}
	
	/**
	 * Sets a rectangle's offset along the Y axis within the focal plane.
	 * 
	 * @param id Rectangle ID
	 * @param value New offset along Y
	 */
	public void setOffsetY(int id, short value)
	{
		synchronized (m_sync)
		{
			int address = indicesMap[id] * 2 + 1;
			if (hostOffset[address] != value)
			{
				hostOffset[address] = value;
				deviceOffset.putInt(address * GLBuffers.SIZEOF_INT, value);
				needsUpdateOffset = true;
			}
		}
	}

	/**
	 * Sets the rectangle's bindless texture ID.
	 * 
	 * @param id Rectangle ID
	 * @param value New texture ID
	 */
	public void setTexture(int id, long value)
	{
		synchronized (m_sync) 
		{
			int address = indicesMap[id];
			if (hostTexture[address] != value)
			{
				hostTexture[address] = value;
				needsUpdateTexture = true;
			}
		}
	}

	/**
	 * Resets the rectangle's bindless texture ID to the default one.
	 * 
	 * @param id Rectangle ID
	 */
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
	
	/**
	 * Sets a bindless texture ID as the new default texture,
	 * and updates all rectangles using the default texture
	 * with the new ID.
	 * 
	 * @param newDefault New default bindless texture ID
	 * @param newWidth New texture width
	 * @param newHeight New texture height
	 */
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
	
	/**
	 * Pushes all changes in host buffers to device buffers.
	 */
	public void flush()
	{
		synchronized (m_sync) 
		{		
			if (elements == 0)
				return;
			
			if (needsUpdatePosition)
			{
				gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[0]);
				gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 3 * GLBuffers.SIZEOF_FLOAT);
				needsUpdatePosition = false;
			}
			
			if (needsUpdateSize)
			{
				gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[1]);
				gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 4 * GLBuffers.SIZEOF_INT);
				needsUpdateSize = false;
			}
			
			if (needsUpdateOffset)
			{
				gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, attributeBuffers[2]);
				gl.glFlushMappedBufferRange(GL4.GL_ARRAY_BUFFER, 0, elements * 2 * GLBuffers.SIZEOF_INT);
				needsUpdateOffset = false;
			}
	
			if (needsUpdateTexture)
			{				
				gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, attributeBuffers[3]);
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
	
	/**
	 * Gets the vertex array handle associated with this rectangle manager.
	 * 
	 * @return Vertex array handle
	 */
	public int getVertexArray()
	{
		return vertexArray[0];
	}
	
	/**
	 * Gets the number of rectangles currently managed.
	 * 
	 * @return Number of rectangles
	 */
	public int size()
	{
		return elements;
	}
	
	/**
	 * Gets the current buffer capacity.
	 * 
	 * @return Buffer capacity
	 */
	public int capacity()
	{
		return capacity;
	}
	
	/**
	 * Binds vertex array and texture ID storage buffer to the current GL context.
	 */
	public void bind()
	{
		gl.glBindVertexArray(getVertexArray());
		
		gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, attributeBuffers[3]);
		gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 0, attributeBuffers[3]);
	}

	/**
	 * Frees all associated device resources.
	 */
	public void dispose()
	{
		deleteBuffers();
	}
}