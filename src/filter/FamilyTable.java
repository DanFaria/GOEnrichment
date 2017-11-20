package filter;

import java.util.HashSet;
import java.util.LinkedHashMap;

import main.GOEnrichment;
import ontology.GeneOntology;
import statistics.TestResult;
import util.MapSorter;
import util.Table2Set;

public class FamilyTable
{
	//Attributes
	//- The Gene Ontology Enrichment instance
	private GOEnrichment goe;
	//- The Gene Ontology
	private GeneOntology go;
	//- The array of statistical test results
	private TestResult[] test;

	private Table2Set<Integer,Integer> family;
	private LinkedHashMap<Integer,Double> scores;

	//Constructor
	/**
	 * Constructs a new FamilyTable
	 */
	public FamilyTable()
	{
		family = new Table2Set<Integer,Integer>();
		scores = new LinkedHashMap<Integer,Double>();
		goe = GOEnrichment.getInstance();
		go = goe.getOntology();
		test = goe.getResults();
	}

	//Public Methods
	/**
	 * @param node: the node to be added to the FamilyTable
	 */
	public void add(int node)
	{
		if(scores.containsKey(node))
			return;
		for(Integer edge : scores.keySet())
		{
			if(goe.useAllRelations())
			{
				if(go.containsRelationship(node, edge) || go.containsRelationship(edge,node))
				{
					family.add(node, edge);
					family.add(edge, node);
				}
			}
			else
			{
				if(go.containsSubClass(node, edge) || go.containsSubClass(edge,node))
				{
					family.add(node, edge);
					family.add(edge, node);
				}
			}
			

		}
		family.add(node, node);
		int type = go.getTypeIndex(node);
		scores.put(node, go.getInfoContent(node)*test[type].getStudyCount(node)/test[type].getStudyTotal()*
				Math.ceil(-Math.log10(test[type].getCorrectedPValue(node))));
	}

	/**
	 * @return whether the family is empty
	 */
	public boolean isEmpty()
	{
		return scores.size() == 0;
	}
	
	/**
	 * @return the term with the highest score
	 */
	public Integer next()
	{
		if(scores.isEmpty())
			return null;
		LinkedHashMap<Integer,Double> weightedScores = new LinkedHashMap<Integer,Double>();
		for(Integer i : scores.keySet())
		{
			weightedScores.put(i,scores.get(i)*family.entryCount(i)/family.keyCount());
		}
		
		//The weighted scores are sorted, and the term with the highest one is considered the best term
		weightedScores = (LinkedHashMap<Integer, Double>) MapSorter.sortDescending(weightedScores);
		int bestTerm = weightedScores.keySet().iterator().next();
		HashSet<Integer> neighbours = new HashSet<Integer>(family.get(bestTerm));

		//The best term's neighbours are removed
		for(Integer i : neighbours)
		{
			HashSet<Integer> toRemove = new HashSet<Integer>();
			HashSet<Integer> aux = new HashSet<Integer>(family.get(i));
			if(neighbours.containsAll(aux))
			{
				family.remove(i);
				scores.remove(i);
			}			
			else
			{
				family.remove(i,bestTerm);
				for(Integer j : neighbours)
					family.remove(i,j);	
			}

			for(Integer j : family.keySet())
				if(family.contains(j,i))
				{
					family.remove(j,i);
					if(family.get(j).isEmpty())
						toRemove.add(j);
				}
			garbageCollector(toRemove);
		}

		family.remove(bestTerm);
		scores.remove(bestTerm);
		
		return bestTerm;
	}
	
//Private Methods
	
	/**
	 * Removes the given terms from a FamilyTable
	 * @param toRemove: the set of terms to be removed
	 */
	private void garbageCollector(HashSet<Integer> toRemove)
	{
		HashSet<Integer> toRemove2 = new HashSet<Integer>();
		for(Integer j : toRemove)
		{
			family.remove(j);
			scores.remove(j);
			for(Integer k : family.keySet())
				if(family.contains(k,j))
				{
					family.remove(k,j);
					if(family.get(k).isEmpty())
						toRemove2.add(k);
				}
		}
		if(toRemove2.size() != 0)
			garbageCollector(toRemove2);
	}
}
