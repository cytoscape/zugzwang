package org.cytoscape.zugzwang.internal.tools;


import com.jogamp.opengl.GL4;

/**
 * Helper class to keep track of the current memory usage. Objects that allocate
 * device memory should call tryGetMemory before the allocation and only proceed
 * with allocation if it returns true. Upon resource deallocation, freeMemory should
 * be called.
 */
public class GLMemoryLimit
{
	private static Object m_sync = new Object();// For thread synchronization
	private static long maxMemory = 1 << 28;	// 256 MB, tryGetMemory will return false if this limit is exceeded
	private static long currentMemory = 0;		// Current memory usage in byte
	
	/**
	 * Checks if there is enough memory left within the defined memory limit.
	 * If there is, it will add the requested amount of memory to the counter
	 * for currently used memory.
	 * 
	 * @param size Desired amount of memory
	 * @return True if enough memory is available, false otherwise
	 */
	public static boolean tryGetMemory(long requested, long freed)
	{
		synchronized (m_sync)
		{			
			if (maxMemory - currentMemory + freed > requested)
			{
				currentMemory += requested - freed;
				//System.out.println((currentMemory >> 20) + " after malloc.");
				return true;
			}
			
			return false;
		}
	}
	
	/**
	 * Decreases the counter for currently used memory by the specified amount.
	 * 
	 * @param size Amount of memory to be freed
	 */
	public static void freeMemory(long size)
	{
		synchronized (m_sync)
		{
			currentMemory = Math.max(0, currentMemory - size);
			//System.out.println((currentMemory >> 20) + " MB after free.");
		}
	}
	
	/**
	 * Gets the amount of memory currently registered as occupied.
	 * 
	 * NOTE: This only represents memory allocated and freed through
	 * tryGetMemory and freeMemory, which does not have to correspond
	 * to the actual amount of free memory on the device.
	 * 
	 * @return Occupied memory in bytes
	 */
	public static long getCurrentMemory()
	{
		return currentMemory;
	}
	
	/**
	 * Gets the maximum amount of memory this class is 
	 * allowed to distribute for allocations.
	 * 
	 * @return Memory limit in bytes
	 */
	public static long getMaxMemory()
	{
		return maxMemory;
	}
	
	public static void setMaxMemory(long value)
	{
		synchronized (m_sync)
		{
			maxMemory = value;
		}
	}
}