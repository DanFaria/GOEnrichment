/**
 * Calculates hypergeometric distribution probabilities.
 * Adapted from Ontologizer (Peter N. Robinson, Sebastian Bauer)
 * 
 * @author Daniel Faria
 */

package statistics;

import java.util.Vector;
import java.lang.Math;

public class Hypergeometric
{
	//Cache of log factorials for each index value
	private Vector<Double> logFact;
	
	/**
	 * Initializes the logFact list
	 */
	public Hypergeometric()
	{
		logFact = new Vector<Double>();
		logFact.add(0, new Double(0.0));
		logFact.add(1, new Double(0.0));
	}

	/**
	 * Calculates P(X = samplePos) where X is the hypergeometric distribution
	 * with indices popTotal,popPos,sampleTotal.
	 * @param samplePos: number of successes in the sample
	 * @param sampleTotal: sample size
	 * @param popPos: number of successes in the population
	 * @param popTotal: population size
	 * @return the probability
	 */
	public double probability(int samplePos, int sampleTotal, int popPos, int popTotal)
	{
		//The number of successes in the sample cannot be greater than the number of
		//successes in the population OR the sample size; likewise, the number of failures
		//in the sample cannot be greater than the number of failures in the population
		if(samplePos > popPos || samplePos > sampleTotal || sampleTotal - samplePos > popTotal - popPos)
			return 0;
		//The max(0.0,probability) handles cases where we reach the double precision limit
		//ensuring that we don't get a negative probability
		return Math.max(0.0, Math.exp(logCombination(popPos,samplePos) + 
				logCombination(popTotal-popPos,sampleTotal-samplePos) -
				logCombination(popTotal,sampleTotal)));
	}

	/**
	 * Calculates P(X > samplePos) where X is the hypergeometric distribution
	 * with indices popTotal,popPos,sampleTotal. If lowerTail is specified, then
	 * P(X <= samplePos) is calculated.
	 * @param samplePos: number of successes in the sample
	 * @param sampleTotal: sample size
	 * @param popPos: number of successes in the population
	 * @param popTotal: population size
	 * @param lowerTail defines if P(X > samplePos) [false] or P(X <= samplePos) [true] is calculated.
	 * @return the probability
	 */
	public double probability(int samplePos, int sampleTotal, int popPos, int popTotal, boolean lowerTail)
	{
		int up = Math.min(sampleTotal,popPos);
		double p = 0.0;

		for(int i = samplePos+1; i <= up; i++)
			p += probability(i,sampleTotal,popPos,popTotal);
		//The min/max handles cases where we reach the double precision limit and
		//would get a cumulative probability > 1 when adding probabilities
		if(lowerTail)
			p = Math.max(0.0, 1.0 - p);
		else
			p = Math.min(1.0, p);
		return p;
	}
	
	//Computes the log k-combinations of sampleTotal elements 
	private double logCombination(int sampleTotal, int k)
	{
		return logFactorial(sampleTotal) - logFactorial(k) - logFactorial(sampleTotal - k);
	}

	//Returns the log factorial of i.
	//Uses a cache to avoid repeated calculations
	private double logFactorial(int i)
	{
		if(i > (logFact.size() - 1))
		{
			for(int j = logFact.size(); j <= i; j++)
			{
				double lf = logFact.get(j - 1).doubleValue()
						+ java.lang.Math.log(j);
				logFact.add(j, new Double(lf));
			}
		}
		return logFact.get(i).doubleValue();
	}
}