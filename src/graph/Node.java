package graph;

import util.NumberFormatter;
import main.GOEnrichment;

public class Node
{
	private int id;
	private String color;
	private String label;
	private final boolean TRANSPARENT = false;
	private final String BORDER_TYPE = "line";
	private final double BORDER_WIDTH = 1.0;
	private final String BORDER_COLOR= "#000000";
	private final String SHAPE_TYPE = "rectangle";
	private final String LABEL_ALIGN = "center";
	private final String FONT_FAMILY = "Dialog";
	private final int FONT_SIZE = 12;
	private final String FONT_STYLE = "plain";
	private final String TEXT_COLOR = "#000000";
	private final String MODEL_NAME = "internal";
	private final char MODEL_POSITION = 'c';
	private final String AUTO_SIZE = "content";
	
	public Node(int term, double fraction, String color)
	{
		id = term;
		//Get and format the label
		String[] words = GOEnrichment.getInstance().getOntology().getLabel(term).split(" ");
		int limit = 15;
		for(String w : words)
			limit = Math.max(limit, w.length());
		label = words[0];
		for(int i = 1; i < words.length; i++)
		{
			String lastWord = label.substring(label.lastIndexOf("\n")+1);
			if(words[i].matches("[0-9]+") || words[i].matches("I+") || lastWord.length() + words[i].length() <= limit)
				label += " ";
			else
				label += "\n";
			label += words[i];
		}
		words = label.split("-");
		limit = 20;
		for(String w : words)
			limit = Math.max(limit, w.length());
		label = words[0];
		for(int i = 1; i < words.length; i++)
		{
			String lastWord = label.substring(label.lastIndexOf("\n")+1);
			if(lastWord.length() + words[i].length() <= limit)
				label += "-";
			else
				label += "-\n";
			label += words[i];
		}
		label += "\n(" + NumberFormatter.formatPercent(fraction) + ")";
		this.color = color;
	}
	
	public String toString()
	{
		return "\t\t<node id=\"" + id + "\">\n" + 
			   "\t\t\t<data key=\"d0\">\n" +
			   "\t\t\t\t<y:ShapeNode>\n" +
			   "\t\t\t\t\t<y:Fill color=\"" + color + "\"  transparent=\"" + TRANSPARENT + "\"/>\n" +
			   "\t\t\t\t\t<y:BorderStyle type=\"" + BORDER_TYPE + "\" width=\"" + BORDER_WIDTH + "\" color=\"" + BORDER_COLOR + "\"/>\n" +
			   "\t\t\t\t\t<y:NodeLabel visible=\"true\" alignment=\"" + LABEL_ALIGN + "\" fontFamily=\"" + FONT_FAMILY + "\" fontSize=\"" +
			   		FONT_SIZE + "\" fontStyle=\"" + FONT_STYLE + "\" textColor=\"" + TEXT_COLOR + "\" modelName=\"" + MODEL_NAME +
			   		"\" modelPosition=\"" +	MODEL_POSITION + "\" autoSizePolicy=\"" + AUTO_SIZE + "\">" + label + "\n</y:NodeLabel>\n" +
			   "\t\t\t\t\t<y:Shape type=\"" + SHAPE_TYPE + "\"/>\n" +
			   "\t\t\t\t</y:ShapeNode>\n" +
			   "\t\t\t</data>\n" +
			   "\t\t</node>";
	}
}