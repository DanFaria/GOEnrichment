package graph;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import ontology.GeneOntology;
import statistics.TestResult;
import main.GOEnrichment;

public class Graph
{
	private GeneOntology go;
	private TestResult t;
	private HashSet<Integer> nodeIds;
	private Vector<Node> nodes;
	private Vector<Edge> edges;
	private double cutOff;
	
	private final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
								  "<!-- This file was written by GOEnrichment.-->\n" +
								  "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" " +
								  	 "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
								  	 "xmlns:y=\"http://www.yworks.com/xml/graphml\" " +
								  	 "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns " +
								  	 "http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd\">";
	private final String KEY = "\t<key id=\"d0\" for=\"node\" yfiles.type=\"nodegraphics\"/>\n" +
							   "\t<key id=\"d1\" for=\"edge\" yfiles.type=\"edgegraphics\"/>";
	private final String START = "\t<graph edgedefault=\"directed\" id=\"G\">";
	private final String END = "\t</graph>\n</graphml>";

	public Graph(int type)
	{
		//Start with the nodes
		nodes = new Vector<Node>();
		nodeIds = new HashSet<Integer>();
		//Create and add the root node
		go = GOEnrichment.getInstance().getOntology();
		cutOff = GOEnrichment.getInstance().getCuttoff();
		int root = go.getRoot(type);
		nodes.add(new Node(root,1.0,"#FFFFFF"));
		nodeIds.add(root);
		//Create and add each test result node below the cut-off
		t = GOEnrichment.getInstance().getFilteredResults()[type];
		for(int term : t.getTerms())
		{
			double pValue = t.getCorrectedPValue(term);
			if(pValue <= cutOff)
			{
				nodes.add(new Node(term,t.getStudyCount(term)*1.0/t.getStudyTotal(),getColor(pValue)));
				nodeIds.add(term);
			}
		}
		//Create and add each test result node that is an ancestor of a 
		//node below the cut-off
		t = GOEnrichment.getInstance().getFilteredResults()[type];
		for(int term : t.getTerms())
		{
			if(nodeIds.contains(term))
				continue;
			Set<Integer> descendants = go.getDescendants(term);
			for(int d : descendants)
			{
				if(nodeIds.contains(d))
				{
					nodes.add(new Node(term,t.getStudyCount(term)*1.0/t.getStudyTotal(),"#FFFFFF"));
					nodeIds.add(term);
					break;
				}
			}
		}
		//Proceed with the edges
		edges = new Vector<Edge>();
		for(int term : nodeIds)
		{
			//We create an edge between each term and each of its ancestors that...
			Set<Integer> ancestors = go.getAncestors(term);
			for(int ancestor : ancestors)
			{
				//...is present as a node in the graph and...
				boolean toAdd = nodeIds.contains(ancestor);
				if(!toAdd)
					continue;
				//...has no descendant in its path to the term (i.e., a descendant that
				//is an ancestor of the term and present as a node in the graph)
				for(int descendant : go.getDescendants(ancestor))
				{
					if(ancestors.contains(descendant) && nodeIds.contains(descendant))
					{
						toAdd = false;
						break;
					}
				}
				if(toAdd)
					edges.add(new Edge(term,ancestor));
			}
		}
	}
	
	public void save(String file) throws IOException
	{
		PrintWriter out = new PrintWriter(new FileWriter(file));
		out.println(HEADER);
		out.println(KEY);
		out.println(START);
		for(Node n : nodes)
			out.println(n.toString());
		for(Edge e : edges)
			out.println(e.toString());
		out.println(END);
		out.close();
		
	}
	
	private String getColor(double pValue)
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