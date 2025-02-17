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
package de.ims.icarus2.model.api.members.structure;

import de.ims.icarus2.model.api.members.container.ContainerTest;
import de.ims.icarus2.test.guard.ApiGuard;

/**
 * @author Markus Gärtner
 *
 */
public interface StructureTest<S extends Structure> extends ContainerTest<S> {

	/**
	 * @see de.ims.icarus2.model.api.members.container.ContainerTest#configureApiGuard(de.ims.icarus2.test.guard.ApiGuard)
	 */
	@Override
	default void configureApiGuard(ApiGuard<S> apiGuard) {
		ContainerTest.super.configureApiGuard(apiGuard);

		apiGuard.defaultReturnValue("augmented",
				Boolean.valueOf(Structure.DEFAULT_AUGMENTED));
		apiGuard.defaultReturnValue("edgesComplete",
				Boolean.valueOf(Structure.DEFAULT_EDGES_COMPLETE));
	}
}
