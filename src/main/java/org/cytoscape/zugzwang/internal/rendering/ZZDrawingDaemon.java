package org.cytoscape.zugzwang.internal.rendering;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.cytoscape.zugzwang.internal.algebra.*;

import com.jogamp.opengl.GL4;

/**
 * 
 */
public class ZZDrawingDaemon 
{
	private volatile boolean doShutdown = false;
	
	private Semaphore stateUpdateSemaphore;
	
	private final Thread[] poolState;
	private final Thread[] poolDraw;
	
	private volatile List<ZZDrawingDaemonPrimitive> forStateUpdate;
	private volatile Queue<ZZDrawingDaemonPrimitive> forDrawUpdate = new LinkedBlockingQueue<>();
	private volatile Queue<ZZDrawingDaemonPrimitive> forResourceUpdate = new LinkedBlockingQueue<>();
	
	private volatile Matrix4 viewMatrix, projMatrix;
	private volatile Vector2 halfScreen;
	private volatile GL4 gl;
	private volatile boolean updateVisualProperties;
	
	public ZZDrawingDaemon()
	{		
		poolState = new Thread[Runtime.getRuntime().availableProcessors()];
		poolDraw = new Thread[Runtime.getRuntime().availableProcessors()];
		
		for (int i = 0; i < poolState.length; i++)
		{
			StateRunner runner = new StateRunner(i, poolState.length);
			poolState[i] = new Thread(runner);
			runner.run();
		}		

		for (int i = 0; i < poolDraw.length; i++)
		{
			DrawRunner runner = new DrawRunner(i, poolState.length);
			poolDraw[i] = new Thread(runner);
			runner.run();
		}		
	}
	
	/** 
	 * Multiple threads go through the forStateUpdate queue in a non-deterministic order,
	 * calling updateState on each primitive. If it returns true, the primitive is queued
	 * for a redraw of its textures. This is the first stage of a redrawing iteration. The
	 * method returns only after each primitive has been processed.
	 * 
	 * @param forStateUpdate List of primitives that should be updated
	 * @param gl Current OpenGL context
	 * @param viewMatrix Current view matrix
	 * @param projMatrix Current projection matrix
	 * @param halfScreen 2D vector containing half the screen's width and height 
	 */
	public void updateState(List<ZZDrawingDaemonPrimitive> forStateUpdate, boolean updateVisualProperties, GL4 gl, Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen)
	{
		this.forStateUpdate = forStateUpdate;
		this.updateVisualProperties = updateVisualProperties;
		this.gl = gl;
		this.viewMatrix = viewMatrix;
		this.projMatrix = projMatrix;
		
		stateUpdateSemaphore = new Semaphore(-poolState.length + 1);
		
		for (int i = 0; i < poolState.length; i++)
			poolState[i].notify();
		
		try 
		{
			stateUpdateSemaphore.acquire();
		} 
		catch (InterruptedException e) { }
	}
	
	/**
	 * Multiple threads go through the forDrawUpdate queue sequentially, 
	 * calling redrawTextures on each primitive. If it returns true, the primitive
	 * is queued for an update of its GPU resources (e. g. texture upload). This
	 * is the second stage of a drawing iteration. The method returns immediately,
	 * i. e. relying on the caller to stop the redrawing after a certain time span.
	 * 
	 * @param viewMatrix Current view matrix
	 * @param projMatrix Current projection matrix
	 */
	public void updateDraw(Matrix4 viewMatrix, Matrix4 projMatrix)
	{
		// Each thread has been paused since the queue was emptied the last time.
		// Notify all threads to resume them.
		for (int i = 0; i < poolDraw.length; i++)
			poolDraw[i].notify();
	}
	
	/**
	 * Clears the forDrawUpdate queue. If updateDraw has been called before, this
	 * will effectively stop the redrawing operation.
	 */
	public void clearDraw()
	{
		synchronized (forDrawUpdate)
		{
			forDrawUpdate.clear();
		}
	}
	
	/**
	 * A single thread goes through the forResourceUpdate queue (because OpenGL),
	 * calling updateResources on each primitive. This is the last stage of a
	 * drawing iteration.
	 * @param gl OpenGL context
	 */
	public void updateResources(GL4 gl)
	{
		synchronized (forResourceUpdate)
		{
			while (forResourceUpdate.size() > 0)
				forResourceUpdate.poll().updateResources(gl);
		}
	}
	
	/**
	 * Forces all running operations to finish and the threads to return.
	 */
	public void dispose()
	{
		doShutdown = true;
	}
	
	/**
	 * Internal class used to call the updateState method
	 * on queued primitives using multiple threads.
	 */
	private class StateRunner implements Runnable
	{
		private int threadID;
		private int numThreads;
		
		public StateRunner(int threadID, int numThreads)
		{
			this.threadID = threadID;
			this.numThreads = numThreads;
		}
		
		/**
		 * Multiple threads go through the forStateUpdate queue in a non-deterministic order,
		 * calling updateState on each primitive. If it returns true, the primitive is queued
		 * for a redraw of its textures.
		 */
		@Override
		public void run()
		{
			while (!doShutdown)
			{
				if (forStateUpdate != null)
					for (int i = threadID; i < forStateUpdate.size(); i += numThreads)
						if (forStateUpdate.get(i).updateState(updateVisualProperties, gl, viewMatrix, projMatrix, halfScreen))	// Update state and enqueue for redraw if updateState returns true
							synchronized (forDrawUpdate)
							{
								forDrawUpdate.add(forStateUpdate.get(i));
							}
				
				if (stateUpdateSemaphore != null)
					stateUpdateSemaphore.release();
				
				try
				{
					wait();
				} 
				catch (InterruptedException e) 
				{
					return;
				}
			}
		}		
	}
	
	/**
	 * Internal class used to call the redrawTexture method 
	 * on queued primitives using multiple threads.
	 */
	private class DrawRunner implements Runnable
	{
		private int threadID;
		private int numThreads;
		
		public DrawRunner(int threadID, int numThreads)
		{
			this.threadID = threadID;
			this.numThreads = numThreads;
		}
		
		/**
		 * Multiple threads go through the forDrawUpdate queue sequentially, 
		 * calling redrawTextures on each primitive. If it returns true, the primitive
		 * is queued for an update of its GPU resources (e. g. texture upload).
		 */
		@Override
		public void run() 
		{
			while (!doShutdown)
			{
				while (true)
				{
					ZZDrawingDaemonPrimitive primitive;
					synchronized (forDrawUpdate) 
					{
						primitive = forDrawUpdate.poll();
					}
					if (primitive == null)
						break;
					
					if (primitive.redrawTextures(viewMatrix, projMatrix))	// If device resource update is needed after drawing
						synchronized (forResourceUpdate)
						{
							forResourceUpdate.add(primitive);
						}
				}
				
				try 
				{
					wait();
				} 
				catch (InterruptedException e) 
				{
					return;
				}
			}
		}		
	}
}