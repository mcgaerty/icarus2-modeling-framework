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
/**
 *
 */
package de.ims.icarus2.model.api.members.structure;

import static de.ims.icarus2.test.TestUtils.displayString;
import static de.ims.icarus2.test.util.Pair.pair;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;

import de.ims.icarus2.model.api.ModelTestUtils;
import de.ims.icarus2.model.api.members.item.Edge;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.test.TestUtils;
import de.ims.icarus2.test.util.Pair;
import de.ims.icarus2.test.util.Triple;
import de.ims.icarus2.util.collections.seq.DataSequence;

@SuppressWarnings("boxing")
public class StructureEditVerifierTestBuilder {

	public static final long ROOT = -1L;

	final StructureEditVerifier verifier;

	/**
	 * Legal values for {@link StructureEditVerifier#canAddEdge(long, Edge)}
	 */
	final List<Pair<Long, Edge>> addSingleLegal = new ArrayList<>();
	/**
	 * Illegal values for {@link StructureEditVerifier#canAddEdge(long, Edge)}
	 */
	final List<Pair<Long, Edge>> addSingleIllegal = new ArrayList<>();

	/**
	 * Legal values for {@link StructureEditVerifier#canAddEdges(long, DataSequence)}
	 */
	final List<Pair<Long, DataSequence<Edge>>> addBatchLegal = new ArrayList<>();
	/**
	 * Illegal values for {@link StructureEditVerifier#canAddEdges(long, DataSequence)}
	 */
	final List<Pair<Long, DataSequence<Edge>>> addBatchIllegal = new ArrayList<>();

	/**
	 * Legal values for {@link StructureEditVerifier#canRemoveEdge(long)}
	 */
	final List<Long> removeSingleLegal = new ArrayList<>();
	/**
	 * Illegal values for {@link StructureEditVerifier#canRemoveEdge(long)}
	 */
	final List<Long> removeSingleIllegal = new ArrayList<>();

	/**
	 * Legal values for {@link StructureEditVerifier#canRemoveEdges(long, long)}
	 */
	final List<Pair<Long, Long>> removeBatchLegal = new ArrayList<>();
	/**
	 * Illegal values for {@link StructureEditVerifier#canRemoveEdges(long, long)}
	 */
	final List<Pair<Long, Long>> removeBatchIllegal = new ArrayList<>();

	/**
	 * Legal values for {@link StructureEditVerifier#canSwapEdges(long, long)}
	 */
	final List<Pair<Long, Long>> swapSingleLegal = new ArrayList<>();
	/**
	 * Illegal values for {@link StructureEditVerifier#canSwapEdges(long, long)}
	 */
	final List<Pair<Long, Long>> swapSingleIllegal = new ArrayList<>();

	/**
	 * Legal values for {@link StructureEditVerifier#canSetTerminal(Edge, Item, boolean)}
	 */
	final List<Triple<Edge, Item, Boolean>> setTerminalLegal = new ArrayList<>();
	/**
	 * Illegal values for {@link StructureEditVerifier#canSetTerminal(Edge, Item, boolean)}
	 */
	final List<Triple<Edge, Item, Boolean>> setTerminalIllegal = new ArrayList<>();

	/**
	 * Legal values for {@link StructureEditVerifier#canCreateEdge(Item, Item)}
	 * <p>
	 * Note that we store index values here for simplicity. The actual test code translates
	 * them into the respective {@code Item} instances located at those indices.
	 */
	final List<Pair<Item, Item>> createEdgeLegal = new ArrayList<>();
	/**
	 * Illegal values for {@link StructureEditVerifier#canCreateEdge(Item, Item)}
	 * <p>
	 * Note that we store index values here for simplicity. The actual test code translates
	 * them into the respective {@code Item} instances located at those indices.
	 */
	final List<Pair<Item, Item>> createEdgeIllegal = new ArrayList<>();

	//TODO add list of Executable calls with expected error types

	public StructureEditVerifierTestBuilder(StructureEditVerifier verifier) {
		this.verifier = requireNonNull(verifier);
	}

	public StructureEditVerifierTestBuilder addSingleLegal(Edge edge, long...values) {
		for(long index : values) {
			addSingleLegal.add(pair(index, edge));
		}
		return this;
	}

	public StructureEditVerifierTestBuilder addSingleLegal(long...values) {
		return addSingleLegal(ModelTestUtils.EDGE, values);
	}

	public StructureEditVerifierTestBuilder addSingleIllegal(Edge edge, long...values) {
		for(long index : values) {
			addSingleIllegal.add(pair(index, edge));
		}
		return this;
	}

	public StructureEditVerifierTestBuilder addSingleIllegal(long...values) {
		return addSingleIllegal(ModelTestUtils.EDGE, values);
	}

	public StructureEditVerifierTestBuilder addBatchLegal(DataSequence<Edge> edges, long...values) {
		for(long index : values) {
			addBatchLegal.add(pair(index, edges));
		}
		return this;
	}

	public StructureEditVerifierTestBuilder addBatchLegal(long...values) {
		return addBatchLegal(ModelTestUtils.EDGE_SEQUENCE, values);
	}

	public StructureEditVerifierTestBuilder addBatchIllegal(DataSequence<Edge> edges, long...values) {
		for(long index : values) {
			addBatchIllegal.add(pair(index, edges));
		}
		return this;
	}

	public StructureEditVerifierTestBuilder addBatchIllegal(long...values) {
		return addBatchIllegal(ModelTestUtils.EDGE_SEQUENCE, values);
	}

	public StructureEditVerifierTestBuilder removeSingleLegal(long...values) {
		for(long index : values) {
			removeSingleLegal.add(index);
		}
		return this;
	}

	public StructureEditVerifierTestBuilder removeSingleIllegal(long...values) {
		for(long index : values) {
			removeSingleIllegal.add(index);
		}
		return this;
	}

	public StructureEditVerifierTestBuilder removeBatchLegal(
			@SuppressWarnings("unchecked") Pair<Long, Long>...entries) {
		Collections.addAll(removeBatchLegal, entries);
		return this;
	}
	public StructureEditVerifierTestBuilder removeBatchLegal(long from, long to) {
		removeBatchLegal.add(pair(from, to));
		return this;
	}

	public StructureEditVerifierTestBuilder removeBatchIllegal(
			@SuppressWarnings("unchecked") Pair<Long, Long>...entries) {
		Collections.addAll(removeBatchIllegal, entries);
		return this;
	}
	public StructureEditVerifierTestBuilder removeBatchIllegal(long from, long to) {
		removeBatchIllegal.add(pair(from, to));
		return this;
	}

	public StructureEditVerifierTestBuilder swapSingleLegal(long index0, long index1) {
		swapSingleLegal.add(pair(index0, index1));
		return this;
	}
	public StructureEditVerifierTestBuilder swapSingleLegal(
			@SuppressWarnings("unchecked") Pair<Long, Long>...entries) {
		Collections.addAll(swapSingleLegal, entries);
		return this;
	}

	public StructureEditVerifierTestBuilder swapSingleIllegal(long index0, long index1) {
		swapSingleIllegal.add(pair(index0, index1));
		return this;
	}
	public StructureEditVerifierTestBuilder swapSingleIllegal(
			@SuppressWarnings("unchecked") Pair<Long, Long>...entries) {
		Collections.addAll(swapSingleIllegal, entries);
		return this;
	}


	public StructureEditVerifierTestBuilder setTerminalLegal(
			@SuppressWarnings("unchecked") Triple<Edge, Item, Boolean>...entries) {
		Collections.addAll(setTerminalLegal, entries);
		return this;
	}
	public StructureEditVerifierTestBuilder setTerminalIllegal(
			@SuppressWarnings("unchecked") Triple<Edge, Item, Boolean>...entries) {
		Collections.addAll(setTerminalIllegal, entries);
		return this;
	}

	public StructureEditVerifierTestBuilder setTerminalLegalIndirect(
			@SuppressWarnings("unchecked") Triple<Edge, Long, Boolean>...entries) {
		Stream.of(entries)
				.map(t -> Triple.of(t.first, itemAt(t.second), t.third))
				.forEach(setTerminalLegal::add);
		return this;
	}
	public StructureEditVerifierTestBuilder setTerminalIllegalIndirect(
			@SuppressWarnings("unchecked") Triple<Edge, Long, Boolean>...entries) {
		Stream.of(entries)
			.map(t -> Triple.of(t.first, itemAt(t.second), t.third))
			.forEach(setTerminalIllegal::add);
		return this;
	}

	public StructureEditVerifierTestBuilder createEdgeLegal(
			@SuppressWarnings("unchecked") Pair<Item, Item>...entries) {
		Collections.addAll(createEdgeLegal, entries);
		return this;
	}
	public StructureEditVerifierTestBuilder createEdgeIllegal(
			@SuppressWarnings("unchecked") Pair<Item, Item>...entries) {
		Collections.addAll(createEdgeIllegal, entries);
		return this;
	}


	public StructureEditVerifierTestBuilder createEdgeLegalIndirect(
			@SuppressWarnings("unchecked") Pair<Long, Long>...entries) {
		Stream.of(entries)
			.map(p -> Pair.pair(itemAt(p.first), itemAt(p.second)))
			.forEach(createEdgeLegal::add);
		return this;
	}
	public StructureEditVerifierTestBuilder createEdgeIllegalIndirect(
			@SuppressWarnings("unchecked") Pair<Long, Long>...entries) {
		Stream.of(entries)
			.map(p -> Pair.pair(itemAt(p.first), itemAt(p.second)))
			.forEach(createEdgeIllegal::add);
		return this;
	}

	Edge edgeAt(long index) {
		return verifier.getSource().getEdgeAt(index);
	}

	Item itemAt(long index) {
		Structure structure = verifier.getSource();
		return index==ROOT ? structure.getVirtualRoot() : structure.getItemAt(index);
	}

	public Stream<DynamicTest> createTests() {
		return createTestsForSpec(this).stream();
	}

	public static List<DynamicTest> createTestsForSpec(StructureEditVerifierTestBuilder spec) {
		List<DynamicTest> tests = new ArrayList<>();

		// SINGLE ADD
		TestUtils.makeTests(spec.addSingleLegal,
				p -> displayString("add single legal: %s", p.first),
				p -> spec.verifier.canAddItem(p.first, p.second), true, tests::add);
		TestUtils.makeTests(spec.addSingleIllegal,
				p -> displayString("add single illegal: %s", p.first),
				p -> spec.verifier.canAddItem(p.first, p.second), false, tests::add);

		// BATCH ADD
		TestUtils.makeTests(spec.addBatchLegal,
				p -> displayString("add batch legal: %s [len=%s]", p.first, p.second.entryCount()),
				p -> spec.verifier.canAddItems(p.first, p.second), true, tests::add);
		TestUtils.makeTests(spec.addBatchIllegal,
				p -> displayString("add batch illegal: %s [len=%s]", p.first, p.second.entryCount()),
				p -> spec.verifier.canAddItems(p.first, p.second), false, tests::add);

		// SINGLE REMOVE
		TestUtils.makeTests(spec.removeSingleLegal,
				idx -> displayString("remove single legal: %s", idx),
				idx -> spec.verifier.canRemoveItem(idx), true, tests::add);
		TestUtils.makeTests(spec.removeSingleIllegal,
				idx -> displayString("remove single illegal: %s", idx),
				idx -> spec.verifier.canRemoveItem(idx), false, tests::add);

		// BATCH REMOVE
		TestUtils.makeTests(spec.removeBatchLegal,
				p -> displayString("remove batch legal: %s to %s", p.first, p.second),
				p -> spec.verifier.canRemoveItems(p.first, p.second), true, tests::add);
		TestUtils.makeTests(spec.removeBatchIllegal,
				p -> displayString("remove batch illegal: %s to %s", p.first, p.second),
				p -> spec.verifier.canRemoveItems(p.first, p.second), false, tests::add);

		// MOVE
		TestUtils.makeTests(spec.swapSingleLegal,
				p -> displayString("swap single legal: %s to %s", p.first, p.second),
				p -> spec.verifier.canSwapItems(p.first, p.second), true, tests::add);
		TestUtils.makeTests(spec.swapSingleIllegal,
				p -> displayString("swap single illegal: %s to %s", p.first, p.second),
				p -> spec.verifier.canSwapItems(p.first, p.second), false, tests::add);

		// TERMINAL
		TestUtils.makeTests(spec.setTerminalLegal,
				p -> displayString("set terminal legal: item_%s as %s at %s",
						p.second, label(p.third), p.first),
				p -> spec.verifier.canSetTerminal(p.first, p.second, p.third),
						true, tests::add);
		TestUtils.makeTests(spec.setTerminalIllegal,
				p -> displayString("set terminal illegal: item_%s as %s at %s",
						p.second, label(p.third), p.first),
				p -> spec.verifier.canSetTerminal(p.first, p.second, p.third),
						false, tests::add);

		// CREATE
		TestUtils.makeTests(spec.createEdgeLegal,
				p -> displayString("create edge legal: item_%s to item_%s",
						p.first, p.second),
				p -> spec.verifier.canCreateEdge(p.first, p.second),
						true, tests::add);
		TestUtils.makeTests(spec.createEdgeIllegal,
				p -> displayString("create edge illegal: item_%s to item_%s",
						p.first, p.second),
				p -> spec.verifier.canCreateEdge(p.first, p.second),
						false, tests::add);

		return tests;
	}

	static String label(boolean isSource) {
		return isSource ? "source" : "target";
	}
}