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

 * $Revision: 457 $
 * $Date: 2016-04-20 15:08:11 +0200 (Mi, 20 Apr 2016) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus2Core/core/de.ims.icarus2.core/source/de/ims/icarus2/util/collections/MinHeap.java $
 *
 * $LastChangedDate: 2016-04-20 15:08:11 +0200 (Mi, 20 Apr 2016) $
 * $LastChangedRevision: 457 $
 * $LastChangedBy: mcgaerty $
 */
package de.ims.icarus2.util.collections;

import static de.ims.icarus2.model.util.Conditions.checkArgument;
import static de.ims.icarus2.model.util.Conditions.checkNotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;

/**
 * @author Markus Gärtner
 * @version $Id: MinHeap.java 457 2016-04-20 13:08:11Z mcgaerty $
 *
 */
public abstract class MinHeap<T extends Object> extends Heap {

	// Mapped objects, order matches values in heap
	protected final Object[] elements;

	protected MinHeap(int size) {
		checkArgument(size>0);

		elements = new Object[size];
	}

	@SuppressWarnings("unchecked")
	public T peekObject() {
		if(size<1)
			throw new NoSuchElementException();

		return (T) elements[0];
	}

	@Override
	public void clear() {
		super.clear();
		Arrays.fill(elements, null);
	}

	@Override
	protected void swap(int index0, int index1) {
		// Default value swap
		super.swap(index0, index1);

		// Now do the element swap
		Object tmpObj = elements[index0];
		elements[index0] = elements[index1];
		elements[index1] = tmpObj;
	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: MinHeap.java 457 2016-04-20 13:08:11Z mcgaerty $
	 *
	 * @param <T>
	 */
	public static class IntMinHeap<T extends Object> extends MinHeap<T> implements ObjIntConsumer<T> {
		private final int[] values;

		public IntMinHeap(int size) {
			super(size);

			values = new int[size];
		}

		public int peekValue() {
			if(size<1)
				throw new NoSuchElementException();

			return values[0];
		}

		/**
		 * @see de.ims.icarus2.util.collections.MinHeap#compareValuesAt(int, int)
		 */
		@Override
		protected int compareValuesAt(int index0, int index1) {
			return Integer.compare(values[index0], values[index1]);
		}

		public void push(T source, int value) {
			int index = size;
			size++;
			elements[index] = source;
			values[index] = value;

			refreshUp(index);
		}

		public T pop() {
			size--;

			@SuppressWarnings("unchecked")
			T result = (T)elements[0];

			elements[0] = elements[size];
			values[0] = values[size];

			if (size > 0) {
				refreshDown(0);
			}

			return result;
		}

		@Override
		protected void swapValues(int index0, int index1) {
			int tmpInt = values[index0];
			values[index0] = values[index1];
			values[index1] = tmpInt;
		}

		/**
		 * @see java.util.function.ObjIntConsumer#accept(java.lang.Object, int)
		 */
		@Override
		public void accept(T t, int value) {
			push(t, value);
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: MinHeap.java 457 2016-04-20 13:08:11Z mcgaerty $
	 *
	 * @param <T>
	 */
	public static class LongMinHeap<T extends Object> extends MinHeap<T> implements ObjLongConsumer<T> {
		private final long[] values;

		public LongMinHeap(int size) {
			super(size);

			values = new long[size];
		}

		public long peekValue() {
			if(size<1)
				throw new NoSuchElementException();

			return values[0];
		}

		/**
		 * @see de.ims.icarus2.util.collections.MinHeap#compareValuesAt(int, int)
		 */
		@Override
		protected int compareValuesAt(int index0, int index1) {
			return Long.compare(values[index0], values[index1]);
		}

		public void push(T source, long value) {
			int index = size;
			size++;
			elements[index] = source;
			values[index] = value;

			refreshUp(index);
		}

		public T pop() {
			size--;

			@SuppressWarnings("unchecked")
			T result = (T)elements[0];

			elements[0] = elements[size];
			values[0] = values[size];

			if (size > 0) {
				refreshDown(0);
			}

			return result;
		}

		@Override
		protected void swapValues(int index0, int index1) {
			long tmpLong = values[index0];
			values[index0] = values[index1];
			values[index1] = tmpLong;
		}

		/**
		 * @see java.util.function.ObjLongConsumer#accept(java.lang.Object, long)
		 */
		@Override
		public void accept(T t, long value) {
			push(t, value);
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: MinHeap.java 457 2016-04-20 13:08:11Z mcgaerty $
	 *
	 * @param <T>
	 */
	public static class DoubleMinHeap<T extends Object> extends MinHeap<T> implements ObjDoubleConsumer<T> {
		private final double[] values;

		public DoubleMinHeap(int size) {
			super(size);

			values = new double[size];
		}

		public double peekValue() {
			if(size<1)
				throw new NoSuchElementException();

			return values[0];
		}

		/**
		 * @see de.ims.icarus2.util.collections.MinHeap#compareValuesAt(int, int)
		 */
		@Override
		protected int compareValuesAt(int index0, int index1) {
			return Double.compare(values[index0], values[index1]);
		}

		public void push(T source, double value) {
			int index = size;
			size++;
			elements[index] = source;
			values[index] = value;

			refreshUp(index);
		}

		public T pop() {
			size--;

			@SuppressWarnings("unchecked")
			T result = (T)elements[0];

			elements[0] = elements[size];
			values[0] = values[size];

			if (size > 0) {
				refreshDown(0);
			}

			return result;
		}

		@Override
		protected void swapValues(int index0, int index1) {
			double tmpDouble = values[index0];
			values[index0] = values[index1];
			values[index1] = tmpDouble;
		}

		/**
		 * @see java.util.function.ObjDoubleConsumer#accept(java.lang.Object, double)
		 */
		@Override
		public void accept(T t, double value) {
			push(t, value);
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: MinHeap.java 457 2016-04-20 13:08:11Z mcgaerty $
	 *
	 * @param <T> type of the associated elements in the heap
	 * @param <E> type of the values in the heap
	 */
	public static class ObjectMinHeap<E extends Object, T extends Object> extends MinHeap<T> implements BiConsumer<T, E> {
		private final Object[] values;
		private final Comparator<? super E> comparator;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public ObjectMinHeap(int size) {
			super(size);
			values = new Object[size];
			comparator = (o1, o2) -> ((Comparable)o1).compareTo(o2);
		}

		public ObjectMinHeap(int size, Comparator<? super E> comparator) {
			super(size);
			checkNotNull(comparator);

			values = new Object[size];
			this.comparator = comparator;
		}

		@SuppressWarnings("unchecked")
		public T peekValue() {
			if(size<1)
				throw new NoSuchElementException();

			return (T) values[0];
		}

		/**
		 * @see de.ims.icarus2.util.collections.MinHeap#compareValuesAt(int, int)
		 */
		@SuppressWarnings("unchecked")
		@Override
		protected int compareValuesAt(int index0, int index1) {
			return comparator.compare((E)values[index0], (E)values[index1]);
		}

		public void push(T source, E value) {
			int index = size;
			size++;
			elements[index] = source;
			values[index] = value;

			refreshUp(index);
		}

		public T pop() {
			size--;

			@SuppressWarnings("unchecked")
			T result = (T)elements[0];

			elements[0] = elements[size];
			values[0] = values[size];

			if (size > 0) {
				refreshDown(0);
			}

			return result;
		}

		@Override
		protected void swapValues(int index0, int index1) {
			Object tmp = values[index0];
			values[index0] = values[index1];
			values[index1] = tmp;
		}

		@Override
		public void accept(T t, E u) {
			push(t, u);
		}
	}
}
