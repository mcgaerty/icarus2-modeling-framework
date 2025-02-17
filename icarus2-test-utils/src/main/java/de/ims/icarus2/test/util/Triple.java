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

import java.util.Objects;

import de.ims.icarus2.test.TestUtils;

/**
 * @author Markus Gärtner
 *
 */
public final class Triple<FIRST, SECOND, THIRD> {

	public static <FIRST, SECOND, THIRD> Triple<FIRST, SECOND, THIRD> triple(FIRST first, SECOND second, THIRD third) {
		return new Triple<>(first, second, third, false);
	}

	@SuppressWarnings("boxing")
	public static Triple<Integer, Integer, Integer> triple(int first, int second, int third) {
		return new Triple<>(first, second, third, false);
	}

	public static <FIRST, SECOND, THIRD> Triple<FIRST, SECOND, THIRD> nullableTriple(FIRST first, SECOND second, THIRD third) {
		return new Triple<>(first, second, third, true);
	}

	public final FIRST first;
	public final SECOND second;
	public final THIRD third;

	/**
	 * @param first
	 * @param second
	 * @param third
	 */
	public Triple(FIRST first, SECOND second, THIRD third) {
		this(first, second, third, false);
	}

	private Triple(FIRST first, SECOND second, THIRD third, boolean allowNull) {
		this.first = allowNull ? first : requireNonNull(first);
		this.second = allowNull ? second : requireNonNull(second);
		this.third = allowNull ? third : requireNonNull(third);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return TestUtils.displayString("<%s,%s,%s>", first, second, third);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(first, second, third);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof Triple) {
			@SuppressWarnings("rawtypes")
			Triple other = (Triple) obj;
			return Objects.equals(first, other.first)
					&& Objects.equals(second, other.second)
					&& Objects.equals(third, other.third);
		}
		return false;
	}
}
