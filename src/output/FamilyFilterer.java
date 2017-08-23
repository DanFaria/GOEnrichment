package output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import main.GOEnrichment;
import ontology.GeneOntology;
import statistics.TestResult;

public class FamilyFilterer
{
	//Attributes
	//- The array of statistical test results
	private TestResult[] test;
	//- The Gene Ontology Enrichment instance
	private GOEnrichment goe;
	//- The Gene Ontology
	private GeneOntology go;
	//- The set which contains the families
	private HashSet<Family> families;
	//- The list which contains the families
	private ArrayList<Family> familyList;

	//Constructor
	public FamilyFilterer()
	{
		goe = GOEnrichment.getInstance();
		go = goe.getOntology();
		test = goe.getResults();
	}
	
	//Public Methods
	/**
	 * Filters the shown terms after a GOEnrichment analysis
	 */
	public void filterer()
	{
		TestResult[] finalTestResults = new TestResult[3];
		//Goes through the GO types
		for(int i = 0; i < 3; i++)
		{
			//Initializes the set and list which will contain the families
			families = new HashSet<Family>();
			familyList = new ArrayList<Family>();
			//Builds the families
			for(Integer j : go.getDescendants(go.getRoot(i),3,-1))
				familyBuilder(null,i,j);

			//Removes repeated families that may be contained in bigger families
			HashSet<Family> aux = new HashSet <Family>();	
			for(int j = familyList.size()-1; j > 0; j--)
			{
				for(int k = j-1; k >= 0; k--)
					if(familyList.get(j).contains(familyList.get(k)))
						aux.add(familyList.get(k));
			}		
			for(Family fam : aux)
				families.remove(fam);

			//Initializes a set which will include the filtered terms
			HashSet<Integer> initialResult = new HashSet<Integer>();
			HashSet<Integer> finalResults = new HashSet<Integer>();
			FamilyTable famTab = new FamilyTable();
			
			//Clones the TestResult used at the moment
			TestResult filteredTest = new TestResult(test[i]);
			for(Family fam : families)
			{
				initialResult.addAll(fam.toList());
				if(fam.isSingleton())
				{
					finalResults.add(fam.getNode());
					continue;
				}

				for(int node : fam.toList())
					famTab.add(node);
				
				//Filters the terms for each family
				HashSet<Integer> filtered = new HashSet<Integer>();
				while(!famTab.isEmpty())
				{
					int next = famTab.next();
					filtered.add(next);
				}
				//The final results now include the singletons and the filtered terms per family
				finalResults.addAll(filtered);

			}
			
			HashMap<Integer,Double> scores = new HashMap<Integer,Double>(filteredTest.getScore());
			
			for(int term : scores.keySet())
			{
				if(!finalResults.contains(term) && (initialResult.contains(term) || filteredTest.getCorrectedPValue(term)>=0.01))
				{
					filteredTest.removeTerm(term);
				}
			}
			finalTestResults[i] = filteredTest;
		}
		goe.setFilteredResults(finalTestResults);
	}

	
	//Private Methods	
	/**
	 * @param term: the term to search in the family
	 * @return whether the given term is contained on the family
	 */
	private boolean contains(int term)
	{
		for(Family fam : families)
		{
			if(fam.contains(term))
				return true;
		}
		return false;
	}
	
	/**
	 * @param family: current family, where the term is to be added (initially null)
	 * @param branch: the branch in which the family is to be inserted, depending on its GO type
	 * @param term: the term which may be added
	 */
	private void familyBuilder(Family family, int branch, int term)
	{
		//The term is only added if it is present in the test results
		if(!test[branch].contains(term))
			return;

		Set<Integer> children = go.getSubClasses(term, true);

		//If this condition is fulfilled, the term will be added to the family
		if(test[branch].getCorrectedPValue(term) < 0.01)
		{ 
			if(family==null)
			{
				if(!contains(term))
				{
					family = new Family(term);
					if(families.add(family))
						familyList.add(family);
					//Continues to build the family from the children of the term
					for(Integer i : children)
						familyBuilder(family,branch,i);
				}
				else 
					return;
			}
			else
			{
				Family child = new Family(term);
				family.add(child);
				//Continues to build the family from the children of the term
				for(Integer i : children)
					familyBuilder(child,branch,i);
			}
		}
		//Otherwise, new families may be built from the descendants 
		else
		{
			for(Integer i : children)
				familyBuilder(null,branch,i);
		}
	}

}