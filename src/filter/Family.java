package filter;

import java.util.ArrayList;
import java.util.HashSet;

import main.GOEnrichment;
import ontology.GeneOntology;

public class Family
{
	//Attributes
	private int node;
	private HashSet<Family> edges;

	
	//Constructor
	/**
	 * Constructs a new Family with a single given node
	 * @param n: node to initialize the Family
	 */
	public Family(int n)
	{
		node = n;
		edges = new HashSet<Family>();
	}

	
	//Public Methods
	@Override
	public String toString()
	{
		return toString(">");
	}

	public int getNode()
	{
		return node;
	}

	public void add(Family f)
	{
		edges.add(f);
	}

	/**
	 * @return the number of terms in a family
	 */
	public int size()
	{
		if(edges.isEmpty()) 
			return 1;
		int aux = 1;
		for(Family f : edges)
			aux += f.size();
		return aux;
	}

	@Override
	public int hashCode()
	{
		return (new Integer(node)).hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Family && node == ((Family)obj).node;
	}

	/**
	 * @return whether the family is a singleton
	 */
	public boolean isSingleton()
	{
		return edges.isEmpty();
	}

	/**
	 * @param term: the term to search in the Family
	 * @return whether the given term is contained in the family
	 */
	public boolean contains (int term)
	{
		if(node==term)
			return true;
		for(Family fam : edges)
		{
			if(fam.contains(term))
				return true;
		}
		return false;
	}

	/**
	 * @param f: the subfamily to search in the Family
	 * @return whether the given subfamily is contained in the family
	 */
	public boolean contains (Family f)
	{
		return contains(f.node);		
	}

	/**
	 * @return a list with the terms of the family
	 */
	public ArrayList<Integer> toList()
	{
		ArrayList<Integer> list = new ArrayList<Integer>();
		HashSet<Integer> set = new HashSet<Integer>();
		list.addAll(toList(set));
		return list;
	}


	//Private Methods
	/**
	 * @param prefix: the string to be added in the beginning of each 
	 * @return the string with each term in the tree and its depth (indicated with a ">")
	 */
	private String toString(String prefix)
	{
		GOEnrichment goe = GOEnrichment.getInstance();
		GeneOntology go = goe.getOntology();
		String s =	prefix + " " + go.getLabel(node) + "\n";
		for(Family f : edges)
		{
			s += f.toString(prefix + ">");
		}
		return s;
	}
	
	/**
	 * @param set: a set with terms
	 * @return the given set, with the terms of the present family added
	 */
	private HashSet<Integer> toList(HashSet<Integer> set)
	{
		set.add(node);
		for(Family fam : edges)
			fam.toList(set);
		return set;
	}

}
