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

 * $Revision: 382 $
 * $Date: 2015-04-09 16:23:50 +0200 (Do, 09 Apr 2015) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus2Core/core/de.ims.icarus2.model/source/de/ims/icarus2/model/standard/members/structure/builder/AbstractStaticEdgeStorage.java $
 *
 * $LastChangedDate: 2015-04-09 16:23:50 +0200 (Do, 09 Apr 2015) $
 * $LastChangedRevision: 382 $
 * $LastChangedBy: mcgaerty $
 */
package de.ims.icarus2.model.standard.members.structure.builder;

import static de.ims.icarus2.model.standard.util.CorpusUtils.ensureIntegerValueRange;
import de.ims.icarus2.model.api.ModelErrorCode;
import de.ims.icarus2.model.api.ModelException;
import de.ims.icarus2.model.api.members.item.Edge;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.api.members.structure.Structure;
import de.ims.icarus2.model.standard.members.structure.AbstractImmutableEdgeStorage;
import de.ims.icarus2.util.collections.LookupList;

/**
 * @author Markus Gärtner
 * @version $Id: AbstractStaticEdgeStorage.java 382 2015-04-09 14:23:50Z mcgaerty $
 *
 */
public abstract class AbstractStaticEdgeStorage<R extends Item> extends AbstractImmutableEdgeStorage {

	protected final R root;

	protected final LookupList<Edge> edges;

	public AbstractStaticEdgeStorage(R root, LookupList<Edge> edges) {
		if (root == null)
			throw new NullPointerException("Invalid root");
		if (edges == null)
			throw new NullPointerException("Invalid edges");

		this.root = root;
		this.edges = edges;
	}

	@Override
	public void recycle() {
		throw new ModelException(ModelErrorCode.UNSUPPORTED_OPERATION, "Cannot recycle static edge storage");
	}

	@Override
	public boolean revive() {
		throw new ModelException(ModelErrorCode.UNSUPPORTED_OPERATION, "Cannot revive static edge storage");
	}

	@Override
	public void addNotify(Structure context) {
		// no-op
	}

	@Override
	public void removeNotify(Structure context) {
		// no-op
	}

	@Override
	public Item getVirtualRoot(Structure context) {
		return root;
	}

	@Override
	public boolean isRoot(Structure context, Item node) {
		return getParent(context, node)==root;
	}

	/**
	 * @see de.ims.icarus2.model.standard.members.structure.EdgeStorage#getEdgeCount(de.ims.icarus2.model.api.members.structure.Structure)
	 */
	@Override
	public long getEdgeCount(Structure context) {
		return edges.size();
	}

	protected Edge getEdgeAt(int index) {
		return edges.get(index);
	}

	/**
	 * @see de.ims.icarus2.model.standard.members.structure.EdgeStorage#getEdgeAt(de.ims.icarus2.model.api.members.structure.Structure, long)
	 */
	@Override
	public Edge getEdgeAt(Structure context, long index) {
		return getEdgeAt(ensureIntegerValueRange(index));
	}

	protected int indexOfEdge(Edge edge) {
		return edges.indexOf(edge);
	}

	/**
	 * @see de.ims.icarus2.model.standard.members.structure.EdgeStorage#indexOfEdge(de.ims.icarus2.model.api.members.structure.Structure, de.ims.icarus2.model.api.members.item.Edge)
	 */
	@Override
	public long indexOfEdge(Structure context, Edge edge) {
		return indexOfEdge(edge);
	}

	/**
	 * @see de.ims.icarus2.model.standard.members.structure.EdgeStorage#getEdgeCount(de.ims.icarus2.model.api.members.structure.Structure, de.ims.icarus2.model.api.members.item.Item)
	 */
	@Override
	public long getEdgeCount(Structure context, Item node) {
		return getEdgeCount(context, node, true) + getEdgeCount(context, node, false);
	}

}
