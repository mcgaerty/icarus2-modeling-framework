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
package de.ims.icarus2.model.standard.members.item;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import de.ims.icarus2.model.api.driver.ChunkInfo;
import de.ims.icarus2.model.api.driver.indices.IndexSet;
import de.ims.icarus2.model.api.layer.ItemLayer;
import de.ims.icarus2.model.api.layer.Layer;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.api.members.item.manager.ItemLayerManager;
import de.ims.icarus2.util.collections.LazyCollection;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

/**
 * @author Markus Gärtner
 *
 */
public class CompoundItemLayerManager implements ItemLayerManager {

	//FIXME

	private final Map<ItemLayer, ItemLayerManager> managerLookup;

	public CompoundItemLayerManager() {
		managerLookup = new Reference2ObjectOpenHashMap<>();
	}

	protected ItemLayerManager getManager(ItemLayer layer) {
		return managerLookup.get(layer);
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.manager.ItemLayerManager#getItemLayers()
	 */
	@Override
	public Collection<Layer> getItemLayers() {
		LazyCollection<Layer> result = LazyCollection.lazyList();

		managerLookup.keySet().forEach(result);

		return result.getAsList();
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.manager.ItemLayerManager#getItemCount(de.ims.icarus2.model.api.layer.ItemLayer)
	 */
	@Override
	public long getItemCount(ItemLayer layer) {
		return getManager(layer).getItemCount(layer);
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.manager.ItemLayerManager#getItem(de.ims.icarus2.model.api.layer.ItemLayer, long)
	 */
	@Override
	public Item getItem(ItemLayer layer, long index) {
		return getManager(layer).getItem(layer, index);
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.manager.ItemLayerManager#load(de.ims.icarus2.model.api.driver.indices.IndexSet[], de.ims.icarus2.model.api.layer.ItemLayer, java.util.function.Consumer)
	 */
	@Override
	public long load(IndexSet[] indices, ItemLayer layer,
			Consumer<ChunkInfo> action) throws InterruptedException {
		return getManager(layer).load(indices, layer, action);
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.manager.ItemLayerManager#release(de.ims.icarus2.model.api.driver.indices.IndexSet[], de.ims.icarus2.model.api.layer.ItemLayer)
	 */
	@Override
	public void release(IndexSet[] indices, ItemLayer layer)
			throws InterruptedException {
		getManager(layer).release(indices, layer);
	}

}