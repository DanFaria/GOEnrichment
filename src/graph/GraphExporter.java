package graph;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;

import main.GOEnrichment;
import ontology.GeneOntology;
import statistics.TestResult;
import util.NumberFormatter;

public class GraphExporter
{
	private static mxGraph graph;
	private static Object parent;
	private static HashMap<Integer,Object> nodes;
	private static Vector<Object> edges;
	private static GeneOntology go;
	private static TestResult t;
	private static double cutOff;
	
	private GraphExporter(){}
	
	public static void saveGraph(int type, String file) throws IOException
	{
		go = GOEnrichment.getInstance().getOntology();
		cutOff = GOEnrichment.getInstance().getCuttoff();

		graph = new mxGraph();
		parent = graph.getDefaultParent();

		for(String s : graph.getStylesheet().getStyles().keySet())
		{
			System.out.println(s);
			for(String t : graph.getStylesheet().getStyles().get(s).keySet())
				System.out.println(t + "=" + graph.getStylesheet().getStyles().get(s).get(t));
			System.out.println("\n");
		}
			
		graph.getModel().beginUpdate();
		try
		{
			int root = go.getRoot(type);
			addNode(root,1.0,"#FFFFFF");
			//Create and add each test result node below the cut-off
			t = GOEnrichment.getInstance().getFilteredResults()[type];
			for(int term : t.getTerms())
			{
				double pValue = t.getCorrectedPValue(term);
				if(pValue <= cutOff)
					addNode(term,t.getStudyCount(term)*1.0/t.getStudyTotal(),getColor(pValue));
			}
			//Create and add each test result node that is an ancestor of a 
			//node below the cut-off
			t = GOEnrichment.getInstance().getFilteredResults()[type];
			for(int term : t.getTerms())
			{
				if(nodes.keySet().contains(term))
					continue;
				Set<Integer> descendants = go.getDescendants(term);
				for(int d : descendants)
				{
					if(nodes.keySet().contains(d))
					{
						addNode(term,t.getStudyCount(term)*1.0/t.getStudyTotal(),"#FFFFFF");
						break;
					}
				}
			}
			//Proceed with the edges
			edges = new Vector<Object>();
			for(int term : nodes.keySet())
			{
				//We create an edge between each term and each of its ancestors that...
				Set<Integer> ancestors = go.getAncestors(term);
				for(int ancestor : ancestors)
				{
					//...is present as a node in the graph and...
					boolean toAdd = nodes.keySet().contains(ancestor);
					if(!toAdd)
						continue;
					//...has no descendant in its path to the term (i.e., a descendant that
					//is an ancestor of the term and present as a node in the graph)
					for(int descendant : go.getDescendants(ancestor))
					{
						if(ancestors.contains(descendant) && nodes.keySet().contains(descendant))
						{
							toAdd = false;
							break;
						}
					}
					if(toAdd)
						addEdge(term,ancestor);
				}
			}
			mxHierarchicalLayout l = new mxHierarchicalLayout(graph);
			l.execute(parent);
		}
		finally
		{
			graph.getModel().endUpdate();
		}

		BufferedImage image = mxCellRenderer.createBufferedImage(graph, graph.getChildCells(parent), 1.0, Color.WHITE, false, null);
		ImageIO.write(image, "PNG", new File(file));
	}
	
	private static void addNode(int term, double fraction, String color)
	{
		//Get and format the label
		String[] words = GOEnrichment.getInstance().getOntology().getLabel(term).split(" ");
		int limit = 15;
		for(String w : words)
			limit = Math.max(limit, w.length());
		String label = words[0];
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
		mxCell node = (mxCell) graph.insertVertex(parent, ""+term, label, 0.0, 0.0, 0.0, 0.0, "strokeColor=#000000;fillColor="+color);
		graph.updateCellSize(node);
		nodes.put(term,node);
	}
	
	private static void addEdge(int descendant, int ancestor)
	{
		int relId = go.getRelationship(descendant, ancestor).getProperty();
		String label;
		String color;
		int dashed = 0;
		if(relId == -1)
		{
			label = null;
			color = "#000000";
		}
		else
		{
			label = go.getPropertyName(relId);
			color = "#CCCCCC";
		}
		int distance = go.getDistance(descendant, ancestor);
		if(distance != 1)
			dashed = 1;
		mxCell edge = (mxCell) graph.insertEdge(parent, descendant + "-" + ancestor, label, ancestor, descendant, "startArrow=classic;endArrow=null;dashed="+
			dashed+";fontColor="+color+";strokeColor="+color);
		edges.add(edge);
	}
	
	private static String getColor(double pValue)
	{
		String color;
		double fraction = (Math.log(pValue)-Math.log(cutOff))/Math.log(t.getMinCorrectedPValue());
		if(fraction < 0.1)
			color = "#FFFF99";
		else if(fraction < 0.2)
			color = "#FFFF77";
		else if(fraction < 0.3)
			color = "#FFFF55";
		else if(fraction < 0.4)
			color = "#FFFF33";
		else if(fraction < 0.5)
			color = "#FFFF11";
		else if(fraction < 0.6)
			color = "#FFEE00";
		else if(fraction < 0.7)
			color = "#FFCC00";
		else if(fraction < 0.8)
			color = "#FFAA00";
		else if(fraction < 0.9)
			color = "#FF8800";
		else
			color = "#FF6600";
		return color;
	}
}