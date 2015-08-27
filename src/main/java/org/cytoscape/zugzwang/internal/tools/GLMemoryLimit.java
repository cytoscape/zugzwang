package org.cytoscape.zugzwang.internal.tools;


import com.jogamp.opengl.GL4;

public class GLMemoryLimit
{
	private static Object m_sync = new Object();
	private static long maxMemory = 1 << 28;	// 256 MB
	private static long currentMemory = 0;
	
	public static boolean tryGetMemory(long size)
	{
		synchronized (m_sync)
		{			
			if (maxMemory - currentMemory > size)
			{
				currentMemory += size;
				//System.out.println((currentMemory >> 20) + " after malloc.");
				return true;
			}
			
			return false;
		}
	}
	
	public static void freeMemory(long size)
	{
		synchronized (m_sync)
		{
			currentMemory = Math.max(0, currentMemory - size);
			//System.out.println((currentMemory >> 20) + " after free.");
		}
	}
}