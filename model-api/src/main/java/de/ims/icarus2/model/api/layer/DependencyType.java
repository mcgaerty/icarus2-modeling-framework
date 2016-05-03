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

 * $Revision: 381 $
 * $Date: 2015-04-09 00:25:23 +0200 (Do, 09 Apr 2015) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus2Core/core/de.ims.icarus2.model/source/de/ims/icarus2/model/api/layer/DependencyType.java $
 *
 * $LastChangedDate: 2015-04-09 00:25:23 +0200 (Do, 09 Apr 2015) $
 * $LastChangedRevision: 381 $
 * $LastChangedBy: mcgaerty $
 */
package de.ims.icarus2.model.api.layer;

import de.ims.icarus2.model.api.raster.Rasterizer;

/**
 * @author Markus Gärtner
 * @version $Id: DependencyType.java 381 2015-04-08 22:25:23Z mcgaerty $
 *
 */
public enum DependencyType {

	/**
	 * Used to assign boundary layers or containers.
	 */
	BOUNDARY,

	/**
	 * Used to assign foundation layers.
	 */
	FOUNDATION,

	/**
	 * Used to indicate the {@link AnnotationLayer} used for a certain {@link Rasterizer}.
	 */
	VALUE,

	/**
	 * Default type indicating a direct dependency (typically a base layer relationship).
	 */
	STRONG,

	;
}
