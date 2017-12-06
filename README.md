# GOEnrichment

<b>GOEnrichment is a tool for performing GO Enrichment Analysis of a set of gene products.</b>

<b>It requires as input:</b>

1) A Gene Ontology file in either OBO or OWL, and either the full GO or a GOSlim

2) An Annotation file, which can be in GAF format (from the Gene Ontology website),
   BLAST2GO format, or in tabular format (with gene ids in the first column and GO term ids in the second one)

3) A Study Set file listing the gene ids in the study (one gene product per line)
   [NOTE: the gene ids in the Study Set file must match the gene ids in the Annotation file]

4) Optionally, a Population Set listing the gene ids in the population (one gene product per line)
   [NOTE: if no Population Set file is provided, the population is assumed to consist of all genes listed in
   the Annotation file]

5) A multiple test correction strategy ("Bonferroni", "Bonferroni-Holm", "Sidak", "SDA", or "Benjamini-Hochberg")

<b>It produces as output, for each GO category</b> (Molecular Function, Biological Process, and Cellular Component)<b>:</b>

1) A tabular Result file listing all non-redundant GO terms present in the study set, their frequencies and p-values

2) A graph file in either PNG, SVG or TXT (list of relations)

<b>The [XML file](https://github.com/DanFaria/GOEnrichment/blob/master/goenrichment.xml) is setup for the [Galaxy platform](https://usegalaxy.org/)</b>


<b>Command Line Usage</b>

To run the [GOEnrichment.jar](https://github.com/DanFaria/GOEnrichment/releases/download/v2.0/GOEnrichment.jar) file from the command line, you need to have Java installed in your computer. You can run it by typing:

"java -jar GOEnrichment.jar [OPTIONS]"

The options are:

"-g,--go FILE_PATH"				Path to the Gene Ontology OBO or OWL file [Mandatory]

"-a,--annotation FILE_PATH"	Path to the tabular annotation file in GAF, BLAST2GO or 2-column table format [Mandatory]

"-s,--study FILE_PATH"			Path to the file listing the study set gene products [Mandatory]

"-p,--population FILE_PATH"	Path to the file listing the population set gene products [Optional] (Default: all the genes in the annotation file)

"-c,--correction OPTION"		Multiple test correction strategy; Options: "Bonferroni", "Bonferroni-Holm", "Sidak", "SDA", "Benjamini-Hochberg" [Optional] (Default: "Benjamini-Hochberg")

"-gf,--graph_format OPTION"	Output graph format; Options: "PNG", "SVG", "TXT" [Optional] (Default: "PNG")

"-so,--summarize_output"		Summarizes the list of enriched GO terms by removing closely related terms [Optional] (Default: FALSE)

"-e,--exclude_singletons"		Exclude GO terms that are annotated to a single gene product in the study set[Optional] (Default: FALSE)

"-o,--cut_off"						q-value or corrected p-value cut-off to apply [Optional] (Default: 0.01)

"-r,--use_all_relations"		Infer annotations through 'part_of' and other non-hierarchical relations [Optional] (Default: FALSE)

"-mfr,--mf_result FILE_PATH"	Path to the output MF result file [Optional] (Default: "MF_Result.txt")

"-bpr,--bp_result FILE_PATH"	Path to the output BP result file [Optional] (Default: "BP_Result.txt")

"-ccr,--cc_result FILE_PATH"	Path to the output CC result file [Optional] (Default: "CC_Result.txt")

"-mfg,--mf_graph FILE_PATH"	Path to the output MF graph file [Optional] (Default: "MF_Graph")

"-bpg,--bp_graph FILE_PATH"	Path to the output BP graph file [Optional] (Default: "BP_Graph")

"-ccg,--cc_graph FILE_PATH"	Path to the output CC graph file [Optional] (Default: "CC_Graph")

"-h,--help"							Display command line usage instructions
