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

package info.semanticsoftware.lodexporter;

import info.semanticsoftware.lodexporter.TripleStoreInterface.TransactionType;
import info.semanticsoftware.lodexporter.tdb.TDBTripleStoreImpl;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDateTime;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Controller;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.RunTime;
import gate.relations.Relation;
import gate.relations.RelationSet;
import org.apache.log4j.Logger;

/**
 * This class is the implementation of the LODeXporter PR.
 */
@CreoleResource(name = "LODeXporter", comment = "A PR to transform GATE annotations to RDF triples.")
public class LODeXporter extends AbstractLanguageAnalyser implements ProcessingResource, ControllerAwarePR {
	private static final long serialVersionUID = 1L;
	protected static final String LODEXPORTER_SESSION_FEATURE = "LODeXporterSession";
	private Boolean exportToFile; // true if we export triples to file, false if we use an external KB

	/**
	 * A HashMap where keys are rule names, and values are SubjectMapping
	 * objects. It is used to map each unique rule name to its corresponding
	 * baseURI, GATEtype and rdf:type for export. Example:
	 * &lt;map:GATEAnnotation1, &lt;rule= map:GATEAnnotation1,
	 * baseURI=http://semanticsoftware.info/lodexporter/, GATEType= Person,
	 * type= foaf:Person&gt;&gt;
	 * info.semanticsoftware.lodexporter#getSubjectMappings() for populating the
	 * map.
	 */
	private transient Map<String, SubjectMapping> subjectMap;

	/**  */
	private transient Map<String, LinkedList<PropertyMapping>> propertyMapList;

	/**  */
	private transient Map<String, LinkedList<RelationMapping>> relationMapList;
	private TripleStoreInterface myTripleStore;
	private String pipelineName;
	private String corpusName;
	private transient String sessionID;
	protected static final Logger LOGGER = Logger.getLogger(LODeXporter.class);

	// creole parameters
	@CreoleParameter(comment = "TDB RDF store directory (direct export to triplestore)", defaultValue = "")
	private String rdfStoreDir;

	@CreoleParameter(comment = "Mapping rules file (when not using RDF store directory)", defaultValue = "resources/mapping.rdf")
	private URL mappingFile;

	@CreoleParameter(comment = "Directory for exported triples (when not using RDF store directory)", defaultValue = "/tmp")
	@RunTime
	private String exportFilePath;

	@CreoleParameter(comment = "SubjectMapping SPARQL query", defaultValue = "SELECT ?rule ?type ?baseURI ?GATEtype "
			+ "WHERE { " + "?rule ?p <map:Mapping> . "
			+ "?rule <http://lod.semanticsoftware.info/mapping/mapping#type> ?type . "
			+ "?rule <http://lod.semanticsoftware.info/mapping/mapping#baseURI> ?baseURI . "
			+ "?rule <http://lod.semanticsoftware.info/mapping/mapping#GATEtype> ?GATEtype .} ")
	private String subjectMappingSparql;

	@CreoleParameter(comment = "PropertyMapping SPARQL query", defaultValue = "SELECT ?rule ?GATEtype ?GATEattribute ?GATEfeature ?type "
			+ "WHERE { " + "?rule ?p <map:Mapping> . "
			+ "?rule <http://lod.semanticsoftware.info/mapping/mapping#GATEtype> ?GATEtype ."
			+ "?rule <http://lod.semanticsoftware.info/mapping/mapping#hasMapping> ?mapping ."
			+ "?mapping <http://lod.semanticsoftware.info/mapping/mapping#type> ?type . "
			+ "OPTIONAL {?mapping <http://lod.semanticsoftware.info/mapping/mapping#GATEattribute> ?GATEattribute . }"
			+ "OPTIONAL {?mapping <http://lod.semanticsoftware.info/mapping/mapping#GATEfeature> ?GATEfeature . }}")
	private String propertyMappingSparql;

	@CreoleParameter(comment = "RelationMapping SPARQL query", defaultValue = "SELECT ?rule ?type ?domain ?range ?GATEattribute "
			+ "WHERE { " + "?rule ?p <map:Mapping> . "
			+ "?rule <http://lod.semanticsoftware.info/mapping/mapping#type> ?type . "
			+ "?rule <http://lod.semanticsoftware.info/mapping/mapping#domain> ?domain . "
			+ "?rule <http://lod.semanticsoftware.info/mapping/mapping#range> ?range . "
			+ "OPTIONAL {?rule <http://lod.semanticsoftware.info/mapping/mapping#GATEattribute> ?GATEattribute . }}")
	private String relationMappingSparql;

	@CreoleParameter(comment = "The annotation set to use as input", defaultValue = "")
	@RunTime
	private String inputASName;

	@CreoleParameter(comment = "Use custom URIs", defaultValue = "false")
	@RunTime
	private Boolean customURI;

	/**
	 * Sets whether custom URI generation style should be used.
	 * 
	 * @param myUriStyle
	 *            the URI generation style (e.g., default or custom)
	 */
	public final void setCustomURI(final Boolean myUriStyle) {
		this.customURI = myUriStyle;
	}

	/**
	 * @return the exportToFile
	 */	
	public final Boolean getExportToFile() {
		return exportToFile;
	}

	/**
	 * @return the uriStyle
	 */
	public final Boolean getCustomURI() {
		return customURI;
	}

	/**
	 * Sets the path to the triple store directory.
	 * 
	 * @param myRdfStoreDir
	 *            the path to the triple store directory
	 */
	public final void setrdfStoreDir(final String myRdfStoreDir) {
		this.rdfStoreDir = myRdfStoreDir;
	}

	/**
	 * @return the rdfstoredir
	 */
	public final String getrdfStoreDir() {
		return rdfStoreDir;
	}

	/**
	 * @return the mappingFile
	 */
	public final URL getMappingFile() {
		return mappingFile;
	}

	/**
	 * @param myMappingFile
	 *            the file containing the mapping rules (only when not using a
	 *            TDB file store)
	 */
	public final void setMappingFile(final URL myMappingFile) {
		this.mappingFile = myMappingFile;
	}

	/**
	 * @return the export file path
	 */
	public final String getExportFilePath() {
		return exportFilePath;
	}

	/**
	 * @param myExportFilePath
	 *            output file name (full path) when using memory-backed mode
	 */
	public final void setExportFilePath(final String myExportFilePath) {
		this.exportFilePath = myExportFilePath;
	}

	/**
	 * @return the subjectMappingSparql
	 */
	public final String getSubjectMappingSparql() {
		return subjectMappingSparql;
	}

	/**
	 * @param mySubjectMappingSparql
	 *            the subjectMappingSparql to set
	 */
	public final void setSubjectMappingSparql(final String mySubjectMappingSparql) {
		this.subjectMappingSparql = mySubjectMappingSparql;
	}

	/**
	 * @return the propertyMappingSparql
	 */
	public final String getPropertyMappingSparql() {
		return propertyMappingSparql;
	}

	/**
	 * @param myPropertyMappingSparql
	 *            the propertyMappingSparql to set
	 */
	public final void setPropertyMappingSparql(final String myPropertyMappingSparql) {
		this.propertyMappingSparql = myPropertyMappingSparql;
	}

	/**
	 * @return the relationMappingSparql
	 */
	public final String getRelationMappingSparql() {
		return relationMappingSparql;
	}

	/**
	 * @param myRelationMappingSparql
	 *            the relationMappingSparql to set
	 */
	public final void setRelationMappingSparql(final String myRelationMappingSparql) {
		this.relationMappingSparql = myRelationMappingSparql;
	}

	/**
	 * @param myInputASName
	 *            the InputAsName to set
	 */
	public final void setInputASName(final String myInputASName) {
		this.inputASName = myInputASName;
	}

	/**
	 * @return the inputASName
	 */
	public final String getInputASName() {
		return this.inputASName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gate.creole.AbstractProcessingResource#init()
	 */
	@Override
	public final gate.Resource init() throws ResourceInstantiationException {
		LOGGER.debug("LODeXporter loaded!");
		myTripleStore = new TDBTripleStoreImpl();
		// check if user wants to use file export mode using a mapping file
		final URL mappingRulesFile = getMappingFile() == null || getMappingFile().toString().length() == 0 ? null : getMappingFile();
		// check if user wants to connect to an existing TDB-based triplestore
		final String tdbDiskDirectory = getrdfStoreDir() == null || getrdfStoreDir().trim().length() == 0 ? null : getrdfStoreDir();

		if (mappingRulesFile != null && tdbDiskDirectory == null) {
			// create a memory-backed dataset
			myTripleStore.connect();
			exportToFile = true;
			LOGGER.debug("[init] created memory-backed dataset with mapping rules from " + getMappingFile());
		} else if (mappingRulesFile == null && tdbDiskDirectory != null) {
			// create a TDB-backed dataset
			myTripleStore.connect(getrdfStoreDir());
			exportToFile = false;
			LOGGER.debug("[init] created file-backed dataset in " + getrdfStoreDir());
		} else if (mappingRulesFile != null && tdbDiskDirectory != null) {
			throw new ResourceInstantiationException("Cannot set both mappingFile and RDFStoreDir.");
		} else {
			throw new ResourceInstantiationException("Must set exactly one of mappingFile or RDFStoreDir.");
		}
		try {
			myTripleStore.beginTransaction(TransactionType.WRITE);
			myTripleStore.initModel();
			// load mapping rules from file if using memory-backed dataset
			if (mappingRulesFile != null) {
				myTripleStore.loadMappingRulesFromFile(mappingRulesFile);
			}
			subjectMap = myTripleStore.getSubjectMappings(getSubjectMappingSparql());
			propertyMapList = (HashMap<String, LinkedList<PropertyMapping>>) myTripleStore
					.getPropertyMappings(getPropertyMappingSparql());
			relationMapList = (HashMap<String, LinkedList<RelationMapping>>) myTripleStore
					.getRelationMappings(getRelationMappingSparql());
		} catch (Exception e) { // NOPMD
			throw new ResourceInstantiationException("Error initializing LODeXporter", e);
		} finally {
			myTripleStore.endTransaction();
		}

		return this;
	}

	/* (non-Javadoc)
	 * @see gate.creole.AbstractProcessingResource#reInit()
	 */
	@Override
	public final void reInit() throws ResourceInstantiationException {
		myTripleStore.disconnect();
		init();
	}

	@Override
	public final void execute() throws ExecutionException {
		sessionID = UUID.randomUUID().toString();
		String docURL = "";
		String corpusURI = "";
		// find out whether we should use custom URIs for corpus and documents
		if (getCustomURI()) {
			LOGGER.info("generating custom URIs");
			try {
				corpusURI = java.net.URLDecoder.decode(corpusName, "UTF-8");
				String docName = document.getName();
				int index = docName.indexOf(".txt");
				if (index > -1) {
					docName = docName.substring(0, index);
				}
				docURL = corpusURI + "#" + docName;
				docURL = java.net.URLDecoder.decode(docURL, "UTF-8");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			if (document.getSourceUrl() == null) {
				LOGGER.error("Document URL is null, cannot export.");
				return;
			}
			docURL = document.getSourceUrl().toString();
			docURL = fixProtocol(docURL);
			docURL = fixURI(docURL); // TODO handle in more generic fashion
			corpusURI = "http://semanticsoftware.info/lodexporter/Corpus/" + corpusName;
			// TODO ^^ provide for a custom prefix for "corpus"
		}
		
		// store the session ID as a document-level feature (used as name in file-based export)
        document.getFeatures().put(LODEXPORTER_SESSION_FEATURE, sessionID); 

		/*
		 * System.out.println("Mapping inside execute:"); for (SubjectMapping m
		 * : subjectMapList) { System.out.println(m.toString()); }
		 */

		try {
			// one transaction per document
			myTripleStore.beginTransaction(TransactionType.WRITE);
			// first, export the document-corpus relation triple
			myTripleStore.storeTriple(docURL, corpusURI);

			final AnnotationSet inputAS = inputASName == null || inputASName.trim().length() == 0
					? document.getAnnotations() : document.getAnnotations(inputASName); // NOPMD

			final Map<String, Object> exportPropertyMap = new HashMap<String, Object>();
			final Map<String, Object> exportRelationMap = new HashMap<String, Object>();

			for (final SubjectMapping aMapping : subjectMap.values()) {
				final AnnotationSet annotSet = inputAS.get(aMapping.getGateType());
				final String currentRule = aMapping.getRule();
				final List<PropertyMapping> propsForType = propertyMapList.get(currentRule);
				final List<RelationMapping> relationsForType = relationMapList.get(currentRule);

				LOGGER.debug("Mapping " + aMapping.getGateType() + " with props: " + propsForType + " and relations "
						+ relationsForType + " for rule: " + currentRule);

				for (final Annotation currAnnot : annotSet) {
					exportPropertyMap.clear();
					exportRelationMap.clear();

					final FeatureMap feats = currAnnot.getFeatures();
					processProperties(docURL, propsForType, currAnnot, exportPropertyMap, feats);

					myTripleStore.storeTriple(docURL,
							getURIforAnnotation(currAnnot, aMapping.getBaseURI(), currentRule),
							aMapping.getType(), exportPropertyMap, propertyMapList);

					processRelations(docURL, relationsForType, currAnnot, exportRelationMap, aMapping);
				}
			}

			// the second empty string means the relations are in the default
			// annotation set
			processRelationsAdHoc(docURL, "");

		} catch (Exception e) {
			LOGGER.error("Error in processing document " + document.getName(), e);
		} finally {
			myTripleStore.endTransaction();
			if (exportToFile) {
				myTripleStore.beginTransaction(TransactionType.READ);
				myTripleStore.exportTriplesToFile(getExportFilePath() + "/" + sessionID + ".nq");
				myTripleStore.endTransaction();
				try {
					reInit(); // FIXME do we really need a re-init here?
				} catch (ResourceInstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void processRelationsAdHoc(final String docURL, final String annotationSetName) {
		final RelationSet relationSet = document.getAnnotations(annotationSetName).getRelations();
		String domainURI = null;
		String rangeURI = null;

		for (final Relation relation : relationSet) {
			final String relationURI = getURIforRelation(relation, "http://semanticsoftware.info/lodexporter/",
					relation.getType());
			// System.out.println("Storing rel: " + relationURI);

			final int[] members = relation.getMembers();
			if (members.length == 2) {
				final Annotation domainAnnot = document.getAnnotations().get(members[0]);
				final Annotation rangeAnnot = document.getAnnotations().get(members[1]);

				for (final SubjectMapping aMapping : subjectMap.values()) {
					if (aMapping.getGateType().equals(domainAnnot.getType())) {
						domainURI = getURIforAnnotation(domainAnnot, aMapping.getBaseURI(), aMapping.getRule());
					} else if (aMapping.getGateType().equals(rangeAnnot.getType())) {
						rangeURI = getURIforAnnotation(rangeAnnot, aMapping.getBaseURI(), aMapping.getRule());
					}
				}

				myTripleStore.storeTriple(docURL, relationURI, relation.getFeatures(), domainURI, rangeURI);

			} else {
				System.out.println("This relation does not have two members. Skipping relation #" + relation.getId());
			}
		}
	}

	// FIXME merge with the other with a superclass of annotation and relation?
	private String getURIforRelation(final Relation relation, final String baseURI, final String rule) {
		final String reID = relation.getId().toString();
		return baseURI + sessionID + "/" + relation.getType() + "/" + reID + "#" + rule;
	}

	// FIXME look into why exportRelationMap is passed but not used?
	private void processRelations(final String docURL, final List<RelationMapping> relationsForType,
			final Annotation currAnnot, final Map<String, Object> exportRelationMap,
			final SubjectMapping currentSubjMapping) {
		if (relationsForType != null) {
			for (final RelationMapping rMap : relationsForType) {
				if (rMap.getGATEattribute() != null && rMap.getGATEattribute().equals("contains")) {
					final SubjectMapping rangeMapping = subjectMap.get(rMap.getRange());
					final String rangeGATEType = rangeMapping.getGateType();
					final String rangeBaseURI = rangeMapping.getBaseURI();
					final String rangeRuleName = rangeMapping.getRule();

					// System.out.println("----------------------- range " +
					// rMap.getRange() + " - type: " + rangeGATEType +
					// " baseuri: " + rangeBaseURI + " rule: " + rangeRuleName);

					final AnnotationSet containedEntities = document.getAnnotations()
							.getContained(currAnnot.getStartNode().getOffset(), currAnnot.getEndNode().getOffset())
							.get(rangeGATEType);

					for (final Annotation aNE : containedEntities) {
						final String rangeURI = getURIforAnnotation(aNE, rangeBaseURI, rangeRuleName);
						myTripleStore.storeTriple(docURL, rMap, getURIforAnnotation(currAnnot,
								currentSubjMapping.getBaseURI(), rMap.getDomain()), rangeURI);
					}
				} else if (rMap.getGATEattribute() != null && rMap.getGATEattribute().equals("employedBy")) {
					SubjectMapping rangeMapping = subjectMap.get(rMap.getRange());
					String rangeBaseURI = rangeMapping.getBaseURI();
					String rangeRuleName = rangeMapping.getRule();

					Integer affiliationID = (Integer) currAnnot.getFeatures().get("employedBy");
					Annotation affiliationAnnot = document.getAnnotations().get(affiliationID);
					String rangeURI = getURIforAnnotation(affiliationAnnot, rangeBaseURI, rangeRuleName);
					myTripleStore.storeTriple(docURL, rMap, getURIforAnnotation(currAnnot,
							currentSubjMapping.getBaseURI(), rMap.getDomain()), rangeURI);

				} else {
					// we have the URI of the domain (i.e., the subject), we
					// only need to find the URI of the range (i.e., the object)
					final String rangeURI = getURIforAnnotation(currAnnot, currentSubjMapping.getBaseURI(), rMap.getRange());
					myTripleStore.storeTriple(docURL, rMap, getURIforAnnotation(currAnnot,
							currentSubjMapping.getBaseURI(), rMap.getDomain()), rangeURI);
				}
			}
		}
	}

	private void processProperties(final String docURL, final List<PropertyMapping> propsForType,
			final Annotation currAnnot, final Map<String, Object> exportPropertyMap, final FeatureMap feats)
			throws ExecutionException {
		if (propsForType != null) {
			for (final PropertyMapping pMap : propsForType) {
				if (pMap.getGATEfeature() != null && pMap.getGATEattribute() != null) {
					throw new ExecutionException("Both GATE feature and attributes have values.");
				} else if (pMap.getGATEfeature() != null) {
					// export the property only if the feature key exists
					if (feats.containsKey(pMap.getGATEfeature())) {
						// export the property only if the declared feature has
						// a value (i.e., not null)
						final Object featValue = feats.get(pMap.getGATEfeature());
						if (featValue != null) {
							exportPropertyMap.put(pMap.getGATEfeature(), featValue);
						} else {
							System.err.println("WARNING: " + pMap.getGATEfeature() + " has a NULL value in document ("
									+ docURL + ") for annotation #" + currAnnot.getId()
									+ ". I'm going to skip exporting this feature.");
						}
					}

				} else if (pMap.getGATEattribute() != null) {
					exportPropertyMap.put(pMap.getGATEattribute(),
							getValueforGATEAttribute(pMap.getGATEattribute(), currAnnot));
				} else {
					throw new ExecutionException("Both GATE feature and attributes are null.");
				}
			}
		}
	}

	private Object getValueforGATEAttribute(final String gateAttribute, final Annotation currAnnot) {
		Object value = null;
		try {
			switch (gateAttribute) {
			case "content":
				value = document
						.getContent()
						.getContent(currAnnot.getStartNode().getOffset(), currAnnot.getEndNode().getOffset()).toString()
						.replaceAll("\n", " ");
				break;
			case "startOffset":
				value = currAnnot.getStartNode().getOffset();
				break;
			case "endOffset":
				value = currAnnot.getEndNode().getOffset();
				break;
			case "docURL":
				value = new URI((String) document.getFeatures().get("gate.SourceURL"));
				break;
			case "annotatedAt":
				// TODO keep the time zone in a separate properties file
				value = new XSDDateTime(Calendar.getInstance(TimeZone.getTimeZone("America/Montreal")));
				break;
			case "annotatedBy":
				value = pipelineName;
				break;
			default:
				throw new IllegalArgumentException("Unsuppport GATE attribute: " + gateAttribute);
			}
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return value;
	}

	private String getURIforAnnotation(final Annotation re, final String baseURI, final String ruleName) {
		final String reID = re.getId().toString();
		return baseURI + sessionID + "/" + re.getType() + "/" + reID + "#" + ruleName;
	}

	private String fixProtocol(final String docURL) {
		return docURL.replaceFirst("file:\\/", "http://");
	}

	private String fixURI(final String docUrl) throws ExecutionException {
		final String exp = "(https?://.*/.*)/.*";
		final Pattern pattern = Pattern.compile(exp);
		final Matcher matcher = pattern.matcher(docUrl.toString());
		// System.out.println("---Storing doc with URL=" + docUrl.toString());

		if (matcher.find()) {
			return matcher.group(1);
		} else {
			throw new ExecutionException("no http found in : " + docUrl);
		}
	}

	/* (non-Javadoc)
	 * @see gate.creole.ControllerAwarePR#controllerExecutionStarted(gate.Controller)
	 */
	@Override
	public final void controllerExecutionStarted(final Controller controller) throws ExecutionException {
		pipelineName = controller.getName();
		try {
			corpusName = URLEncoder.encode(corpus.getName(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Exception in controllerExecutionFinished", e);
		}
		LOGGER.debug("[controllerExecutionStarted] Dataset is now: " + myTripleStore.printDataset());
	}
	
	/* (non-Javadoc)
	 * @see gate.creole.ControllerAwarePR#controllerExecutionFinished(gate.Controller)
	 */
	@Override
	public final void controllerExecutionFinished(final Controller controller) throws ExecutionException {
		LOGGER.debug("[controllerExecutionFinished] Dataset is now: " + myTripleStore.printDataset());
	}

	/* (non-Javadoc)
	 * @see gate.creole.ControllerAwarePR#controllerExecutionAborted(gate.Controller, java.lang.Throwable)
	 */
	@Override
	public final void controllerExecutionAborted(final Controller controller, final Throwable t) throws ExecutionException {
		LOGGER.debug("[controllerExecutionAborted] Dataset is now: " + myTripleStore.printDataset());
	}
}