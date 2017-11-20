package statistics;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import util.Table2Set;

public class TestResult
{
	private int populationTotal;
	private int studyTotal;
	private HashMap<Integer,Integer> populationCount;
	private HashMap<Integer,Integer> studyCount;
	private LinkedHashMap<Integer,Double> pValue;
	private LinkedHashMap<Integer,Double> correctedPValue;
	private Table2Set<Integer,String> studyAnnotations;
	private double minCorrectedPValue;

	public TestResult()
	{
		populationTotal = 0;
		studyTotal = 0;
		populationCount = new HashMap<Integer,Integer>();
		studyCount = new HashMap<Integer,Integer>();
		pValue = new LinkedHashMap<Integer,Double>();
		correctedPValue = new LinkedHashMap<Integer,Double>();
		studyAnnotations = new Table2Set<Integer,String>();
		minCorrectedPValue = 1.0;
	}
	
	public TestResult(TestResult test) {
		populationTotal = new Integer(test.populationTotal);
		studyTotal = new Integer(test.studyTotal);
		populationCount = new HashMap<Integer,Integer>(test.populationCount);
		studyCount = new HashMap<Integer,Integer>(test.studyCount);
		pValue = new LinkedHashMap<Integer,Double>(test.pValue);
		correctedPValue = new LinkedHashMap<Integer,Double>(test.correctedPValue);
		studyAnnotations = new Table2Set<Integer,String>(test.studyAnnotations);
		minCorrectedPValue = new Double (test.minCorrectedPValue);
	}
	
	public void addStudyAnnotation(int term, String gene)
	{
		studyAnnotations.add(term,gene);
	}
	
	public boolean contains(int term)
	{
		return studyCount.containsKey(term);
	}

	public double getCorrectedPValue(int term)
	{
		if(correctedPValue.containsKey(term))
			return correctedPValue.get(term);
		return -1.0;
	}
	
	public double getMinCorrectedPValue()
	{
		return minCorrectedPValue;
	}
	
	public double getPValue(int term)
	{
		if(pValue.containsKey(term))
			return pValue.get(term);
		return -1.0;
	}
	
	public int getPopulationCount(int term)
	{
		if(populationCount.containsKey(term))
			return populationCount.get(term);
		return 0;
	}
	
	public int getPopulationTotal()
	{
		return populationTotal;
	}
	
	public Set<String> getStudyAnnotations(int term)
	{
		return studyAnnotations.get(term);
	}
	
	public int getStudyCount(int term)
	{
		if(studyCount.containsKey(term))
			return studyCount.get(term);
		return 0;
	}
	
	public int getStudyTotal()
	{
		return studyTotal;
	}
	
	/**
	 * @return the set of GO terms present in the study set of gene products,
	 * ordered by p-value if p-values have been calculated, or unordered otherwise
	 */
	public Set<Integer> getTerms()
	{
		if(pValue.size() == studyCount.size())
			return pValue.keySet();
		else
			return studyCount.keySet();
	}
	
	public int getWeight(int term)
	{
		if(!correctedPValue.containsKey(term) || correctedPValue.get(term) > 0.1)
			return 4;
		if(correctedPValue.get(term) > 0.05)
			return 8;
		return 16;
	}
	
	public void incrementPopulationCount(int term)
	{
		int count = 1;
		if(populationCount.containsKey(term))
			count += populationCount.get(term);
		populationCount.put(term,count);
	}
	
	public void incrementPopulationTotal()
	{
		populationTotal++;
	}
	
	public void incrementStudyCount(int term)
	{
		int count = 1;
		if(studyCount.containsKey(term))
			count += studyCount.get(term);
		studyCount.put(term,count);
	}
	
	public void incrementStudyTotal()
	{
		studyTotal++;
	}
	
	public void removeTerm(int term)
	{
		populationCount.remove(term);
		studyCount.remove(term);
		pValue.remove(term);
		correctedPValue.remove(term);
	}
	
	public void setCorrectedPValue(int term, double p)
	{
		correctedPValue.put(term,p);
		if(minCorrectedPValue > p)
			minCorrectedPValue = p;
	}

	public void setPValue(int term, double p)
	{
		pValue.put(term,p);
	}
	
	public void setPopulationTotal(int total)
	{
		populationTotal = total;
	}
	
	public void setPopulationCount(int term, int count)
	{
		populationCount.put(term,count);
	}
	
	public void setStudyTotal(int total)
	{
		studyTotal = total;
	}
	
	public void sortPValues()
	{
        LinkedList<Map.Entry<Integer,Double>> list = new LinkedList<Map.Entry<Integer,Double>>(pValue.entrySet());
	    Collections.sort(list,new Comparator<Map.Entry<Integer,Double>>()
	    {
	    	public int compare(Map.Entry<Integer,Double> o1, Map.Entry<Integer,Double> o2)
	        {
	    		return o1.getValue().compareTo(o2.getValue());
	        }
	    } );

	    pValue = new LinkedHashMap<Integer,Double>();
        for (Map.Entry<Integer,Double> entry : list)
            pValue.put(entry.getKey(),entry.getValue());
	}
}