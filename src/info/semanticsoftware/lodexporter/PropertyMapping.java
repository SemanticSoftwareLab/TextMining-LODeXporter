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
 * @author Bahar Sateli
 * @author René Witte
 */
public class PropertyMapping extends Mapping {

    private final String gateAttribute;
    private final String gateFeature;

    public PropertyMapping(final String rule, final String type, final String gateType,
            final String myGATEfeature, final String myGATEattribute) {
        super(rule, type, gateType);
        this.gateFeature = myGATEfeature;
        this.gateAttribute = myGATEattribute;
        if (this.gateFeature != null && this.gateAttribute != null) {
            throw new IllegalArgumentException("Both GATEfeature and GATEattribute have values.");
        } else if (this.gateFeature == null && this.gateAttribute == null) {
            throw new IllegalArgumentException("Both GATEfeature and GATEattribute are null.");
        }
    }

    /**
     * @return the GATEfeature
     */
    public final String getGATEfeature() {
        return gateFeature;
    }

    /**
     * @return the GATEattribute
     */
    public final String getGATEattribute() {
        return gateAttribute;
    }

    /* (non-Javadoc)
     * @see info.semanticsoftware.lodexporter.Mapping#toString()
     */
    @Override
	public String toString() {
        return super.toString() + " GATEfeature=" + gateFeature + " GATEattribute=" + gateAttribute;
    }
}