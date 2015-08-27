package org.cytoscape.zugzwang.internal.cytoscape.view;

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

public abstract class ZZView<S> implements View<S>
{	
	protected final CyEventHelper eventHelper;
	
	private final Long suid;
	protected final VisualLexicon lexicon;

	protected final Map<VisualProperty<?>, Object> visualProperties = Collections.synchronizedMap(new IdentityHashMap<VisualProperty<?>, Object>());	// Carried over from Ding
	protected final Map<VisualProperty<?>, Object> directLocks = Collections.synchronizedMap(new IdentityHashMap<VisualProperty<?>, Object>());
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
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getVisualProperty(VisualProperty<T> visualProperty) 
	{
		Object value;
		synchronized (getZZNetworkView().m_lock) 
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

	public <T, V extends T> void setVisualProperty(VisualProperty<? extends T> visualProperty, V value) 
	{
		if (value == null)
			visualProperties.remove(visualProperty);
		else
			visualProperties.put(visualProperty, value);

		// Ding has it's own listener for selection events.  If we
		// don't do this, we might get into a deadlock state
		if (visualProperty == BasicVisualLexicon.NODE_SELECTED || visualProperty == BasicVisualLexicon.EDGE_SELECTED)
			return;

		if (!isValueLocked(visualProperty)) 
		{
			synchronized (getZZNetworkView().m_lock) 
			{
				applyVisualProperty(visualProperty, value);
			}
		}
		
		fireViewChangedEvent(visualProperty, value, false);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void propagateLockedVisualProperty(final VisualProperty parent, final Collection<VisualLexiconNode> roots, final Object value) 
	{
		final LinkedList<VisualLexiconNode> nodes = new LinkedList<VisualLexiconNode>();
		nodes.addAll(roots);
		
		while (!nodes.isEmpty()) 
		{
			final VisualLexiconNode node = nodes.pop();
			final VisualProperty vp = node.getVisualProperty();
			
			if (!isDirectlyLocked(vp)) 
			{
				if (parent.getClass() == vp.getClass())	// Preventing ClassCastExceptions
				{
					// Caller should already have write lock to modify this
					synchronized (getZZNetworkView().m_lock) 
					{
						allLocks.put(vp, value);
					}
					applyVisualProperty(vp, value);
				}
				
				nodes.addAll(node.getChildren());
			}
		}
	}

	@Override
	public <T, V extends T> void setLockedValue(VisualProperty<? extends T> visualProperty,	V value) 
	{
		synchronized (getZZNetworkView().m_lock) 
		{
			directLocks.put(visualProperty, value);
			allLocks.put(visualProperty, value);
			
			applyVisualProperty(visualProperty, value);
			VisualLexiconNode node = lexicon.getVisualLexiconNode(visualProperty);
			propagateLockedVisualProperty(visualProperty, node.getChildren(), value);
		}
		
		fireViewChangedEvent(visualProperty, value, true);
	}

	@Override
	public boolean isValueLocked(VisualProperty<?> visualProperty) 
	{
		return allLocks.get(visualProperty) != null;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void clearValueLock(VisualProperty<?> visualProperty) 
	{
		synchronized (getZZNetworkView().m_lock) 
		{
			directLocks.remove(visualProperty);
			
			VisualLexiconNode root = lexicon.getVisualLexiconNode(visualProperty);
			LinkedList<VisualLexiconNode> nodes = new LinkedList<VisualLexiconNode>();
			nodes.add(root);
			
			while (!nodes.isEmpty()) 
			{
				VisualLexiconNode node = nodes.pop();
				VisualProperty nodeVP = node.getVisualProperty();
				allLocks.remove(nodeVP);
				
				// Re-apply the regular visual property value
				if (visualProperties.containsKey(nodeVP))
				{
					applyVisualProperty(nodeVP, visualProperties.get(nodeVP));
				// TODO else: reset to the visual style default if visualProperties map doesn't contain this vp
				}
				else 
				{
					// Apply default if necessary.
					final Object newValue = getVisualProperty(nodeVP);
					applyVisualProperty(nodeVP, newValue);
				}
				
				for (VisualLexiconNode child : node.getChildren())
					if (!isDirectlyLocked(child.getVisualProperty()))
						nodes.add(child);
				
				nodes.addAll(node.getChildren());
			}
		}
		
		fireViewChangedEvent(visualProperty, null, true);
	}
	
	
	@Override
	public boolean isSet(VisualProperty<?> vp) 
	{
		synchronized (getZZNetworkView().m_lock)
		{
			return visualProperties.get(vp) != null || allLocks.get(vp) != null || getDefaultValue(vp) != null;
		}
	}

	@Override
	public boolean isDirectlyLocked(VisualProperty<?> visualProperty)
	{
		return directLocks.get(visualProperty) != null;
	}

	@Override
	public void clearVisualProperties() 
	{
		synchronized (getZZNetworkView().m_lock) 
		{
			final Iterator<Entry<VisualProperty<?>, Object>> it = visualProperties.entrySet().iterator();
			
			while (it.hasNext()) 
			{
				final Entry<VisualProperty<?>, Object> entry = it.next();
				final VisualProperty<?> vp = entry.getKey();
				
				if (!vp.shouldIgnoreDefault()) 
				{
					it.remove(); // do this first to prevent ConcurrentModificationExceptions later
					setVisualProperty(vp, null);
				}
			}
		}
	}

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
