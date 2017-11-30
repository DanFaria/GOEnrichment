/******************************************************************************
* Creates and writes graph image/text files from GOEnrichment results.        *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package graph;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;

import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.canvas.mxSvgCanvas;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxCellRenderer.CanvasFactory;
import com.mxgraph.util.mxDomUtils;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;

import main.GOEnrichment;
import ontology.GeneOntology;
import statistics.TestResult;
import util.NumberFormatter;

public class Graph
{
	private static mxGraph graph;
	private static Object parent;
	private static HashMap<Integer,Object> nodes;
	private static Vector<Object> edges;
	private static GeneOntology go;
	private static TestResult t;
	private static double cutOff;
	private static GraphFormat gf;
	
	private Graph(){}
	
	public static void save(int type, String file) throws IOException
	{
		//Get the parameters and data from the GOEnrichment class
		GOEnrichment ge = GOEnrichment.getInstance();
		go = ge.getOntology();
		cutOff = ge.getCuttoff();
		if(ge.summarizeOutput())
			t = ge.getFilteredResults()[type];
		else
			t = ge.getResults()[type];
		gf = ge.getGraphFormat();
		//Initialize the data structures
		nodes = new HashMap<Integer,Object>();
		edges = new Vector<Object>();
		//If the output is not text, initialize the mxGraph
		if(!gf.equals(GraphFormat.TXT))
		{
			graph = new mxGraph();
			parent = graph.getDefaultParent();
			graph.getModel().beginUpdate();
		}
		try
		{
			//Start with the root
			addNode(go.getRoot(type),1.0,"#FFFFFF");
			//Create and add each test result node below the cut-off
			for(int term : t.getTerms())
			{
				double pValue = t.getCorrectedPValue(term);
				if(pValue <= cutOff)
					addNode(term,t.getStudyCount(term)*1.0/t.getStudyTotal(),getColor(pValue));
			}
			//Create and add each test result node that is an ancestor of a 
			//node below the cut-off
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
			if(!gf.equals(GraphFormat.TXT))
			{
				mxHierarchicalLayout l = new mxHierarchicalLayout(graph);
				l.execute(parent);
			}
		}
		finally
		{
			if(!gf.equals(GraphFormat.TXT))
				graph.getModel().endUpdate();
		}		
		

		if(gf.equals(GraphFormat.TXT))
		{
			PrintWriter out = new PrintWriter(new FileWriter(file));
			for(Object o : edges)
				out.println(o.toString());
			out.close();
		}
		else if(gf.equals(GraphFormat.SVG))
		{
			mxSvgCanvas canvas = (mxSvgCanvas) mxCellRenderer.drawCells(graph, graph.getChildCells(parent), 1, null, new CanvasFactory()
				{
				    public mxICanvas createCanvas(int width, int height)
				    {
				        mxSvgCanvas canvas = new mxSvgCanvas(mxDomUtils.createSvgDocument(width, height));
				        canvas.setEmbedded(true);
				        return canvas;
				    } 
				}
			);
			mxUtils.writeFile(mxXmlUtils.getXml(canvas.getDocument()), file);
		}
		else
		{
			BufferedImage image = mxCellRenderer.createBufferedImage(graph, graph.getChildCells(parent), 1.0, Color.WHITE, false, null);
			ImageIO.write(image, "PNG", new File(file));
		}
	}
	
	private static void addNode(int term, double fraction, String color)
	{
		if(gf.equals(GraphFormat.TXT))
			nodes.put(term, go.getLocalName(term));
		else
		{
			String label = formatLabel(term) + "\n(" + NumberFormatter.formatPercent(fraction) + ")";
			mxCell node = (mxCell) graph.insertVertex(parent, ""+term, label, 0.0, 0.0, 0.0, 0.0, "strokeColor=#000000;fillColor="+color);
			mxRectangle rect = graph.getPreferredSizeForCell(node);
			rect.setHeight(rect.getHeight()+4.0);
			rect.setWidth(rect.getWidth()+6.0);
			graph.resizeCell(node, rect);
			nodes.put(term,node);
		}
	}
	
	private static void addEdge(int descendant, int ancestor)
	{
		int relId = go.getRelationship(descendant, ancestor).getProperty();
		String label = go.getPropertyName(relId);
		if(gf.equals(GraphFormat.TXT))
		{
			edges.add(nodes.get(descendant) + "\t" + label + "\t" +
					nodes.get(ancestor));
		}
		else
		{
			String color = "#CCCCCC";
			if(relId == -1)
			{
				label = null;
				color = "#000000";
			}
			int dashed = 0;
			if(go.getDistance(descendant, ancestor) != 1)
				dashed = 1;
			mxCell edge = (mxCell) graph.insertEdge(parent, descendant + "-" + ancestor, label,
					nodes.get(ancestor), nodes.get(descendant), "startArrow=classic;endArrow=null;dashed="+
					dashed+";fontColor="+color+";strokeColor="+color);
			edges.add(edge);
		}
	}
	
	private static String formatLabel(int term)
	{
		//Get and format the label
		String[] words = go.getLabel(term).split("[ -]");
		int limit = 15;
		for(String w : words)
			limit = Math.max(limit, w.length());
		String label = words[0];
		for(int i = 1; i < words.length; i++)
		{
			if(go.getLabel(term).charAt(label.length()) == '-')
				label += "-";
			String lastWord = label.substring(label.lastIndexOf("\n")+1);
			if(!lastWord.matches("[0-9]{1}\\-") && !words[i].matches("[0-9]{1}") && !words[i].matches("I{1,3}|IV|V") && lastWord.length() + words[i].length() > limit)
				label += "\n";
			else if(!label.endsWith("-"))
				label += " ";
			label += words[i];
		}
		return label.trim();
	}
	
	private static String getColor(double pValue)
	{
		String color;
		double fraction = (Math.log(pValue)-Math.log(cutOff))/Math.log(t.getMinCorrectedPValue());
		if(fraction <= 0)
			color = "#FFFFFF";
		else if(fraction < 0.1)
			color = "#FFFFCC";
		else if(fraction < 0.2)
			color = "#FFFFAA";
		else if(fraction < 0.3)
			color = "#FFFF88";
		else if(fraction < 0.4)
			color = "#FFFF66";
		else if(fraction < 0.5)
			color = "#FFFF00";
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