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
package de.ims.icarus2.test.util;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;

import javax.annotation.Nullable;

import de.ims.icarus2.test.TestUtils;

public class Pair<FIRST, SECOND> {

	public static <FIRST, SECOND> Pair<FIRST, SECOND> pair(FIRST first, SECOND second) {
		return new Pair<>(first, second);
	}

//	public static Pair<Integer, Integer> pair(int first, int second) {
//		return new Pair<>(Integer.valueOf(first), Integer.valueOf(second));
//	}

	public static <FIRST, SECOND> Pair<FIRST, SECOND> nullablePair(
			@Nullable FIRST first, @Nullable SECOND second) {
		return new Pair<>(first, second, true);
	}

	@SuppressWarnings("boxing")
	public static Pair<Integer, Integer> pair(int first, int second) {
		return new Pair<>(first, second);
	}

	public static Pair<Integer, Integer>[] intChain(int from, int to) {
		@SuppressWarnings("unchecked")
		Pair<Integer, Integer>[] array = new Pair[to-from];

		for(int idx = 0; idx<array.length; idx++) {
			array[idx] = pair(from+idx, from+idx+1);
		}

		return array;
	}

	public static Pair<Long, Long>[] longChain(long from, long to) {
		long diff = to-from;
		assertTrue(diff<TestUtils.MAX_INTEGER_INDEX);

		@SuppressWarnings("unchecked")
		Pair<Long, Long>[] array = new Pair[(int)diff];

		for(int idx = 0; idx<array.length; idx++) {
			array[idx] = pair(from+idx, from+idx+1);
		}

		return array;
	}

	@SuppressWarnings("boxing")
	public static Pair<Long, Long> pair(long first, long second) {
		return new Pair<>(first, second);
	}

	public final FIRST first;
	public final SECOND second;

	public Pair(FIRST first, SECOND second) {
		this(first, second, false);
	}

	private Pair(FIRST first, SECOND second, boolean allowNull) {
		this.first = allowNull ? first : requireNonNull(first);
		this.second = allowNull ? second : requireNonNull(second);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return TestUtils.displayString("<%s,%s>", first, second);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof Pair) {
			Pair<?,?> other = (Pair<?, ?>) obj;
			return Objects.equals(first, other.first)
					&& Objects.equals(second, other.second);
		}
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(first, second);
	}
}