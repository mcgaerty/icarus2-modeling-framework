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
package de.ims.icarus2.model.standard.members.structure;

import static de.ims.icarus2.model.standard.ModelDefaultsTestUtils.fillEdges;
import static de.ims.icarus2.model.standard.ModelDefaultsTestUtils.fillItems;
import static de.ims.icarus2.model.standard.ModelDefaultsTestUtils.makeStructure;
import static de.ims.icarus2.test.TestUtils.longRange;
import static de.ims.icarus2.test.util.Pair.intChain;
import static de.ims.icarus2.test.util.Pair.intPair;
import static de.ims.icarus2.test.util.Pair.longChain;
import static de.ims.icarus2.test.util.Pair.longPair;
import static de.ims.icarus2.test.util.Triple.triple;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import de.ims.icarus2.model.api.members.structure.Structure;
import de.ims.icarus2.model.manifest.ManifestTestUtils;
import de.ims.icarus2.model.manifest.api.ManifestType;
import de.ims.icarus2.model.manifest.api.StructureManifest;
import de.ims.icarus2.model.manifest.api.StructureType;
import de.ims.icarus2.model.standard.members.container.ContainerEditVerifierTestBuilder;
import de.ims.icarus2.model.standard.members.container.ImmutableContainerEditVerifierTest;
import de.ims.icarus2.model.standard.members.container.ItemStorage;
import de.ims.icarus2.model.standard.members.container.ListItemStorageInt;
import de.ims.icarus2.model.standard.members.structure.FixedSizeChainStorage.FixedSizeChainEditVerifier;
import de.ims.icarus2.test.util.Pair;

/**
 * @author Markus Gärtner
 *
 */
class FixedSizeChainEditVerifierTest {

	private FixedSizeChainStorage edgeStorage;
	private ItemStorage itemStorage;
	private Structure structure;

	/**
	 * Verifier under test
	 */
	private FixedSizeChainEditVerifier verifier;

	/**
	 * Artificial index to indicate an edge should use the structrue's
	 * {@link Structure#getVirtualRoot() root} node as source.
	 */
	private static final int ROOT = -1;

	@BeforeEach
	void setUp() {
		StructureManifest manifest = ManifestTestUtils.MANIFEST_FACTORY
				.create(ManifestType.STRUCTURE_MANIFEST, StructureManifest.class)
				.setStructureType(StructureType.CHAIN);

		itemStorage = new ListItemStorageInt();
		edgeStorage = new FixedSizeChainStorage();
		structure = makeStructure(manifest, itemStorage, edgeStorage);

		verifier = new FixedSizeChainEditVerifier(edgeStorage, structure);
	}

	@AfterEach
	void tearDown() {
		verifier = null;
		structure = null;
		edgeStorage = null;
		itemStorage = null;
	}

	private void prepareStructureAndStorage(final int nodeCount,
			final @SuppressWarnings("unchecked") Pair<Integer, Integer>...edges) {
		assertTrue(nodeCount>0);
		assertTrue(edges.length>0);

		fillItems(structure, nodeCount);

		/*
		 *  This takes care of internal maintenance the storage has to perform.
		 *  We need this as the edge storage relies on static information from the
		 *  underlying item storage.
		 */
		edgeStorage.addNotify(structure);

		fillEdges(structure, edges);

	}

	@TestFactory
	public Stream<DynamicTest> testNullArguments() {
		return new StructureEditVerifierTestBuilder(verifier)
				.failForNullMembers()
				.createTests();
	}

	@SuppressWarnings("unchecked")
	@TestFactory
	public Stream<DynamicTest> testIllegalMemberArguments() {
		prepareStructureAndStorage(2, intPair(0, 1));
		return new StructureEditVerifierTestBuilder(verifier)
				.failForIllegalMembers()
				.createTests();
	}

	@TestFactory
	public Stream<DynamicTest> testContainerEditVerifierEmpty() {
		return ImmutableContainerEditVerifierTest.configureBuilderEmpty(
				new ContainerEditVerifierTestBuilder(verifier))
				.createTests();
	}

	@TestFactory
	public Stream<DynamicTest> testContainerEditVerifierFull() {
		prepareStructureAndStorage(10, intChain(ROOT, 9));
		return ImmutableContainerEditVerifierTest.configureBuilder(
				new ContainerEditVerifierTestBuilder(verifier))
				.createTests();
	}

	/**
	 * Empty structure with no nodes or edges
	 */
	@SuppressWarnings({ "unchecked" })
	@TestFactory
	@DisplayName("empty []")
	public Stream<DynamicTest> testStructureEmpty() {

		/*
		 * Structure under test contains no items/edges, so all
		 * verifier methods requiring live items or edges will
		 * throw an exception.
		 *
		 * This is covered in testIllegalMemberArguments()
		 */
		return new StructureEditVerifierTestBuilder(verifier)
				.removeSingleIllegal(longRange(-1, 1))
				.removeBatchIllegal(longPair(0, 0), longPair(0,  1))
				.swapSingleIllegal(longPair(0,  0), longPair(0,  1))
				.createTests();
	}

	/**
	 * Test with a full chain of 3 fully connected nodes:
	 * <blockquote><pre>
	 *   0   1   2
	 *  / \ / \ / \
	 * R   0   1   2
	 * </pre></blockquote>
	 *
	 */
	@SuppressWarnings({ "unchecked", "boxing" })
	@TestFactory
	@DisplayName("full [↷↷↷]")
	public Stream<DynamicTest> testFullStructure() {
		prepareStructureAndStorage(3, intChain(ROOT, 2));

		return new StructureEditVerifierTestBuilder(verifier)
				// no legal single edges to add
				.addSingleIllegal(intPair(0,2), intPair(ROOT, 2))
				// no legal batches of edges to add
				.addBatchIllegalIndirect(intPair(0, 2), intPair(ROOT, 1))
				.removeSingleLegal(0, 1, 2)
				.removeSingleIllegal(-1, 3)
				.removeBatchLegal(intPair(0, 0), intPair(0, 1), intPair(1, 2), intPair(0, 2))
				.removeBatchIllegal(intPair(-1, 0), intPair(1, 3))
				// no legal swap indices
				.swapSingleIllegal(longChain(-1, 2))
				.setTerminalLegalIndirect(triple(1, ROOT, true), triple(2, ROOT, true)) // legal to split chains into subchains
				.setTerminalIllegalIndirect(
						triple(0, 1, false), triple(1, 2, false), triple(2, 0, false), // edgeCount(target)>1
						triple(0, 1, true), triple(1, 1, true), triple(2, 0, true), // edgeCount(source)>1
						triple(0, ROOT, false), triple(1, 0, false), triple(2, 1, false)) // loops
				.createEdgeLegalIndirect(
						intPair(ROOT, 0), intPair(0, 1), intPair(1, 2), // single-step edges
						intPair(ROOT, 1), intPair(ROOT, 2), intPair(0, 2)) // long-distance edges
				.createEdgeIllegalIndirect(intPair(0, ROOT), intPair(1, ROOT), intPair(2, ROOT)) // no edges towards root
				.createTests();
	}

	/**
	 * Test with a partial chain of 3 nodes and only 2 edges:
	 * <blockquote><pre>
	 *   0       1
	 *  / \     / \
	 * R   0   1   2
	 * </pre></blockquote>
	 *
	 */
	@SuppressWarnings({ "unchecked", "boxing" })
	@TestFactory
	@DisplayName("partial [↷_↷]")
	public Stream<DynamicTest> testPartialStructure() {
		prepareStructureAndStorage(3, intPair(ROOT, 0), intPair(1, 2));

		return new StructureEditVerifierTestBuilder(verifier)
				.addSingleLegal(intPair(0, 1), intPair(ROOT, 1))
				.addSingleIllegal(intPair(0,2), intPair(ROOT, 2))
				.addBatchLegalIndirect(intPair(0, 1))
					.addBatchLegalIndirect(intPair(ROOT, 1))
					.addBatchLegalIndirect(intPair(ROOT, 1), intPair(0, 1))
				.addBatchIllegalIndirect(intPair(0, 2), intPair(ROOT, 1))
				.removeSingleLegal(0, 1)
				.removeSingleIllegal(-1, 2)
				.removeBatchLegal(intPair(0, 0), intPair(0, 1), intPair(1, 1))
				.removeBatchIllegal(intPair(-1, 0), intPair(-1, 1), intPair(1, 2))
				// no legal swap indices
				.swapSingleIllegal(longChain(-1, 2))
				.setTerminalLegalIndirect(
						// edge_0, source
						triple(0, 2, true),
						// edge_1, source
						triple(1, 0, true), triple(1, ROOT, true),
						// edge_0, target,
						triple(0, 1, false))
				.setTerminalIllegalIndirect(
						triple(0, 2, false), triple(1, 0, false), // edgeCount(target)>1
						triple(0, 1, true), // edgeCount(source)>1
						triple(0, ROOT, false), triple(1, 1, false)) // loops
				.createEdgeLegalIndirect(
						intPair(ROOT, 0), intPair(0, 1), intPair(1, 2), // single-step edges
						intPair(ROOT, 1), intPair(ROOT, 2), intPair(0, 2)) // long-distance edges
				.createEdgeIllegalIndirect(intPair(0, ROOT), intPair(1, ROOT), intPair(2, ROOT)) // no edges towards root
				.createTests();
	}

	/**
	 * Test with a partial chain of 3 nodes and only 1 edge:
	 * <blockquote><pre>
	 *       0
	 *      / \
	 * R   0   1   2
	 * </pre></blockquote>
	 *
	 */
	@SuppressWarnings({ "unchecked", "boxing" })
	@TestFactory
	@DisplayName("partial [_↷_]")
	public Stream<DynamicTest> testPartialStructure2() {
		prepareStructureAndStorage(3, intPair(0, 1));

		return new StructureEditVerifierTestBuilder(verifier)
				.addSingleLegal(intPair(ROOT, 0), intPair(ROOT, 2), intPair(1, 2))
				.addSingleIllegal(intPair(0, 1), intPair(ROOT, 1))
				.addBatchLegalIndirect(intPair(ROOT, 0))
					.addBatchLegalIndirect(intPair(ROOT, 2))
					.addBatchLegalIndirect(intPair(1, 2))
					.addBatchLegalIndirect(intPair(ROOT, 0), intPair(1, 2), intPair(ROOT, 2))
				.addBatchIllegalIndirect(intPair(0, 2), intPair(ROOT, 1))
				.removeSingleLegal(0)
				.removeSingleIllegal(-1, 1)
				.removeBatchLegal(intPair(0, 0))
				.removeBatchIllegal(intPair(-1, 0), intPair(-1, 1), intPair(1, 1))
				// no legal swap indices
				.swapSingleIllegal(longChain(-1, 1))
				.setTerminalLegalIndirect(
						// edge_0, source
						triple(0, 2, true), triple(0, ROOT, true),
						// edge_0, target,
						triple(0, 2, false))
				.setTerminalIllegalIndirect(
						triple(0, ROOT, false), triple(0, 0, false)) // loops+ROOT target
				.createEdgeLegalIndirect(
						intPair(ROOT, 0), intPair(0, 1), intPair(1, 2), // single-step edges
						intPair(ROOT, 1), intPair(ROOT, 2), intPair(0, 2)) // long-distance edges
				.createEdgeIllegalIndirect(intPair(0, ROOT), intPair(1, ROOT), intPair(2, ROOT)) // no edges towards root
				.createTests();
	}
}
