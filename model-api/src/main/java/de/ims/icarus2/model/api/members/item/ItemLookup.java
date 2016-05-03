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

 * $Revision: 450 $
 * $Date: 2016-01-28 11:55:54 +0100 (Do, 28 Jan 2016) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus2Core/core/de.ims.icarus2.model/source/de/ims/icarus2/model/api/members/item/ItemLookup.java $
 *
 * $LastChangedDate: 2016-01-28 11:55:54 +0100 (Do, 28 Jan 2016) $
 * $LastChangedRevision: 450 $
 * $LastChangedBy: mcgaerty $
 */
package de.ims.icarus2.model.api.members.item;

import java.util.function.Consumer;
import java.util.function.ObjLongConsumer;

import de.ims.icarus2.model.api.ModelConstants;

/**
 * Specifies an abstract read-only storage for {@code Item} instances.
 * Access to those items is modeled in an array-like style together with
 * a lookup method to fetch the index within the storage a given item
 * is located at.
 * <p>
 * Note that all index values returned or expected are of type {@link long}!
 *
 * @author Markus Gärtner
 * @version $Id: ItemLookup.java 450 2016-01-28 10:55:54Z mcgaerty $
 *
 */
public interface ItemLookup extends ModelConstants {

	long getItemCount();

	default boolean isEmpty() {
		return getItemCount()==0L;
	}

	Item getItemAt(long index);

	long indexOfItem(Item item);

	default void forEachItem(ObjLongConsumer<? super Item> action) {
		long size = getItemCount();
		for(long i = 0; i<size; i++) {
			action.accept(getItemAt(i), i);
		}
	}

	default void forEachItem(Consumer<? super Item> action) {
		long size = getItemCount();
		for(long i = 0; i<size; i++) {
			action.accept(getItemAt(i));
		}
	}
}
