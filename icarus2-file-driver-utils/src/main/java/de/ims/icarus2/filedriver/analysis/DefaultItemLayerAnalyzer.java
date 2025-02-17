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
package de.ims.icarus2.filedriver.analysis;

import static de.ims.icarus2.util.IcarusUtils.UNSET_LONG;
import static java.util.Objects.requireNonNull;

import java.util.function.Function;

import de.ims.icarus2.filedriver.FileDataStates;
import de.ims.icarus2.filedriver.FileDataStates.ContainerInfo;
import de.ims.icarus2.filedriver.FileDataStates.FileInfo;
import de.ims.icarus2.filedriver.FileDataStates.LayerInfo;
import de.ims.icarus2.model.api.layer.ItemLayer;
import de.ims.icarus2.model.api.members.container.Container;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.manifest.api.ContainerManifestBase;
import de.ims.icarus2.model.manifest.api.ContainerType;
import de.ims.icarus2.model.manifest.api.Hierarchy;
import de.ims.icarus2.model.manifest.api.ItemLayerManifestBase;
import de.ims.icarus2.model.util.ModelUtils;
import de.ims.icarus2.util.IcarusUtils;
import de.ims.icarus2.util.LongCounter;
import de.ims.icarus2.util.stat.Histogram;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * @author Markus Gärtner
 *
 */
public class DefaultItemLayerAnalyzer extends AbstractFileDriverAnalyzer implements ItemLayerAnalyzer {

	/**
	 * Layer this analyzer is collecting information for
	 */
	private final ItemLayer layer;

	/**
	 * Index of file (if multiple) the data is being extracted from.
	 * This is relevant for writing back coverage information into
	 * the matching {@link FileInfo} object.
	 */
	protected final int fileIndex;

	/**
	 * Total number of elements encountered for given layer during
	 * analysis. Does not take into account numbers from other files.
	 */
	private long elementCount = UNSET_LONG;
	private long firstIndex = UNSET_LONG, lastIndex = UNSET_LONG;

	private final Function<Container, ContainerStats> statsLookup;
	private final ContainerStats[] stats;

	protected static class ContainerStats {

		private final ContainerManifestBase<?> manifest;
		private final int level;

		/**
		 * Total number of containers encountered for given layer during
		 * analysis. Does not take into account numbers from other files.
		 *
		 * Summed up {@link Container#getItemCount() size} of all containers
		 * in the given layer during analysis.
		 *
		 * Smallest {@link Container#getItemCount() size} of a container
		 * encountered during analysis.
		 *
		 * Largest {@link Container#getItemCount() size} of a container
		 * encountered during analysis.
		 */
		private final Histogram containerSizes = Histogram.openHistogram(100);
		private final Histogram spanSizes = Histogram.openHistogram(100);
		/**
		 * Individual counts for every {@link ContainerType type} of
		 * container encountered during analysis.
		 */
		private LongCounter<ContainerType> containerTypeCount = new LongCounter<>();

		protected ContainerStats(ContainerManifestBase<?> manifest, int level) {
			this.manifest = requireNonNull(manifest);
			this.level = level;
		}

	}

	public DefaultItemLayerAnalyzer(FileDataStates states, ItemLayer layer, int fileIndex) {
		super(states);

		this.layer = requireNonNull(layer);
		this.fileIndex = fileIndex;

		Hierarchy<ContainerManifestBase<?>> hierarchy = layer.getManifest().getContainerHierarchy().orElse(null);
		if(hierarchy==null || hierarchy.isEmpty()) {
			statsLookup = null;
			stats = new ContainerStats[0];
		} else if(hierarchy.getDepth()==1) {
			final ContainerStats rootStats = createStats(hierarchy.getRoot(), 0);
			statsLookup = c -> rootStats;
			stats = new ContainerStats[] {rootStats};
		} else {
			stats = new ContainerStats[hierarchy.getDepth()];
			Int2ObjectMap<ContainerStats> map = new Int2ObjectOpenHashMap<>();
			hierarchy.forEachItem((c, level) -> {
				ContainerStats s = createStats(c, level);
				map.put(c.getUID(), s);
				stats[level] = s;
			});
			statsLookup = c -> requireNonNull(map.get(c.getManifest().getUID()), "missing container stats");
		}
	}

	protected ContainerStats createStats(ContainerManifestBase<?> manifest, int level) {
		return new ContainerStats(manifest, level);
	}

	protected ItemLayer getLayer() {
		return layer;
	}

	/**
	 * @see de.ims.icarus2.filedriver.analysis.AbstractFileDriverAnalyzer#writeStates(de.ims.icarus2.filedriver.FileDataStates)
	 */
	@Override
	protected void writeStates(FileDataStates states) {
		ItemLayerManifestBase<?> layerManifest = layer.getManifest();

		// Refresh file info
		FileInfo fileInfo = states.getFileInfo(fileIndex);
//		long beginIndex = fileInfo.getBeginIndex(layerManifest);
//		if(beginIndex==IcarusUtils.UNSET_LONG) {
//			if(fileIndex==0) {
//				beginIndex = 0L;
//			} else {
//				FileInfo previousInfo = states.getFileInfo(fileIndex-1);
//				long previousEndIndex = previousInfo.getEndIndex(layerManifest);
//				if(previousEndIndex==IcarusUtils.UNSET_LONG)
//					throw new ManifestException(ModelErrorCode.DRIVER_METADATA_CORRUPTED,
//							String.format("Missing information of end index for layer %s in file %d",
//									ManifestUtils.getName(layerManifest), _int(fileIndex)));
//
//				beginIndex = previousEndIndex+1;
//			}
//		}
//		long endIndex = beginIndex + elementCount - 1;

		// Refresh layer info
		LayerInfo layerInfo = states.getLayerInfo(layer.getManifest());
		layerInfo.setSize(layerInfo.getSize() + elementCount);

		fileInfo.setCoverage(layerManifest, elementCount, firstIndex, lastIndex);

		for (int i = 0; i < stats.length; i++) {
			ContainerStats cs = stats[i];
			ContainerInfo ci = layerInfo.getContainerInfo(cs.manifest);
			writeContainerStats(cs, ci);
		}
	}

	protected void writeContainerStats(ContainerStats stats, ContainerInfo info) {
		info.getItemCountStats().copyFrom(stats.containerSizes);
		info.getSpanSizeStats().copyFrom(stats.spanSizes);

		if(!stats.containerTypeCount.isEmpty()) {
			info.addCountsForContainerTypes(stats.containerTypeCount);
		}
	}

	/**
	 * @see java.util.function.ObjLongConsumer#accept(java.lang.Object, long)
	 */
	@Override
	public void accept(Item item, long index) {
		if(elementCount==UNSET_LONG) {
			elementCount = 1;
		} else {
			elementCount++;
		}

		if(firstIndex==UNSET_LONG) {
			firstIndex = index;
		}
		assert index>=firstIndex;
		lastIndex = index;

//		System.out.printf("item=%s index=%d first=%d, last=%d, count=%d%n", item, _long(index), _long(firstIndex), _long(lastIndex), _long(elementCount));

		if(stats.length>0 && ModelUtils.isContainerOrStructure(item)) {
			Container container = (Container) item;
			collectStats(container, statsLookup.apply(container));
		}
	}

	protected void collectStats(Container container, ContainerStats stats) {
		stats.containerSizes.accept(container.getItemCount());
//		System.out.printf("entry %d, size=%d span=%d%n",_long(elementCount),_long(container.getItemCount()),_long(container.getSpan()));

		long spanSize = container.getSpan();
		if(spanSize!=IcarusUtils.UNSET_LONG) {
			stats.spanSizes.accept(spanSize);
		}

		stats.containerTypeCount.increment(container.getContainerType());
	}
}
