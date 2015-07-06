/******************************************************************************
* A relationship between two terms (classes) in the GeneOntology, including   *
* the distance between them (number of edges) and the type of relationship    *
* (i.e., the id of the object property).                                      *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package ontology;

public class Relationship implements Comparable<Relationship>
{
	
//Attributes

	//The distance between the terms
	private int distance;
	//The subclass property of the relationship (-1 for 'is_a' relationships)
	private int property;

//Constructors
	
	/**
	 * Constructs a new Relationship with the given distance and type
	 * @param dist: the distance in the Relationship
	 * @param p: the property of the Relationship
	 */
	public Relationship(int dist, int p)
	{
		distance = dist;
		property = p;
	}
	
	/**
	 * Constructs a new Relationship with the distance and type of
	 * the given Relationship
	 * @param r: the Relationship to copy
	 */
	public Relationship(Relationship r)
	{
		distance = r.distance;
		property = r.property;
	}
	
//Public Methods

	@Override
	/**
	 * Relationships are compared with regard to property, restriction,
	 * then distance ('is_a' relationships supersede all other properties,
	 * then 'all values' supersede 'some values' restrictions, then if all
	 * else is equal, distance is the tie-breaker)
	 */
	public int compareTo(Relationship r)
	{
		int value = 0;
		if(property == -1 && r.property == -1)
			value = distance - r.distance;
		else if(property == -1)
			value = 1;
		else if(r.property == -1)
			value = -1;
		return value;
	}
	
	/**
	 * Two Relationships are equal if they have the same property and restriction
	 * regardless of distance, to enable checking whether two entities are
	 * related via a given type of relationship
	 */
	public boolean equals(Object o)
	{
		if(o instanceof Relationship)
		{
			Relationship r = (Relationship)o;
			return property == r.property;
		}
		else
			return false;
	}
	
	/**
	 * @return the distance of the Relationship
	 */
	public int getDistance()
	{
		return distance;
	}
	
	/**
	 * @return the property of the Relationship
	 */
	public int getProperty()
	{
		return property;
	}
}