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

2) A graphml file using the yworks graphml syntax extension, which can be viewed in the free yED graph editor
   (http://www.yworks.com/xml/schema/graphml/1.0/doc/index.html)
   [NOTE: the graphml does not include layout information, but can be layed-out within yED]

<b>The JAR and XML files are setup for the Galaxy platform</b> (https://usegalaxy.org/)
