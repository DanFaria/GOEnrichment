package util;

import java.text.DecimalFormat;

public class NumberFormatter
{

	private NumberFormatter(){}
	
	/**
	 * Formats a number in percentage with up to 2 significant figures
	 * and with a maximum precision of 0.01% 
	 * @param d: the number to convert
	 * @return the rounded percentage corresponding to the number
	 */
	public static String formatPercent(double d)
	{
		double percent = d * 100;
		if(percent >= 10)
			return Math.round(percent) + "%";
		else if(percent >= 1)
			return Math.round(percent*10)/10.0 + "%";
		else
			return Math.round(percent*100)/100.0 + "%";
	}
	
	/**
	 * Formats a double p-value (i.e. a number between 0 and 1) with
	 * 3 significant figures
	 * @param d: the number to convert
	 * @return the rounded number
	 */
	public static String formatPValue(double d)
	{
		if(d == 0.0)
			return "<1E-323";
		double f = 1000;
		if(d > 0.001)
		{
			for(double i = 0.1; i > 0.001; i/=10, f*=10)
				if(d >= i)
					return Math.round(d*f)/f + "";
		}
		DecimalFormat formatter = new DecimalFormat("0.##E0");
		return formatter.format(d).replace(',','.');
	}
}
