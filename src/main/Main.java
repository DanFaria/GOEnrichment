/******************************************************************************
 * Runs the GO Enrichment program.                                             *
 *                                                                             *
 * @author Daniel Faria                                                        *
 ******************************************************************************/

package main;

import graph.GraphFormat;
import statistics.CorrectionOption;

public class Main
{
	//Get the GOEnrichment instance
	private static GOEnrichment ea = GOEnrichment.getInstance();

	private static String logFile = null;
	private static String goFile = null;
	private static String annotFile = null;
	private static String popFile = null;
	private static String studyFile = null;
	private static String mfResult = "MF_result.txt";
	private static String bpResult = "BP_result.txt";
	private static String ccResult = "CC_result.txt";
	private static String mfGraph = "MF_graph";
	private static String bpGraph = "BP_graph";
	private static String ccGraph = "CC_graph";
	private static GraphFormat format = GraphFormat.PNG;
	private static boolean summarizeOutput = false;
	private static boolean excludeSingletons = false;
	private static boolean useAllRelations = false;
	private static double cutOff = 0.01;
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

		ea.setSummarizeOutput(summarizeOutput);
		ea.setUseAllRelations(useAllRelations);
		ea.setExcludeSingletons(excludeSingletons);
		ea.setCutOff(cutOff);
		ea.setGraphFormat(format);
		ea.openOntology(goFile);
		ea.openAnnotationSet(annotFile);
		if(popFile != null)
			ea.openGeneSet(popFile, true);
		ea.openGeneSet(studyFile, false);
		ea.runTest();
		ea.setCorrectionOption(co);
		ea.runCorrection();
		if(summarizeOutput)
			ea.filter();
		ea.saveResult(0, mfResult);
		ea.saveGraph(0, mfGraph);
		ea.saveResult(1, bpResult);
		ea.saveGraph(1, bpGraph);
		ea.saveResult(2, ccResult);
		ea.saveGraph(2, ccGraph);
		
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
		System.out.println("[-c, --correction OPTION\tMultiple test correction strategy (Bonferroni, Bonferroni-Holm, Sidak, SDA, or Benjamini-Hochberg)]");
		System.out.println("[-gf, --graph_format OPTION\tOutput graph format (PNG,SVG,TXT)]");
		System.out.println("[-so, --summarize_output\tSummarizes the list of enriched GO terms by removing closely related terms]");
		System.out.println("[-e, --exclude_singletons\tExclude GO terms that are annotated to a single gene product in the study set]");
		System.out.println("[-o, --cut_off\tq-value (or corrected p-value) cut-off to apply for the graph output]");
		System.out.println("[-r, --use_all_relations\tInfer annotations through 'part_of' and other non-hierarchical relations]");
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
			else if((args[i].equalsIgnoreCase("-gf") || args[i].equalsIgnoreCase("--graph_format")) &&
					i < args.length-1)
			{
				format = GraphFormat.parseFormat(args[++i]);
			}
			else if((args[i].equalsIgnoreCase("-so") || args[i].equalsIgnoreCase("--summarize_output")))
			{
				summarizeOutput = true;
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
