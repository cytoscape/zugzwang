package org.cytoscape.zugzwang.internal.viewmodel;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.SUIDFactory;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualLexiconNode;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.ViewChangeRecord;
import org.cytoscape.view.model.events.ViewChangedEvent;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

/**
 * Abstract base for node/edge view model types. The methods implemented here 
 * take care of visual property value assignment and locking (= bypass). VPs 
 * can have a tree-like hierarchy. If a VP is locked, the lock implicitly 
 * propagates down the hierarchy. Directly locked values take precedence over 
 * implicitly locked, which override regular values, which in turn override 
 * default values. 
 * 
 * @param <S> Wrapped data model type
 */
public abstract class ZZView<S> implements View<S>
{	
	/** Handle to Cytoscape's central {@link CyEventHelper} **/
	protected final CyEventHelper eventHelper;
	
	/** The view model's UID **/
	private final Long suid;
	
	/** Lexicon of supported visual properties **/
	protected final VisualLexicon lexicon;

	/** 
	 * Values assigned to visual properties in a regular manner. If the map doesn't
	 * contain a property, the respective getter function will use the default value. 
	 */
	protected final Map<VisualProperty<?>, Object> visualProperties = Collections.synchronizedMap(new IdentityHashMap<VisualProperty<?>, Object>());
	
	/** 
	 * Values assigned to visual properties that are locked. This 
	 * takes precedence over regular and indirectly locked values.
	 */
	protected final Map<VisualProperty<?>, Object> directLocks = Collections.synchronizedMap(new IdentityHashMap<VisualProperty<?>, Object>());
	
	/**
	 * Values assigned to visual properties that are indirectly locked, i. e. through a locked ancestor property.
	 * This takes precedence over regular values, but is lower than direct lock values.
	 */
	protected final Map<VisualProperty<?>, Object> allLocks = Collections.synchronizedMap(new IdentityHashMap<VisualProperty<?>, Object>());
		
	public ZZView(VisualLexicon lexicon, CyEventHelper eventHelper) 
	{
		this.eventHelper = eventHelper;
		
		this.suid = SUIDFactory.getNextSUID();
		this.lexicon = lexicon;
	}
	
	@Override
	public Long getSUID() 
	{
		return suid;
	}	
	
	/**
	 * Gets the current value for the given VP. The hierarchy for this is:
	 * 1st: value assigned through direct lock
	 * 2nd: value assigned through indirect lock, i. e. direct lock of an ancestor VP node
	 * 3rd: regular value
	 * 4th: default value from the network's {@link DefaultValueVault}
	 * 
	 * @param visualProperty VP of which to get the value
	 * @return VP value
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getVisualProperty(VisualProperty<T> visualProperty) 
	{
		Object value;
		synchronized (getZZNetworkView().m_sync) 
		{
			value = directLocks.get(visualProperty);
			if (value != null)
				return (T)value;
	
			value = allLocks.get(visualProperty);
			if (value != null)
				return (T)value;
		
			value = visualProperties.get(visualProperty);
			if (value != null)
				return (T)value;
		}
		
		// Mapped value is null.  Try default
		value = this.getDefaultValue(visualProperty);
		if(value != null)
			return (T)value;
		else
			return visualProperty.getDefault();
	}

	/**
	 * Sets the value of the specified VP. This only sets the regular value, i. e. if
	 * the VP is (indirectly) locked, there won't be an immediate change. Setting the 
	 * value to null defaults it to the value from {@link DefaultValueVault}.
	 * 
	 * @param visualProperty VP to be set
	 * @param value New VP value
	 */
	public <T, V extends T> void setVisualProperty(VisualProperty<? extends T> visualProperty, V value) 
	{
		if (value == null)
			visualProperties.remove(visualProperty);
		else
			visualProperties.put(visualProperty, value);

		// Zugzwang (like Ding!) has its own listener for selection events.
		// If we don't do this, we might get into a deadlock state
		if (visualProperty == BasicVisualLexicon.NODE_SELECTED || visualProperty == BasicVisualLexicon.EDGE_SELECTED)
			return;

		if (!isValueLocked(visualProperty)) 
		{
			synchronized (getZZNetworkView().m_sync) 
			{
				applyVisualProperty(visualProperty, value);
			}
		}
		
		fireViewChangedEvent(visualProperty, value, false);
	}

	/**
	 * Sets a VP as locked (adds it to directLocks), and gives it the specified value. 
	 * The change is propagated to all of its children of the same type. They are put 
	 * into the allLocks collection and set to the same value as their parent.
	 * 
	 * @param visualProperty VP to be locked
	 * @param value New value to set the locked VP to
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T, V extends T> void setLockedValue(VisualProperty<? extends T> visualProperty,	V value) 
	{
		synchronized (getZZNetworkView().m_sync) 
		{
			directLocks.put(visualProperty, value);
			allLocks.put(visualProperty, value);
			
			applyVisualProperty(visualProperty, value);
			VisualLexiconNode rootNode = lexicon.getVisualLexiconNode(visualProperty);
			
			
			// Perform a breadth-first search of child VPs to indirectly lock them and set their values:
			
			final LinkedList<VisualLexiconNode> nodes = new LinkedList<VisualLexiconNode>();
			nodes.addAll(rootNode.getChildren());
			
			while (!nodes.isEmpty()) 
			{
				final VisualLexiconNode node = nodes.pop();
				final VisualProperty vp = node.getVisualProperty();
				
				if (!isDirectlyLocked(vp)) 
				{
					if (visualProperty.getClass() == vp.getClass())	// Preventing ClassCastExceptions
					{
						allLocks.put(vp, value);
						applyVisualProperty(vp, value);
					}
					
					nodes.addAll(node.getChildren());
				}
			}
		}
		
		fireViewChangedEvent(visualProperty, value, true);
	}

	/**
	 * Gets the status of a VP's lock.
	 * 
	 * @param visualProperty Visual property for which the lock status is inquired
	 */
	@Override
	public boolean isValueLocked(VisualProperty<?> visualProperty) 
	{
		return allLocks.containsKey(visualProperty);
	}

	/**
	 * Clears a direct lock on the specified VP, and propagates the
	 * change to all indirectly locked child VPs of the same type.
	 * 
	 * @param visualProperty VP to unlock
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void clearValueLock(VisualProperty<?> visualProperty) 
	{
		synchronized (getZZNetworkView().m_sync) 
		{
			directLocks.remove(visualProperty);
			
			VisualLexiconNode root = lexicon.getVisualLexiconNode(visualProperty);
			LinkedList<VisualLexiconNode> nodes = new LinkedList<VisualLexiconNode>();
			nodes.add(root);
			
			while (!nodes.isEmpty()) 
			{
				VisualLexiconNode node = nodes.pop();
				VisualProperty nodeVP = node.getVisualProperty();
				
				// Only children of the same type were locked before.
				if (visualProperty.getClass() != nodeVP.getClass())
					continue;
				
				// Don't propagate to directly locked nodes.
				if (isDirectlyLocked(nodeVP))
					continue;
				
				allLocks.remove(nodeVP);
				
				// Re-apply the regular visual property value.
				if (visualProperties.containsKey(nodeVP))
					applyVisualProperty(nodeVP, visualProperties.get(nodeVP));
				// Apply default if necessary.
				else
					applyVisualProperty(nodeVP, getVisualProperty(nodeVP));
				
				for (VisualLexiconNode child : node.getChildren())
					// Don't propagate to directly locked nodes.
					if (!isDirectlyLocked(child.getVisualProperty()))
						nodes.add(child);
			}
		}
		
		fireViewChangedEvent(visualProperty, null, true);
	}
	
	/**
	 * Checks if the specified VP has any (even default) value associated with it.
	 * 
	 * @param vp Visual property for which to check the value
	 * @return True if at least a default value exists, false otherwise
	 */
	@Override
	public boolean isSet(VisualProperty<?> vp) 
	{
		synchronized (getZZNetworkView().m_sync)
		{
			return visualProperties.get(vp) != null || allLocks.get(vp) != null || getDefaultValue(vp) != null;
		}
	}

	/**
	 * Checks if the specified VP is directly locked.
	 * 
	 * @param visualProperty Visual property for which to check the direct lock
	 * @return True if VP is directly locked, false otherwise
	 */
	@Override
	public boolean isDirectlyLocked(VisualProperty<?> visualProperty)
	{
		return directLocks.containsKey(visualProperty);
	}

	/**
	 * Clears the regular values of all VPs, unless they are 
	 * set to ignore default values (e. g. for position VPs).
	 * This does not clear any direct/indirect lock values.
	 */
	@Override
	public void clearVisualProperties() 
	{
		synchronized (getZZNetworkView().m_sync) 
		{
			final Iterator<Entry<VisualProperty<?>, Object>> it = visualProperties.entrySet().iterator();
			
			while (it.hasNext()) 
			{
				final VisualProperty<?> vp = it.next().getKey();
				
				if (!vp.shouldIgnoreDefault()) 
				{
					it.remove(); // do this first to prevent ConcurrentModificationExceptions later
					setVisualProperty(vp, null);
				}
			}
		}
	}

	/**
	 * Asynchronously adds a new {@link ViewChangedEvent} to 
	 * {@link CyEventHelper}'s payload for the current network view.
	 * 
	 * @param vp Visual property that was changed
	 * @param value New VP value
	 * @param lockedValue Whether the VP is locked
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <T, V extends T> void fireViewChangedEvent(final VisualProperty<? extends T> vp, final V value, final boolean lockedValue) 
	{
		final ViewChangeRecord record = new ViewChangeRecord(this, vp, value, lockedValue);
		eventHelper.addEventPayload(getZZNetworkView(), record, ViewChangedEvent.class);
	}
	
	protected abstract ZZNetworkView getZZNetworkView();
	protected abstract <T, V extends T> void applyVisualProperty(final VisualProperty<? extends T> vp, V value);
	protected abstract <T, V extends T> V getDefaultValue(final VisualProperty<T> vp);
}
