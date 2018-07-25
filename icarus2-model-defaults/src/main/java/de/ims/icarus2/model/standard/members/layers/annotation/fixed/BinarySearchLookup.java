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
package de.ims.icarus2.model.standard.members.layers.annotation.fixed;

import java.util.Arrays;

/**
 *
 * @author Markus Gärtner
 *
 */
public class BinarySearchLookup extends IndexLookup {

	/**
	 * @param keys
	 */
	public BinarySearchLookup(String[] keys) {
		super(keys);
	}

	/**
	 * @see de.ims.icarus2.model.standard.members.layers.annotation.fixed.IndexLookup#indexOf(java.lang.String)
	 */
	@Override
	public int indexOf(String key) {
		return Math.max(-1, Arrays.binarySearch(keys, key));
	}
}