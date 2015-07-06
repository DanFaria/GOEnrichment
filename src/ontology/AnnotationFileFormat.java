package ontology;

public enum AnnotationFileFormat
{
	BINGO ("bingo"),
	GAF ("GAF"),
	TABULAR ("tabular");

	private String label;
		
	AnnotationFileFormat(String l)
	{
		label = l;
	}
		
	public static AnnotationFileFormat parse(String s)
	{
		for(AnnotationFileFormat ff : AnnotationFileFormat.values())
			if(ff.label.equalsIgnoreCase(s))
				return ff;
		return null;
	}
}
