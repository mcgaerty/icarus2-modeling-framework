/*
 * ICARUS2 Corpus Modeling Framework
 * Copyright (C) 2014-2018 Markus Gärtner <markus.gaertner@ims.uni-stuttgart.de>
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
package de.ims.icarus2.filedriver.mapping;

import static de.ims.icarus2.util.Conditions.checkState;
import static java.util.Objects.requireNonNull;

import de.ims.icarus2.model.api.driver.Driver;
import de.ims.icarus2.model.api.driver.indices.IndexValueType;
import de.ims.icarus2.model.api.driver.mapping.Mapping;
import de.ims.icarus2.model.manifest.api.ItemLayerManifestBase;
import de.ims.icarus2.model.manifest.api.MappingManifest;
import de.ims.icarus2.util.AbstractBuilder;

/**
 * @author Markus Gärtner
 *
 */
public abstract class AbstractVirtualMapping implements Mapping {

	private final Driver driver;
	private final MappingManifest manifest;
	private final ItemLayerManifestBase<?> sourceLayer;
	private final ItemLayerManifestBase<?> targetLayer;

	protected AbstractVirtualMapping(Driver driver, MappingManifest manifest,
			ItemLayerManifestBase<?> sourceLayer, ItemLayerManifestBase<?> targetLayer) {
		requireNonNull(driver);
		requireNonNull(manifest);
		requireNonNull(sourceLayer);
		requireNonNull(targetLayer);

		this.driver = driver;
		this.manifest = manifest;
		this.sourceLayer = sourceLayer;
		this.targetLayer = targetLayer;
	}

	protected AbstractVirtualMapping(AbstractMappingBuilder<?, ?> builder) {
		requireNonNull(builder);

		driver = builder.getDriver();
		manifest = builder.getManifest();
		sourceLayer = builder.getSourceLayer();
		targetLayer = builder.getTargetLayer();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder()
		.append(getClass().getName()).append('[');
		sb.append("id=").append(manifest.getId());
		sb.append(" sourceLayer=").append(sourceLayer.getId());
		sb.append(" targetLayer=").append(targetLayer.getId());

		toString(sb);

		return sb.append(']').toString();
	}

	protected void toString(StringBuilder sb) {
		// for subclasses
	}

	/**
	 * @see de.ims.icarus2.model.api.driver.mapping.Mapping#getDriver()
	 */
	@Override
	public Driver getDriver() {
		return driver;
	}

	/**
	 * @see de.ims.icarus2.model.api.driver.mapping.Mapping#getSourceLayer()
	 */
	@Override
	public ItemLayerManifestBase<?> getSourceLayer() {
		return sourceLayer;
	}

	/**
	 * @see de.ims.icarus2.model.api.driver.mapping.Mapping#getTargetLayer()
	 */
	@Override
	public ItemLayerManifestBase<?> getTargetLayer() {
		return targetLayer;
	}

	/**
	 * @see de.ims.icarus2.model.api.driver.mapping.Mapping#getManifest()
	 */
	@Override
	public MappingManifest getManifest() {
		return manifest;
	}

	/**
	 * The default implementation does nothing.
	 *
	 * @see de.ims.icarus2.model.api.driver.mapping.Mapping#close()
	 */
	@Override
	public void close() {
		// no-op
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 * @param <B>
	 * @param <M>
	 */
	public static abstract class AbstractMappingBuilder<B extends AbstractMappingBuilder<B, M>, M extends Mapping> extends AbstractBuilder<B, M> {
		private Driver driver;
		private MappingManifest manifest;
		private ItemLayerManifestBase<?> sourceLayer, targetLayer;
		private IndexValueType valueType;

		protected AbstractMappingBuilder() {
			// no-op
		}

		public B driver(Driver driver) {
			requireNonNull(driver);
			checkState(this.driver==null);

			this.driver = driver;

			return thisAsCast();
		}

		public B manifest(MappingManifest manifest) {
			requireNonNull(manifest);
			checkState(this.manifest==null);

			this.manifest = manifest;

			return thisAsCast();
		}

		public B sourceLayer(ItemLayerManifestBase<?> sourceLayer) {
			requireNonNull(sourceLayer);
			checkState(this.sourceLayer==null);

			this.sourceLayer = sourceLayer;

			return thisAsCast();
		}

		public B targetLayer(ItemLayerManifestBase<?> targetLayer) {
			requireNonNull(targetLayer);
			checkState(this.targetLayer==null);

			this.targetLayer = targetLayer;

			return thisAsCast();
		}

		public B valueType(IndexValueType valueType) {
			requireNonNull(valueType);
			checkState(this.valueType==null);

			this.valueType = valueType;

			return thisAsCast();
		}

		public Driver getDriver() {
			return driver;
		}

		public MappingManifest getManifest() {
			return manifest;
		}

		public ItemLayerManifestBase<?> getSourceLayer() {
			return sourceLayer;
		}

		public ItemLayerManifestBase<?> getTargetLayer() {
			return targetLayer;
		}

		public IndexValueType getValueType() {
			return valueType;
		}

		public IndexBlockStorage getBlockStorage() {
			return IndexBlockStorage.forValueType(getValueType());
		}

		@Override
		protected void validate() {
			checkState("Missing driver", driver!=null);
			checkState("Missing manifest", manifest!=null);
			checkState("Missing target layer", targetLayer!=null);
			checkState("Missing source layer", sourceLayer!=null);
			checkState("Missing value type", valueType!=null);
		}
	}
}
