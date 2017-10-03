/*
 * LODeXporter -- http://www.semanticsoftware.info/lodexporter
 *
 * This file is part of the LODeXporter component.
 *
 * Copyright (c) 2015, 2016, 2017 Semantic Software Lab, http://www.semanticsoftware.info
 *    Rene Witte
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

public class RelationMapping extends Mapping {

	private final String domain;
	private final String range;
    private final String GATEattribute; //NOPMD
    
	public RelationMapping(final String mRule, final String mType, final String myDomain, final String myRange, final String myGATEattribute){
		super(mRule,mType,null);
		this.domain = myDomain;
		this.range = myRange;
		this.GATEattribute = myGATEattribute;
	}

	/**
	 * @return the gATEattribute
	 */
	public final String getGATEattribute() {
		return GATEattribute;
	}
	/**
	 * @return the domain
	 */
	public final String getDomain() {
		return domain;
	}
	/**
	 * @return the range
	 */
	public final String getRange() {
		return range;
	}

	@Override
	public String toString(){
		return super.toString() + " domain=" + domain + " range=" + range + " GATEattribute=" + GATEattribute; 
	}
}
