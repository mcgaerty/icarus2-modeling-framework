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
package de.ims.icarus2.model.standard.members.layers.annotation.single;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ims.icarus2.apiguard.Unguarded;
import de.ims.icarus2.model.api.layer.AnnotationLayer;
import de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.manifest.api.AnnotationLayerManifest;
import de.ims.icarus2.model.manifest.api.AnnotationManifest;
import de.ims.icarus2.model.util.ModelUtils;
import de.ims.icarus2.util.IcarusUtils;
import de.ims.icarus2.util.annotations.TestableImplementation;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * @author Markus Gärtner
 *
 */
@TestableImplementation(AnnotationStorage.class)
public class SingleKeyIntegerStorage extends AbstractSingleKeyStorage {

	private final static Logger log = LoggerFactory.getLogger(SingleKeyIntegerStorage.class);

	private Object2IntMap<Item> annotations;
	private int noEntryValue = DEFAULT_NO_ENTRY_VALUE;

	public static final int DEFAULT_NO_ENTRY_VALUE = IcarusUtils.UNSET_INT;

	public SingleKeyIntegerStorage() {
		this(-1);
	}

	public SingleKeyIntegerStorage(int initialCapacity) {
		this(false, initialCapacity);
	}

	public SingleKeyIntegerStorage(boolean weakKeys, int initialCapacity) {
		super(weakKeys, initialCapacity);
	}

	protected Object2IntMap<Item> buildBuffer(AnnotationLayer layer) {
		if(isWeakKeys()) {
			log.warn("Storage implementation does not support weak key references to stored items in layer {}", ModelUtils.getUniqueId(layer));
		}

		Object2IntMap<Item> result = new Object2IntOpenHashMap<>(getInitialCapacity(layer));
		result.defaultReturnValue(getNoEntryValue());

		return result;
	}

	@SuppressWarnings("boxing")
	@Override
	public void addNotify(AnnotationLayer layer) {
		super.addNotify(layer);

		AnnotationLayerManifest manifest = layer.getManifest();
		String key = requireDefaultKey(manifest);
		AnnotationManifest annotationManifest = requireAnnotationsManifest(manifest, key);

		noEntryValue = annotationManifest.getNoEntryValue()
				.map(Integer.class::cast)
				.orElse(DEFAULT_NO_ENTRY_VALUE);
		annotations = buildBuffer(layer);
	}

	@Override
	public void removeNotify(AnnotationLayer layer) {
		super.removeNotify(layer);

		noEntryValue = DEFAULT_NO_ENTRY_VALUE;
		annotations = null;
	}

	/**
	 * @see de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#getValue(de.ims.icarus2.model.api.members.item.Item, java.lang.String)
	 */
	@Override
	public Object getValue(Item item, String key) {
		return Integer.valueOf(getInteger(item, key));
	}

	/**
	 * @see de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#setValue(de.ims.icarus2.model.api.members.item.Item, java.lang.String, java.lang.Object)
	 */
	@Unguarded(Unguarded.DELEGATE)
	@Override
	public void setValue(Item item, String key, Object value) {
		setInteger(item, key, ((Number) value).intValue());
	}

	@Override
	public int getInteger(Item item, String key) {
		checkKey(key);

		return annotations.getInt(item);
	}

	@Override
	public float getFloat(Item item, String key) {
		return getInteger(item, key);
	}

	@Override
	public double getDouble(Item item, String key) {
		return getInteger(item, key);
	}

	@Override
	public long getLong(Item item, String key) {
		return getInteger(item, key);
	}

	@Override
	public void setInteger(Item item, String key, int value) {
		checkKey(key);

		if(value==noEntryValue) {
			annotations.removeInt(item);
		} else {
			annotations.put(item, value);
		}
	}

	@Override
	public void setLong(Item item, String key, long value) {
		setInteger(item, key, IcarusUtils.ensureIntegerValueRange(value));
	}

	/**
	 * @see de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#removeAllValues(java.util.function.Supplier)
	 */
	@Override
	public void removeAllValues(Supplier<? extends Item> source) {
		Item item;
		while((item=source.get())!=null) {
			annotations.removeInt(item);
		}
	}

	@Override
	public boolean hasAnnotations() {
		return !annotations.isEmpty();
	}

	@Override
	public boolean hasAnnotations(Item item) {
		return annotations.containsKey(item);
	}

	@Override
	public boolean removeItem(Item item) {
		if(annotations.containsKey(item)) {
			annotations.removeInt(item);
			return true;
		}

		return false;
	}

	@Override
	public boolean addItem(Item item) {
		if(!annotations.containsKey(item)) {
			annotations.put(item, noEntryValue);
			return true;
		}

		return false;
	}

	public int getNoEntryValue() {
		return noEntryValue;
	}
}
