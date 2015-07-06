/******************************************************************************
* A set of gene product <-> GO term annotations, read from an input file and  *
* represented as an 'indexed table'.                                          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package ontology;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import main.GOEnrichment;
import util.Table2Set;
import util.Table2Map;

public class AnnotationSet
{
	//Link to the EnrichmentAnalysis instance
	private GOEnrichment ea;
	
	//The annotation map of gene accs <-> GO terms
	private Table2Set<String,Integer> geneTerms;
	private Table2Set<Integer,String> termGenes;
	//The map of gene synonyms (for GAF file)
	private HashMap<String,String> geneSynonyms;
	
	//The table of correlation coefficients (to avoid redundant computations)
	private Table2Map<Integer,Integer,Double> corr;
	/**
	 * Constructs an AnnotationSet by reading a set of annotations from a file
	 * in one of the recognized "AnnotationFileFormat"s, then extending it
	 * for transitive closure 
	 * @param annotFile: the path to the file containing the annotations
	 * @throws IOException if it cannot open or read the input file
	 */
	public AnnotationSet(String annotFile) throws IOException
	{
		ea = GOEnrichment.getInstance();
		geneTerms = new Table2Set<String,Integer>();
		termGenes = new Table2Set<Integer,String>();
		geneSynonyms = new HashMap<String,String>();
		corr = new Table2Map<Integer,Integer,Double>();
		
		readAnnotationFile(annotFile);
		extendAnnotations();
	}
	
	/**
	 * @param gene: identifier of the gene product
	 * @return whether the gene product is listed in the annotation or synonym
	 * tables
	 */
	public boolean contains(String gene)
	{
		return geneTerms.contains(gene) || geneSynonyms.containsKey(gene);
	}
	
	/**
	 * @param gene: the identifier of the gene product
	 * @param go: the index of the GO term
	 * @return whether the gene product is listed in the annotation or synonym
	 * tables and is annotated to the GO term
	 */
	public boolean contains(String gene, int go)
	{
		return geneTerms.contains(gene,go) || (geneSynonyms.containsKey(gene)
				&& geneTerms.contains(geneSynonyms.get(gene), go));
	}
	
	/**
	 * @param go: the index of the GO term
	 * @return whether the GO term is listed in the annotation table
	 */
	public boolean contains(int go)
	{
		return termGenes.contains(go);
	}
	
	public double correlation(int goA, int goB)
	{
		int go1 = Math.min(goA, goB);
		int go2 = Math.max(goA, goB);
		if(corr.contains(go1, go2))
			return corr.get(go1, go2);
		//The total count
		int total = geneTerms.keySet().size();
		//Number of times 'go1' occurs
		int n1_ = countAnnotations(go1);
		//Number of times 'go1' doesn't occur
		int n0_ = total - n1_;
		//Number of times 'go2' occurs
		int n_1 = countAnnotations(go2);
		//Number of times 'go2' doesn't occur
		int n_0 = total - n_1;
		//Number of times they both occur
		int n11 = countAnnotations(go1,go2);
		//Number of times they both don't occur
		//which is the total minus their union
		//(with: union = sum - intersection)
		int n00 = total - (n1_ + n_1 - n11);
		//Number of times only 'go1' occurs
		int n10 = n1_ - n11;
		//Number of times only 'go2' occurs
		int n01 = n_1 - n11;
		//The phi coefficient = Pearson's correlation coefficient for binary variables
		double correlation = (n11*n00 - n10*n01)/Math.sqrt(1.0*n1_*n0_*n_1*n_0);
		corr.add(go1,go2,correlation);
		return correlation;
	}
	
	public int countAnnotations(int go)
	{
		if(termGenes.contains(go))
			return termGenes.get(go).size();
		else
			return 0;
	}
	
	public int countAnnotations(int go1, int go2)
	{
		if(!termGenes.contains(go1) || !termGenes.contains(go2))
			return 0;
		int count = 0;
		Set<String> go1Genes = termGenes.get(go1);
		for(String g : termGenes.get(go2))
			if(go1Genes.contains(g))
				count++;
		return count;
	}
	
	/**
	 * @param go: the index of the GO term for which to retrieve annotations
	 * @return the set of gene products annotated with the given GO term
	 */
	public Set<String> getAnnotations(int go)
	{
		if(termGenes.contains(go))
			return new HashSet<String>(termGenes.get(go));
		else
			return new HashSet<String>();		
	}
	
	/**
	 * @param gene: the identifier of the gene product for which to retrieve annotations
	 * @return the set of GO terms annotated to the gene product
	 */
	public Set<Integer> getAnnotations(String gene)
	{
		if(geneTerms.contains(gene))
			return new HashSet<Integer>(geneTerms.get(gene));
		else if(geneSynonyms.containsKey(gene) && geneTerms.contains(geneSynonyms.get(gene)))
			return new HashSet<Integer>(geneTerms.get(geneSynonyms.get(gene)));
		else
			return new HashSet<Integer>();		
	}
	
	/**
	 * @return the set of gene products that have annotations in this AnnotationSet
	 */
	public Set<String> getGenes()
	{
		return geneTerms.keySet();
	}
	
	/**
	 * @return the set of GO terms that have annotations in this AnnotationSet
	 */
	public Set<Integer> getGOTerms()
	{
		return termGenes.keySet();
	}
	
	/**
	 * @return the number of annotations in this AnnotationSet
	 */
	public int size()
	{
		return geneTerms.size();
	}
	
	//Extends the AnnotationSet for transitive closure
	private void extendAnnotations()
	{
		GeneOntology o = ea.getOntology();
		//We must store the new annotations in a temporary table in order
		//to avoid concurrent modifications
		Table2Set<String,Integer> tempAnnotations = new Table2Set<String,Integer>();
		for(String gene : geneTerms.keySet())
		{		
			for(int go : geneTerms.get(gene))
			{
				if(ea.useAllRelations())
				{
					for(int ancestor : o.getAncestors(go))
						tempAnnotations.add(gene, ancestor);
				}
				else
				{
					for(int ancestor : o.getSuperClasses(go, false))
						tempAnnotations.add(gene, ancestor);
				}
			}
		}
		//Once we have all the new annotations, we can add them to the
		//AnnotationSet tables
		for(String gene : tempAnnotations.keySet())
		{
			for(int go : tempAnnotations.get(gene))
			{
				geneTerms.add(gene,go);
				termGenes.add(go,gene);
			}
		}
	}

	//Reads the set of annotations listed in an input file
	private void readAnnotationFile(String annotFile) throws IOException
	{
		//Open the input file or die
		BufferedReader in = new BufferedReader(new FileReader(annotFile));
		String line = in.readLine();
		//Get the ontology
		GeneOntology o = ea.getOntology();
		//Detect the annotation file format
		AnnotationFileFormat f;
		if(line.startsWith("!"))
		{
			//A GO annotation file should start with a commented section
			//with '!' being the comment sign
			f = AnnotationFileFormat.GAF;
			while(line != null && line.startsWith("!"))
				line = in.readLine();
		}
		else if(line.startsWith("("))
		{
			//A BINGO file should start with an info line which contains
			//information within parenthesis
			f = AnnotationFileFormat.BINGO;
			while(line != null && line.startsWith("("))
				line = in.readLine();
		}
		else
		{
			//Otherwise, we assume we have a tabular file, which may or may
			//not include header information, so we skip lines until a GO
			//term is found
			f = AnnotationFileFormat.TABULAR;
			while(line != null && !line.contains("GO:"))
				line = in.readLine();
		}
		while(line != null)
		{
			String[] values;
			String gene = null, go = null, geneSyn = null;
			if(f.equals(AnnotationFileFormat.BINGO))
			{
				values = line.split(" = ");
				gene = values[0];
				go = ("GO:" + values[1]);
			}
			else
			{
				values = line.split("\t");
				if(f.equals(AnnotationFileFormat.GAF))
				{
					if(values[3].equalsIgnoreCase("NOT"))
					{
						line = in.readLine();
						continue;
					}
					gene = values[1];
					geneSyn = values[2];
					go = values[4];					
				}
				else
				{
					gene = values[0].trim();
					for(int i = 1; i < values.length; i++)
					{
						if(values[i].trim().startsWith("GO:"))
						{
							go = values[i].trim();
							break;
						}
					}
				}
			}
			line = in.readLine();
			if(!o.containsName(go))
				continue;
			int index = o.getIndexName(go);
			geneTerms.add(gene,index);
			termGenes.add(index, gene);
			if(geneSyn != null)
				geneSynonyms.put(geneSyn, gene);
		}
		in.close();
	}
}