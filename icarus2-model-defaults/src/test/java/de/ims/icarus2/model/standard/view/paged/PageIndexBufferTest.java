/*
 * ICARUS2 Corpus Modeling Framework
 * Copyright (C) 2014-2025 Markus Gärtner <markus.gaertner@ims.uni-stuttgart.de>
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
package de.ims.icarus2.model.standard.view.paged;

import static de.ims.icarus2.model.api.ModelAssertions.assertThat;
import static de.ims.icarus2.model.api.ModelTestUtils.assertModelException;
import static de.ims.icarus2.model.api.ModelTestUtils.mockIndices;
import static de.ims.icarus2.model.api.ModelTestUtils.range;
import static de.ims.icarus2.model.api.driver.indices.IndexUtils.wrap;
import static de.ims.icarus2.model.api.driver.indices.IndexUtils.wrapSpan;
import static de.ims.icarus2.test.TestUtils.RUNS;
import static de.ims.icarus2.test.TestUtils.assertNPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.ims.icarus2.GlobalErrorCode;
import de.ims.icarus2.model.api.driver.indices.IndexSet;
import de.ims.icarus2.model.api.driver.indices.IndexUtils;
import de.ims.icarus2.test.annotations.RandomizedTest;
import de.ims.icarus2.test.random.RandomGenerator;

/**
 * @author Markus Gärtner
 *
 */
class PageIndexBufferTest {

	@Nested
	class Constructor {
		@Test
		void testNullIndices() {
			assertNPE(() -> new PageIndexBuffer(null, 0));
		}

		@ParameterizedTest
		@ValueSource(ints = {0, -1})
		void testInvalidPageSize(int size) {
			assertModelException(GlobalErrorCode.INVALID_INPUT,
					() -> new PageIndexBuffer(wrap(1), size));
		}

		@Test
		void testEmptyIndices() {
			assertModelException(GlobalErrorCode.INVALID_INPUT,
					() -> new PageIndexBuffer(IndexUtils.EMPTY, 1));
		}
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.standard.view.paged.PageIndexBuffer#getPageSize()}.
	 */
	@Test
	@RandomizedTest
	void testGetPageSize(RandomGenerator rng) {
		int size = rng.random(1, Integer.MAX_VALUE);
		PageIndexBuffer instance = new PageIndexBuffer(wrap(1), size);
		assertEquals(size, instance.getPageSize());
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.standard.view.paged.PageIndexBuffer#getSize()}.
	 */
	@Test
	@RandomizedTest
	void testGetSizeSingle(RandomGenerator rng) {
		int size = rng.random(1, Integer.MAX_VALUE);
		PageIndexBuffer instance = new PageIndexBuffer(mockIndices(size), 1);
		assertEquals(size, instance.getSize());
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.standard.view.paged.PageIndexBuffer#getSize()}.
	 */
	@RepeatedTest(value=RUNS)
	@RandomizedTest
	void testGetSize(RandomGenerator rng) {
		int count = rng.random(2, 10);
		int[] sizes = rng.randomInts(count, 1, Integer.MAX_VALUE);
		IndexSet[] sets = mockIndices(sizes);
		PageIndexBuffer instance = new PageIndexBuffer(sets, 10);
		long size = IndexUtils.count(sets);
		assertEquals(size, instance.getSize());
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.standard.view.paged.PageIndexBuffer#getPageCount()}.
	 */
	@TestFactory
	List<DynamicTest> testGetPageCount() {
		return Arrays.asList(
				dynamicTest("size==page_size", () -> assertEquals(
						1, new PageIndexBuffer(mockIndices(10), 10).getPageCount())),
				dynamicTest("size<page_size", () -> assertEquals(
						1, new PageIndexBuffer(mockIndices(10), 100).getPageCount())),
				dynamicTest("size==page_size*2", () -> assertEquals(
						2, new PageIndexBuffer(mockIndices(10,10), 10).getPageCount())),
				dynamicTest("size<page_size*2", () -> assertEquals(
						2, new PageIndexBuffer(mockIndices(10,5), 10).getPageCount())),
				dynamicTest("size==page_size*100", () -> assertEquals(
						100, new PageIndexBuffer(mockIndices(500,500), 10).getPageCount()))
		);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.standard.view.paged.PageIndexBuffer#createPage(int)}.
	 */
	@TestFactory
	List<DynamicNode> testCreatePage() {
		return Arrays.asList(
				dynamicTest("size==page_size [single set]",
						() -> assertThat(new PageIndexBuffer(wrapSpan(0, 9), 10).createPage(0))
						.containsExactlyIndices(LongStream.rangeClosed(0, 9).toArray())),

				dynamicTest("size==page_size [multi sets]",
						() -> assertThat(new PageIndexBuffer(new IndexSet[] {
								range(0, 3), range(4, 6), range(7, 9)
						}, 10).createPage(0))
						.containsExactlyIndices(LongStream.rangeClosed(0, 9).toArray())),

				dynamicTest("size>page_size [single set, 1/2]",
						() -> assertThat(new PageIndexBuffer(wrapSpan(0, 9), 5).createPage(0))
						.containsExactlyIndices(LongStream.rangeClosed(0, 4).toArray())),

				dynamicTest("size>page_size [single set, 2/2]",
						() -> assertThat(new PageIndexBuffer(wrapSpan(0, 9), 5).createPage(1))
						.containsExactlyIndices(LongStream.rangeClosed(5, 9).toArray())),

				dynamicTest("size==page_size [multi sets, 1/2]",
						() -> assertThat(new PageIndexBuffer(new IndexSet[] {
								range(0, 3), range(4, 6), range(7, 9)
						}, 5).createPage(0))
						.containsExactlyIndices(LongStream.rangeClosed(0, 4).toArray())),

				dynamicTest("size==page_size [multi sets, 2/2]",
						() -> assertThat(new PageIndexBuffer(new IndexSet[] {
								range(0, 3), range(4, 6), range(7, 9)
						}, 5).createPage(1)).containsExactlyIndices(LongStream.rangeClosed(5, 9).toArray()))
		);
	}

}
