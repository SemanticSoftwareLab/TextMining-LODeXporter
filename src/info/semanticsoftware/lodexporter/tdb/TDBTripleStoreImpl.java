/*
 * LODeXporter -- http://www.semanticsoftware.info/lodexporter
 *
 * This file is part of the LODeXporter component.
 *
 * Copyright (c) 2015, 2016, 2017 Semantic Software Lab, http://www.semanticsoftware.info
 *    René Witte
 *    Bahar Sateli
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package info.semanticsoftware.lodexporter.tdb;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang.NullArgumentException;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.apache.jena.vocabulary.RDF;

import gate.FeatureMap;
import gate.util.GateRuntimeException;
import info.semanticsoftware.lodexporter.PropertyMapping;
import info.semanticsoftware.lodexporter.RelationMapping;
import info.semanticsoftware.lodexporter.SubjectMapping;
import info.semanticsoftware.lodexporter.TripleStoreInterface;

import org.apache.log4j.Logger;

/**
 * Concrete implementation for interacting with a TDB-based triple store.
 * 
 * @author Bahar Sateli
 * @author René Witte
 */
public class TDBTripleStoreImpl implements TripleStoreInterface {

    private Dataset dataset;
    private Model model;
    private Map<String, Property> propertyModelHash;
    private Map<String, Property> relationModelHash;
    // FIXME why using a diff uri?
    private final String puboBaseURI = "http://lod.semanticsoftware.info/pubo/pubo#";
    private Property hasAnnotation;
    private Property hasDocument;
    private Property hasCompetencyRecord;
    private Property competenceFor;

    protected static final Logger LOGGER = Logger.getLogger(TDBTripleStoreImpl.class);

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#connect(java.lang.String)
     */
    @Override
    public final void connect(final String dir) {
        dataset = TDBFactory.createDataset(dir);
        LOGGER.debug("[connect] File-based Dataset is now: " + dataset);
    }

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#connect()
     */
    @Override
	public final void connect() {
		dataset = TDBFactory.createDataset();
        LOGGER.debug("[connect] Memory-based Dataset is now: " + dataset);
	}
    
    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#loadRules(java.net.URL)
     */
    @Override
	public final void loadMappingRulesFromFile(final URL file) {
    	model = dataset.getDefaultModel();
    	TDBLoader.loadModel(model, file.toExternalForm());
        LOGGER.debug("[loadRules] Finished loading mapping rules from " + file.toExternalForm());
	}
    
    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#disconnect()
     */
    @Override
    public final void disconnect() {
    	// TODO should we call TDBFactory.release(dataset) here, too?
        dataset.close();
        TDBFactory.release(dataset);  // was TDBFactory.reset();
        LOGGER.debug("[disconnect] Dataset is now: " + dataset);
    }

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#beginTransaction(info.semanticsoftware.lodexporter.TripleStoreInterface.TransactionType)
     */
    @Override
    public final void beginTransaction(final TransactionType type) {
        if (type == TransactionType.READ) {
            dataset.begin(ReadWrite.READ);
        } else {
            dataset.begin(ReadWrite.WRITE);
        }
    }

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#endTransaction()
     */
    @Override
    public final void endTransaction() {
        dataset.commit(); // commit the transaction, otherwise it would be
                          // aborted when calling end()
        dataset.end();
    }

    /**
     * Generates a map of &lt;rulename,SubjectMapping&gt; objects from the query
     * results.
     * 
     * @param query
     *            the SPARQL query
     * @return a map of &lt;rulename,SubjectMapping&gt; objects
     * @throws Exception
     *             from the TDB implementation
     * 
     * @see info.semanticsoftware.lodexporter.LODeXporter#init()
     * 
     */
    @Override
    public final Map<String, SubjectMapping> getSubjectMappings(final String query) throws Exception {
        final ResultSet rs = queryMappings(query);
        return populateSubjectHash(rs);
    }

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#getPropertyMappings(java.lang.String)
     */
    @Override
    public final Map<String, LinkedList<PropertyMapping>> getPropertyMappings(final String query)
            throws Exception {
        HashMap<String, LinkedList<PropertyMapping>> propMapList = null;
        final ResultSet rs = queryMappings(query);
        propMapList = populatePropertyMapList(rs);
        prepareTDBPropertyModel(propMapList);

        return propMapList;
    }

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#getRelationMappings(java.lang.String)
     */
    @Override
    public final Map<String, LinkedList<RelationMapping>> getRelationMappings(final String query) throws Exception {
        HashMap<String, LinkedList<RelationMapping>> relationMapList = null;
        final ResultSet rs = queryMappings(query);
        relationMapList = (HashMap<String, LinkedList<RelationMapping>>) populateRelationMapList(rs);
        prepareTDBRelationModel(relationMapList);

        return relationMapList;
    }

    private ResultSet queryMappings(final String query) {
        final QueryExecution qExec = QueryExecutionFactory.create(query, dataset);
        return qExec.execSelect();
    }

    private Map<String, SubjectMapping> populateSubjectHash(final ResultSet rs) {
        final Map<String, SubjectMapping> subjectHash = new HashMap<String, SubjectMapping>();

        try {
            /*
             * Iterate through the SPARQL query results and creates a new
             * SubjectMapping object. Each result is supposed to contain: 
             * - ?rule rule name 
             * - ?baseURI base URI 
             * - ?GATEtype GATE annotation type 
             * - ?type rdf:type value
             */
            while (rs.hasNext()) {
                // TODO issue a warning/exception when the rule is incomplete?
                final QuerySolution soln = rs.nextSolution();
                
                final RDFNode ruleNode = soln.get("?rule");
                String ruleString = null;
                if (ruleNode != null)
                    ruleString = ruleNode.asResource().getURI();

                final RDFNode baseURINode = soln.get("?baseURI");
                String baseURIString = null;
                if (baseURINode != null)
                    baseURIString = baseURINode.asResource().getURI();

                final RDFNode GATETypeNode = soln.get("?GATEtype");
                String GATETypeString = null;
                if (GATETypeNode != null)
                    GATETypeString = GATETypeNode.asLiteral().getString();

                final RDFNode typeNode = soln.get("?type");
                String typeString = null;
                if (typeNode != null)
                    typeString = typeNode.asResource().getURI();

                final SubjectMapping newMap = new SubjectMapping(ruleString, baseURIString,
                        typeString, GATETypeString); // NOPMD
                subjectHash.put(ruleString, newMap);
            }
        } catch (Exception e) {
            LOGGER.error("Error reading the subject mappings.", e);
        }

        LOGGER.debug("----- SUBJECT MAPLIST: " + subjectHash);
        return subjectHash;
    }

    private HashMap<String, LinkedList<PropertyMapping>> populatePropertyMapList(final ResultSet rs) {
        final HashMap<String, LinkedList<PropertyMapping>> propertyHash = new HashMap<String, LinkedList<PropertyMapping>>();

        try {
            while (rs.hasNext()) {
                final QuerySolution soln = rs.nextSolution();

                final RDFNode ruleNode = soln.get("?rule");
                String ruleString = null;
                if (ruleNode != null)
                    ruleString = ruleNode.asResource().getURI();

                final RDFNode GATEtypeNode = soln.get("?GATEtype");
                String GATEtypeString = null;
                if (GATEtypeNode != null)
                    GATEtypeString = GATEtypeNode.asLiteral().getString();

                /*
                 * RDFNode baseURINode = soln.get("?baseURI"); String
                 * baseURIString = null; if (baseURINode != null) baseURIString
                 * = baseURINode.asResource().getURI();
                 */

                final RDFNode GATEfeatureNode = soln.get("?GATEfeature");
                String GATEfeatureString = null;
                if (GATEfeatureNode != null)
                    GATEfeatureString = GATEfeatureNode.asLiteral().getString();

                final RDFNode GATEattributeNode = soln.get("?GATEattribute");
                String GATEattributeString = null;
                if (GATEattributeNode != null)
                    GATEattributeString = GATEattributeNode.asLiteral().getString();

                final RDFNode typeNode = soln.get("?type");
                String typeString = null;
                if (typeNode != null)
                    typeString = model.expandPrefix(typeNode.asResource().getURI());

                final PropertyMapping newMap = new PropertyMapping(ruleString, typeString,
                        GATEtypeString, GATEfeatureString, GATEattributeString); // NOPMD

                if (propertyHash.containsKey(ruleString)) {
                    propertyHash.get(ruleString).add(newMap);
                } else {
                    final LinkedList<PropertyMapping> propertyMaps = new LinkedList<>();
                    propertyMaps.add(newMap);
                    propertyHash.put(ruleString, propertyMaps);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error populating the property hashmap.", e);
        }

        LOGGER.debug("----- PROPERTY HASHMAP:" + propertyHash);
        return propertyHash;
    }

    private Map<String, LinkedList<RelationMapping>> populateRelationMapList(final ResultSet rs) {
        final Map<String, LinkedList<RelationMapping>> relationHash = new HashMap<String, LinkedList<RelationMapping>>();

        try {
            while (rs.hasNext()) {
                final QuerySolution soln = rs.nextSolution();

                final RDFNode ruleNode = soln.get("?rule");
                String ruleString = null;
                if (ruleNode != null)
                    ruleString = ruleNode.asResource().getURI();
                // System.out.println("Rule:" + ruleNode + ", localName=" +
                // ruleNode.asResource().getLocalName() + ", nameSpace=" +
                // ruleNode.asResource().getNameSpace());

                final RDFNode domainNode = soln.get("?domain");
                String domainString = null;
                if (domainNode != null) {
                    domainString = domainNode.asResource().getURI();
                } else {
                    throw new NullArgumentException("Missing domain for rule: " + ruleString);
                }

                final RDFNode rangeNode = soln.get("?range");
                String rangeString = null;
                if (rangeNode != null) {
                    rangeString = rangeNode.asResource().getURI();
                } else {
                    throw new NullArgumentException("Missing range for rule: " + ruleString);
                }

                final RDFNode typeNode = soln.get("?type");
                String typeString = null;
                if (typeNode != null) {
                    typeString = model.expandPrefix(typeNode.asResource().getURI());
                }
                
                final RDFNode GATEattributeNode = soln.get("?GATEattribute");
                String GATEattributeString = null;
                if (GATEattributeNode != null) {
                    GATEattributeString = GATEattributeNode.asLiteral().getString();
                }
                
                final RelationMapping newMap = new RelationMapping(ruleString, typeString,
                        domainString, rangeString, GATEattributeString);

                if (relationHash.containsKey(domainString)) {
                    relationHash.get(domainString).add(newMap);
                } else {
                    LinkedList<RelationMapping> relationMaps = new LinkedList<>();
                    relationMaps.add(newMap);
                    relationHash.put(domainString, relationMaps);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error populating the relation hashmap.", e);
        }

        LOGGER.debug("----- RELATION HASHMAP:" + relationHash);
        return relationHash;
    }

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#initModel()
     */
    @Override
	public final void initModel() {
        model = dataset.getDefaultModel();
    }

    private void prepareTDBPropertyModel(final Map<String, LinkedList<PropertyMapping>> propMapList) {
        // TODO define relations in the RDF rather than hard-coding it here
        hasAnnotation = model.createProperty(puboBaseURI, "hasAnnotation");
        hasDocument = model.createProperty(puboBaseURI, "hasDocument");

        // properties for relation annotations
        hasCompetencyRecord = model.createProperty("http://intelleo.eu/ontologies/user-model/ns/",
                "hasCompetencyRecord"); // FIXME
        competenceFor = model.createProperty("http://www.intelleo.eu/ontologies/competences/ns/",
                "competenceFor"); // FIXME

        propertyModelHash = new HashMap<>();
        for (final LinkedList<PropertyMapping> propMapElement : propMapList.values()) {
            for (final PropertyMapping map : propMapElement) {
                final String propKey = (map.getGATEattribute() == null) ? map.getGATEfeature()
                        : map.getGATEattribute();
                /*
                 * if(map.getGATEattribute() == null){ propKey =
                 * map.getGATEfeature(); }else{ propKey =
                 * map.getGATEattribute(); }
                 */
                propertyModelHash.put(propKey, model.createProperty(map.getType()));
            }
        }
    }

    private void prepareTDBRelationModel(final Map<String, LinkedList<RelationMapping>> relationMapList) {
        model = dataset.getDefaultModel();
        relationModelHash = new HashMap<>();
        for (final LinkedList<RelationMapping> relationMapElement : relationMapList.values()) {
            for (final RelationMapping rMap : relationMapElement) {
                relationModelHash.put(rMap.getRule(), model.createProperty(rMap.getType()));
            }
        }
    }

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#storeTriple(java.lang.String, java.lang.String, java.lang.String, java.util.HashMap, java.util.HashMap)
     */
    @Override
    public final void storeTriple(final String docURL, final String URIforAnnotation, final String type,
            final Map<String, Object> exportProps,
            final Map<String, LinkedList<PropertyMapping>> propertyMapList) {
        //System.out.println(docURL + ", " + URIforAnnotation + ", " + type);

        model = dataset.getDefaultModel();

        final Resource newTriple = model.createResource(model.expandPrefix(URIforAnnotation));

        exportProps.keySet()
         		   .stream()
        		   .forEach(propKey -> addPropertyToSubject(exportProps, newTriple, propKey));
        newTriple.addProperty(RDF.type, model.createResource(model.expandPrefix(type)));
        model.createResource(docURL).addProperty(hasAnnotation, newTriple);
    }

	private void addPropertyToSubject(final Map<String, Object> exportProps, final Resource newTriple, final String propKey) {
		if (exportProps.get(propKey).getClass() == java.net.URI.class) {
		    // System.out.println("URI feature found: " + exportProps.get(propKey));
		    newTriple.addProperty(propertyModelHash.get(propKey),
		            model.createResource(exportProps.get(propKey).toString()));
		} else if ("URI".equals(propKey) || "URI1".equals(propKey)) { //FIXME remove the URI-n hack
		    // TODO remove this if, instead update the previous pipelines JAPE rules
			// & have new mapping rule vocab for URI vs literal export?
		    newTriple.addProperty(propertyModelHash.get(propKey),
		            model.createResource((String) exportProps.get(propKey)));
		} else {
		    final Literal propValueLit = model.createTypedLiteral(exportProps.get(propKey));
		    newTriple.addProperty(propertyModelHash.get(propKey), propValueLit);
		}
	}

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#storeTriple(java.lang.String, info.semanticsoftware.lodexporter.RelationMapping, java.lang.String, java.lang.String)
     */
    @Override
	public final void storeTriple(final String docURL, final RelationMapping rMap, final String URIforAnnotation,
        final String rangeURI) {

        model = dataset.getDefaultModel();
        final Resource newTriple = model.createResource(URIforAnnotation);
        newTriple.addProperty(relationModelHash.get(rMap.getRule()), model.createResource(rangeURI));
    }

    // method for storing relation annotations
    @Override
    public final void storeTriple(final String docURL, final String annotationURI, final FeatureMap feats,
        final String domainURI, final String rangeURI) {
        model = dataset.getDefaultModel();

        final Resource relationNode = model.createResource(annotationURI);
        relationNode.addProperty(RDF.type, model.createResource((String) feats.get("type")));
        relationNode.addProperty(competenceFor, model.createResource(rangeURI)); // FIXME move
        model.createResource(domainURI).addProperty(hasCompetencyRecord, relationNode);  //FIXME move
        model.createResource(docURL).addProperty(hasAnnotation, relationNode);
    }

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#storeTriple(java.lang.String, java.lang.String)
     */
    @Override
    public final void storeTriple(final String docURL, final String corpusURI) {
        model = dataset.getDefaultModel();
        model.createResource(corpusURI).addProperty(hasDocument, model.createResource(docURL));
        LOGGER.info("Exported " + corpusURI + " hasDocument " + docURL);
    }

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.TripleStoreInterface#printDataset()
     */
    @Override
    public final String printDataset() {
        return dataset.toString();
    }


	/* (non-Javadoc)
	 * @see info.semanticsoftware.lodexporter.TripleStoreInterface#exportTriplesToFile(java.lang.String)
	 */
	@Override
	public final void exportTriplesToFile(final String fileName) {
		model = dataset.getDefaultModel();
		try (FileOutputStream os = new FileOutputStream(fileName)) {
			RDFDataMgr.write(os, model, RDFFormat.NQUADS_UTF8) ;
		} catch (IOException e) {
			LOGGER.error("Error writing triples to file: " + fileName, e);
			throw new GateRuntimeException("Error writing triples to file: " + fileName, e);
		}
	}
}