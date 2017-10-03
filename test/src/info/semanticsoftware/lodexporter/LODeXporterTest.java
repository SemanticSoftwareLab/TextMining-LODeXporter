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

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;

import gate.CreoleRegister;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.LanguageAnalyser;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;

/**
 * JUnit tests for LODeXporter.
 */
public class LODeXporterTest {
    private static final String MAPPING_FILE = "resources/mapping.rdf";
	private static final String PARAM_RDF_STORE_DIR = "rdfStoreDir";
	private static final String PARAM_MAPPING_FILE = "mappingFile";
	private static final String EXPORT_FILE_PATH = "exportFilePath";
	protected static final Logger LOGGER = Logger.getLogger(LODeXporterTest.class);

    /**
     * Rule for expected exceptions in tests.
     */
    @Rule
    public transient ExpectedException exception = ExpectedException.none();

	/**
	 * Initialize GATE and load the LODeXporter plugin.
	 * 
	 * @throws GateException GateException
	 * @throws MalformedURLException MalformedURLException
	 */
	@BeforeClass
	public static void initGate() throws GateException, MalformedURLException {
		Gate.init();
		final CreoleRegister cReg = Gate.getCreoleRegister();
        final String pluginDir = System.getProperties().getProperty("lodexporter.plugin.dir");
		cReg.registerDirectories(Paths.get(pluginDir).toAbsolutePath().toUri().toURL());
	}

	/**
	 * @param initParams Initialisation parameters for the LODeXporter PR
	 * @throws GateException GateException
	 * @throws MalformedURLException MalformedURLException
	 * @throws ResourceInstantiationException ResourceInstantiationException
	 * @return the initialised LODeXporter PR
	 */
	private static LanguageAnalyser getPR(final FeatureMap initParams)
			throws GateException, MalformedURLException, ResourceInstantiationException {
		return (LanguageAnalyser) Factory.createResource("info.semanticsoftware.lodexporter.LODeXporter", initParams);
	}
	
	/**
	 * Test that the PR refuses to initialize when both export options (TDB and file) are set.
	 * 
	 * @throws MalformedURLException MalformedURLException
	 * @throws ResourceInstantiationException ResourceInstantiationException
	 * @throws GateException GateException
	 */
	@Test
	public final void testInitConflict() throws MalformedURLException, ResourceInstantiationException, GateException {
		final FeatureMap fm = Factory.newFeatureMap();
		fm.put(PARAM_MAPPING_FILE, MAPPING_FILE);
		fm.put(PARAM_RDF_STORE_DIR, "/tmp");
	    exception.expect(ResourceInstantiationException.class);
	    getPR(fm);
	}

	/**
	 * Test that the PR refuses to initialize when no export options (neither TDB nor file) are set.
	 * 
	 * @throws MalformedURLException MalformedURLException
	 * @throws ResourceInstantiationException ResourceInstantiationException
	 * @throws GateException GateException
	 */
	@Test
	public final void testInitFail() throws MalformedURLException, ResourceInstantiationException, GateException {
		final FeatureMap fm = Factory.newFeatureMap();
		fm.put(PARAM_MAPPING_FILE, null);
		fm.put(PARAM_RDF_STORE_DIR, null);
	    exception.expect(ResourceInstantiationException.class);
	    getPR(fm);
	}

	@Test
	public final void testReInit() throws MalformedURLException, GateException {
		final FeatureMap fm = Factory.newFeatureMap();
		fm.put(PARAM_MAPPING_FILE, MAPPING_FILE);
		fm.put(PARAM_RDF_STORE_DIR, null);
	    final LanguageAnalyser lodexpr = getPR(fm);
		lodexpr.reInit(); // TODO check for something...
	}
	
	@Test
	public final void testGetCustomURI() throws MalformedURLException, GateException {
		final FeatureMap fm = Factory.newFeatureMap();
		fm.put(PARAM_MAPPING_FILE, MAPPING_FILE);
		fm.put(PARAM_RDF_STORE_DIR, "");
	    final LanguageAnalyser lodexpr = getPR(fm);
		assertFalse(((LODeXporter) lodexpr).getCustomURI());
	}

	@Test
	public final void testGetMappingFile() throws MalformedURLException, GateException {
		final FeatureMap fm = Factory.newFeatureMap();
		fm.put(PARAM_MAPPING_FILE, MAPPING_FILE);
		fm.put(PARAM_RDF_STORE_DIR, "");
	    final LanguageAnalyser lodexpr = getPR(fm);
		assertThat(((LODeXporter) lodexpr).getMappingFile().getPath(), endsWith("mapping.rdf"));
		LOGGER.debug(((LODeXporter) lodexpr).getMappingFile());
	}
	
	@Test
	public final void testRunPR() throws Exception {
		System.out.println(" ----------- testRunPR() -------------");
		final FeatureMap fm = Factory.newFeatureMap();
		fm.put(PARAM_MAPPING_FILE, MAPPING_FILE);
		fm.put(PARAM_RDF_STORE_DIR, "");
		fm.put(EXPORT_FILE_PATH, "/tmp/testdoc1.nq");
	    final LanguageAnalyser lodexpr = getPR(fm);
		
	    final Document doc = Factory.newDocument(getClass().getResource("/testdoc1.xml").toURI().toURL());
		lodexpr.setDocument(doc);
		lodexpr.setCorpus(null);
		lodexpr.execute();
		
		checkTriples("/tmp/testdoc1.nq");
	}

	/**
	 * Check that we got the triples that we expect.
	 * 
	 * @param exportFile filename of the exported triples
	 * @throws FileNotFoundException filename not found exception
	 * @throws IOException triple file read error
	 */
	private void checkTriples(final String exportFile) throws FileNotFoundException, IOException {
		// Sanity check on number of exported triples
		final Path path = Paths.get(exportFile);
		assertThat("Wrong number of generated triples", Files.lines(path).count(), equalTo(34L));

		// Load the generated triples into a model
		final Model model = ModelFactory.createDefaultModel();
		model.read(new FileInputStream(exportFile), null, "N-TRIPLES");
		
		// Check exported Document
        StmtIterator iterDoc = model.listStatements((Resource)null, (Property)model.createProperty("http://lod.semanticsoftware.info/pubo/pubo#hasDocument"), (RDFNode)null);
        assertThat("Model should contain one pubo#hasDocument triple", iterDoc.toList().size(), equalTo(1));
		
        // Check exported hasAnno
        StmtIterator iterAnno = model.listStatements((Resource)null, (Property)model.createProperty("http://lod.semanticsoftware.info/pubo/pubo#hasAnnotation"), (RDFNode)null);
        assertThat("Model should contain two pubo#hasAnnotation triples", iterAnno.toList().size(), equalTo(2));
		
		// Check exported "Person"
        StmtIterator iterPerson = model.listStatements((Resource)null, (Property)null, model.createResource("http://xmlns.com/foaf/0.1/Person"));
        assertThat("Model should contain one foaf:Person triple", iterPerson.toList().size(), equalTo(1));
        
        // Check exported "gender" feature
        StmtIterator iterGender = model.listStatements((Resource)null, (Property)model.createProperty("http://xmlns.com/foaf/0.1/gender"), (RDFNode)null);
        assertThat("Model should contain one foaf:gender triple", iterGender.toList().size(), equalTo(1));
        assertThat("Gender triple should be 'male'", 
        		model
        		.listStatements((Resource)null, (Property)model.createProperty("http://xmlns.com/foaf/0.1/gender"), (RDFNode)null)
        		.nextStatement()
        		.getObject()
        		.toString(),
        		equalTo("male")); // "male^^http://www.w3.org/2001/XMLSchema#string"

        // Check exported "startOffset" feature
        StmtIterator iterStart = model.listStatements((Resource)null, (Property)model.createProperty("http://purl.org/dc/terms/start"), (RDFNode)null);
        assertThat("Model should contain one purl:start triple", iterStart.toList().size(), equalTo(1));
        assertThat("Start offset should be '0'", 
        		model
        		.listStatements((Resource)null, (Property)model.createProperty("http://purl.org/dc/terms/start"), (RDFNode)null)
        		.nextStatement()
        		.getObject()
        		.toString(),
        		equalTo("0^^http://www.w3.org/2001/XMLSchema#integer"));
        
        // Check exported "Location"
        final StmtIterator iterLocation = model.listStatements((Resource)null, (Property)null, model.createResource("http://xmlns.com/foaf/0.1/Location"));
        assertThat("Model should contain one foaf:Location triple", iterLocation.toList().size(), equalTo(1));
        
        // Check exported purl:chunk
        final StmtIterator iterChunk = model.listStatements((Resource)null, (Property)model.createProperty("http://purl.org/dc/terms/chunk"), (RDFNode)null);
        assertThat("Model should contain two purl:chunk triples", iterChunk.toList().size(), equalTo(2));
	}
}
