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
package de.ims.icarus2.filedriver.analysis;

import static de.ims.icarus2.util.lang.Primitives._int;
import static de.ims.icarus2.util.strings.StringUtil.getName;
import static java.util.Objects.requireNonNull;

import java.util.function.LongConsumer;

import de.ims.icarus2.filedriver.FileDataStates;
import de.ims.icarus2.filedriver.FileDataStates.FileInfo;
import de.ims.icarus2.filedriver.FileDataStates.LayerInfo;
import de.ims.icarus2.filedriver.FileDataStates.NumericalStats;
import de.ims.icarus2.model.api.ModelErrorCode;
import de.ims.icarus2.model.api.layer.ItemLayer;
import de.ims.icarus2.model.api.members.container.Container;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.manifest.api.ContainerType;
import de.ims.icarus2.model.manifest.api.ItemLayerManifest;
import de.ims.icarus2.model.manifest.api.ManifestException;
import de.ims.icarus2.model.util.ModelUtils;
import de.ims.icarus2.util.IcarusUtils;
import de.ims.icarus2.util.LongCounter;

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
	private long elementCount = 0L;

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
	private final StatsBuffer containerSizes = new StatsBuffer();

	private final StatsBuffer spanSizes = new StatsBuffer();

	/**
	 * Individual counts for every {@link ContainerType type} of
	 * container encountered during analysis.
	 */
	private LongCounter<ContainerType> containerTypeCount = new LongCounter<>();

	public DefaultItemLayerAnalyzer(FileDataStates states, ItemLayer layer, int fileIndex) {
		super(states);

		this.layer = requireNonNull(layer);
		this.fileIndex = fileIndex;
	}

	protected ItemLayer getLayer() {
		return layer;
	}

	/**
	 * @see de.ims.icarus2.filedriver.analysis.AbstractFileDriverAnalyzer#writeStates(de.ims.icarus2.filedriver.FileDataStates)
	 */
	@Override
	protected void writeStates(FileDataStates states) {
		ItemLayerManifest layerManifest = layer.getManifest();

		// Refresh file info
		FileInfo fileInfo = states.getFileInfo(fileIndex);
		long beginIndex = fileInfo.getBeginIndex(layerManifest);
		if(beginIndex==IcarusUtils.UNSET_LONG) {
			if(fileIndex==0) {
				beginIndex = 0L;
			} else {
				FileInfo previousInfo = states.getFileInfo(fileIndex-1);
				long previousEndIndex = previousInfo.getEndIndex(layerManifest);
				if(previousEndIndex==IcarusUtils.UNSET_LONG)
					throw new ManifestException(ModelErrorCode.DRIVER_METADATA_CORRUPTED,
							String.format("Missing information of end index for layer %s in file %d",
									getName(layerManifest), _int(fileIndex)));

				beginIndex = previousEndIndex+1;
			}
		}
		long endIndex = beginIndex + elementCount;

		fileInfo.setCoverage(layerManifest, elementCount, beginIndex, endIndex);

		// Refresh layer info
		LayerInfo layerInfo = states.getLayerInfo(layer.getManifest());
		layerInfo.setSize(layerInfo.getSize() + elementCount);

		containerSizes.saveTo(layerInfo.getItemCountStats());

		if(!containerTypeCount.isEmpty()) {
			layerInfo.addCountsForContainerTypes(containerTypeCount);
		}

		spanSizes.saveTo(layerInfo.getSpanSizeStats());
	}

	/**
	 * @see java.util.function.ObjLongConsumer#accept(java.lang.Object, long)
	 */
	@Override
	public void accept(Item item, long index) {
		elementCount++;

		if(ModelUtils.isContainerOrStructure(item)) {
			Container container = (Container) item;
			containerSizes.accept(container.getItemCount());

			long spanSize = container.getSpan();
			if(spanSize!=IcarusUtils.UNSET_LONG) {
				spanSizes.accept(spanSize);
			}

			containerTypeCount.increment(container.getContainerType());
		}
	}

	public static class StatsBuffer implements LongConsumer {
		private long sum = 0L;
		private long count = 0L;
		private long min = Long.MAX_VALUE;
		private long max = Long.MIN_VALUE;

		/**
		 * @see java.util.function.LongConsumer#accept(long)
		 */
		@Override
		public void accept(long value) {
			if(value<0) { // Takes care of UNSET_LONG and any other invalid values
				return;
			}

			count++;
			sum += value;
			min = Math.min(min, value);
			max = Math.max(max, value);
		}

		public long getCount() {
			return count;
		}

		public boolean isEmpty() {
			return count==0L;
		}

		public void saveTo(NumericalStats target) {
			if(isEmpty()) {
				target.reset();
			} else {
				target.setMin(min);
				target.setMax(max);
				target.setAvg(sum/(double)count);
			}
		}
	}
}