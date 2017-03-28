/*
 *  ICARUS 2 -  Interactive platform for Corpus Analysis and Research tools, University of Stuttgart
 *  Copyright (C) 2015-2016 Markus Gärtner
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
 *
 */
package de.ims.icarus2.model.api.driver.indices;

import static de.ims.icarus2.util.Conditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator.OfLong;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import de.ims.icarus2.GlobalErrorCode;
import de.ims.icarus2.model.api.ModelConstants;
import de.ims.icarus2.model.api.ModelErrorCode;
import de.ims.icarus2.model.api.ModelException;
import de.ims.icarus2.model.api.driver.indices.func.IndexIterativeIntersection;
import de.ims.icarus2.model.api.driver.indices.func.IndexSetMerger;
import de.ims.icarus2.model.api.driver.indices.standard.IndexBuffer;
import de.ims.icarus2.model.api.driver.indices.standard.SingletonIndexSet;
import de.ims.icarus2.model.api.driver.indices.standard.SpanIndexSet;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.manifest.util.Messages;
import de.ims.icarus2.util.IcarusUtils;
import de.ims.icarus2.util.stream.AbstractFencedSpliterator;

/**
 * @author Markus Gärtner
 *
 */
public class IndexUtils implements ModelConstants {

	public static final IndexSet[] EMPTY = new IndexSet[0];

	public static IndexValueType getDominantType(IndexSet[] indices) {
		return getDominantType(indices, 0, indices.length);
	}

	/**
	 *
	 * @param indices
	 * @param from first index to get value type from (inclusive)
	 * @param to last index to get value type from (exclusive)
	 * @return
	 */
	public static IndexValueType getDominantType(IndexSet[] indices, int from, int to) {
		IndexValueType result = IndexValueType.BYTE;

		for(int i=from; i<to; i++) {
			IndexValueType type = indices[i].getIndexValueType();

			if(type.compareTo(result)>0) {
				result = type;
			}

			if(result==IndexValueType.LONG) {
				break;
			}
		}

		return result;
	}

	public static IndexValueType getDominantType(Collection<IndexSet> indices) {
		IndexValueType result = IndexValueType.BYTE;

		for(IndexSet set : indices) {
			IndexValueType type = set.getIndexValueType();

			if(type.compareTo(result)>0) {
				result = type;
			}

			if(result==IndexValueType.LONG) {
				break;
			}
		}

		return result;
	}

	public static void checkNonEmpty(IndexSet[] indices) {
		requireNonNull(indices);
		if(indices.length==0)
			throw new ModelException(GlobalErrorCode.INVALID_INPUT, "Empty indices array"); //$NON-NLS-1$
		if(count(indices)==0)
			throw new ModelException(GlobalErrorCode.INVALID_INPUT, "Index array contains no actual index values"); //$NON-NLS-1$
	}

	public static boolean isSorted(IndexSet[] indices) {

		long previousMax = UNSET_LONG;

		for(int i=0; i<indices.length; i++) {
			IndexSet indexSet = indices[i];
			if(!indexSet.isSorted()) {
				return false;
			}

			if(previousMax!=UNSET_LONG && indexSet.firstIndex()<previousMax) {
				return false;
			}
		}

		return true;
	}

	public static void checkSorted(IndexSet indices) {
		if(!indices.isSorted())
			throw new ModelException(ModelErrorCode.MODEL_UNSORTED_INDEX_SET, "Index set is unsorted");
	}

	public static void checkSorted(IndexSet[] indices) {

		long previousMax = UNSET_LONG;

		for(int i=0; i<indices.length; i++) {
			IndexSet indexSet = indices[i];
			if(!indexSet.isSorted())
				throw new ModelException(ModelErrorCode.MODEL_UNSORTED_INDEX_SET, "Index set at position "+i+" is unsorted");

			if(previousMax!=UNSET_LONG && indexSet.firstIndex()<previousMax)
				throw new ModelException(ModelErrorCode.MODEL_UNSORTED_INDEX_SET, "Index set at position "+i+" is overlapping with previous one");
		}
	}

	public static void ensureSorted(IndexSet indices) {
		if(!indices.isSorted()) {
			indices.sort();
		}
	}

	public static void ensureSorted(IndexSet[] indices) {
		for(int i=0; i<indices.length; i++) {
			ensureSorted(indices[i]);
		}
	}

	public static long count(IndexSet[] indices) {
		long result = 0;

		for(IndexSet indexSet : indices) {
			result += (long)indexSet.size();
		}

		return result;
	}

	public static int minSize(IndexSet[] indices) {
		int result = 0;

		for(IndexSet indexSet : indices) {
			int size = indexSet.size();
			if(result==0 || size<result) {
				result = size;
			}
		}

		return result;
	}

	public static int maxSize(IndexSet[] indices) {
		int result = 0;

		for(IndexSet indexSet : indices) {
			int size = indexSet.size();
			if(result==0 || size>result) {
				result = size;
			}
		}

		return result;
	}

	public static boolean isContinuous(IndexSet indices) {
		return indices.isSorted() && indices.lastIndex()-indices.firstIndex()==indices.size()-1;
	}

	public static boolean isContinuous(IndexSet indices, int from, int to) {
		return indices.isSorted() && indices.indexAt(from)-indices.indexAt(to)==(to-from);
	}

	public static boolean isContinuous(IndexSet[] indices) {
		// Check first set
		if(!isContinuous(indices[0])) {
			return false;
		}

		// Check subsequent sets
		for(int i=1; i<indices.length; i++) {
			if(!isContinuous(indices[i]) || indices[i].firstIndex()!=indices[i-1].lastIndex()+1) {
				return false;
			}
		}

		return true;
	}

	public static <I extends Item> IndexSet toIndices(Collection<I> items, boolean forceSorted) {
		requireNonNull(items);
		checkArgument(!items.isEmpty());

		IndexBuffer result = new IndexBuffer(items.size());

		boolean requiresSorting = false;
		long lastIndex = UNSET_LONG;

		//TODO iterate over items, add index values and check if sorting is required!

		for(Item item : items) {
			long index = item.getIndex();

			requiresSorting |= (index<lastIndex);

			result.add(index);

			lastIndex = index;
		}

		if(requiresSorting && forceSorted) {
			// Expensive!!!
			result.sort();
		}

		return result;
	}

	/**
	 * Wraps a single index value into an array of {@link IndexSet} instances.
	 * The result will be an empty array iff the given {@code index} is {@code -1}
	 * or an array of exactly size {@code 1} containing a single {@link SingletonIndexSet}.
	 */
	public static IndexSet[] wrap(long index) {
		return index==UNSET_LONG ? EMPTY : new IndexSet[]{new SingletonIndexSet(index)};
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @return
	 *
	 * @throws IllegalArgumentException if {@code from < 0} or {@code to < 0} or {@code to < from}
	 */
	public static IndexSet[] wrap(long from, long to) {
		return new IndexSet[]{new SpanIndexSet(from, to)};
	}

	public static IndexSet[] wrap(IndexSet indices) {
		return (indices==null || indices.size()==0) ? EMPTY : new IndexSet[]{indices};
	}

	/**
	 * If the given collection of indices contains exactly {@code 1} index value, returns that
	 * value, otherwise the result will be {@code -1}.
	 */
	public static long unwrap(IndexSet[] indices) {
		return (indices.length==1 && indices[0].size()==1) ? firstIndex(indices) : UNSET_LONG;
	}

	public static void sort(IndexSet[] indices) {
		Arrays.sort(indices, IndexSet.INDEX_SET_SORTER);
	}

	public static long firstIndex(IndexSet[] indices) {
		return indices[0].firstIndex();
	}

	public static long lastIndex(IndexSet[] indices) {
		return indices[indices.length-1].lastIndex();
	}

	/**
	 * Combines a number of {@code IndexSet}s into a single one.
	 * Note that all the sets in the given array must be disjoint
	 * in addition to being sorted according to the specification
	 * in {@link IndexSet}.
	 *
	 * @param indices
	 * @return
	 */
	public static IndexSet combine(IndexSet[] indices) {
		long size = 0;

		long previousMax = UNSET_LONG;
		for(IndexSet set : indices) {
			if(set.firstIndex()<=previousMax)
				throw new ModelException(GlobalErrorCode.INVALID_INPUT,
						"Provided index sets are not disjoint");

			size+=set.size();
			previousMax = set.lastIndex();
		}

		if(size>IcarusUtils.MAX_INTEGER_INDEX)
			throw new ModelException(GlobalErrorCode.INDEX_OVERFLOW,
					Messages.outOfBoundsMessage(null, size, 0, IcarusUtils.MAX_INTEGER_INDEX));

		IndexValueType valueType = getDominantType(indices);

		IndexBuffer result = new IndexBuffer(valueType, (int) size);

		result.add(indices);

		return result;
	}

	public static IndexSet merge(IndexSet...indices) {
		return new IndexSetMerger(indices).mergeAllToSingle();
	}

	public static IndexSet merge(Collection<? extends IndexSet> indices) {
		return new IndexSetMerger(indices).mergeAllToSingle();
	}

	public static IndexSet[] mergeToArray(IndexSet...indices) {
		return new IndexSetMerger(indices).mergeAllToArray();
	}

	public static IndexSet[] mergeToArray(Collection<? extends IndexSet> indices) {
		return new IndexSetMerger(indices).mergeAllToArray();
	}

	public static IndexSet intersect(IndexSet set1, IndexSet set2) {
		if(set1.firstIndex()>set2.lastIndex() || set1.lastIndex()<set2.firstIndex()) {
			return null;
		}

		IndexBuffer buffer = new IndexBuffer(Math.min(set1.size(), set2.size()));

		if(IndexIterativeIntersection.intersect(set1, set2, buffer)) {
			return buffer;
		}

		return null;
	}

	public static IndexSet intersect(IndexSet...indices) {
		return new IndexIterativeIntersection(indices).intersectAll();
	}

	public static IndexSet intersect(Collection<? extends IndexSet> indices) {
		return new IndexIterativeIntersection(indices).intersectAll();
	}

	public static IndexSet[] intersect(long from1, long to1, long from2, long to2) {
		long from = Math.max(from1, from2);
		long to = Math.min(to1, to2);

		if(from>to) {
			return EMPTY;
		}

		long count = to-from+1;
		//FIXME allow for a flexible definition of the upper bound for chunk size
		int chunks = (int) Math.ceil(count/(double)IcarusUtils.MAX_INTEGER_INDEX);

		IndexSet[] result = new IndexSet[chunks];

		for(int i=0; i<chunks; i++) {
			long begin = from;
			long end = Math.min(begin+IcarusUtils.MAX_INTEGER_INDEX, to);

			result[i] = new SpanIndexSet(begin, end);

			from = end+1;
		}

		return result;
	}

	public static boolean forEachSpan(IndexSet[] indices, SpanProcedure procedure) throws InterruptedException {
		if(isContinuous(indices)) {
			return procedure.process(firstIndex(indices), lastIndex(indices));
		} else {

			boolean result = false;

			for(IndexSet set : indices) {
				boolean b= forEachSpan(set, procedure);
				result |= b;

				if(!b) {
					break;
				}
			}

			return result;
		}
	}

	public static boolean forEachSpan(IndexSet indices, SpanProcedure procedure) throws InterruptedException {
		if(isContinuous(indices)) {
			return procedure.process(indices.firstIndex(), indices.lastIndex());
		} else {
			long from = indices.firstIndex();
			long last = from;

			boolean result = false;

			for(int i=1; i<indices.size(); i++) {
				long val = indices.indexAt(i);

				if(val>last+1) {
					boolean b = procedure.process(from, last);
					result |= b;

					if(!b) {
						break;
					}

					from = val;
				}

				last = val;
			}

			result |= procedure.process(from, last);

			return result;
		}
	}

	public static long forEachPair(IndexSet setA, IndexSet setB, LongBinaryOperator action) {
		requireNonNull(setA);
		requireNonNull(setB);
		requireNonNull(action);

		int sizeA = setA.size();
		int sizeB = setB.size();
		if(sizeA!=sizeB)
			throw new ModelException(GlobalErrorCode.INVALID_INPUT,
					Messages.mismatchMessage("Mismatching index set sizes", sizeA, sizeB));

		long result = 0L;

		for(int i=0; i<sizeA; i++) {
			result += action.applyAsLong(setA.indexAt(i), setB.indexAt(i));
		}

		return result;
	}

	public static long forEachPair(IndexSet[] indicesA, IndexSet[] indicesB, LongBinaryOperator action) {
		requireNonNull(indicesA);
		requireNonNull(indicesB);
		requireNonNull(action);

		long indicesACount = count(indicesA);
		long indicesBCount = count(indicesB);
		if(indicesACount!=indicesBCount)
			throw new ModelException(GlobalErrorCode.INVALID_INPUT,
					Messages.mismatchMessage("Mismatching index value counts", indicesACount, indicesBCount));

		long result = 0L;

		int setIndexA = 0;
		int setIndexB = 0;
		int indexA = 0;
		int indexB = 0;

		while(setIndexA<indicesACount && setIndexB<indicesBCount) {
			IndexSet setA = indicesA[setIndexA];
			IndexSet setB = indicesB[setIndexB];

			int sizeA = setA.size();
			int sizeB = setB.size();
			while(indexA<sizeA && indexB<sizeB) {
				result += action.applyAsLong(setA.indexAt(indexA++), setB.indexAt(indexB++));
			}

			if(indexA>=sizeA) {
				indexA = 0;
				setIndexA++;
			}

			if(indexB>=sizeB) {
				indexB = 0;
				setIndexB++;
			}
		}

		return result;
	}

	public static void forEachIndex(IndexSet[] indices, LongConsumer action) {
		requireNonNull(indices);
		requireNonNull(action);

		for(IndexSet set : indices) {
			set.forEachIndex(action);
		}
	}

	public static void forEachIndex(IndexSet[] indices, IntConsumer action) {
		requireNonNull(indices);
		requireNonNull(action);

		for(IndexSet set : indices) {
			set.forEachIndex(action);
		}
	}

	public interface SpanProcedure {

		boolean process(long from, long to) throws InterruptedException;
	}

    /**
     * Checks that {@code fromIndex} and {@code toIndex} are in
     * the range and throws an exception if they aren't.
     */
    private static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                    "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        }
        if (toIndex > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }

	/**
	 *
     * @param indices the {@code IndexSet} to be searched
     * @param value the value to be searched for
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or <tt>toIndex</tt> if all
     *         elements in the range are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > indices.size()}
	 */
	public static int binarySearch(IndexSet indices, long value) {
		return binarySearch(indices, 0, indices.size(), value);
	}

	/**
	 *
     * @param indices the {@code IndexSet} to be searched
     * @param fromIndex the index of the first element (inclusive) to be
     *          searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param value the value to be searched for
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or <tt>toIndex</tt> if all
     *         elements in the range are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > indices.size()}
	 */
	public static int binarySearch(IndexSet indices, int fromIndex, int toIndex, long value) {

		// Copied from Arrays.binarySearch(long[], long)
		rangeCheck(indices.size(), fromIndex, toIndex);

        int low = fromIndex;
        int high = toIndex - 1;

        // Fast checks for values outside the specified range
		if(value<indices.indexAt(low)) {
			return -(low + 1);
		} else if(value>indices.indexAt(high)) {
			return -toIndex;
		}

		// From here on 'value' is guaranteed to be located within the index range of 'indices'!

		// Fast version in case of continuous index set
		if(isContinuous(indices)) {
			// Directly calculate the position our search valeu is supposed to be lcoated at
			int index = (int) (value-indices.firstIndex());
			if(indices.indexAt(index)!=value) {
				index = -(index + 1);
			}
			return index;
		}

		// Now perform regular binary search ont he index set
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = indices.indexAt(mid);

            if (midVal < value)
                low = mid + 1;
            else if (midVal > value)
                high = mid - 1;
            else
                return mid; // key found
        }

        return -(low + 1);  // key not found.
	}

	public static String toString(IndexSet indices) {
		StringBuilder sb = new StringBuilder();
		appendIndices(sb, indices);
		return sb.toString();
	}

	private static void appendIndices(StringBuilder sb, IndexSet indices) {
		sb.append('[');
		for(int i=0; i<indices.size(); i++) {
			if(i>0) {
				sb.append(',');
			}
			sb.append(indices.indexAt(i));
		}
		sb.append(']');
	}

	public static String toString(IndexSet[] indices) {
		StringBuilder sb = new StringBuilder();
		for(IndexSet set : indices) {
			appendIndices(sb, set);
		}
		return sb.toString();
	}

	public static IntStream asIntStream(IndexSet set) {
		return StreamSupport.intStream(new IndexSetIntSpliterator(set), false);
	}

	public static LongStream asLongStream(IndexSet set) {
		return StreamSupport.longStream(new IndexSetLongSpliterator(set), false);
	}

	public static class IndexSetIntSpliterator extends AbstractFencedSpliterator.OfInt {

		private final IndexSet source;

		public IndexSetIntSpliterator(IndexSet source) {
			this(source, 0, source.size());
		}

		/**
		 * @param pos
		 * @param fence
		 */
		public IndexSetIntSpliterator(IndexSet source, int pos, int fence) {
			super(pos, fence);

			requireNonNull(source);

			this.source = source;
		}

		private IndexSetIntSpliterator(IndexSet source, long pos, long fence) {
			this(source, IcarusUtils.ensureIntegerValueRange(pos), IcarusUtils.ensureIntegerValueRange(fence));
		}

		/**
		 * @see de.ims.icarus2.util.stream.AbstractFencedSpliterator.OfInt#currentInt()
		 */
		@Override
		protected int currentInt() {
			return IcarusUtils.ensureIntegerValueRange(source.indexAt((int) pos));
		}

		/**
		 * @see de.ims.icarus2.util.stream.AbstractFencedSpliterator#split(long, long)
		 */
		@Override
		protected Spliterator<Integer> split(long pos, long fence) {
			return new IndexSetIntSpliterator(source, pos, fence);
		}

		/**
		 * @see de.ims.icarus2.util.stream.AbstractFencedSpliterator#characteristics()
		 */
		@Override
		public int characteristics() {
			int characteristics = super.characteristics();
			if(source.isSorted()) {
				characteristics |= SORTED;
			}

			return characteristics;
		}
	}

	public static class IndexSetLongSpliterator extends AbstractFencedSpliterator.OfLong {

		private final IndexSet source;

		public IndexSetLongSpliterator(IndexSet source) {
			this(source, 0, source.size());
		}

		/**
		 * @param pos
		 * @param fence
		 */
		public IndexSetLongSpliterator(IndexSet source, int pos, int fence) {
			super(pos, fence);

			requireNonNull(source);

			this.source = source;
		}

		private IndexSetLongSpliterator(IndexSet source, long pos, long fence) {
			this(source, IcarusUtils.ensureIntegerValueRange(pos), IcarusUtils.ensureIntegerValueRange(fence));
		}

		/**
		 *
		 * @see de.ims.icarus2.util.stream.AbstractFencedSpliterator.OfLong#currentLong()
		 */
		@Override
		protected long currentLong() {
			return source.indexAt((int) pos);
		}

		/**
		 * @see de.ims.icarus2.util.stream.AbstractFencedSpliterator#split(long, long)
		 */
		@Override
		protected Spliterator<Long> split(long pos, long fence) {
			return new IndexSetLongSpliterator(source, pos, fence);
		}

		/**
		 * @see de.ims.icarus2.util.stream.AbstractFencedSpliterator#characteristics()
		 */
		@Override
		public int characteristics() {
			int characteristics = super.characteristics();
			if(source.isSorted()) {
				characteristics |= SORTED;
			}

			return characteristics;
		}
	}

	public static OfLong asIterator(IndexSet[] indices) {
		requireNonNull(indices);

		if(indices.length==1) {
			return new OfLongImpl(indices[0]);
		} else {
			return new CompositeOfLongImpl(indices);
		}
	}

	public static OfLong asIterator(IndexSet indices) {
		return new OfLongImpl(indices);
	}

	public static OfLong asIterator(IndexSet indices, int start) {
		return new OfLongImpl(indices, start);
	}

	public static OfLong asIterator(IndexSet indices, int start, int limit) {
		return new OfLongImpl(indices, start, limit);
	}

	/**
	 * Iterator implementation for traversal of a {@code IndexSet} or a specified
	 * subsection of it.
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class OfLongImpl implements OfLong {

		private final IndexSet source;
		private int pointer;
		private final int limit;

		public OfLongImpl(IndexSet source) {
			this(source, 0, -1);
		}

		public OfLongImpl(IndexSet source, int start) {
			this(source, start, -1);
		}

		/**
		 *
		 * @param source data to traverse
		 * @param start begin of region to be iterated over, inclusive
		 * @param limit end of region to be iterated over, exclusive
		 */
		public OfLongImpl(IndexSet source, int start, int limit) {
			requireNonNull(source);
			checkArgument(start>=0);

			this.source = source;
			pointer = start;

			if(limit<0) {
				limit = source.size();
			}

			this.limit = limit;
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return pointer<limit;
		}

		/**
		 * @see java.util.PrimitiveIterator.OfLong#nextLong()
		 */
		@Override
		public long nextLong() {
			if(pointer>=limit)
				throw new NoSuchElementException();

			return source.indexAt(pointer++);
		}

	}

	/**
	 * Iterator implementation for a set of {@link IndexSet} instances in
	 * succession.
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class CompositeOfLongImpl implements OfLong {

		private final IndexSet[] indices;
		private final int limit;

		private int index;
		private OfLong iterator;

		public CompositeOfLongImpl(IndexSet[] indices) {
			requireNonNull(indices);
			checkArgument(indices.length>0);

			this.indices = indices;
			limit = indices.length-1;

			index = -1;
		}

		public CompositeOfLongImpl(IndexSet[] indices, int fromIndex, int toIndex) {
			requireNonNull(indices);
			checkArgument(indices.length>0);

			this.indices = indices;
			limit = toIndex;

			index = fromIndex-1;
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {

			if(iterator==null || !iterator.hasNext()) {
				if(index>=limit) {
					iterator = null;
					return false;
				}

				index++;
				iterator = indices[index].iterator();
			}

			return iterator.hasNext();
		}

		/**
		 * @see java.util.PrimitiveIterator.OfLong#nextLong()
		 */
		@Override
		public long nextLong() {
			if(iterator==null)
				throw new NoSuchElementException();

			return iterator.nextLong();
		}

	}
}
