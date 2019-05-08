package ontology;

public enum GOType
{
	MF ("molecular_function", "MF", "F"),
	BP ("biological_process", "BP", "P"),
	CC ("cellular_component", "CC", "C");

	private String label;
	private String accronym;
	private String code;
	
	private GOType(String l, String a, String c)
	{
		label = l;
		accronym = a;
		code = c;
	}
	
	public static int index(String s)
	{
		int index = 0;
		for(GOType t : GOType.values())
		{
			if(s.equals(t.label))
				return index;
			index++;
		}
		return -1;
	}
	
	public static GOType parse(String s)
	{
		if(s.length() == 1)
		{
			for(GOType t : GOType.values())
				if(s.equals(t.code))
					return t;
		}
		else if(s.length() == 3)
		{
			for(GOType t : GOType.values())
				if(s.equals(t.accronym))
					return t;			
		}
		else if(s.length() == 18)
		{
			for(GOType t : GOType.values())
				if(s.equals(t.label))
					return t;			
		}
		return null;
	}	

	public String toString()
	{
		return label;
	}
}
