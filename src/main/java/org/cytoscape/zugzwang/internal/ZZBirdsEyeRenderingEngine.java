package org.cytoscape.zugzwang.internal;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.zugzwang.internal.task.TaskFactoryListener;
import org.cytoscape.zugzwang.internal.viewmodel.ZZNetworkView;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;

/** 
 * Cytoscape expects a rendering engine to have 3 renderers: 
 * main network view renderer, less detailed bird's eye view,
 * and visual property editor preview. Zugzwang doesn't have
 * bird's eye view and implements more sophisticated camera
 * controls instead. They allow to quickly switch between a
 * zoomed-in view and a global overview.
 */
class ZZBirdsEyeRenderingEngine implements RenderingEngine<CyNetwork> 
{	
	private final ZZNetworkView networkView;
	private final VisualLexicon visualLexicon;
	
	private GLJPanel panel;
	
	
	public ZZBirdsEyeRenderingEngine(JComponent component,
									 ZZNetworkView viewModel, 
									 VisualLexicon visualLexicon, 
									 TaskFactoryListener taskFactoryListener, 
									 DialogTaskManager taskManager) 
	{		
		this.networkView = viewModel;
		this.visualLexicon = visualLexicon;
	}
	
	
	@Override
	public View<CyNetwork> getViewModel() 
	{
		return networkView;
	}

	@Override
	public VisualLexicon getVisualLexicon() 
	{
		return visualLexicon;
	}

	@Override
	public Properties getProperties() 
	{
		return null;
	}
	
	@Override
	public Printable createPrintable() 
	{
		return null;
	}

	@Override
	public Image createImage(int width, int height) 
	{
		return null;
	}

	@Override
	public <V> Icon createIcon(VisualProperty<V> vp, V value, int width, int height) 
	{
		return null;
	}

	@Override
	public void printCanvas(java.awt.Graphics printCanvas) 
	{
		
	}
	
	@Override
	public String getRendererId() 
	{
		return ZZNetworkViewRenderer.ID;
	}
	
	@Override
	public void dispose() 
	{
		
	}
}
