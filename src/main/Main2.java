/******************************************************************************
* Runs the GO Enrichment program, but produces tabular relationship files     *
* instead of graphml files, to enable viewing the results in Cytoscape.       *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package main;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import ontology.GeneOntology;
import statistics.CorrectionOption;
import statistics.TestResult;

public class Main2
{
	//Get the EnrichmentAnalysis instance
	private static GOEnrichment ea = GOEnrichment.getInstance();
	
	private static String logFile = null;
	private static String goFile = null;
	private static String annotFile = null;
	private static String popFile = null;
	private static String studyFile = null;
	private static String mfResult = "MF_result.txt";
	private static String bpResult = "BP_result.txt";
	private static String ccResult = "CC_result.txt";
	private static String mfGraph = "MF_graph.txt";
	private static String bpGraph = "BP_graph.txt";
	private static String ccGraph = "CC_graph.txt";
	private static boolean excludeSingletons = true;
	private static boolean useAllRelations = false;
	private static double cutOff = 0.05;
	private static CorrectionOption co = CorrectionOption.BENJAMINI_HOCHBERG;
	
	public static void main(String[] args)
	{
		//Process the arguments
		processArgs(args);
		//If a log file was specified, start the log
		if(logFile != null)
			ea.startLog(logFile);
		//Verify the arguments
		verifyArgs();
		
		ea.setUseAllRelations(useAllRelations);
		ea.setExcludeSingletons(excludeSingletons);
		ea.setCutOff(cutOff);
		ea.openOntology(goFile);
		ea.openAnnotationSet(annotFile);
		if(popFile != null)
			ea.openGeneSet(popFile, true);
		ea.openGeneSet(studyFile, false);
		ea.runTest();
		ea.setCorrectionOption(co);
		ea.runCorrection();
		ea.saveResult(0, mfResult);
		saveGraph(0, mfGraph);
		ea.saveResult(1, bpResult);
		saveGraph(1, bpGraph);
		ea.saveResult(2, ccResult);
		saveGraph(2, ccGraph);
		

		ea.exit();
	}

	private static void exitHelp()
	{
		System.out.println("GOEnrichment analyses a set of gene products for GO term enrichment\n");
		System.out.println("Usage: 'java -jar GOEnrichment.jar OPTIONS'\n");
		System.out.println("Options:");
		System.out.println("-g, --go FILE_PATH\tPath to the Gene Ontology OBO or OWL file");
		System.out.println("-a, --annotation FILE_PATH\tPath to the tabular annotation file (GAF, BLAST2GO or 2-column table format");
		System.out.println("-s, --study FILE_PATH\tPath to the file listing the study set gene products");
		System.out.println("[-p, --population FILE_PATH\tPath to the file listing the population set gene products]");
		System.out.println("-c, --correction OPTION\tMultiple test correction strategy (Bonferroni, Bonferroni-Holm, Sidak, SDA, or Benjamini-Hochberg");
		System.out.println("-e, --exclude_singletons\tExclude GO terms that are annotated to a single gene product in the study set");
		System.out.println("-o, --cut_off\tq-value (or corrected p-value) cut-off to apply for the graph output");
		System.out.println("-r, --use_all_relations\tInfer annotations through 'part_of' and other non-hierarchical relations");
		System.out.println("[-mfr, --mf_result FILE_PATH\tPath to the output MF result file]");
		System.out.println("[-bpr, --bp_result FILE_PATH\tPath to the output BP result file]");
		System.out.println("[-ccr, --cc_result FILE_PATH\tPath to the output CC result file]");
		System.out.println("[-mfg, --mf_graph FILE_PATH\tPath to the output MF graph file]");
		System.out.println("[-bpg, --bp_graph FILE_PATH\tPath to the output BP graph file]");
		System.out.println("[-ccg, --cc_graph FILE_PATH\tPath to the output CC graph file]");
		System.exit(0);		
	}

	private static void exitError()
	{
		System.err.println("Type 'java -jar GOEnrichment.jar -h' for details on how to run the program.");
		System.exit(1);		
	}
	
	private static void processArgs(String[] args)
	{
		if(args.length == 0)
			exitHelp();
		
		//Process the arguments
		for(int i = 0; i < args.length; i++)
		{
			if((args[i].equalsIgnoreCase("-l") || args[i].equalsIgnoreCase("--log")) &&
					i < args.length-1)
			{
				logFile = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-g") || args[i].equalsIgnoreCase("--go")) &&
					i < args.length-1)
			{
				goFile = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--annotation")) &&
					i < args.length-1)
			{
				annotFile = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-p") || args[i].equalsIgnoreCase("--population")) &&
					i < args.length-1)
			{
				popFile = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-s") || args[i].equalsIgnoreCase("--study")) &&
					i < args.length-1)
			{
				studyFile = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-c") || args[i].equalsIgnoreCase("--correction")) &&
					i < args.length-1)
			{
				co = CorrectionOption.parse(args[++i]);
			}
			else if((args[i].equalsIgnoreCase("-e") || args[i].equalsIgnoreCase("--exclude_singletons")))
			{
				excludeSingletons = true;
			}
			else if((args[i].equalsIgnoreCase("-r") || args[i].equalsIgnoreCase("--use_all_relations")))
			{
				useAllRelations = true;
			}
			else if((args[i].equalsIgnoreCase("-o") || args[i].equalsIgnoreCase("--cut_off")) &&
					i < args.length-1)
			{
				cutOff = Double.parseDouble(args[++i]);
			}
			else if((args[i].equalsIgnoreCase("-mfr") || args[i].equalsIgnoreCase("--mf_result")) &&
					i < args.length-1)
			{
				mfResult = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-bpr") || args[i].equalsIgnoreCase("--bp_result")) &&
					i < args.length-1)
			{
				bpResult = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-ccr") || args[i].equalsIgnoreCase("--cc_result")) &&
					i < args.length-1)
			{
				ccResult = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-mfg") || args[i].equalsIgnoreCase("--mf_graph")) &&
					i < args.length-1)
			{
				mfGraph = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-bpg") || args[i].equalsIgnoreCase("--bp_graph")) &&
					i < args.length-1)
			{
				bpGraph = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-ccg") || args[i].equalsIgnoreCase("--cc_graph")) &&
					i < args.length-1)
			{
				ccGraph = args[++i];
			}
			else if(args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--help"))
			{
				exitHelp();
			}
		}
	}
	
	private static void saveGraph(int type, String file)
	{
		HashSet<Integer> nodeIds = new HashSet<Integer>();
		GeneOntology go = ea.getOntology();
		int root = go.getRoot(type);
		nodeIds.add(root);
		//Create and add each test result node below the cut-off
		TestResult t = GOEnrichment.getInstance().getResults()[type];
		for(int term : t.getTerms())
		{
			double pValue = t.getCorrectedPValue(term);
			if(pValue <= cutOff)
				nodeIds.add(term);
		}
		//Create and add each test result node that is an ancestor of a 
		//node below the cut-off
		t = GOEnrichment.getInstance().getResults()[type];
		for(int term : t.getTerms())
		{
			if(nodeIds.contains(term))
				continue;
			Set<Integer> descendants = go.getDescendants(term);
			for(int d : descendants)
			{
				if(nodeIds.contains(d))
				{
					nodeIds.add(term);
					break;
				}
			}
		}
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(new FileWriter(file));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		//Proceed with the edges
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
					out.println(go.getLocalName(term) + "\t" +
							go.getPropertyName(go.getRelationship(term, ancestor).getProperty()) +
							"\t" + go.getLocalName(ancestor));
			}
		}
		out.close();
	}
	
	//Checks that all mandatory parameters were entered so that the program can proceed
	private static void verifyArgs()
	{
		if(goFile == null)
		{
			System.err.println("Error: you must specify an input ontology file.");
			exitError();
		}
		if(annotFile == null)
		{
			System.err.println("Error: you must specify an input annotation file.");
			exitError();
		}
		if(studyFile == null)
		{
			System.err.println("Error: you must specify an input study-set file.");
			exitError();
		}
		if(co == null)
		{
			System.err.println("Error: unrecognized multiple test correction option.");
			exitError();
		}		
	}
}