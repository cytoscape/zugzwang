package org.cytoscape.zugzwang.internal.viewport;


public interface ViewportMouseEventListener 
{
	public void viewportMouseDown(ViewportMouseEvent e);
	public void viewportMouseUp(ViewportMouseEvent e);
	public void viewportMouseClick(ViewportMouseEvent e);
	public void viewportMouseMove(ViewportMouseEvent e);
	public void viewportMouseDrag(ViewportMouseEvent e);
	public void viewportMouseEnter(ViewportMouseEvent e);
	public void viewportMouseLeave(ViewportMouseEvent e);
	public void viewportMouseScroll(ViewportMouseEvent e);
}