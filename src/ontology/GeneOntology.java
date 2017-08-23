/******************************************************************************
* The Gene Ontology object, loaded from either the OWL or OBO release, using  *
* the OWL API.                                                                *
* Adapted from AgreementMakerLight.                                           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package ontology;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import util.Table2Set;
import util.Table3List;

public class GeneOntology
{

//Attributes

	//The OWL Ontology Manager and Data Factory
	private OWLOntologyManager manager;
	//The entity expansion limit property
    private final String LIMIT = "entityExpansionLimit";

	//The uri <-> numeric index map of ontology classes 
	protected HashMap<String,Integer> uriClasses;
	protected HashMap<Integer,String> classUris;
	//The local name <-> numeric index map of ontology classes 
	protected HashMap<String,Integer> nameClasses;
	protected HashMap<Integer,String> classNames;
	//The numeric index -> label map of ontology classes
	protected HashMap<Integer,String> classLabels;
	protected HashMap<String,Integer> labelClasses;

	//The uri <-> numeric index map of ontology object properties 
	protected HashMap<String,Integer> uriProperties;
	protected HashMap<Integer,String> propertyUris;
	protected HashMap<Integer,String> propertyNames;
	//Map of object properties that are transitive over other object properties
	//(including themselves)
	protected Table2Set<Integer,Integer> transitiveOver;
	
	//Map between ancestor classes and their descendants (with transitive closure)
	private Table3List<Integer,Integer,Relationship> descendantMap;
	//Map between descendant classes and their ancestors (with transitive closure)
	private Table3List<Integer,Integer,Relationship> ancestorMap;
	
	//The map of term indexes -> GOType indexes in the ontology
	private HashMap<Integer,Integer> termTypes;
	//The array of term indexes of the GOType roots
	private int[] rootIndexes;
	
	private HashSet<String> deprecated;
	private HashMap<String,String> alternatives;

//Constructors

	/**
	 * Constructs an empty ontology
	 */
	public GeneOntology()
	{
		//Initialize the data structures
		uriClasses = new HashMap<String,Integer>();
		classUris = new HashMap<Integer,String>();
		nameClasses = new HashMap<String,Integer>();
		classNames = new HashMap<Integer,String>();
		classLabels = new HashMap<Integer,String>();
		labelClasses = new HashMap<String,Integer>();
		uriProperties = new HashMap<String,Integer>();
		propertyUris = new HashMap<Integer,String>();
		propertyNames = new HashMap<Integer,String>();
		transitiveOver = new Table2Set<Integer,Integer>();
		descendantMap = new Table3List<Integer,Integer,Relationship>();
		ancestorMap = new Table3List<Integer,Integer,Relationship>();
		termTypes = new HashMap<Integer,Integer>();
		rootIndexes = new int[3];
		deprecated = new HashSet<String>();
		alternatives = new HashMap<String,String>();
        //Increase the entity expansion limit to allow large ontologies
        System.setProperty(LIMIT, "1000000");
        //Get an Ontology Manager
        manager = OWLManager.createOWLOntologyManager();
	}
	
	/**
	 * Constructs an Ontology from file 
	 * @param path: the path to the input Ontology file
	 * @throws OWLOntologyCreationException 
	 */
	public GeneOntology(String path) throws OWLOntologyCreationException
	{
		this();
        //Load the local ontology
        File f = new File(path);
        OWLOntology o;
		o = manager.loadOntologyFromOntologyDocument(f);
		init(o);
		//Close the OntModel
        manager.removeOntology(o);
        //Reset the entity expansion limit
        System.clearProperty(LIMIT);
	}
	
	/**
	 * Constructs an Ontology from an URI  
	 * @param uri: the URI of the input Ontology
	 * @throws OWLOntologyCreationException 
	 */
	public GeneOntology(URI uri) throws OWLOntologyCreationException
	{
		this();
        OWLOntology o;
        //Check if the URI is local
        if(uri.toString().startsWith("file:"))
		{
			File f = new File(uri);
			o = manager.loadOntologyFromOntologyDocument(f);
		}
		else
		{
			IRI i = IRI.create(uri);
			o = manager.loadOntology(i);
		}
		init(o);
		//Close the OntModel
        manager.removeOntology(o);
        //Reset the entity expansion limit
        System.clearProperty(LIMIT);
	}
	
//Public Methods

	/**
	 * @return the number of Classes in the Ontology
	 */
	public int classCount()
	{
		return classUris.size();
	}
	
	/**
	 * @param index: the index of the class in the ontology
	 * @return whether the class belongs to the ontology
	 */
	public boolean containsIndex(int index)
	{
		return classUris.containsKey(index);
	}

	/**
	 * @param name: the local name of the class in the ontology
	 * @return whether the class belongs to the ontology
	 */
	public boolean containsName(String name)
	{
		return nameClasses.containsKey(name) || alternatives.containsKey(name);
	}
	
	/**
	 * @param child: the index of the child class
	 * @param parent: the index of the parent class
	 * @return whether the ontology contains a relationship between child and parent
	 */
	public boolean containsRelationship(int child, int parent)
	{
		return descendantMap.contains(parent,child);
	}
	
	/**
	 * @param child: the index of the child class
	 * @param parent: the index of the parent class
	 * @return whether the ontology contains an 'is_a' relationship between child and parent
	 */	
	public boolean containsSubClass(int child, int parent)
	{
		if(!descendantMap.contains(parent,child))
			return false;
		Vector<Relationship> rels = descendantMap.get(parent,child);
		for(Relationship r : rels)
			if(r.getProperty() == -1)
				return true;
		return false;
	}
	
	/**
	 * @param uri: the URI of the class in the ontology
	 * @return whether the class belongs to the ontology
	 */
	public boolean containsUri(String uri)
	{
		return uriClasses.containsKey(uri);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of ancestors of the given class
	 */
	public Set<Integer> getAncestors(int classId)
	{
		if(ancestorMap.contains(classId))
			return ancestorMap.keySet(classId);
		return new HashSet<Integer>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @return the list of ancestors at the given distance from the input class
	 */
	public Set<Integer> getAncestors(int classId, int distance)
	{
		HashSet<Integer> asc = new HashSet<Integer>();
		if(!ancestorMap.contains(classId))
			return asc;
		for(Integer i : ancestorMap.keySet(classId))
			for(Relationship r : ancestorMap.get(classId, i))
				if(r.getDistance() == distance)
					asc.add(i);
		return asc;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of ancestors of the input class that are at the given
	 * distance and with the given property
	 */
	public Set<Integer> getAncestors(int classId, int distance, int prop)
	{
		HashSet<Integer> asc = new HashSet<Integer>();
		if(!ancestorMap.contains(classId))
			return asc;
		for(Integer i : ancestorMap.keySet(classId))
			for(Relationship r : ancestorMap.get(classId, i))
				if(r.getDistance() == distance && r.getProperty() == prop)
					asc.add(i);
		return asc;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of ancestors at the given distance from the input class
	 */
	public Set<Integer> getAncestorsProperty(int classId, int prop)
	{
		HashSet<Integer> asc = new HashSet<Integer>();
		if(!ancestorMap.contains(classId))
			return asc;
		for(Integer i : ancestorMap.keySet(classId))
			for(Relationship r : ancestorMap.get(classId, i))
				if(r.getProperty() == prop)
					asc.add(i);
		return asc;
	}
	
	/**
	 * @return the set of classes with ancestors in the map
	 */
	public Set<Integer> getChildren()
	{
		if(ancestorMap != null)
			return ancestorMap.keySet();
		return new HashSet<Integer>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of direct children of the given class
	 */
	public Set<Integer> getChildren(int classId)
	{
		return getDescendants(classId,1);
	}

	/**
	 * @param classes: the set the class to search in the map
	 * @return the list of direct subclasses shared by the set of classes
	 */
	public Set<Integer> getCommonSubClasses(Set<Integer> classes)
	{
		if(classes == null || classes.size() == 0)
			return null;
		Iterator<Integer> it = classes.iterator();
		Vector<Integer> subclasses = new Vector<Integer>(getSubClasses(it.next(),false));
		while(it.hasNext())
		{
			HashSet<Integer> s = new HashSet<Integer>(getSubClasses(it.next(),false));
			for(int i = 0; i < subclasses.size(); i++)
			{
				if(!s.contains(subclasses.get(i)))
				{
					subclasses.remove(i);
					i--;
				}
			}
		}
		for(int i = 0; i < subclasses.size()-1; i++)
		{
			for(int j = i+1; j < subclasses.size(); j++)
			{
				if(containsSubClass(subclasses.get(i),subclasses.get(j)))
				{
					subclasses.remove(i);
					i--;
					j--;
				}
				if(containsSubClass(subclasses.get(j),subclasses.get(i)))
				{
					subclasses.remove(j);
					j--;
				}
			}
		}
		return new HashSet<Integer>(subclasses);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of descendants of the input class
	 */
	public Set<Integer> getDescendants(int classId)
	{
		if(descendantMap.contains(classId))
			return descendantMap.keySet(classId);
		return new HashSet<Integer>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @return the list of descendants at the given distance from the input class
	 */
	public Set<Integer> getDescendants(int classId, int distance)
	{
		HashSet<Integer> desc = new HashSet<Integer>();
		if(!descendantMap.contains(classId))
			return desc;
		for(Integer i : descendantMap.keySet(classId))
			for(Relationship r : descendantMap.get(classId, i))
				if(r.getDistance() == distance)
					desc.add(i);
		return desc;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of descendants of the input class at the given distance
	 * and with the given property
	 */
	public Set<Integer> getDescendants(int classId, int distance, int prop)
	{
		HashSet<Integer> desc = new HashSet<Integer>();
		if(!descendantMap.contains(classId))
			return desc;
		for(Integer i : descendantMap.keySet(classId))
			for(Relationship r : descendantMap.get(classId, i))
				if(r.getDistance() == distance && r.getProperty() == prop)
					desc.add(i);
		return desc;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of descendants at the given distance from the input class
	 */
	public Set<Integer> getDescendantsProperty(int classId, int prop)
	{
		HashSet<Integer> desc = new HashSet<Integer>();
		if(!descendantMap.contains(classId))
			return desc;
		for(Integer i : descendantMap.keySet(classId))
			for(Relationship r : descendantMap.get(classId, i))
				if(r.getProperty() == prop)
					desc.add(i);
		return desc;
	}
	
	/**
	 * @param child: the index of the child class
	 * @param parent: the index of the parent class
	 * @return the minimal distance between the child and parent,
	 * or 0 if child==parent, or -1 if they aren't related
	 */
	public int getDistance(int child, int parent)
	{
		if(child == parent)
			return 0;
		if(!ancestorMap.contains(child, parent))
			return -1;
		Vector<Relationship> rels = ancestorMap.get(child,parent);
		int distance = rels.get(0).getDistance();
		for(Relationship r : rels)
			if(r.getDistance() < distance)
				distance = r.getDistance();
		return distance;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of equivalences of the given class
	 */
	public Set<Integer> getEquivalences(int classId)
	{
		return getDescendants(classId, 0);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of classes equivalent to the given class
	 */
	public Set<Integer> getEquivalentClasses(int classId)
	{
		return getDescendants(classId,0,-1);
	}
	
	/**
	 * @param name: the local name of the class to get from the Ontology
	 * @return the index of the corresponding name in the Ontology
	 */
	public int getIndexName(String name)
	{
		if(nameClasses.containsKey(name))
			return nameClasses.get(name);
		if(alternatives.containsKey(name) &&
				nameClasses.containsKey(alternatives.get(name)))
			return nameClasses.get(alternatives.get(name));
		return -1;
	}
	
	/**
	 * @param uri: the URI of the class to get from the Ontology
	 * @return the index of the corresponding name in the Ontology
	 */
	public int getIndexUri(String uri)
	{
		return uriClasses.get(uri);
	}	
	
	/**
	 * @param term: the integer representing a term, from the Ontology
	 * @return the information content of the given term
	 */
	public double getInfoContent(int term) {
		return 1-Math.log(1+getSubClasses(term,false).size())/
				Math.log(1+getSubClasses(rootIndexes[getTypeIndex(term)],false).size());
	}
	
	
	/**
	 * @param index: the index of the class to get the label
	 * @return the label of the class with the given index
	 */
	public String getLabel(int index)
	{
		return classLabels.get(index);
	}
	
	/**
	 * @param index: the index of the class to get the name
	 * @return the local name of the class with the given index
	 */
	public String getLocalName(int index)
	{
		return classNames.get(index);
	}
	
	/**
	 * @return the set of classes with ancestors in the map
	 */
	public Set<Integer> getParents()
	{
		if(descendantMap != null)
			return descendantMap.keySet();
		return new HashSet<Integer>();
	}

	/**
	 * @param class: the id of the class to search in the map
	 * @return the list of direct parents of the given class
	 */
	public Set<Integer> getParents(int classId)
	{
		return getAncestors(classId,1);
	}
	
	/**
	 * @param index: the index of the class to get the name
	 * @return the local name of the class with the given index
	 */
	public String getPropertyName(int index)
	{
		if(index == -1)
			return "is_a";
		else
			return propertyNames.get(index);
	}
	
	/**
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @return the 'best' relationship between the two classes
	 */
	public Relationship getRelationship(int child, int parent)
	{
		if(!ancestorMap.contains(child, parent))
			return null;
		Relationship rel = ancestorMap.get(child).get(parent).get(0);
		for(Relationship r : ancestorMap.get(child).get(parent))
			if(r.compareTo(rel) > 0)
				rel = r;
		return rel;
	}

	/**
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @return the relationships between the two classes
	 */
	public Vector<Relationship> getRelationships(int child, int parent)
	{
		return ancestorMap.get(child).get(parent);
	}
	
	/**
	 * @param index: the GOType index (0 = MF; 1 = BP; 2 = CC)
	 * @return the Ontology index of the root term for that GOType
	 */
	public int getRoot(int index)
	{
		return rootIndexes[index];
	}
		
	/**
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return just the direct subclasses or all subclasses
	 * @return the list of direct or indirect subclasses of the input class
	 */
	public Set<Integer> getSubClasses(int classId, boolean direct)
	{
		if(direct)
			return getDescendants(classId,1,-1);
		else
			return getDescendantsProperty(classId,-1);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return just the direct superclasses or all superclasses
	 * @return the list of direct or indirect superclasses of the input class
	 */
	public Set<Integer> getSuperClasses(int classId, boolean direct)
	{
		if(direct)
			return getAncestors(classId,1,-1);
		else
			return getAncestorsProperty(classId,-1);
	}
	
	/**
	 * @param index: the index of the GO term to get
	 * @return the GOType of the GO term with the given index
	 */
	public GOType getType(int index)
	{
		return GOType.values()[termTypes.get(index)];
	}
	
	/**
	 * @param index: the index of the GO term to get
	 * @return the GOType index of the GO term with the given index
	 * (0 = MF; 1 = BP; 2 = CC)
	 */
	public int getTypeIndex(int index)
	{
		if(termTypes.containsKey(index))
			return termTypes.get(index);
		else
		{
			//System.out.println(classNames.get(index));
			return 1;
		}
	}
	
	/**
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @param property: the id of the property between child and parent
	 * @return whether there is a relationship between child and parent
	 *  with the given property
	 */
	public boolean hasProperty(int child, int parent, int property)
	{
		Vector<Relationship> rels = getRelationships(child,parent);
		for(Relationship r : rels)
			if(r.getProperty() == property)
				return true;
		return false;
	}
	
	/**
	 * @return the number of relationships in the map
	 */
	public int relationshipCount()
	{
		return ancestorMap.size();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return all subclasses or just the direct ones
	 * @return the number of direct or indirect subclasses of the input class
	 */
	public int subClassCount(int classId, boolean direct)
	{
		return getSubClasses(classId,direct).size();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return all superclasses or just the direct ones
	 * @return the number of direct or indirect superclasses of the input class
	 */
	public int superClassCount(int classId, boolean direct)
	{
		return getSuperClasses(classId,direct).size();
	}	
	
//Private Methods	

	//Builds the ontology data structures
	private void init(OWLOntology o)
	{
		//Get the classes and their names and synonyms
		getClasses(o);
		//Get the properties
		getProperties(o);
		//Build the relationship map
		getRelationships(o);
		//Extend the relationship map
		transitiveClosure();
	}
	
	//Processes the classes, their lexical information and cross-references
	private void getClasses(OWLOntology o)
	{
		int index = 0;
		//Get an iterator over the ontology classes
		Set<OWLClass> classes = o.getClassesInSignature(true);
		for(OWLClass c : classes)
		{
			//Then get the URI for each class
			String classUri = c.getIRI().toString();
			if(classUri == null || classUri.endsWith("owl#Thing") || classUri.endsWith("owl:Thing"))
				continue;
			//Check if the class is deprecated
			boolean dep = false;
			for(OWLAnnotation a : c.getAnnotations(o))
			{
				if(a.getProperty().toString().equals("owl:deprecated")  && ((OWLLiteral)a.getValue()).parseBoolean())
				{
					dep = true;
					break;
				}
			}
			//If it is, record it and skip it
			if(dep)
			{
				deprecated.add(getLocalName(classUri).replace('_', ':'));
				continue;
			}
			
			classUris.put(++index,classUri);
			uriClasses.put(classUri, index);
			
			//Get the local name from the URI
			String name = getLocalName(classUri).replace('_', ':');
			classNames.put(index,name);
			nameClasses.put(name, index);
			
			//Now get the class's label and type
			Set<OWLAnnotation> annots = c.getAnnotations(o);
			for(OWLOntology ont : o.getImports())
				annots.addAll(c.getAnnotations(ont));
            for(OWLAnnotation annotation : annots)
            {
            	//Label
            	if(annotation.getProperty().toString().equals("rdfs:label") && annotation.getValue() instanceof OWLLiteral)
            	{
            		OWLLiteral val = (OWLLiteral) annotation.getValue();
            		String lab = val.getLiteral();
            		classLabels.put(index,lab);
            		labelClasses.put(lab, index);
            		//If the label is a GOType, then the term is a root
            		int typeIndex = GOType.index(lab);
            		if(typeIndex > -1)
            			rootIndexes[typeIndex] = index;
	            }
            	//Type
            	else if(annotation.getProperty().toString().contains("hasOBONamespace") && annotation.getValue() instanceof OWLLiteral)
            	{
            		OWLLiteral val = (OWLLiteral) annotation.getValue();
            		String type = val.getLiteral();
            		int typeIndex = GOType.index(type);
            		if(typeIndex > -1)
            			termTypes.put(index, typeIndex);
	            }
            	//Alternative
            	if(annotation.getProperty().toString().contains("hasAlternativeId") && annotation.getValue() instanceof OWLLiteral)
            	{
            		OWLLiteral val = (OWLLiteral) annotation.getValue();
            		String alt = val.getLiteral();
            		alternatives.put(alt, name);
	            }
	        }
		}
	}
	
	//Reads the object properties
	private void getProperties(OWLOntology o)
	{
		int index = 0;
		//Get the Object Properties
		Set<OWLObjectProperty> oProps = o.getObjectPropertiesInSignature(true);
    	for(OWLObjectProperty op : oProps)
    	{
    		//Get the URI of each property
    		String propUri = op.getIRI().toString();
    		if(propUri == null)
    			continue;
    		propertyUris.put(++index,propUri);
    		uriProperties.put(propUri, index);
    		//Get its label
			Set<OWLAnnotation> annots = op.getAnnotations(o);
			for(OWLOntology ont : o.getImports())
				annots.addAll(op.getAnnotations(ont));
            for(OWLAnnotation annotation : annots)
            {
            	//Label
            	if(annotation.getProperty().toString().equals("rdfs:label") && annotation.getValue() instanceof OWLLiteral)
            	{
            		OWLLiteral val = (OWLLiteral) annotation.getValue();
            		String lab = val.getLiteral();
            		propertyNames.put(index,lab);
            		break;
	            }
            }
			//If it is transitive, add it to the transitiveOver map
    		//(as transitive over itself)
			if(op.isTransitive(o))
				transitiveOver.add(index,index);
    	}
    	//Process transitive_over relations (this needs to be done
    	//in a 2nd loop as all properties must already be indexed)
    	for(OWLObjectProperty op : oProps)
    	{
			//Transitive over relations go to the transitiveOver map
			for(OWLAxiom e : op.getReferencingAxioms(o))
			{
				//In OWL, the OBO transitive_over relation is encoded as a sub-property chain of
				//the form: "SubObjectPropertyOf(ObjectPropertyChain( <p1> <p2> ) <this_p> )"
				//in which 'this_p' is usually 'p1' but can also be 'p2' (in case you want to
				//define that another property is transitive over this one, which may happen when
				//the other property is imported and this property occurs only in this ontology)
				if(!e.isOfType(AxiomType.SUB_PROPERTY_CHAIN_OF))
					continue;
				//Unfortunately, there isn't much support for "ObjectPropertyChain"s in the OWL
				//API, so the only way to get the referenced properties while preserving their
				//order is to parse the String representation of the sub-property chain
				//(getObjectPropertiesInSignature() does NOT preserve the order)
				String[] chain = e.toString().split("[\\(\\)]");
				//Make sure the structure of the sub-property chain is in the expected format
				if(!chain[0].equals("SubObjectPropertyOf") || !chain[1].equals("ObjectPropertyChain"))
					continue;
				//Get the indexes of the tags surrounding the URIs
				int index1 = chain[2].indexOf("<")+1;
				int index2 = chain[2].indexOf(">");
				int index3 = chain[2].lastIndexOf("<")+1;
				int index4 = chain[2].lastIndexOf(">");
				//Make sure the indexes check up
				if(index1 < 0 || index2 <= index1 || index3 <= index2 || index4 <= index3)
					continue;
				String uri1 = chain[2].substring(index1,index2);
				String uri2 = chain[2].substring(index3,index4);
				//Make sure the URIs are listed object properties
				if(!uriProperties.containsKey(uri1) || !uriProperties.containsKey(uri2))
					continue;
				//If everything checks up, add the relation to the transitiveOver map
				transitiveOver.add(uriProperties.get(uri1), uriProperties.get(uri2));
			}
    	}
	}

	//Reads all class relationships
	private void getRelationships(OWLOntology o)
	{
		//Get an iterator over the ontology classes
		Set<OWLClass> classes = o.getClassesInSignature(true);
		//For each term index (from 'termURIs' list)
		for(OWLClass c : classes)
		{
			if(!uriClasses.containsKey(c.getIRI().toString()))
				continue;
			//Get the subclass expressions to capture and add relationships
			Set<OWLClassExpression> superClasses = c.getSuperClasses(o);
			for(OWLOntology ont : o.getDirectImports())
				superClasses.addAll(c.getSuperClasses(ont));
			for(OWLClassExpression e : superClasses)
				addRelationship(o,c,e,true);
			
			//Get the equivalence expressions to capture and add relationships
			Set<OWLClassExpression> equivClasses = c.getEquivalentClasses(o);
			for(OWLOntology ont : o.getDirectImports())
				equivClasses.addAll(c.getEquivalentClasses(ont));
			for(OWLClassExpression e : equivClasses)
				addRelationship(o,c,e,false);
		}
		
	}
	
	//Get the local name of an entity from its URI
	private String getLocalName(String uri)
	{
		int index = uri.indexOf("#") + 1;
		if(index == 0)
			index = uri.lastIndexOf("/") + 1;
		return uri.substring(index);
	}
	
	private void addRelationship(OWLOntology o, OWLClass c, OWLClassExpression e, boolean sub)
	{
		int child = uriClasses.get(c.getIRI().toString());
		int parent = -1;
		int distance = (sub) ? 1 : 0;
		int prop = -1;
		ClassExpressionType type = e.getClassExpressionType();
		//If it is a class, process it here
		if(type.equals(ClassExpressionType.OWL_CLASS))
		{
			String par = e.asOWLClass().getIRI().toString();
			if(!uriClasses.containsKey(par))
				return;
			parent = uriClasses.get(par);
		}
		//If it is a 'some values' object property restriction, process it
		else if(type.equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM) ||
				type.equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
		{
			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLObjectProperty p = props.iterator().next();
			String propUri = p.getIRI().toString();
			if(!uriProperties.containsKey(propUri))
				return;
			prop = uriProperties.get(propUri);
			Set<OWLClass> sup = e.getClassesInSignature();
			if(sup == null || sup.size() != 1)
				return;					
			OWLClass cls = sup.iterator().next();
			String par = cls.getIRI().toString();
			if(!uriClasses.containsKey(par))
				return;
			parent = uriClasses.get(par);
		}
		//If it is an intersection of classes, capture the implied subclass relationships
		else if(type.equals(ClassExpressionType.OBJECT_INTERSECTION_OF))
		{
			Set<OWLClassExpression> inter = e.asConjunctSet();
			for(OWLClassExpression cls : inter)
				addRelationship(o,c,cls,true);
		}
		if(parent < 1)
			return;
		Relationship r = new Relationship(distance,prop);
		descendantMap.add(parent,child,r);
		ancestorMap.add(child,parent,r);

	}
	
	/**
	 * Compute the transitive closure of the RelationshipMap
	 * by adding inherited relationships (and their distances)
	 * This is an implementation of the Semi-Naive Algorithm
	 */
	public void transitiveClosure()
	{
		Set<Integer> t = descendantMap.keySet();
		int lastCount = 0;
		for(int distance = 1; lastCount != descendantMap.size(); distance++)
		{
			lastCount = descendantMap.size();
			for(Integer i : t)
			{
				Set<Integer> childs = getChildren(i);
				childs.addAll(getEquivalences(i));
				Set<Integer> pars = getAncestors(i,distance);
				for(Integer j : pars)
				{
					Vector<Relationship> rel1 = getRelationships(i,j);
					for(int k = 0; k < rel1.size(); k++)
					{
						Relationship r1 = rel1.get(k);
						int p1 = r1.getProperty();
						for(Integer h : childs)
						{
							Vector<Relationship> rel2 = getRelationships(h,i);
							for(int l = 0; l < rel2.size(); l++)
							{
								Relationship r2 = rel2.get(l);
								int p2 = r2.getProperty();
								//We only do transitive closure if at least one of the properties
								//is 'is_a' (-1) or the child property is transitive over the parent
								//(which covers the case where they are both the same transitive
								//property)
								if(!(p1 == -1 || p2 == -1 || transitiveOver.contains(p2,p1)))
									continue;
								int dist = r1.getDistance() + r2.getDistance();
								//The child property wins in most cases: if p2 = p1,
								//if p2 transitive_over p1, and otherwise if p1 = is_a
								int prop = p2;
								//The parent property only wins if p2 = is_a and p1 != is_a
								if(p2 == -1 || p1 != -1)
									prop = p1;
								Relationship r = new Relationship(dist,prop);
								descendantMap.add(j,h,r);
								ancestorMap.add(h,j,r);
							}
						}
					}
				}
			}
		}
	}	
}