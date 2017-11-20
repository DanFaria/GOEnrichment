/******************************************************************************
* Singleton class that controls the state of the EnrichmentAnalysis program,  *
* by recording all options and holding links to all data structures.          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package main;

import graph.Graph;
import graph.GraphFormat;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import filter.FamilyFilterer;
import ontology.AnnotationSet;
import ontology.GeneOntology;
import statistics.CorrectionOption;
import statistics.FisherExactTest;
import statistics.MultipleTestCorrection;
import statistics.TestResult;
import util.NumberFormatter;

public class GOEnrichment
{
	//Singleton pattern: unique instance
	private static GOEnrichment ea = new GOEnrichment();
	
	//Logging:
	//- Output stream 
	private FileOutputStream log;
	//- Date format
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//Data Structures:
	//- The Gene Ontology
	private GeneOntology go;
	//- The set of annotations
	private AnnotationSet as;
	//- The set of study gene products
	private HashSet<String> studySet = null;
	//- The (optional) set of population gene products
	private HashSet<String> populationSet = null;
	//- The array of statistical test results
	private TestResult[] results;
	//- The array of statistical filtered test results
	private TestResult[] filteredResults;
	
	//Options:
	private GraphFormat gf;
	private CorrectionOption c;
	private boolean summarizeOutput;
	private boolean excludeSingletons;
	private boolean useAllRelations;
	private double cutOff;
		
	
	private GOEnrichment()
	{
		results = new TestResult[3];
	}
	
	public void exit()
	{
		if(log != null)
		{
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
		}
		System.exit(0);
	}
	
	public void filter() 
	{
		System.out.println(df.format(new Date()) + " - Running family filterer");
		FamilyFilterer fam = new FamilyFilterer();
		fam.filterer();
		System.out.println(df.format(new Date()) + " - Finished");
	}
	
	public static GOEnrichment getInstance()
	{
		return ea;
	}
	
	public AnnotationSet getAnnotationSet()
	{
		return as;
	}
	
	public boolean excludeSingletons()
	{
		return excludeSingletons;
	}
	
	public CorrectionOption getCorrectionOption()
	{
		return c;
	}
	
	public double getCuttoff()
	{
		return cutOff;
	}
	
	public GraphFormat getGraphFormat()
	{
		return gf;
	}

	public GeneOntology getOntology()
	{
		return go;
	}
	
	public HashSet<String> getPopulationSet()
	{
		return populationSet;
	}
	
	public TestResult[] getResults()
	{
		return results;
	}
	
	public TestResult[] getFilteredResults()
	{
		return filteredResults;
	}

	public HashSet<String> getStudySet()
	{
		return studySet;
	}
	
	public void openAnnotationSet(String file)
	{
		try
		{
			System.out.println(df.format(new Date()) + " - Reading annotations from '" + file + "'");
			as = new AnnotationSet(file);
			System.out.println(df.format(new Date()) + " - Read " + as.size() + " annotations");
		}
		catch(IOException e)
		{
			System.err.println(df.format(new Date()) + " - Error: could not read annotation set '" + file + "'!");
			e.printStackTrace();
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
			System.exit(1);
		}
	}
	
	public void openOntology(String file)
	{
		try
		{
			System.out.println(df.format(new Date()) + " - Reading ontology from '" + file + "'");
			go = new GeneOntology(file);
			System.out.println(df.format(new Date()) + " - Finished");
		}
		catch(OWLOntologyCreationException e)
		{
			System.err.println(df.format(new Date()) + " - Error: could not read ontology '" + file + "'!");
			e.printStackTrace();
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
			System.exit(1);
		}
	}
	
	/**
	 * Opens a gene product set file, which is expected to be a plain text file
	 * containing one or more columns (separated by one of: space, tab, comma, or
	 * semicolon) with the gene product identifier listed in the first column 
	 * @param file: the path to the input gene product file
	 * @param isPopulation: whether the set is a population set or a study set
	 */
	public void openGeneSet(String file, boolean isPopulation)
	{
		if(isPopulation)
		{
			populationSet = new HashSet<String>();
			System.out.println(df.format(new Date()) + " - Reading population set from '" + file + "'");
		}
		else
		{
			studySet = new HashSet<String>();
			System.out.println(df.format(new Date()) + " - Reading study set from '" + file + "'");
		}
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			String notFound = "";
			int count = 0;
			while((line = in.readLine()) != null)
			{	
				String[] word = line.split("[ \t,;]");
				if(word[0].length() > 0)
				{
					if(as.contains(word[0]))
					{
						if(isPopulation)
							populationSet.add(word[0]);
						else
							studySet.add(word[0]);
					}
					else
					{
						notFound += word[0] + ",";
						count++;
						if(count%15==0)
							notFound += "\n";
					}
				}
			}
			in.close();
			if(notFound.length() > 0)
				System.out.println("Warning: the following gene products were not listed in the " +
					"annotation file and were ignored:\n" + notFound.substring(0, notFound.length()-1));

		}
		catch(IOException e)
		{
			System.err.println("Error: could not read gene product set '" + file + "'!");
			e.printStackTrace();
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
			System.exit(1);
		}
		if(isPopulation)
			System.out.println(df.format(new Date()) + " - Read " + populationSet.size() + " genes");
		else
			System.out.println(df.format(new Date()) + " - Read " + studySet.size() + " genes");
	}
	
	public void runCorrection()
	{
		System.out.println(df.format(new Date()) + " - Running multiple test correction");
		MultipleTestCorrection mtc = new MultipleTestCorrection();
		mtc.correct();
		System.out.println(df.format(new Date()) + " - Finished");
	}
	
	public void runTest()
	{
		System.out.println(df.format(new Date()) + " - Running Fisher's exact test");
		FisherExactTest f = new FisherExactTest();
		f.test();
		System.out.println(df.format(new Date()) + " - Finished");
	}
	
	public void saveGraph(int index, String file)
	{
		try
		{
			System.out.println(df.format(new Date()) + " - Saving graph file '" + file + "'");
			Graph.save(index, file);
			System.out.println(df.format(new Date()) + " - Finished");
		}
		catch(IOException e)
		{
			System.err.println("Error: could not write graph file '" + file + "'!");
			e.printStackTrace();
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
			System.exit(1);
		}
	}
	
	public void saveResult(int index, String file)
	{
		try
		{
			System.out.println(df.format(new Date()) + " - Saving result file '" + file + "'");
			PrintWriter out = new PrintWriter(new FileWriter(file));
			TestResult r;
			if(summarizeOutput)
				r = filteredResults[index];
			else
				r = results[index];
				
			//First write the header
			out.print("GO Term\tStudy #\tStudy Freq.\tPop. Freq.\tp-value\t");
			if(c.equals(CorrectionOption.BENJAMINI_HOCHBERG))
				out.print("q-value\t");
			else
				out.print("corrected p-value\t");
			out.println("name\tgene products");
			//Then write the term information (in ascending p-value order)
			for(int term : r.getTerms())
			{
				out.print(go.getLocalName(term) + "\t");
				out.print(r.getStudyCount(term) + "\t");
				out.print(NumberFormatter.formatPercent(r.getStudyCount(term)*1.0/r.getStudyTotal()) + "\t");
				out.print(NumberFormatter.formatPercent(r.getPopulationCount(term)*1.0/r.getPopulationTotal()) + "\t");
				out.print(NumberFormatter.formatPValue(r.getPValue(term)) + "\t");
				out.print(NumberFormatter.formatPValue(r.getCorrectedPValue(term)) + "\t");
				out.print(go.getLabel(term) + "\t");
				String genes = "";
				for(String gene : r.getStudyAnnotations(term))
					genes += gene + ",";
				out.println(genes.substring(0, genes.length()-1));
			}
			out.close();
			System.out.println(df.format(new Date()) + " - Finished");
		}
		catch(IOException e)
		{
			System.err.println("Error: could not write result file '" + file + "'!");
			e.printStackTrace();
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
			System.exit(1);			
		}
	}
	
	public void setCorrectionOption(CorrectionOption c)
	{
		this.c = c;
	}
	
	public void setCutOff(double c)
	{
		this.cutOff = c;
	}
	
	public void setExcludeSingletons(boolean b)
	{
		this.excludeSingletons = b;
	}
	
	public void setGraphFormat(GraphFormat gf)
	{
		this.gf = gf;
	}
	
	public void setResults(TestResult[] results)
	{
		this.results = results;
	}
	
	public void setFilteredResults(TestResult[] results)
	{
		this.filteredResults = results;
	}
	
	public void setSummarizeOutput(boolean b)
	{
		this.summarizeOutput = b;
	}
	
	public void setUseAllRelations(boolean b)
	{
		this.useAllRelations = b;
	}
	
	public void startLog(String file)
	{
		try
		{
			//Initialize the log
			log = new FileOutputStream(file);
			//Redirect stdOut and stdErr to the log file
	      	System.setOut(new PrintStream(log, true));
	       	System.setErr(new PrintStream(log, true));		
		}
		catch(IOException e)
		{
			System.out.println(df.format(new Date()) + " - Warning: could not initiate log file!");
		}
	}
	
	public boolean summarizeOutput()
	{
		return summarizeOutput;
	}
	
	public boolean useAllRelations()
	{
		return useAllRelations;
	}
}