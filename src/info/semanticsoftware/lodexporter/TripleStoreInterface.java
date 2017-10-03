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

import java.net.URL;
import java.util.LinkedList;
import java.util.Map;

import gate.FeatureMap;

/**
 * @author Bahar Sateli
 * @author René Witte
 */
public interface TripleStoreInterface {
	/** Transactions on the triple store can be of type READ or READ/WRITE. */
	enum TransactionType{READ, WRITE}; 

	void connect();

	void connect(String dir);

	void disconnect();

	void initModel();

	void loadMappingRulesFromFile(URL mappingRulesFile);
	
	/**
	 * Queries the triple store for mapping rules using the supplied SPARQL query string 
	 * and populates a map of &lt;rulename, {@link SubjectMapping}&gt; objects.
	 * @param query the SPARQL query string
	 * @return A HashMap of &lt;rulename, {@link SubjectMapping}&gt; objects
	 * @throws Exception from the underlying triple store implementation
	 */
	Map<String, SubjectMapping> getSubjectMappings(String query) throws Exception;
	Map<String,LinkedList<PropertyMapping>> getPropertyMappings( String query )throws Exception;
	Map<String,LinkedList<RelationMapping>> getRelationMappings( String query )throws Exception;
	void beginTransaction(TransactionType type);
	void endTransaction();
	void storeTriple(String docURL, String URIforAnnotation,
			String type, Map<String, Object> exportProps, Map<String, LinkedList<PropertyMapping>> propertyMapList);
	void storeTriple(String docURL, RelationMapping rMap, String URIforAnnotation, String rangeURI);
	void storeTriple(String docURL, String corpusURI);
	void storeTriple(String docURL, String annotationURI, FeatureMap feats, String domainURI, String rangeURI);
	void exportTriplesToFile(String url);
	String printDataset();
}