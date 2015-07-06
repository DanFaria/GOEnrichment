package statistics;

public enum CorrectionOption
{
	BONFERRONI ("Bonferroni"),
	BONFERRONI_HOLM ("Bonferroni-Holm"),
	SIDAK ("Sidak"),
	SDA ("SDA"),
	BENJAMINI_HOCHBERG ("Benjamini-Hochberg");
	
	String label;
	
	private CorrectionOption(String label)
	{
		this.label = label;
	}

	public static CorrectionOption parse(String string)
	{
		for(CorrectionOption c : CorrectionOption.values())
			if(c.label.equalsIgnoreCase(string))
				return c;
		return null;
	}
}
