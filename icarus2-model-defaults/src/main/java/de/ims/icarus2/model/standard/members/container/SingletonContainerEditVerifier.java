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
package de.ims.icarus2.model.standard.members.container;

import de.ims.icarus2.model.api.members.container.Container;

/**
 *
 * @author Markus Gärtner
 *
 */
public class SingletonContainerEditVerifier extends DefaultContainerEditVerifier {

	/**
	 * @param source
	 */
	public SingletonContainerEditVerifier(Container source) {
		super(source);
	}

	/**
	 * @see de.ims.icarus2.model.standard.members.container.DefaultContainerEditVerifier#isValidAddItemIndex(long)
	 */
	@Override
	protected boolean isValidAddItemIndex(long index) {
		return index==0L && getSource().getItemCount()==0L;
	}

	/**
	 * @see de.ims.icarus2.model.standard.members.container.DefaultContainerEditVerifier#isValidRemoveItemIndex(long)
	 */
	@Override
	protected boolean isValidRemoveItemIndex(long index) {
		return index==0L && getSource().getItemCount()==1L;
	}

	/**
	 * @see de.ims.icarus2.model.standard.members.container.DefaultContainerEditVerifier#canSwapItems(long, long)
	 */
	@Override
	public boolean canSwapItems(long index0, long index1) {
		return false;
	}
}