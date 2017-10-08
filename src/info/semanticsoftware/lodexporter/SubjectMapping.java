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

/***
 * Extends a mapping object with a baseURI field for URI generation of exported
 * triples.
 * 
 * @see info.semanticsoftware.lodexporter.TripleStoreInterface#getSubjectMappings(String)
 * @see info.semanticsoftware.lodexporter.LODeXporter#init()
 * 
 * @author Bahar Sateli
 * @author René Witte
 *
 */
public class SubjectMapping extends Mapping {

	private String baseURI;

	/**
	 * The constructor for creating a SubjectMapping instance.
	 * 
	 * @param rule
	 *            the rule name
	 * @param myBaseURI
	 *            the baseURI must be a fully-qualified URI (e.g.,
	 *            "http://semanticsoftware.info/lodexporter/")
	 * @param type
	 *            the rdf:type value
	 * @param gateType
	 *            the GATE annotation type
	 */
	public SubjectMapping(final String rule, final String myBaseURI, final String type, final String gateType) {
		super(rule, type, gateType);
		baseURI = myBaseURI;
	}

	/**
	 * Returns the baseURI of this subject mapping instance.
	 * 
	 * @return the baseURI as String
	 */
	public final String getBaseURI() {
		return baseURI;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see info.semanticsoftware.lodexporter.Mapping#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + " URI=" + baseURI;
	}
}