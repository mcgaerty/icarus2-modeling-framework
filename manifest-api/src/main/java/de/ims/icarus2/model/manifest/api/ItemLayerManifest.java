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
package de.ims.icarus2.model.manifest.api;

import java.util.List;
import java.util.function.Consumer;

import de.ims.icarus2.util.access.AccessControl;
import de.ims.icarus2.util.access.AccessMode;
import de.ims.icarus2.util.access.AccessPolicy;
import de.ims.icarus2.util.access.AccessRestriction;
import de.ims.icarus2.util.collections.LazyCollection;




/**
 * @author Markus Gärtner
 *
 */
@AccessControl(AccessPolicy.DENY)
public interface ItemLayerManifest extends LayerManifest {

	/**
	 * Returns the number of nested containers and/or structures within this
	 * layer.
	 * @return
	 */
	@AccessRestriction(AccessMode.READ)
	int getContainerDepth();

	boolean hasLocalContainers();

	/**
	 * Returns the manifest for the top-level container in this layer.
	 * Note that usually this will always be a manifest describing a list
	 * type container.
	 *
	 * @return
	 */
	@AccessRestriction(AccessMode.READ)
	ContainerManifest getRootContainerManifest();

	/**
	 * Returns the manifest for the container at depth {@code level}.
	 * For a {@code level} value of {@code 0} the result is equal to
	 * {@link #getRootContainerManifest()}.
	 *
	 * @param level the depth for which the manifest should be returned
	 * @return the manifest for the container at the given depth
     * @throws IndexOutOfBoundsException if the level is out of range
     *         (<tt>level &lt; 0 || level &gt;= getContainerDepth()</tt>)
	 */
	@AccessRestriction(AccessMode.READ)
	ContainerManifest getContainerManifest(int level);

	default void forEachContainerManifest(Consumer<? super ContainerManifest> action) {
		for(int i=0; i<getContainerDepth(); i++) {
			action.accept(getContainerManifest(i));
		}
	}

	default List<ContainerManifest> getContainerManifests() {
		LazyCollection<ContainerManifest> result = LazyCollection.lazyList();

		for(int i=0; i<getContainerDepth(); i++) {
			result.add(getContainerManifest(i));
		}

		return result.getAsList();
	}

	/**
	 * Looks up the level at which the given {@link ContainerManifest} is placed within
	 * this layer.
	 *
	 * @param containerManifest
	 * @return
	 */
	@AccessRestriction(AccessMode.READ)
	int indexOfContainerManifest(ContainerManifest containerManifest);

	/**
	 * Returns the {@code ItemLayerManifest} that describes the layer hosting
	 * <i>boundary containers</i> for the items in this manifests'
	 * {@code ItemLayer}. If the items are not restricted by <i>boundary containers</i>
	 * this method should return {@code null}.
	 * <p>
	 * Being restricted by a <i>boundary container</i> means that all non-virtual members of a
	 * container (or structure) must reside within the same range of indices defined by the boundary.
	 * So for example in the case of containers they are not allowed to span across borders of
	 * their respective <i>boundary container</i>.
	 *
	 * @return
	 */
	@AccessRestriction(AccessMode.READ)
	TargetLayerManifest getBoundaryLayerManifest();

	boolean isLocalBoundaryLayerManifest();

	/**
	 * Returns the
	 *
	 * @return
	 */
	@AccessRestriction(AccessMode.READ)
	TargetLayerManifest getFoundationLayerManifest();

	boolean isLocalFoundationLayerManifest();

	default boolean isPrimaryLayerManifest() {
		LayerGroupManifest groupManifest = getGroupManifest();
		return groupManifest!=null && groupManifest.getPrimaryLayerManifest()==this;
	}

	// Modification methods

	TargetLayerManifest setBoundaryLayerId(String boundaryLayerId);

	TargetLayerManifest setFoundationLayerId(String foundationLayerId);

	void removeContainerManifest(ContainerManifest containerManifest);

	/**
	 * Adds a new container manfiest for the specified level.
	 * If the level value is {@code -1} then the container manifest will be
	 * appended to the list of container manifests.
	 *
	 * @param containerManifest
	 * @param level
	 */
	void addContainerManifest(ContainerManifest containerManifest, int level);

	default void addContainerManifest(ContainerManifest containerManifest) {
		addContainerManifest(containerManifest, -1);
	}
}
