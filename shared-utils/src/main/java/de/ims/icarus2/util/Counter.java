/*
 *  ICARUS 2 -  Interactive platform for Corpus Analysis and Research tools, University of Stuttgart
 *  Copyright (C) 2015 Markus Gärtner
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses.

 * $Revision: 397 $
 * $Date: 2015-05-21 17:06:01 +0200 (Do, 21 Mai 2015) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus2Core/core/de.ims.icarus2.core/source/de/ims/icarus2/util/Counter.java $
 *
 * $LastChangedDate: 2015-05-21 17:06:01 +0200 (Do, 21 Mai 2015) $
 * $LastChangedRevision: 397 $
 * $LastChangedBy: mcgaerty $
 */
package de.ims.icarus2.util;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Set;

import de.ims.icarus2.util.collections.CollectionUtils;

/**
 * @author Markus Gärtner
 * @version $Id: Counter.java 397 2015-05-21 15:06:01Z mcgaerty $
 *
 */
public class Counter<T extends Object> {

	private final TObjectIntMap<T> counts = new TObjectIntHashMap<>();

	public Counter() {
		// no-op
	}

	public int increment(T data) {
		int c = counts.get(data);
		if(c==counts.getNoEntryValue()) {
			c = 0;
		}

		c++;
		counts.put(data, c);

//		System.out.printf("%s: %d size=%d\n",data,c,counts.size());

		return c;
	}

	public int add(T data, int delta) {
		int c = counts.get(data);
		if(c==counts.getNoEntryValue()) {
			c = 0;
		}

		if(delta>0 && Integer.MAX_VALUE-delta<c)
			throw new IllegalStateException("Positive overflow");

		c += delta;

		if(c<0)
			throw new IllegalStateException("Counter cannot get negative");

		counts.put(data, c);

//		System.out.printf("%s: %d size=%d\n",data,c,counts.size());

		return c;
	}

	public int decrement(T data) {
		int c = counts.get(data);
		if(c<1)
			throw new IllegalStateException("Cannot decrement count for data: "+data); //$NON-NLS-1$

		c--;
		if(c==0) {
			counts.remove(data);
		} else {
			counts.put(data, c);
		}

		return c;
	}

	public void clear() {
		counts.clear();
	}

	public int getCount(Object data) {
		int c = counts.get(data);
		return c==counts.getNoEntryValue() ? 0 : c;
	}

	/**
	 * Returns {@code true} iff the count for the giveb {@code data} is greater
	 * that {@code 0}.
	 *
	 * @param data
	 * @return
	 */
	public boolean hasCount(Object data) {
		int c = counts.get(data);
		return c>0;
	}

	public Set<T> getItems() {
		return CollectionUtils.getSetProxy(counts.keySet());
	}

	public boolean isEmpty() {
		return counts.isEmpty();
	}
}
