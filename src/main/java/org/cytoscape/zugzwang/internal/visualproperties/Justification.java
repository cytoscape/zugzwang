package org.cytoscape.zugzwang.internal.visualproperties;


public enum Justification 
{
	JUSTIFY_CENTER("Center Justified", "c", Anchor.LABEL_WRAP_JUSTIFY_CENTER), 
	JUSTIFY_LEFT("Left Justified", "l", Anchor.LABEL_WRAP_JUSTIFY_LEFT), 
	JUSTIFY_RIGHT("Right Justified", "r", Anchor.LABEL_WRAP_JUSTIFY_RIGHT);
	
	private static String[] JUSTIFY;

	private final String displayName;
	private final String shortName;
	
	private final int nativeConstant;

	private Justification(final String displayName, final String shortName, final int nativeConstant) 
	{
		this.displayName = displayName;
		this.shortName = shortName;
		this.nativeConstant = nativeConstant;
	}

	public String getName() 
	{
		return this.displayName;
	}

	public String getShortName() 
	{
		return this.shortName;
	}
	
	public int getConversionConstant() 
	{
		return this.nativeConstant;
	}
	
	public static Justification parse(final String value) 
	{
		for (final Justification j : values())
			if (j.getName().equals(value) || j.getShortName().equals(value))
				return j;

		return null;
	}


	public static String[] getNames() 
	{
		// Lazy instantiation
		if (JUSTIFY == null) 
		{
			JUSTIFY = new String[values().length];
			int i = 0;
			for (Justification j: values()) 
				JUSTIFY[i++] = j.displayName;
		}
		
		return JUSTIFY;
	}
}
