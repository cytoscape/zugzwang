package org.cytoscape.zugzwang.internal.viewport;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;

import org.cytoscape.zugzwang.internal.algebra.Plane;
import org.cytoscape.zugzwang.internal.algebra.Vector2;
import org.cytoscape.zugzwang.internal.algebra.Vector3;
import org.cytoscape.zugzwang.internal.algebra.Vector4;
import org.cytoscape.zugzwang.internal.camera.Camera;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.math.FloatUtil;

public class Viewport implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener
{
	private HashSet<ViewportEventListener> viewportEventListeners = new HashSet<>();
	private HashSet<ViewportMouseEventListener> viewportMouseEventListeners = new HashSet<>();
	
	private GL4 gl;
	
	private GLJPanel panel;
	private Camera camera;
	
	private float scaleDPI;
	private Vector2 lastMousePosition;
	private static class MouseStates
	{
		public static int IDLE = 0;
		public static int PAN = 1;
		public static int ROTATE = 2;
		public static int SELECT = 3;
	}
	private int mouseState = MouseStates.IDLE;

	public Viewport(JComponent container)
	{
		GLProfile profile = GLProfile.getDefault(); // Use the system's default version of OpenGL
		GLCapabilities capabilities = new GLCapabilities(profile);
		capabilities.setHardwareAccelerated(true);
		capabilities.setDoubleBuffered(true);
		
		panel = new GLJPanel(capabilities);
		panel.setIgnoreRepaint(true);
		panel.addGLEventListener(this);		
		panel.addMouseListener(this);
		panel.addMouseMotionListener(this);
		panel.addMouseWheelListener(this);
		
		camera = new Camera(this);
		
		if (container instanceof JInternalFrame) 
		{
			JInternalFrame frame = (JInternalFrame) container;
			Container pane = frame.getContentPane();
			pane.setLayout(new BorderLayout());
			pane.add(panel, BorderLayout.CENTER);
		} 
		else 
		{
			container.setLayout(new BorderLayout());
			container.add(panel, BorderLayout.CENTER);
		}
	}
	
	public GL4 getContext()
	{
		return gl;
	}
	
	public GLJPanel getPanel()
	{
		return panel;
	}
	
	public Camera getCamera()
	{
		return camera;
	}
	
	// GLEventListener methods:

	@Override
	public void init(GLAutoDrawable drawable) 
	{ 
		gl = drawable.getGL().getGL4();
		
		gl.glEnable(GL4.GL_DEPTH_TEST);
		
		gl.glDisable(GL4.GL_CULL_FACE);

		gl.glDepthFunc(GL.GL_LEQUAL);

		gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
		
		NativeSurface surface = drawable.getNativeSurface();
		int[] windowUnits = new int[] {100, 100};
		windowUnits = surface.convertToPixelUnits(windowUnits);
		scaleDPI = (float)windowUnits[0] / 100.0f;
		
		invokeViewportInitializeEvent(drawable);
	}
	
	@Override
	public void display(GLAutoDrawable drawable) 
	{ 
		long timeStart = System.nanoTime();
		
		gl = drawable.getGL().getGL4();
		
		gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		gl.glClearDepthf(1.0f);
		gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
		
		invokeViewportDisplayEvent(drawable);
		
		long timeFinish = System.nanoTime();
		float FPS = 1.0f / ((float)(timeFinish - timeStart) * 1e-9f);
		System.out.println(FPS + " fps");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) 
	{
		gl = drawable.getGL().getGL4();
		
		gl.glViewport(x, y, width, height);
		
		Vector2 newRawSize = new Vector2(width, height);
		ViewportResizedEvent e = new ViewportResizedEvent(newRawSize, Vector2.ScalarMult(scaleDPI, newRawSize));
		invokeViewportReshapeEvent(drawable, e);
	}

	@Override
	public void dispose(GLAutoDrawable drawable) 
	{ 
		invokeViewportDisposeEvent(drawable);
	}
	
	// Handle mouse events from GLJPanel:

	@Override
	public void mouseClicked(MouseEvent e) 
	{
		ViewportMouseEvent event = new ViewportMouseEvent(e, new Vector2(), scaleDPI, camera);
		invokeViewportMouseDownEvent(event);
		if (event.handled)
			return;
	}

	@Override
	public void mouseEntered(MouseEvent e) 
	{
		lastMousePosition = new Vector2(e.getX(), e.getY());
		ViewportMouseEvent event = new ViewportMouseEvent(e, new Vector2(), scaleDPI, camera);
		invokeViewportMouseEnterEvent(event);
		if (event.handled)
			return;
	}

	@Override
	public void mouseExited(MouseEvent e) 
	{
		lastMousePosition = null;
		ViewportMouseEvent event = new ViewportMouseEvent(e, new Vector2(), scaleDPI, camera);
		invokeViewportMouseLeaveEvent(event);
		if (event.handled)
			return;
	}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		ViewportMouseEvent event = new ViewportMouseEvent(e, new Vector2(), scaleDPI, camera);
		invokeViewportMouseDownEvent(event);
		if (event.handled)
			return;
		
		if (event.m2)
		{
			if (event.keyCtrl)
				mouseState = MouseStates.ROTATE;
			else
				mouseState = MouseStates.PAN;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) 
	{
		ViewportMouseEvent event = new ViewportMouseEvent(e, new Vector2(), scaleDPI, camera);
		invokeViewportMouseUpEvent(event);
		if (event.handled)
			return;
		
		if (mouseState == MouseStates.SELECT)
		{
			// Selection handling
		}
		else
		{
			mouseState = MouseStates.IDLE;
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) 
	{
		Vector2 diff = new Vector2();
		
		if (lastMousePosition == null)
		{
			lastMousePosition = new Vector2(e.getX(), e.getY());
		}
		else
		{
			Vector2 newPosition = new Vector2(e.getX(), e.getY());
			diff = Vector2.Subtract(newPosition, lastMousePosition);
			diff.y *= -1.0f;
			lastMousePosition = newPosition;
		}
		
		ViewportMouseEvent event = new ViewportMouseEvent(e, diff, scaleDPI, camera);
		invokeViewportMouseMoveEvent(event);
		if (event.handled)
			return;
		
		if (mouseState == MouseStates.PAN)
		{
			camera.panByPixels(Vector2.ScalarMult(-1.0f, diff));
			panel.repaint();
		}
		else if (mouseState == MouseStates.ROTATE)
		{
			camera.orbitBy(-diff.x / (float)panel.getWidth() * FloatUtil.PI, -diff.y / (float)panel.getWidth() * FloatUtil.PI);
			panel.repaint();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) 
	{
		Vector2 diff = new Vector2();
		
		if (lastMousePosition == null)
		{
			lastMousePosition = new Vector2(e.getX(), e.getY());
		}
		else
		{
			Vector2 newPosition = new Vector2(e.getX(), e.getY());
			diff = Vector2.Subtract(newPosition, lastMousePosition);
			lastMousePosition = newPosition;
		}
		
		ViewportMouseEvent event = new ViewportMouseEvent(e, diff, scaleDPI, camera);
		invokeViewportMouseDragEvent(event);
		if (event.handled)
			return;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) 
	{
		ViewportMouseEvent event = new ViewportMouseEvent(e, scaleDPI, camera);
		invokeViewportMouseScrollEvent(event);
		if (event.handled)
			return;
		
		// Zoom in or out while keeping the same point under the mouse pointer
		
		Vector3 fromTarget = Vector3.Subtract(camera.getCameraPosition(), camera.getTargetPosition());
		Plane focalPlane = new Plane(camera.getTargetPosition(), fromTarget.Normalize());
		Vector3 centerPosition = focalPlane.intersect(event.positionRay);
		
		Vector4 oldPositionScreen = Vector4.MatrixMult(camera.getViewProjectionMatrix(), new Vector4(centerPosition, 1.0f)).HomogeneousToCartesian();
		oldPositionScreen.x *= 0.5f * (float)panel.getWidth();
		oldPositionScreen.y *= 0.5f * (float)panel.getHeight();
		
		if (event.delta > 0)
			fromTarget = Vector3.ScalarMult(1.25f, fromTarget);
		else if (event.delta < 0)
			fromTarget = Vector3.ScalarMult(1.0f / 1.25f, fromTarget);
		
		if (fromTarget.Length() > 0.0f)
			camera.setCameraPosition(Vector3.Add(camera.getTargetPosition(), fromTarget));
				
		Vector4 newPositionScreen = Vector4.MatrixMult(camera.getViewProjectionMatrix(), new Vector4(centerPosition, 1.0f)).HomogeneousToCartesian();
		newPositionScreen.x *= 0.5f * (float)panel.getWidth();
		newPositionScreen.y *= 0.5f * (float)panel.getHeight();
		
		Vector2 correctionOffset = new Vector2(newPositionScreen.x - oldPositionScreen.x, newPositionScreen.y - oldPositionScreen.y);
		camera.panByPixels(correctionOffset);
		
		panel.repaint();
	}
	
	// General events:
	
	public void addViewportEventListener(ViewportEventListener listener)
	{
		viewportEventListeners.add(listener);
	}
	
	public void removeViewportEventListener(ViewportEventListener listener)
	{
		viewportEventListeners.remove(listener);
	}
	
	private void invokeViewportInitializeEvent(GLAutoDrawable drawable)
	{
		for (ViewportEventListener listener : viewportEventListeners)
			listener.viewportInitialize(drawable);
	}
	
	private void invokeViewportReshapeEvent(GLAutoDrawable drawable, ViewportResizedEvent e)
	{
		for (ViewportEventListener listener : viewportEventListeners)
			listener.viewportReshape(drawable, e);
	}
	
	private void invokeViewportDisplayEvent(GLAutoDrawable drawable)
	{
		for (ViewportEventListener listener : viewportEventListeners)
			listener.viewportDisplay(drawable);
	}
	
	private void invokeViewportDisposeEvent(GLAutoDrawable drawable)
	{
		for (ViewportEventListener listener : viewportEventListeners)
			listener.viewportDispose(drawable);
	}
	
	// Mouse events:
	
	public void addViewportMouseEventListener(ViewportMouseEventListener listener)
	{
		viewportMouseEventListeners.add(listener);
	}
	
	public void removeViewportMouseEventListener(ViewportMouseEventListener listener)
	{
		viewportMouseEventListeners.remove(listener);
	}
	
	private void invokeViewportMouseDownEvent(ViewportMouseEvent e)
	{
		for (ViewportMouseEventListener listener : viewportMouseEventListeners)
			listener.viewportMouseDown(e);
	}
	
	private void invokeViewportMouseEnterEvent(ViewportMouseEvent e)
	{
		for (ViewportMouseEventListener listener : viewportMouseEventListeners)
			listener.viewportMouseEnter(e);
	}
	
	private void invokeViewportMouseLeaveEvent(ViewportMouseEvent e)
	{
		for (ViewportMouseEventListener listener : viewportMouseEventListeners)
			listener.viewportMouseLeave(e);
	}
	
	private void invokeViewportMouseUpEvent(ViewportMouseEvent e)
	{
		for (ViewportMouseEventListener listener : viewportMouseEventListeners)
			listener.viewportMouseUp(e);
	}
	
	private void invokeViewportMouseMoveEvent(ViewportMouseEvent e)
	{
		for (ViewportMouseEventListener listener : viewportMouseEventListeners)
			listener.viewportMouseMove(e);
	}
	
	private void invokeViewportMouseDragEvent(ViewportMouseEvent e)
	{
		for (ViewportMouseEventListener listener : viewportMouseEventListeners)
			listener.viewportMouseDrag(e);
	}
	
	private void invokeViewportMouseScrollEvent(ViewportMouseEvent e)
	{
		for (ViewportMouseEventListener listener : viewportMouseEventListeners)
			listener.viewportMouseScroll(e);
	}
}