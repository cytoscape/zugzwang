package org.cytoscape.zugzwang.internal.viewport;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.cytoscape.zugzwang.internal.algebra.Ray3;
import org.cytoscape.zugzwang.internal.algebra.Vector2;
import org.cytoscape.zugzwang.internal.camera.Camera;

public class ViewportMouseEvent 
{
	public final boolean m1;	// Left button
	public final boolean m2;	// Right button
	public final boolean m3;	// Wheel button
	
	public final boolean keyShift;	// Shift key pressed
	public final boolean keyCtrl;	// Ctrl key pressed
	public final boolean keyAlt;	// Alt key pressed
	
	public final Vector2 offsetRaw;		// Mouse movement in raw pixels
	public final Vector2 offsetScaled;	// Mouse movement considering DPI scaling
	public final int delta;				// Wheel scrolling
	
	public final Vector2 positionRaw;	// Mouse position in raw pixels
	public final Vector2 positionScaled;// Mouse position considering DPI scaling
	public final Ray3 positionRay;		// Ray originating at camera position, going through the mouse position, normalized
	
	public boolean handled = false;	// Indicates that the event was handled by one of the subscribers

	public ViewportMouseEvent (boolean m1, boolean m2, boolean m3, 
							   boolean keyShift, boolean keyCtrl, boolean keyAlt, 
							   Vector2 offsetRaw, Vector2 offsetScaled, int delta,
							   Vector2 positionRaw, Vector2 positionScaled, Ray3 positionRay) 
	{
		this.m1 = m1;
		this.m2 = m2;
		this.m3 = m3;
		
		this.keyShift = keyShift;
		this.keyCtrl = keyCtrl;
		this.keyAlt = keyAlt;
		
		this.offsetRaw = offsetRaw;
		this.offsetScaled = offsetScaled;
		this.delta = delta;
		
		this.positionRaw = positionRaw;
		this.positionScaled = positionScaled;
		this.positionRay = positionRay;
	}
	
	public ViewportMouseEvent (MouseEvent e, Vector2 offset, float scaleDPI, Camera camera)
	{
		this.m1 = (e.getButton() & MouseEvent.BUTTON1) > 0;
		this.m2 = (e.getButton() & MouseEvent.BUTTON2) > 0;
		this.m3 = (e.getButton() & MouseEvent.BUTTON3) > 0;
		
		this.keyShift = e.isShiftDown();
		this.keyCtrl = e.isControlDown();
		this.keyAlt = e.isAltDown();
		
		
		this.offsetRaw = offset;
		this.offsetScaled = Vector2.ScalarMult(scaleDPI, offset);
		this.delta = 0;
		
		this.positionRaw = new Vector2(e.getX(), e.getY());
		this.positionScaled = Vector2.ScalarMult(scaleDPI, positionRaw);
		this.positionRay = camera.getRayThroughPixel(positionRaw);
	}
	
	public ViewportMouseEvent (MouseWheelEvent e, float scaleDPI, Camera camera)
	{
		this.m1 = (e.getButton() & MouseEvent.BUTTON1) > 0;
		this.m2 = (e.getButton() & MouseEvent.BUTTON2) > 0;
		this.m3 = (e.getButton() & MouseEvent.BUTTON3) > 0;
		
		this.keyShift = e.isShiftDown();
		this.keyCtrl = e.isControlDown();
		this.keyAlt = e.isAltDown();
		
		
		this.offsetRaw = new Vector2();
		this.offsetScaled = new Vector2();
		this.delta = e.getWheelRotation();
		
		this.positionRaw = new Vector2(e.getX(), e.getY());
		this.positionScaled = Vector2.ScalarMult(scaleDPI, positionRaw);
		this.positionRay = camera.getRayThroughPixel(positionRaw);
	}
}