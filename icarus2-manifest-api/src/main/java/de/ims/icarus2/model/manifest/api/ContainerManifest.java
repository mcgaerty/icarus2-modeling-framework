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
package de.ims.icarus2.model.manifest.api;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import de.ims.icarus2.util.access.AccessControl;
import de.ims.icarus2.util.access.AccessMode;
import de.ims.icarus2.util.access.AccessPolicy;
import de.ims.icarus2.util.access.AccessRestriction;

/**
 * A manifest that describes a container and its content.
 *
 * @author Markus Gärtner
 *
 */
@AccessControl(AccessPolicy.DENY)
public interface ContainerManifest extends MemberManifest {

	/**
	 * Returns the manifest of the {@code ItemLayer} the container
	 * is hosted in.
	 * @return
	 */
	@AccessRestriction(AccessMode.READ)
	ItemLayerManifest getLayerManifest();

	@Override
	default ManifestFragment getHost() {
		return getLayerManifest();
	}

	/**
	 * Returns the type of this container. This provides
	 * information about how contained {@code Item}s are ordered and
	 * if they represent a continuous subset of the corpus. Note that the
	 * container type of a {@link Structure} object is undefined and therefore
	 * this method is optional when this container is a structure.
	 *
	 * @return The {@code ContainerType} of this {@code Container}
	 * @see ContainerType
	 */
	@AccessRestriction(AccessMode.READ)
	ContainerType getContainerType();

	boolean isLocalContainerType();

	/**
	 * Checks whether the given {@code flag} is set for this manifest.
	 *
	 * @return
	 */
	@AccessRestriction(AccessMode.READ)
	boolean isContainerFlagSet(ContainerFlag flag);

	@AccessRestriction(AccessMode.READ)
	void forEachActiveContainerFlag(Consumer<? super ContainerFlag> action);

	@AccessRestriction(AccessMode.READ)
	void forEachActiveLocalContainerFlag(Consumer<? super ContainerFlag> action);

	default Set<ContainerFlag> getActiveContainerFlags() {
		EnumSet<ContainerFlag> result = EnumSet.noneOf(ContainerFlag.class);

		forEachActiveContainerFlag(result::add);

		return result;
	}

	default Set<ContainerFlag> getActiveLocalContainerFlags() {
		EnumSet<ContainerFlag> result = EnumSet.noneOf(ContainerFlag.class);

		forEachActiveLocalContainerFlag(result::add);

		return result;
	}

	@AccessRestriction(AccessMode.READ)
	default ContainerManifest getParentManifest() {
		ItemLayerManifest hostManifest = getLayerManifest();
		int index = hostManifest==null ? -1 : hostManifest.indexOfContainerManifest(this);

		if(index<=0) {
			return null;
		} else {
			return hostManifest.getContainerManifest(index-1);
		}
	}

	@AccessRestriction(AccessMode.READ)
	default ContainerManifest getElementManifest() {
		ItemLayerManifest hostManifest = getLayerManifest();
		if(hostManifest==null) {
			return null;
		}

		int index = hostManifest.indexOfContainerManifest(this);

		if(index>=hostManifest.getContainerDepth()-1) {
			return null;
		} else {
			return hostManifest.getContainerManifest(index+1);
		}
	}

	// Modification methods

	void setContainerType(ContainerType containerType);

	void setContainerFlag(ContainerFlag flag, boolean active);
}
