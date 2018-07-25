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
package de.ims.icarus2.filedriver.io.sets;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.ObjIntConsumer;

import de.ims.icarus2.model.api.io.resources.IOResource;

/**
 * Stores a collection of (file) resources together with their respective checksums.
 *
 * @author Markus Gärtner
 *
 */
public interface ResourceSet {

	/**
	 * Returns the number of resources this storage covers (at least {@code 1}).
	 */
	int getResourceCount();

	/**
	 * Returns the abstract location of the resource specified by the
	 * {@code resourceIndex} argument.
	 *
	 * @param resourceIndex
	 * @return
	 */
	IOResource getResourceAt(int resourceIndex);

	default int indexOfFile(IOResource file) {
		int resourceCount = getResourceCount();

		for(int i=0; i<resourceCount; i++) {
			if(getResourceAt(i).equals(file)) {
				return i;
			}
		}

		return -1;
	}

	default void forEachFile(IntConsumer action) {
		int resourceCount = getResourceCount();

		for(int i=0; i<resourceCount; i++) {
			action.accept(i);
		}
	}

	default void forEachFile(ObjIntConsumer<IOResource> action) {
		int resourceCount = getResourceCount();

		for(int i=0; i<resourceCount; i++) {
			action.accept(getResourceAt(i), i);
		}
	}

	default void forEachFile(Consumer<IOResource> action) {
		int resourceCount = getResourceCount();

		for(int i=0; i<resourceCount; i++) {
			action.accept(getResourceAt(i));
		}
	}
}