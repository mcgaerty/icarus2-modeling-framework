/*
 *  ICARUS 2 -  Interactive platform for Corpus Analysis and Research tools, University of Stuttgart
 *  Copyright (C) 2015 Markus Gärtner
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

 * $Revision: 382 $
 * $Date: 2015-04-09 16:23:50 +0200 (Do, 09 Apr 2015) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus2Core/core/de.ims.icarus2.model/source/de/ims/icarus2/model/standard/members/layers/annotation/fixed/FixedKeysBoolean15BitStorage.java $
 *
 * $LastChangedDate: 2015-04-09 16:23:50 +0200 (Do, 09 Apr 2015) $
 * $LastChangedRevision: 382 $
 * $LastChangedBy: mcgaerty $
 */
package de.ims.icarus2.model.standard.members.layers.annotation.fixed;

import gnu.trove.function.TShortFunction;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TObjectShortHashMap;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ims.icarus2.model.api.ModelErrorCode;
import de.ims.icarus2.model.api.ModelException;
import de.ims.icarus2.model.api.layer.AnnotationLayer;
import de.ims.icarus2.model.api.manifest.AnnotationLayerManifest;
import de.ims.icarus2.model.api.manifest.AnnotationManifest;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.standard.util.CorpusUtils;

/**
 * @author Markus Gärtner
 * @version $Id: FixedKeysBoolean15BitStorage.java 382 2015-04-09 14:23:50Z mcgaerty $
 *
 */
public class FixedKeysBoolean15BitStorage extends AbstractFixedKeysBooleanStorage {
	
	private static final Logger log = LoggerFactory
			.getLogger(FixedKeysBoolean15BitStorage.class);

	public static final int MAX_KEY_COUNT = 15;

	private TObjectShortMap<Item> annotations;
	private short noEntryValues;

	private static final short EMPTY_BUFFER = (short) (0x1<<15);

	public FixedKeysBoolean15BitStorage() {
		this(-1);
	}

	public FixedKeysBoolean15BitStorage(int initialCapacity) {
		this(false, initialCapacity);
	}

	public FixedKeysBoolean15BitStorage(boolean weakKeys, int initialCapacity) {
		super(weakKeys, initialCapacity);
	}

	protected TObjectShortMap<Item> createMap(AnnotationLayer layer) {
		if(isWeakKeys()) {
			log.warn("Storage implementation does not support weak key references to stored items.");
		}

		return new TObjectShortHashMap<>(getInitialCapacity(layer), 0.75F, EMPTY_BUFFER);
	}

	@Override
	public void addNotify(AnnotationLayer layer) {
		super.addNotify(layer);

		IndexLookup indexLookup = getIndexLookup();
		if(indexLookup.keyCount()>MAX_KEY_COUNT)
			throw new ModelException(ModelErrorCode.INVALID_INPUT,
					"Short integer buffer only holds 15 value bits - cannot represent annotations for layer: "+CorpusUtils.getName(layer));

		AnnotationLayerManifest manifest = layer.getManifest();

		int noEntryValues = 0;

		for(int i=0; i<indexLookup.keyCount(); i++) {
			AnnotationManifest annotationManifest = manifest.getAnnotationManifest(indexLookup.keyAt(i));

			Object declaredNoEntryValue = annotationManifest.getNoEntryValue();

			if(declaredNoEntryValue==null || !(boolean)declaredNoEntryValue) {
				continue;
			}

			noEntryValues |= (1<<i);
		}

		this.noEntryValues = (short)noEntryValues;

		annotations = createMap(layer);
	}

	@Override
	public void removeNotify(AnnotationLayer layer) {
		super.removeNotify(layer);

		noEntryValues = 0x0;
		annotations = null;
	}

	@Override
	public boolean collectKeys(Item item, Consumer<String> action) {
		short data = annotations.get(item);

		if(data==EMPTY_BUFFER || data==noEntryValues) {
			return false;
		}

		IndexLookup indexLookup = getIndexLookup();

		for(int i=0; i<indexLookup.keyCount(); i++) {
			int mask = (1<<i);
			if((data & mask) != (noEntryValues & mask)) {
				action.accept(indexLookup.keyAt(i));
			}
		}

		return true;
	}

	@Override
	public boolean getBooleanValue(Item item, String key) {
		int index = checkKeyAndGetIndex(key);
		short b = annotations.get(item);

		if(b==EMPTY_BUFFER) {
			b = noEntryValues;
		}

		return (b & (1<<index))!=0x0;
	}

	@Override
	public void setBooleanValue(Item item, String key, boolean value) {
		int index = checkKeyAndGetIndex(key);
		short b = annotations.get(item);

		if(value) {
			b |= (1<<index);
		} else {
			b &= (EMPTY_BUFFER | ~(1<<index));
		}

		annotations.put(item, b);
	}

	@Override
	public void removeAllValues() {
		annotations.clear();
	}

	@Override
	public void removeAllValues(String key) {
		int index = checkKeyAndGetIndex(key);

		final short mask = (short) (EMPTY_BUFFER | ~(1<<index));

		annotations.transformValues(new TShortFunction() {

			@Override
			public short execute(short value) {
				return (short) (mask & value);
			}
		});
	}

	@Override
	public boolean hasAnnotations() {
		return !annotations.isEmpty();
	}

	@Override
	public boolean hasAnnotations(Item item) {
		return annotations.get(item)!=EMPTY_BUFFER;
	}

	@Override
	public boolean containsItem(Item item) {
		return annotations.containsKey(item);
	}

	@Override
	public boolean addItem(Item item) {
		short b = annotations.get(item);

		if(b!=EMPTY_BUFFER) {
			annotations.put(item, EMPTY_BUFFER);
			return true;
		}

		return false;
	}

	@Override
	public boolean removeItem(Item item) {
		return annotations.remove(item)!=EMPTY_BUFFER;
	}

}
