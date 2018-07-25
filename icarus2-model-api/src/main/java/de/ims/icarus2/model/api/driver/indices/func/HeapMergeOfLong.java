/*
 * ICARUS2 Corpus Modeling Framework
 * Copyright (C) 2014-2018 Markus Gärtner <markus.gaertner@uni-stuttgart.de>
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
package de.ims.icarus2.model.api.driver.indices.func;

import static de.ims.icarus2.util.Conditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

import de.ims.icarus2.model.api.driver.indices.IndexSet;
import de.ims.icarus2.util.collections.MappedMinHeap.MappedLongMinHeap;

/**
 * Implements the <i>merge</i> operation for iterators over long index values.
 * The implementation itself is again a long iterator and uses an internal
 * {@link MappedLongMinHeap heap} to store the current minimal values of each input
 * iterator, requiring the inputs (from whatever source) to be in <b>sorted order</b>!
 *
 * @author Markus Gärtner
 *
 */
public class HeapMergeOfLong implements PrimitiveIterator.OfLong {

	public static HeapMergeOfLong fromArrays(long[]...arrays) {
		requireNonNull(arrays);
		checkArgument(arrays.length>2);

		OfLong[] sources = new OfLong[arrays.length];

		for(int i=0; i<arrays.length; i++) {
			sources[i] = Arrays.stream(arrays[i]).iterator();
		}

		return new HeapMergeOfLong(sources);
	}

	public static HeapMergeOfLong fromIndices(IndexSet...indices) {
		requireNonNull(indices);
		checkArgument(indices.length>2);

		OfLong[] sources = new OfLong[indices.length];

		for(int i=0; i<indices.length; i++) {
			sources[i] = indices[i].iterator();
		}

		return new HeapMergeOfLong(sources);
	}

	public static HeapMergeOfLong fromIndices(Collection<IndexSet> indices) {
		requireNonNull(indices);
		checkArgument(indices.size()>2);

		OfLong[] sources = new OfLong[indices.size()];

		int index = 0;
		for(IndexSet set : indices) {
			sources[index++] = set.iterator();
		}

		return new HeapMergeOfLong(sources);
	}

	private final MappedLongMinHeap<PrimitiveIterator.OfLong> heap;

	public HeapMergeOfLong(PrimitiveIterator.OfLong[] sources) {
		requireNonNull(sources);

		heap = new MappedLongMinHeap<>(sources.length);

		for(int i=0; i<sources.length; i++) {
			OfLong source = sources[i];
			if(source.hasNext()) {
				heap.push(source, source.nextLong());
			}
		}
	}

	/**
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return !heap.isEmpty();
	}

	/**
	 * @see java.util.PrimitiveIterator.OfLong#nextLong()
	 */
	@Override
	public long nextLong() {
		if (heap.isEmpty())
			throw new NoSuchElementException();

		// "Peek" result value and corresponding source
		long value = heap.peekValue();
		PrimitiveIterator.OfLong source = heap.peekObject();

		// Now remove root
		heap.pop();

		// Add replacement value from used input stream if possible
		if(source.hasNext()) {
			heap.push(source, source.nextLong());
		}

		return value;
	}
}