package org.cytoscape.zugzwang.internal.camera;

import java.util.HashSet;

import org.cytoscape.zugzwang.internal.algebra.*;
import org.cytoscape.zugzwang.internal.viewport.*;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;

/**
 * A targeted camera that can be moved and orbited in 3D space.
 * This camera can't roll. The standard viewing direction is
 * the negative Z axis. Clipping occurs outside of [1e-2, 1e+6].
 */
public class Camera implements ViewportEventListener
{
	private Object m_sync = new Object();
	
	private HashSet<CameraEventListener> cameraEventListeners = new HashSet<>();

	/** Target position **/
	private Vector3 targetPos = new Vector3();	
	/** Camera rotation around the target **/
	private Quaternion rotation = Quaternion.identity();
	/** Distance from camera to target **/
	private float distance = 100.0f;
	
	/** Last calculated view matrix **/
	private Matrix4 lastViewMatrix;		
	/** Last calculated projection matrix **/
	private Matrix4 lastProjMatrix;			
	/** Last calculated (projection * view) matrix **/
	private Matrix4 lastViewProjMatrix;					
	/** Last calculated X and Y vectors of the focal plane **/ 
	private Vector3[] lastPlanarXY;							
	/** Last calculated camera position **/
	private Vector3 lastCameraPos;
	/** Last calculated camera direction **/
	private Vector3 lastCameraDir;
	
	/** Field of view, default = 60 deg **/
	private float FOV = FloatUtil.PI * 0.3f;
	/** Near and far clipping plane distance **/
	private Vector2 clippingRange = new Vector2(1e-2f, 1e6f);
	/** Viewport (GLJPanel) size in pixels **/
	private Vector2 viewportSize = new Vector2(100, 100);
	
	public Camera(Viewport viewport)
	{
		viewport.addViewportEventListener(this);
	}
	
	/**
	 * Resets all camera parameters to their default values.
	 */
	public void reset()
	{
		CameraConfiguration oldConfig = getConfiguration();
		
		targetPos = new Vector3();
		rotation = Quaternion.identity();
		distance = 100.0f;
		FOV = FloatUtil.HALF_PI;
		clippingRange = new Vector2(1e-3f, 1e6f);
		
		invalidateMatrices();
		invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), false));
	}
	
	// *******************
	// Getters and setters
	// *******************
	
	/**
	 * Gets the current camera position.
	 * 
	 * @return Camera position
	 */
	public Vector3 getCameraPosition()
	{
		synchronized (m_sync)
		{
			if (lastCameraPos == null)
				lastCameraPos = Vector3.add(targetPos, Vector3.quaternionMult(rotation, new Vector3(0, 0, distance)));
			
			return lastCameraPos.copy();
		}
		
	}
	
	/**
	 * Gets the current target position.
	 * 
	 * @return Target position
	 */
	public Vector3 getTargetPosition()
	{
		return targetPos;
	}
	
	/**
	 * Sets the camera's field of view.
	 * 
	 * @param value New field of view in radians; standard value is 90 deg
	 */
	public void setFOV(float value)
	{
		if (FOV != value)
		{
			CameraConfiguration oldConfig = getConfiguration();
			FOV = value;
			
			invalidateMatrices();
			invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), false));
		}
	}
	
	/**
	 * The the camera's current field of view
	 * 
	 * @return Field of view in radians
	 */
	public float getFOV()
	{
		return FOV;
	}
	
	/**
	 * Sets the camera's clipping range, i. e. distance range that 
	 * corresponds to [0, 1] after the projection transformation
	 * and is thus visible.
	 * 
	 * @param value Clipping range: 1st component = near, 2nd component = far
	 */
	public void setClippingRange(Vector2 value)
	{
		if (!Vector2.equals(clippingRange, value))
		{
			CameraConfiguration oldConfig = getConfiguration();
			clippingRange = value;
			
			invalidateMatrices();
			invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), true));
		}
	}
	
	/**
	 * Gets the camera's current clipping range.
	 * 
	 * @return Clipping range: 1st component = near, 2nd component = far
	 */
	public Vector2 getClippingRange()
	{
		return clippingRange;
	}
	
	/**
	 * Overrides all of the camera's parameters with 
	 * those from the provided configuration.
	 * 
	 * @param config Set of new parameters
	 */
	public void setFromConfiguration(CameraConfiguration config)
	{
		CameraConfiguration oldConfig = getConfiguration();
		
		targetPos = config.targetPos;
		rotation = config.rotation;
		distance = config.distance;
		FOV = config.FOV;
		clippingRange = config.clippingRange;
		
		invalidateMatrices();
		invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, config, getViewProjectionMatrix(), false));
	}
	
	/**
	 * Gets all of the camera's current parameters as a {@link CameraConfiguration}.
	 * 
	 * @return Set of current parameters
	 */
	public CameraConfiguration getConfiguration()
	{
		return new CameraConfiguration("New Configuration", targetPos, rotation, distance, FOV, clippingRange);
	}
	
	// *********
	// Movement:
	// *********
	
	/**
	 * Pans the camera by moving the camera and target positions within 
	 * the focal plane, i. e. without changing the camera's rotation.
	 * 
	 * @param offset 2D offset within the focal plane in absolute space units
	 */
	public void panBy(Vector2 offset)
	{
		CameraConfiguration oldConfig = getConfiguration();
		
		// Get XY within focal plane, multiply by offset
		Vector3[] planarXY = getPlanarXY();
		planarXY[0] = Vector3.scalarMult(offset.x, planarXY[0]);
		planarXY[1] = Vector3.scalarMult(offset.y, planarXY[1]);
		
		Vector3 offset3 = Vector3.add(planarXY[0], planarXY[1]);
		
		targetPos = Vector3.add(targetPos, offset3);
		
		invalidateMatrices();
		invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), true));
	}
	
	/**
	 * Pans the camera by moving the camera and target positions within 
	 * the focal plane, i. e. without changing the camera's rotation.
	 * The offset is calculated so that it moves an object in the focal
	 * plane by the specified amount of pixels on the screen, i. e. taking
	 * the projection matrix and the viewport size into account.
	 * 
	 * @param offset 2D offset within the focal plane in screen pixels
	 */
	public void panByPixels(Vector2 offset)
	{
		Vector3 globalUnitary = Vector3.add(getTargetPosition(), getPlanarXY()[0]);
		Vector4 globalTransformed = Vector4.matrixMult(getViewProjectionMatrix(), new Vector4(globalUnitary.x, globalUnitary.y, globalUnitary.z, 1.0f)).homogeneousToCartesian();
		globalTransformed.x *= 0.5f * viewportSize.x;
		
		offset = Vector2.scalarMult(1.0f / globalTransformed.x, offset);
		
		panBy(offset);
	}
	
	/**
	 * Moves the camera in 3D by the specified amount without changing its direction.
	 * 
	 * @param offset Offset in absolute space units
	 */
	public void moveBy(Vector3 offset)
	{
		CameraConfiguration oldConfig = getConfiguration();
		
		targetPos = Vector3.add(targetPos, offset);
		
		invalidateMatrices();
		invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), false));
	}
	
	/**
	 * Orbits the camera around the target by rotating it around the current 2 planar axes.
	 * 
	 * @param angles .x = angle around planar Y; .y = angle around planar X
	 */
	public void orbitBy(Vector2 angles)
	{
		CameraConfiguration oldConfig = getConfiguration();
		
		Vector3[] planar = getPlanarXY();
		Vector3 axis = Vector3.add(Vector3.scalarMult(angles.x, planar[1]), Vector3.scalarMult(angles.y, planar[0]));
        float angle = axis.length();
        if (angle == 0f)
            return;
        axis = axis.normalize();

        Quaternion rotateBy = Quaternion.fromAxisAngle(axis, angle);
        rotation = Quaternion.multiply(rotation, rotateBy).normalize();
		
		invalidateMatrices();
		invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), false));
	}
	
	// ****************
	// Transformations:
	// ****************
	
	/**
	 * Gets the current view matrix, a 4x4 matrix that transforms
	 * coordinates from world into camera space.
	 * 
	 * @return View matrix
	 */
	public Matrix4 getViewMatrix()
	{
		if (lastViewMatrix == null)
		{
			Vector3 up = calculateUp();
			lastViewMatrix = Matrix4.lookAtRH(getCameraPosition(), targetPos, up);
		}
		
		return lastViewMatrix;
	}
	
	/**
	 * Gets the current projection matrix, a 4x4 matrix that transforms
	 * coordinates from camera to screen space.
	 * @return
	 */
	public Matrix4 getProjectionMatrix()
	{
		if (lastProjMatrix == null)
		{
			if (viewportSize.x > 0 && viewportSize.y > 0)
				lastProjMatrix = Matrix4.projectionPerspective(viewportSize.x / viewportSize.y, FOV, clippingRange.x, clippingRange.y);
			else
				lastProjMatrix = Matrix4.projectionPerspective(1, FOV, clippingRange.x, clippingRange.y);
		}
		
		return lastProjMatrix;
	}
	
	/**
	 * Gets the current (projection * view) matrix, a 4x4 matrix that transforms
	 * coordinates from world to screen space.
	 * @return
	 */
	public Matrix4 getViewProjectionMatrix()
	{
		if (lastViewProjMatrix == null)
			lastViewProjMatrix = Matrix4.mult(getProjectionMatrix(), getViewMatrix());
		
		return lastViewProjMatrix;	
	}
	
	/**
	 * Transforms the provided position from world to screen space.
	 * 
	 * @param pos Position to be transformed
	 * @return Transformed position
	 */
	public Vector4 transformToScreen(Vector4 pos)
	{
		return Vector4.matrixMult(getViewProjectionMatrix(), pos);
	}
	
	/**
	 * Gets the normalized X and Y vectors of the current focal plane.
	 * 
	 * @return Array with the X [=0] and Y [=1] vectors 
	 */
	public Vector3[] getPlanarXY()
	{
		synchronized (m_sync)
		{
			if (lastPlanarXY == null)
			{
				Matrix3 rotationMat = Matrix3.fromQuaternion(rotation);
		
				Vector3 right = Vector3.matrixMult(rotationMat, new Vector3(1, 0, 0));
				Vector3 up = Vector3.matrixMult(rotationMat, new Vector3(0, 1, 0));
				
				lastPlanarXY = new Vector3[] { right, up };
			}
			
			return new Vector3[] { lastPlanarXY[0].copy(), lastPlanarXY[1].copy() };
		}
	}
	
	/**
	 * Gets a 3D ray that originates at the camera position and goes
	 * through the position in the focal plane that matches that of
	 * the provided pixel coordinates (considering the viewport size).
	 * 
	 * @param pixel 2D pixel coordinates in screen space
	 * @return Ray from camera through pixel
	 */
	public Ray3 getRayThroughPixel(Vector2 pixel)
	{
		pixel.x = pixel.x - viewportSize.x * 0.5f;		// Window origin is upper left corner, OGL is bottom right, so flip
		pixel.y = viewportSize.y - pixel.y - viewportSize.y * 0.5f;
		Vector3[] planarXY = getPlanarXY();
		
		Vector4 transformedRight = Vector4.matrixMult(getViewProjectionMatrix(), new Vector4(Vector3.add(getTargetPosition(), planarXY[0]), 1.0f)).homogeneousToCartesian();
		transformedRight.x = transformedRight.x * 0.5f * viewportSize.x;
		float scaleFactor = 1.0f / transformedRight.x;
		
		planarXY[0] = Vector3.scalarMult(scaleFactor * pixel.x, planarXY[0]);
		planarXY[1] = Vector3.scalarMult(scaleFactor * pixel.y, planarXY[1]);
		
		Vector3 direction = Vector3.add(Vector3.subtract(targetPos, getCameraPosition()), Vector3.add(planarXY[0], planarXY[1])).normalize();
		return new Ray3(getCameraPosition(), direction);
	}
	
	/**
	 * Gets the normalized camera direction vector, i. e. pointing from camera to target.
	 * 
	 * @return Direction vector
	 */
	public Vector3 getDirection()
	{
		synchronized (m_sync)
		{
			if (lastCameraDir == null)
				lastCameraDir = Vector3.quaternionMult(rotation, new Vector3(0, 0, -1));
			
			return lastCameraDir.copy();
		}
	}
	
	/**
	 * Gets the distance between camera and target.
	 * 
	 * @return Distance
	 */
	public float getDistance()
	{
		return distance;
	}
	
	/**
	 * Increases/decreases the distance between camera and target without changing the angle.
	 * 
	 * @param distance New distance between camera and target
	 */
	public void setDistance(float distance)
	{
		CameraConfiguration oldConfig = getConfiguration();
		
		this.distance = distance;
		
		invalidateMatrices();
		invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), false));
	}
	
	// *******
	// Events:
	// *******
	
	public void addCameraEventListener(CameraEventListener listener)
	{
		cameraEventListeners.add(listener);
	}
	
	public void removeCameraEventListener(CameraEventListener listener)
	{
		cameraEventListeners.remove(listener);
	}
	
	private void invokeCameraMoveEvent(CameraMoveEvent e)
	{
		for (CameraEventListener listener : cameraEventListeners)
			listener.cameraMoved(e);
	}
	
	// ********
	// Helpers:
	// ********
		
	/**
	 * Gets the current up vector. As there is no rolling in this implementation,
	 * the calculation only depends on the camera and target positions.
	 * 
	 * @return Up vector in 3D space
	 */
	private Vector3 calculateUp()
	{
		return Vector3.quaternionMult(rotation, new Vector3(0, 1, 0));	// Facing up is defined as rotation = identity
	}
	
	/**
	 * Invalidates pre-calculated matrices and plane vectors.
	 */
	private void invalidateMatrices()
	{
		lastViewMatrix = null;
		lastProjMatrix = null;
		lastViewProjMatrix = null;
		lastPlanarXY = null;
		lastCameraPos = null;
		lastCameraDir = null;
	}

	// Viewport event Handling:
	
	/**
	 * Callback method invoked when the viewport is resized. 
	 * Updates the viewport size, and raises a {@link CameraMoveEvent}.
	 * 
	 * @param drawable Viewport's GLJPanel
	 * @param e Information about the new viewport size
	 */
	@Override
	public void viewportReshape(GLAutoDrawable drawable, ViewportResizedEvent e) 
	{
		if (!Vector2.equals(viewportSize, e.newRawSize))
		{
			viewportSize = e.newRawSize;
			
			invalidateMatrices();
			invokeCameraMoveEvent(new CameraMoveEvent(getConfiguration(), getConfiguration(), getViewProjectionMatrix(), false));
		}
	}

	/**
	 * Viewport initialization. Not needed.
	 */
	@Override
	public void viewportInitialize(GLAutoDrawable drawable) { }

	/**
	 * Viewport display, a.k.a. rendering. Not needed.
	 */
	@Override
	public void viewportDisplay(GLAutoDrawable drawable) { }

	/**
	 * Viewport/OpenGL disposal. Not needed as there are no device resources.
	 */
	@Override
	public void viewportDispose(GLAutoDrawable drawable) { }
}