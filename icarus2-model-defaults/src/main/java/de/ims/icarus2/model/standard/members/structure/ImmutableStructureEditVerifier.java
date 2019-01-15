/*
 * ICARUS2 Corpus Modeling Framework
 * Copyright (C) 2014-2019 Markus Gärtner <markus.gaertner@ims.uni-stuttgart.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ims.icarus2.model.standard.members.structure;

import static de.ims.icarus2.model.standard.members.MemberUtils.checkContainsEdge;
import static de.ims.icarus2.model.standard.members.MemberUtils.checkContainsItem;
import static de.ims.icarus2.model.standard.members.MemberUtils.checkHostStructure;
import static de.ims.icarus2.model.standard.members.MemberUtils.checkNotContainsEdge;
import static java.util.Objects.requireNonNull;

import de.ims.icarus2.model.api.members.item.Edge;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.api.members.structure.Structure;
import de.ims.icarus2.model.api.members.structure.StructureEditVerifier;
import de.ims.icarus2.model.standard.members.container.ImmutableContainerEditVerifier;
import de.ims.icarus2.util.collections.seq.DataSequence;

/**
 * A simple {@link StructureEditVerifier} implementation that returns {@code false}
 * for all edit verification methods.
 *
 * @author Markus Gärtner
 *
 */
public class ImmutableStructureEditVerifier extends ImmutableContainerEditVerifier
		implements StructureEditVerifier {

	public ImmutableStructureEditVerifier(Structure source) {
		super(source);
	}

	/**
	 * @see de.ims.icarus2.model.api.members.structure.StructureEditVerifier#getSource()
	 */
	@Override
	public Structure getSource() {
		return (Structure) super.getSource();
	}

	/**
	 * @see de.ims.icarus2.model.api.members.structure.StructureEditVerifier#canAddEdge(de.ims.icarus2.model.api.members.item.Edge)
	 */
	@Override
	public boolean canAddEdge(Edge edge) {
		requireNonNull(edge);

		final Structure structure = getSource();
		final Item source = edge.getSource();
		final Item target = edge.getTarget();

		checkHostStructure(edge, structure);
		checkNotContainsEdge(structure, edge);
		checkContainsItem(structure, source);
		checkContainsItem(structure, target);

		return false;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.structure.StructureEditVerifier#canAddEdges(de.ims.icarus2.util.collections.seq.DataSequence)
	 */
	@Override
	public boolean canAddEdges(DataSequence<? extends Edge> edges) {
		requireNonNull(edges);

		edges.forEach(this::canAddEdge);

		return false;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.structure.StructureEditVerifier#canRemoveEdge(long)
	 */
	@Override
	public boolean canRemoveEdge(long index) {
		return false;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.structure.StructureEditVerifier#canRemoveEdges(long, long)
	 */
	@Override
	public boolean canRemoveEdges(long index0, long index1) {
		return false;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.structure.StructureEditVerifier#canSwapEdges(long, long)
	 */
	@Override
	public boolean canSwapEdges(long index0, long index1) {
		return false;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.structure.StructureEditVerifier#canSetTerminal(de.ims.icarus2.model.api.members.item.Edge, de.ims.icarus2.model.api.members.item.Item, boolean)
	 */
	@Override
	public boolean canSetTerminal(Edge edge, Item terminal, boolean isSource) {
		requireNonNull(edge);
		requireNonNull(terminal);

		final Structure structure = getSource();

		checkHostStructure(edge, structure);
		checkContainsEdge(structure, edge);
		checkContainsItem(structure, terminal);

		return false;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.structure.StructureEditVerifier#canCreateEdge(de.ims.icarus2.model.api.members.item.Item, de.ims.icarus2.model.api.members.item.Item)
	 */
	@Override
	public boolean canCreateEdge(Item source, Item target) {
		requireNonNull(source);
		requireNonNull(target);

		final Structure structure = getSource();

		checkContainsItem(structure, source);
		checkContainsItem(structure, target);

		return false;
	}

}
