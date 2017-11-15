/******************************************************************************
* List of output graph formats supported by the GOEnrichment tool.            *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package graph;

public enum GraphFormat
{
	PNG ("png"),
	SVG ("svg"),
	TXT ("txt");
	
	private String label;

	private GraphFormat(String label)
	{
		this.label = label;
	}
		
	public String toString()
	{
		return label;
	}
		
	public static GraphFormat parseFormat(String label)
	{
		for(GraphFormat g : GraphFormat.values())
			if(g.label.equals(label))
				return g;
		return GraphFormat.PNG;
	}
}