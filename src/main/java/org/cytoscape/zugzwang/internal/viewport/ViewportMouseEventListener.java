package org.cytoscape.zugzwang.internal.viewport;

/**
 * Mouse action-related events raised by a viewport.
 */
public interface ViewportMouseEventListener 
{
	/**
	 * Mouse button pressed.
	 */
	public void viewportMouseDown(ViewportMouseEvent e);
	
	/**
	 * Mouse button released.
	 */
	public void viewportMouseUp(ViewportMouseEvent e);
	
	/**
	 * Mouse click performed, i. e. mouse pressed and released without movement in between.
	 */
	public void viewportMouseClick(ViewportMouseEvent e);
	
	/**
	 * Mouse moved.
	 */
	public void viewportMouseMove(ViewportMouseEvent e);
	
	/**
	 * Mouse moved with a button pressed.
	 */
	public void viewportMouseDrag(ViewportMouseEvent e);
	
	/**
	 * Mouse entered the viewport from outside.
	 */
	public void viewportMouseEnter(ViewportMouseEvent e);
	
	/**
	 * Mouse left the viewport.
	 */
	public void viewportMouseLeave(ViewportMouseEvent e);
	
	/**
	 * Mouse wheel scrolled.
	 */
	public void viewportMouseScroll(ViewportMouseEvent e);
}