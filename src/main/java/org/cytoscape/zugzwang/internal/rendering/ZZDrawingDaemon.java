package org.cytoscape.zugzwang.internal.rendering;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.cytoscape.zugzwang.internal.algebra.*;

import com.jogamp.opengl.GL4;

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
	
	public void updateState(List<ZZDrawingDaemonPrimitive> forStateUpdate, GL4 gl, Matrix4 viewMatrix, Matrix4 projMatrix, Vector2 halfScreen)
	{
		this.forStateUpdate = forStateUpdate;
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
	
	public void updateDraw(Matrix4 viewMatrix, Matrix4 projMatrix)
	{
		for (int i = 0; i < poolDraw.length; i++)
			poolDraw[i].notify();
	}
	
	public void clearDraw()
	{
		synchronized (forDrawUpdate)
		{
			forDrawUpdate.clear();
		}
	}
	
	public void updateResources(GL4 gl)
	{
		synchronized (forResourceUpdate)
		{
			while (forResourceUpdate.size() > 0)
				forResourceUpdate.poll().updateResources(gl);
		}
	}
	
	public void dispose()
	{
		doShutdown = true;
	}
	
	private class StateRunner implements Runnable
	{
		private int threadID;
		private int numThreads;
		
		public StateRunner(int threadID, int numThreads)
		{
			this.threadID = threadID;
			this.numThreads = numThreads;
		}
		
		@Override
		public void run()
		{
			while (!doShutdown)
			{
				if (forStateUpdate != null)
					for (int i = threadID; i < forStateUpdate.size(); i += numThreads)
						if (forStateUpdate.get(i).updateState(gl, viewMatrix, projMatrix, halfScreen))	// Update state and enqueue for redraw if updateState returns true
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
	
	private class DrawRunner implements Runnable
	{
		private int threadID;
		private int numThreads;
		
		public DrawRunner(int threadID, int numThreads)
		{
			this.threadID = threadID;
			this.numThreads = numThreads;
		}
		
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