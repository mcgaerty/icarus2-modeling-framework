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
package de.ims.icarus2.model.standard.members.structure.builder;

import static de.ims.icarus2.util.IcarusUtils.UNSET_INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.ims.icarus2.model.standard.members.structure.builder.StaticTreeEdgeStorage.CompactTreeEdgeStorageInt;
import de.ims.icarus2.test.annotations.DisabledOnCi;

/**
 * @author Markus Gärtner
 *
 */
class CompactTreeEdgeStorageIntTest implements StaticTreeEdgeStorageTest<CompactTreeEdgeStorageInt> {

	/**
	 * @see de.ims.icarus2.test.TargetedTest#getTestTargetClass()
	 */
	@Override
	public Class<? extends CompactTreeEdgeStorageInt> getTestTargetClass() {
		return CompactTreeEdgeStorageInt.class;
	}

	/**
	 * @see de.ims.icarus2.model.standard.members.structure.builder.StaticTreeEdgeStorageTest#createFromBuilder(de.ims.icarus2.model.standard.members.structure.builder.StructureBuilder)
	 */
	@Override
	public CompactTreeEdgeStorageInt createFromBuilder(StructureBuilder builder) {
		return CompactTreeEdgeStorageInt.fromBuilder(builder);
	}


	@Nested
	class EdgeCases {

		@Test
		@DisabledOnCi
		void testMaxChainSize() {
			int size = CompactTreeEdgeStorageInt.MAX_NODE_COUNT;
			ChainsAndTrees.TreeConfig treeConfig = ChainsAndTrees.singleTree(size, 1.0,
					CompactTreeEdgeStorageInt.MAX_HEIGHT, UNSET_INT);
			CompactTreeEdgeStorageInt tree = createFromBuilder(toBuilder(treeConfig));
			assertEquals(size, tree.getEdgeCount(treeConfig.structure));
		}

		@SuppressWarnings("boxing")
		@Test
		void testOverflowNodeCount() {
			StructureBuilder builder = mock(StructureBuilder.class);
			when(builder.getNodeCount()).thenReturn(CompactTreeEdgeStorageInt.MAX_NODE_COUNT+1);
			assertThrows(IllegalArgumentException.class,
					() -> createFromBuilder(builder));
		}

		@SuppressWarnings("boxing")
		@Test
		void testOverflowEdgeCount() {
			StructureBuilder builder = mock(StructureBuilder.class);
			when(builder.getNodeCount()).thenReturn(CompactTreeEdgeStorageInt.MAX_NODE_COUNT);
			when(builder.getEdgeCount()).thenReturn(CompactTreeEdgeStorageInt.MAX_EDGE_COUNT+1);
			assertThrows(IllegalArgumentException.class,
					() -> createFromBuilder(builder));
		}

		@SuppressWarnings("boxing")
		@Test
		void testOverflowHeight() {
			StructureBuilder builder = mock(StructureBuilder.class);
			when(builder.getNodeCount()).thenReturn(CompactTreeEdgeStorageInt.MAX_NODE_COUNT);
			when(builder.getEdgeCount()).thenReturn(CompactTreeEdgeStorageInt.MAX_EDGE_COUNT);
			EdgeBuffer edgeBuffer = mock(EdgeBuffer.class);
			when(edgeBuffer.getMaxHeight()).thenReturn(CompactTreeEdgeStorageInt.MAX_HEIGHT+1);
			when(edgeBuffer.getMaxDepth()).thenReturn(CompactTreeEdgeStorageInt.MAX_DEPTH);

			when(builder.edgeBuffer()).thenReturn(edgeBuffer);

			assertThrows(IllegalArgumentException.class,
					() -> createFromBuilder(builder));
		}

		@SuppressWarnings("boxing")
		@Test
		void testOverflowDepth() {
			StructureBuilder builder = mock(StructureBuilder.class);
			when(builder.getNodeCount()).thenReturn(CompactTreeEdgeStorageInt.MAX_NODE_COUNT);
			when(builder.getEdgeCount()).thenReturn(CompactTreeEdgeStorageInt.MAX_EDGE_COUNT);
			EdgeBuffer edgeBuffer = mock(EdgeBuffer.class);
			when(edgeBuffer.getMaxHeight()).thenReturn(CompactTreeEdgeStorageInt.MAX_HEIGHT);
			when(edgeBuffer.getMaxDepth()).thenReturn(CompactTreeEdgeStorageInt.MAX_DEPTH+1);

			when(builder.edgeBuffer()).thenReturn(edgeBuffer);

			assertThrows(IllegalArgumentException.class,
					() -> createFromBuilder(builder));
		}
	}
}
