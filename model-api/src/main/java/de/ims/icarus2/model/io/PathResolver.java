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

 * $Revision: 402 $
 * $Date: 2015-06-08 17:06:51 +0200 (Mo, 08 Jun 2015) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus2Core/core/de.ims.icarus2.model/source/de/ims/icarus2/model/io/PathResolver.java $
 *
 * $LastChangedDate: 2015-06-08 17:06:51 +0200 (Mo, 08 Jun 2015) $
 * $LastChangedRevision: 402 $
 * $LastChangedBy: mcgaerty $
 */
package de.ims.icarus2.model.io;

import de.ims.icarus2.model.api.ModelException;

/**
 * Utility class for accessing distributed data or for loading
 * little chunks from a very big database.
 *
 * @author Markus Gärtner
 * @version $Id: PathResolver.java 402 2015-06-08 15:06:51Z mcgaerty $
 * @see ResourcePath
 *
 */
public interface PathResolver {

	/**
	 * Translates the given {@code chunkIndex} into a {@code ResourcePath}
	 * information that can be used to access data from an abstract
	 * data source.
	 *
	 * @param chunkIndex
	 * @return
	 * @throws ModelException if the {@code chunkIndex} violates
	 * the bounds of this resolver. For example a resolver translating chunk indices
	 * into row values for a database table might check for the overall size of that
	 * table to make sure the returned rows do not exceed the table's row count.
	 */
	ResourcePath getPath(int chunkIndex);

	/**
	 * Returns the total number of data chunks this resolver can address as individual
	 * paths.
	 *
	 * @return
	 */
	int getPathCount();

	/**
	 * Releases any associated resources and renders this path resolver unusable for further
	 * queries.
	 */
	void close();
}