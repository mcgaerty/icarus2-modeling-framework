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

import static de.ims.icarus2.model.api.ModelTestUtils.mockEdge;
import static de.ims.icarus2.model.api.ModelTestUtils.mockItem;
import static de.ims.icarus2.model.api.ModelTestUtils.mockStructure;
import static de.ims.icarus2.model.api.ModelTestUtils.stubItems;
import static de.ims.icarus2.test.TestUtils.longRange;
import static de.ims.icarus2.test.util.Pair.intChain;
import static de.ims.icarus2.test.util.Pair.intPair;
import static de.ims.icarus2.test.util.Pair.longChain;
import static de.ims.icarus2.test.util.Pair.longPair;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import de.ims.icarus2.model.api.members.item.Edge;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.api.members.structure.Structure;
import de.ims.icarus2.model.api.members.structure.StructureEditVerifierTestBuilder;
import de.ims.icarus2.model.standard.members.structure.FixedSizeChainStorage.FixedSizeChainEditVerifier;
import de.ims.icarus2.test.util.Pair;
import de.ims.icarus2.util.IcarusUtils;
import de.ims.icarus2.util.MutablePrimitives.MutableInteger;

/**
 * @author Markus Gärtner
 *
 */
class FixedSizeChainEditVerifierTest {

	private FixedSizeChainStorage edgeStorage;
	private FixedSizeChainEditVerifier verifier;
	private Structure structure;

	/**
	 * Artificial index to indicate an edge should use the structrue's
	 * {@link Structure#getVirtualRoot() root} node as source.
	 */
	private static final int ROOT = -1;

	@BeforeEach
	void setUp() {
		edgeStorage = mock(FixedSizeChainStorage.class);
		structure = mockStructure(0, 0);

		verifier = new FixedSizeChainEditVerifier(edgeStorage, structure);
	}

	@AfterEach
	void tearDown() {
		verifier = null;
		structure = null;
		edgeStorage = null;
	}

	@SuppressWarnings("boxing")
	private void prepareStructureAndStorage(final int nodeCount,
			final @SuppressWarnings("unchecked") Pair<Integer, Integer>...edges) {
		assertTrue(nodeCount>0);
		assertTrue(edges.length>0);

		Edge[] edgeBuffer = new Edge[edges.length];

		// Make mocks return correct size info
		when(edgeStorage.getEdgeCount(eq(structure))).thenReturn(Long.valueOf(edges.length));
		when(edgeStorage.getEdgeAt(eq(structure), anyLong())).then(
				invocation -> {
					int index = ((Number)invocation.getArgument(1)).intValue();
					Edge edge = edgeBuffer[index];
					if(edge==null) {
						Pair<Integer, Integer> entry = edges[index];
						edge = mockEdge(structure, structure.getItemAt(entry.first),
								structure.getItemAt(entry.second));
						edgeBuffer[index] = edge;
					}
					return edge;
				});

		when(structure.getItemCount()).thenReturn((long)nodeCount);
		when(structure.getEdgeCount()).then(invocation -> edgeStorage.getEdgeCount(structure));
		when(structure.getEdgeAt(anyLong())).then(
				invocation -> edgeStorage.getEdgeAt(structure, invocation.getArgument(0)));

		// Mock container management
		stubItems(structure);

		// A bit more complex: mock edge handling
		boolean[] in = new boolean[nodeCount];
		boolean[] out = new boolean[nodeCount];

		MutableInteger rootOutgoingEdges = new MutableInteger();

		for(int i=0; i<edges.length; i++) {
			Pair<Integer, Integer> edge = edges[i];

			assertFalse(in[edge.second], "Node already has incoming edge: "+edge.second);

			if(edge.first==ROOT) {
				rootOutgoingEdges.incrementAndGet();
			} else {
				assertFalse(out[edge.first], "Node already has outgoing edge: "+edge.first);
				out[edge.first] = true;
			}

			in[edge.second] = true;
		}

		Item root = mockItem();

		// Now mock the actual behavior

		when(edgeStorage.getVirtualRoot(eq(structure))).thenReturn(root);

		when(edgeStorage.getUncheckedEdgeCount(eq(structure), any(), anyBoolean())).then(invocation -> {
			Item item = requireNonNull(invocation.getArgument(1));

			if(item==edgeStorage.getVirtualRoot(structure)) {
				return (boolean) invocation.getArgument(2) ? rootOutgoingEdges.longValue() : 0L;
			} else {
				int index = IcarusUtils.ensureIntegerValueRange(structure.indexOfItem(item));

				boolean[] hint = (boolean) invocation.getArgument(2) ? out : in;
				return hint[index] ? 1L : 0L;
			}
		});
		when(structure.getEdgeCount(any(), anyBoolean())).then(
				invocation -> edgeStorage.getUncheckedEdgeCount(structure,
						invocation.getArgument(0), invocation.getArgument(1)));

		when(edgeStorage.hasEdgeAt(eq(structure), anyLong())).then(invocation -> {
			int index = ((Number)invocation.getArgument(1)).intValue();
			return in[index];
		});

		when(structure.containsEdge(any())).then(invocation -> {
			Edge edge = invocation.getArgument(0);
			boolean sourceMatched = false;
			boolean targetMatched = false;
			for(int i=0; i<nodeCount; i++) {
				Item node = structure.getItemAt(i);
				if(node.equals(edge.getSource())) {
					sourceMatched = true;
				}
				if(node.equals(edge.getTarget())) {
					targetMatched = true;
				}
			}

			return sourceMatched && targetMatched;
		});
	}

	@TestFactory
	public Stream<DynamicTest> testNullArguments() {
		return StructureEditVerifierTestBuilder.createNullArgumentsTests(verifier);
	}

	@SuppressWarnings("unchecked")
	@TestFactory
	public Stream<DynamicTest> testIllegalMemberArguments() {
		prepareStructureAndStorage(2, intPair(0, 1));
		return StructureEditVerifierTestBuilder.createIllegalMemberTests(verifier);
	}

	/**
	 * Empty structure with no nodes or edges
	 */
	@SuppressWarnings({ "unchecked" })
	@TestFactory
	@DisplayName("empty []")
	public Stream<DynamicTest> testEmpty() {

		return new StructureEditVerifierTestBuilder(verifier)
				.addSingleIllegal(longRange(-1, 1))
				.addBatchIllegal(longRange(-1, 1))
				.removeSingleIllegal(longRange(-1, 1))
				.removeBatchIllegal(longPair(0, 0), longPair(0,  1))
				.swapSingleIllegal(longPair(0,  0), longPair(0,  1))
				// structure contains no items/edges, so all

				.createTests();
	}

	/**
	 * Test with a full chain of 3 connected edges:
	 * <blockquote><pre>
	 *   _   _   _
	 *  / \ / \ / \
	 * R   X   X   X
	 * </pre></blockquote>
	 *
	 */
	@TestFactory
	@DisplayName("full [---]")
	public Stream<DynamicTest> testFullStructure() {
		prepareStructureAndStorage(3, intChain(ROOT, 2));

		return new StructureEditVerifierTestBuilder(verifier)
				.addSingleIllegal(longRange(-1, 3))
				.addBatchIllegal(longRange(-1, 3))
				.removeSingleIllegal(longRange(-1, 3))
				.removeBatchIllegal(longChain(-1, 2))
				.swapSingleIllegal(longChain(-1, 2))
				//TODO add illegal values for terminal changes and edge creation!!!
				.createTests();
	}

	//TODO add test methods for different chain scenarios
}
