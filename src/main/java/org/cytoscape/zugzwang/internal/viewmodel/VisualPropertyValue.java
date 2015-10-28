package org.cytoscape.zugzwang.internal.viewmodel;

/**
 * Wraps a visual property value to add a 'locked' flag.
 */
public class VisualPropertyValue<V> 
{	
	private final V value;
	private boolean isValueLocked;
	
	public VisualPropertyValue(V value) 
	{
		this.value = value;
	}
	
	public V getValue() 
	{
		return value;
	}

	public void setValueLocked(boolean isValueLocked) 
	{
		this.isValueLocked = isValueLocked;
	}

	public boolean isValueLocked() 
	{
		return isValueLocked;
	}
}
