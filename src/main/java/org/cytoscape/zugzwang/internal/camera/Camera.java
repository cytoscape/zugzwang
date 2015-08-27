package org.cytoscape.zugzwang.internal.camera;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.cytoscape.zugzwang.internal.algebra.Matrix3;
import org.cytoscape.zugzwang.internal.algebra.Matrix4;
import org.cytoscape.zugzwang.internal.algebra.Ray3;
import org.cytoscape.zugzwang.internal.algebra.Vector2;
import org.cytoscape.zugzwang.internal.algebra.Vector3;
import org.cytoscape.zugzwang.internal.algebra.Vector4;
import org.cytoscape.zugzwang.internal.viewport.Viewport;
import org.cytoscape.zugzwang.internal.viewport.ViewportEventListener;
import org.cytoscape.zugzwang.internal.viewport.ViewportResizedEvent;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.math.FloatUtil;

public class Camera implements ViewportEventListener
{
	private HashSet<CameraEventListener> cameraEventListeners = new HashSet<>();
	
	private Vector3 targetPos = new Vector3(), cameraPos = new Vector3(0, 0, 1);
	
	private Matrix4 lastViewMatrix, lastProjMatrix, lastViewProjMatrix;
	private Vector3[] lastPlanarXY;
	
	private float FOV = FloatUtil.HALF_PI;
	private Vector2 clippingRange = new Vector2(1e-3f, 1e6f);
	private Vector2 viewportSize = new Vector2(100, 100);
	
	public Camera(Viewport viewport)
	{
		viewport.addViewportEventListener(this);
	}
	
	public void reset()
	{
		CameraConfiguration oldConfig = getConfiguration();
		
		targetPos = new Vector3();
		cameraPos = new Vector3(0, 0, -1);
		FOV = FloatUtil.HALF_PI;
		clippingRange = new Vector2(1e-3f, 1e6f);
		
		invalidateMatrices();
		invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), false));
	}
	
	// Getters and setters
	
	public void setCameraPosition(Vector3 value)
	{
		if (!Vector3.Equals(cameraPos, value))
		{
			CameraConfiguration oldConfig = getConfiguration();
			cameraPos = value;
			
			invalidateMatrices();
			invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), false));
		}
	}
	
	public Vector3 getCameraPosition()
	{
		return cameraPos;
	}
	
	public void setTargetPosition(Vector3 value)
	{
		if (!Vector3.Equals(targetPos, value))
		{
			CameraConfiguration oldConfig = getConfiguration();
			targetPos = value;
			
			invalidateMatrices();
			invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), false));
		}
	}
	
	public Vector3 getTargetPosition()
	{
		return targetPos;
	}
	
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
	
	public float getFOV()
	{
		return FOV;
	}
	
	public void setClippingRange(Vector2 value)
	{
		if (!Vector2.Equals(clippingRange, value))
		{
			CameraConfiguration oldConfig = getConfiguration();
			clippingRange = value;
			
			invalidateMatrices();
			invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), true));
		}
	}
	
	public Vector2 getClippingRange()
	{
		return clippingRange;
	}
	
	public void setFromConfiguration(CameraConfiguration config)
	{
		CameraConfiguration oldConfig = getConfiguration();
		
		targetPos = config.targetPos;
		cameraPos = config.cameraPos;
		FOV = config.FOV;
		clippingRange = config.clippingRange;
		
		invalidateMatrices();
		invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, config, getViewProjectionMatrix(), false));
	}
	
	public CameraConfiguration getConfiguration()
	{
		return new CameraConfiguration("New Configuration", targetPos, cameraPos, FOV, clippingRange);
	}
	
	// Movement:
	
	public void panBy(Vector2 offset)
	{
		CameraConfiguration oldConfig = getConfiguration();
		
		// Get XY within focal plane, multiply by offset
		Vector3[] planarXY = getPlanarXY();
		planarXY[0] = Vector3.ScalarMult(offset.x, planarXY[0]);
		planarXY[1] = Vector3.ScalarMult(offset.y, planarXY[1]);
		
		Vector3 offset3 = Vector3.Add(planarXY[0], planarXY[1]);
		
		targetPos = Vector3.Add(targetPos, offset3);
		cameraPos = Vector3.Add(cameraPos, offset3);
		
		invalidateMatrices();
		invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), true));
	}
	
	public void panByPixels(Vector2 offset)
	{
		Vector3 globalUnitary = Vector3.Add(getTargetPosition(), getPlanarXY()[0]);
		Vector4 globalTransformed = Vector4.MatrixMult(getViewProjectionMatrix(), new Vector4(globalUnitary.x, globalUnitary.y, globalUnitary.z, 1.0f)).HomogeneousToCartesian();
		globalTransformed.x *= 0.5f * viewportSize.x;
		
		offset = Vector2.ScalarMult(1.0f / globalTransformed.x, offset);
		
		panBy(offset);
	}
	
	public void moveBy(Vector3 offset)
	{
		CameraConfiguration oldConfig = getConfiguration();
		
		targetPos = Vector3.Add(targetPos, offset);
		cameraPos = Vector3.Add(cameraPos, offset);
		
		invalidateMatrices();
		invokeCameraMoveEvent(new CameraMoveEvent(oldConfig, getConfiguration(), getViewProjectionMatrix(), false));
	}
	
	public void orbitBy(float offsetPhi, float offsetTheta)
	{
		Vector3 direction = Vector3.Subtract(targetPos, cameraPos);
		float distance = direction.Length();
		if (distance == 0)
			return;
		
		Vector2 angles = calculateAngles();
		//System.out.println(angles.toString());
		angles.x += offsetPhi;
		angles.y = Math.max(-FloatUtil.HALF_PI + 1e-3f, Math.min(angles.y + offsetTheta, FloatUtil.HALF_PI - 1e-3f));
		Matrix3 rotation = Matrix3.Mult(Matrix3.RotationY(angles.x), Matrix3.RotationX(angles.y));
		
		Vector3 newDirection = Vector3.MatrixMult(rotation, new Vector3(0, 0, distance));
		Vector3 newCameraPos = Vector3.Add(targetPos, newDirection);
		//System.out.println(newCameraPos.toString());
		
		setCameraPosition(newCameraPos);
	}
	
	// Transformations:
	
	public Matrix4 getViewMatrix()
	{
		if (lastViewMatrix == null)
		{
			Vector3 up = calculateUp();
			lastViewMatrix = Matrix4.LookAtRH(cameraPos, targetPos, up);
		}
		
		return lastViewMatrix;
	}
	
	public Matrix4 getProjectionMatrix()
	{
		if (lastProjMatrix == null)
		{
			if (viewportSize.x > 0 && viewportSize.y > 0)
				lastProjMatrix = Matrix4.ProjectionPerspective(viewportSize.x / viewportSize.y, FOV, clippingRange.x, clippingRange.y);
			else
				lastProjMatrix = Matrix4.ProjectionPerspective(1, FOV, clippingRange.x, clippingRange.y);
		}
		
		return lastProjMatrix;
	}
	
	public Matrix4 getViewProjectionMatrix()
	{
		if (lastViewProjMatrix == null)
			lastViewProjMatrix = Matrix4.Mult(getProjectionMatrix(), getViewMatrix());
		
		return lastViewProjMatrix;	
	}
	
	public Vector4 transformToLocal(Vector4 pos)
	{
		return Vector4.MatrixMult(getViewProjectionMatrix(), pos);
	}
	
	public Vector3[] getPlanarXY()
	{
		Vector2 angles = calculateAngles();		
		Matrix3 rotation = Matrix3.Mult(Matrix3.RotationY(angles.x), Matrix3.RotationX(angles.y));

		Vector3 right = Vector3.MatrixMult(rotation, new Vector3(1, 0, 0));
		Vector3 up = Vector3.MatrixMult(rotation, new Vector3(0, 1, 0));
		
		lastPlanarXY = new Vector3[] { right, up };
		
		return lastPlanarXY;
	}
	
	public Ray3 getRayThroughPixel(Vector2 pixel)
	{
		pixel.x = pixel.x - viewportSize.x * 0.5f;		// Window origin is upper left corner, OGL is bottom right, so flip
		pixel.y = viewportSize.y - pixel.y - viewportSize.y * 0.5f;
		Vector3[] planarXY = getPlanarXY();
		
		Vector4 transformedRight = Vector4.MatrixMult(getViewProjectionMatrix(), new Vector4(Vector3.Add(getTargetPosition(), planarXY[0]), 1.0f)).HomogeneousToCartesian();
		transformedRight.x = transformedRight.x * 0.5f * viewportSize.x;
		float scaleFactor = 1.0f / transformedRight.x;
		
		planarXY[0] = Vector3.ScalarMult(scaleFactor * pixel.x, planarXY[0]);
		planarXY[1] = Vector3.ScalarMult(scaleFactor * pixel.y, planarXY[1]);
		
		Vector3 direction = Vector3.Add(Vector3.Subtract(targetPos, cameraPos), Vector3.Add(planarXY[0], planarXY[1])).Normalize();
		return new Ray3(cameraPos, direction);
	}
	
	// Events:
	
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
	
	// Helpers:
	
	private Vector2 calculateAngles()
	{
		Vector3 direction = Vector3.Subtract(cameraPos, targetPos).Normalize();
		
		float phi = 0;
		if (direction.x != 0.0f || direction.z != 0.0f)
			phi = FloatUtil.atan2(direction.z, direction.x) - FloatUtil.HALF_PI;
		
		float theta = -FloatUtil.asin(direction.y);
		
		return new Vector2(-phi, theta);
	}
	
	private Vector3 calculateUp()
	{
		Vector2 angles = calculateAngles();		
		Matrix3 rotation = Matrix3.Mult(Matrix3.RotationY(angles.x), Matrix3.RotationX(angles.y));
		
		return Vector3.MatrixMult(rotation, new Vector3(0, 1, 0));	// Facing up is defined as theta = -pi/4, facing forward is theta = 0
	}
	
	private void invalidateMatrices()
	{
		lastViewMatrix = null;
		lastProjMatrix = null;
		lastViewProjMatrix = null;
		lastPlanarXY = null;
	}

	// Viewport event Handling:
	
	@Override
	public void viewportReshape(GLAutoDrawable drawable, ViewportResizedEvent e) 
	{
		if (!Vector2.Equals(viewportSize, e.newRawSize))
		{
			viewportSize = e.newRawSize;
			
			invalidateMatrices();
			invokeCameraMoveEvent(new CameraMoveEvent(getConfiguration(), getConfiguration(), getViewProjectionMatrix(), false));
		}
	}

	@Override
	public void viewportInitialize(GLAutoDrawable drawable) { }

	@Override
	public void viewportDisplay(GLAutoDrawable drawable) { }

	@Override
	public void viewportDispose(GLAutoDrawable drawable) { }
}