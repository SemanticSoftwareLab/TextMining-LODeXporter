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

/**
 * This class represents the base structure of a mapping rule. Each Mapping
 * object defines the rule name, the GATE annotation type that needs to be
 * exported, along with the rdf:type value for the output triple.
 * 
 * For example, a concrete rule could map an instance of a GATE <i>Person</i>
 * annotation type into a triple with <tt>rdf:type foaf:Person</tt>.
 * 
 * @author Bahar Sateli
 * @author René Witte
 */
public class Mapping {

    private String rule;
    private String type;
    private String gateType;

    /**
     * Returns the rule name of this mapping instance.
     * 
     * @return the rule name as String
     */
    public final String getRule() {
        return rule;
    }

    /**
     * Returns the GATE annotation type of this mapping instance.
     * 
     * @return the gateType as String
     */
    public final String getGateType() {
        return gateType;
    }

    /**
     * Returns the rdf:type value of this mapping instance.
     * 
     * @return the rdf:type value as String
     */
    public final String getType() {
        return type;
    }

    /**
     * The default constructor.
     * 
     * @param myRule
     *            the rule name
     * @param myType
     *            the rdf:type value
     * @param myGateType
     *            the GATE annotation type (e.g., "Person")
     */
    public Mapping(final String myRule, final String myType, final String myGateType) {
        rule = myRule;
        type = myType;
        gateType = myGateType;
    }

    /**
     * Returns a string representation of the mapping rule.
     * 
     * @return a string representation of the mapping rule
     */
    @Override
	public String toString() {
        return "rule= " + rule + " , type= " + type + " , GATEType= " + gateType;
    }
}