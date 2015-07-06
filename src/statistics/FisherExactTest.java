/******************************************************************************
* An implementation of Fisher's exact test applied to GO enrichment analysis. *
* The p-value of each term is computed from the hypergeometric cumulative     *
* distribution, based on the term's statistics (study count - 1, study total, *
* population count, and population total).                                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package statistics;

import java.util.HashSet;
import java.util.Set;

import main.GOEnrichment;
import ontology.AnnotationSet;
import ontology.GeneOntology;

public class FisherExactTest
{
	private TestResult[] testByType;
	private GOEnrichment ea;
	private GeneOntology o;
	private AnnotationSet a;
	Hypergeometric h;
	
	public FisherExactTest()
	{
		ea = GOEnrichment.getInstance();
		o = ea.getOntology();
		a = ea.getAnnotationSet();
		testByType = ea.getResults();
		for(int i = 0; i < 3; i++)
			testByType[i] = new TestResult();
		h = new Hypergeometric();
	}

	public void test()
	{
		//Iterate through the study set
		for(String s : ea.getStudySet())
		{
			//And through each gene product's annotations
			for(int go : a.getAnnotations(s))
			{
				//Get the type index of the GO term
				int index = o.getTypeIndex(go);
				//Increment the study count of that term
				testByType[index].incrementStudyCount(go);
				//Add the annotation
				testByType[index].addStudyAnnotation(go, s);
				//If the term is a type root, increment the study total
				if(go == o.getRoot(index))
					testByType[index].incrementStudyTotal();
			}
		}
		//Exclude terms annotated to a single gene product (if that option is set)
		if(ea.excludeSingletons())
		{
			for(int i = 0; i < 3; i++)
			{
				//First we identify them
				HashSet<Integer> single = new HashSet<Integer>();
				for(int go : testByType[i].getTerms())
					if(testByType[i].getStudyCount(go) == 1)
						single.add(go);
				//Then we remove them
				for(int r : single)
					testByType[i].removeTerm(r);
				//The root, while not single is irrelevant (it will necessarily
				//(have p-value = 1, so there is no point in testing it)
				testByType[i].removeTerm(o.getRoot(i));
			}
		}
		//Get the population set
		Set<String> populationSet = ea.getPopulationSet();
		//If no population set was defined, the set of genes in
		//the AnnotationSet is considered the population, and
		//we can retrieve the counts directly
		if(populationSet == null)
		{
			for(int i = 0; i < 3; i++)
			{
				testByType[i].setPopulationTotal(a.countAnnotations(o.getRoot(i)));
				for(int go : testByType[i].getTerms())
					testByType[i].setPopulationCount(go, a.countAnnotations(go));
			}
		}
		//Otherwise, we must repeat the computations done for the study set
		//Iterate through the population set
		else
		{
			for(String s : ea.getPopulationSet())
			{
				//And through each gene product's annotations
				for(int go : a.getAnnotations(s))
				{
					//Get the type index of the GO term
					int index = o.getTypeIndex(go);
					//We can skip the GO terms that are not in the study set
					if(!testByType[index].contains(go))
						continue;
					//Increment the population count of that term
					testByType[index].incrementPopulationCount(go);
					//If the term is a type root, increment the population total
					if(go == o.getRoot(index))
						testByType[index].incrementPopulationTotal();
				}
			}
		}
		//Remove redundant terms from the TestResults
		for(int i = 0; i < 3; i++)
		{
			//First we identify them
			HashSet<Integer> redundant = new HashSet<Integer>();
			for(int go : testByType[i].getTerms())
			{
				//A term is redundant if its study count...
				int count = testByType[i].getStudyCount(go);
				Set<Integer> descendants;
				if(ea.useAllRelations())
					descendants = o.getChildren(go);
				else
					descendants = o.getSubClasses(go, true);
				for(int desc : descendants)
				{
					//Is equal to the study count of any one of its descendants
					//(in which case all its annotations are inferred from
					//that descendant)
					if(testByType[i].getStudyCount(desc) == count)
					{
						redundant.add(go);
						break;
					}
				}
			}
			//Then we remove them
			for(int r : redundant)
				testByType[i].removeTerm(r);
			//The root, while not redundant is irrelevant (it will necessarily
			//(have p-value = 1, so there is no point in testing it)
			testByType[i].removeTerm(o.getRoot(i));
		}
		//Finally, we can compute the p-values
		for(int i = 0; i < 3; i++)
		{
			for(int go : testByType[i].getTerms())
			{
				//The p-value is given directly by the hypergeometric cumulative
				//distribution, with studyPos = studyCount-1 because we want the
				//probability of having at least as many annotations
				double p = h.probability(testByType[i].getStudyCount(go) - 1,
					testByType[i].getStudyTotal(), testByType[i].getPopulationCount(go),
					testByType[i].getPopulationTotal(), false);
				testByType[i].setPValue(go, p);
			}
			//We sort the results by p-value for convenience
			//(and to facilitate stepwise corrections)
			testByType[i].sortPValues();
		}
	}
}