package graph;

import ontology.GeneOntology;
import main.GOEnrichment;

public class Edge
{
    private String id;
    private int source;
    private int target;
    private String color;
    private String label;
    private String type;
    private final double WIDTH = 1.0;
    private final String SOURCE_ARROW = "none";
    private final String TARGET_ARROW = "standard";
    private final String LABEL_ALIGN = "center";
	private final String FONT_FAMILY = "Dialog";
	private final int FONT_SIZE = 11;
	private final String FONT_STYLE = "plain";
	private final String MODEL_NAME = "six_pos";
	private final String MODEL_POSITION = "tail";
	private final String PREF_PLACEMENT = "anywhere";
	private final double DISTANCE = 2.0;
	private final double RATIO = 0.5;
	private final boolean BEND_STYLE = false;
	
	
	public Edge(int child, int parent)
	{
		source = child;
		target = parent;
		id = child + "-" + parent;
		GeneOntology o = GOEnrichment.getInstance().getOntology();
		int relId = o.getRelationship(child, parent).getProperty();
		if(relId == -1)
		{
			label = null;
			color = "#000000";
		}
		else
		{
			label = o.getPropertyName(relId);
			color = "#CCCCCC";
		}
		int distance = o.getDistance(child, parent);
		if(distance == 1)
			type = "line";
		else
			type = "dashed";
	}
	
	public String toString()
	{
		String toReturn = "\t\t<edge id=\"" + id + "\" source=\"" + source + "\" target=\"" + target + "\">\n" +
						  "\t\t\t<data key=\"d1\">\n" +
						  "\t\t\t\t<y:PolyLineEdge>\n" +
						  "\t\t\t\t\t<y:Path sx=\"0.0\" sy=\"0.0\" tx=\"0.0\" ty=\"0.0\"/>\n" +
						  "\t\t\t\t\t<y:LineStyle type=\"" + type + "\" width=\"" + WIDTH + "\" color=\"" + color + "\"/>\n" + 
						  "\t\t\t\t\t<y:Arrows source=\"" + SOURCE_ARROW + "\" target=\"" + TARGET_ARROW + "\"/>\n";
		if(label != null)
			toReturn += "\t\t\t\t\t<y:EdgeLabel visible=\"true\" alignment=\"" + LABEL_ALIGN + "\" fontFamily=\"" + FONT_FAMILY +
						"\" fontSize=\"" + FONT_SIZE + "\" fontStyle=\"" + FONT_STYLE + "\" textColor=\"" + color + "\" modelName=\"" +
						MODEL_NAME + "\" modelPosition=\"" + MODEL_POSITION + "\" preferredPlacement=\"" + PREF_PLACEMENT +
						"\" distance=\"" + DISTANCE + "\" ratio=\"" + RATIO + "\">" + label + "</y:EdgeLabel>\n";
		toReturn += "\t\t\t\t\t<y:BendStyle smoothed=\"" + BEND_STYLE + "\"/>\n" +
					"\t\t\t\t</y:PolyLineEdge>\n" +
					"\t\t\t</data>\n" +
					"\t\t</edge>";
		return toReturn;
	}
}