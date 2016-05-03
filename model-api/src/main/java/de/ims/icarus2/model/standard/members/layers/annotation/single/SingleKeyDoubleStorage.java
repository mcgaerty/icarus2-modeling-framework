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

 * $Revision: 457 $
 * $Date: 2016-04-20 15:08:11 +0200 (Mi, 20 Apr 2016) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus2Core/core/de.ims.icarus2.model/source/de/ims/icarus2/model/standard/members/layers/annotation/single/SingleKeyDoubleStorage.java $
 *
 * $LastChangedDate: 2016-04-20 15:08:11 +0200 (Mi, 20 Apr 2016) $
 * $LastChangedRevision: 457 $
 * $LastChangedBy: mcgaerty $
 */
package de.ims.icarus2.model.standard.members.layers.annotation.single;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ims.icarus2.model.api.layer.AnnotationLayer;
import de.ims.icarus2.model.api.manifest.AnnotationLayerManifest;
import de.ims.icarus2.model.api.manifest.AnnotationManifest;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.standard.util.CorpusUtils;

/**
 * @author Markus Gärtner
 * @version $Id: SingleKeyDoubleStorage.java 457 2016-04-20 13:08:11Z mcgaerty $
 *
 */
public class SingleKeyDoubleStorage extends AbstractSingleKeyStorage {

	private final static Logger log = LoggerFactory.getLogger(SingleKeyDoubleStorage.class);

	private TObjectDoubleMap<Item> annotations;
	private double noEntryValue = DEFAULT_NO_ENTRY_VALUE;

	public static final double DEFAULT_NO_ENTRY_VALUE = -1D;

	public SingleKeyDoubleStorage() {
		this(-1);
	}

	public SingleKeyDoubleStorage(int initialCapacity) {
		this(false, initialCapacity);
	}

	public SingleKeyDoubleStorage(boolean weakKeys, int initialCapacity) {
		super(weakKeys, initialCapacity);
	}

	protected TObjectDoubleMap<Item> buildBuffer(AnnotationLayer layer) {
		if(isWeakKeys()) {
			log.warn("Storage implementation does not support weak key references to stored items in layer {}", CorpusUtils.getUniqueId(layer));
		}

		return new TObjectDoubleHashMap<>(getInitialCapacity(layer), 0.75F, getNoEntryValue());
	}

	@Override
	public void addNotify(AnnotationLayer layer) {
		super.addNotify(layer);

		AnnotationLayerManifest manifest = layer.getManifest();
		AnnotationManifest annotationManifest = manifest.getAnnotationManifest(manifest.getDefaultKey());

		Object declaredNoEntryValue = annotationManifest.getNoEntryValue();

		noEntryValue = declaredNoEntryValue==null ? DEFAULT_NO_ENTRY_VALUE : (double) declaredNoEntryValue;
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
		return getFloatValue(item, key);
	}

	/**
	 * @see de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#setValue(de.ims.icarus2.model.api.members.item.Item, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setValue(Item item, String key, Object value) {
		setFloatValue(item, key, (float) value);
	}

	@Override
	public float getFloatValue(Item item, String key) {
		return (float) getDoubleValue(item, key);
	}

	@Override
	public int getIntegerValue(Item item, String key) {
		return (int) getDoubleValue(item, key);
	}

	@Override
	public long getLongValue(Item item, String key) {
		return (long) getDoubleValue(item, key);
	}

	@Override
	public double getDoubleValue(Item item, String key) {
		checkKey(key);

		return annotations.get(item);
	}

	@Override
	public void setDoubleValue(Item item, String key, double value) {
		checkKey(key);

		if(Double.compare(value, noEntryValue)==0) {
			annotations.remove(item);
		} else {
			annotations.put(item, value);
		}
	}

	@Override
	public void removeAllValues() {
		annotations.clear();
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
		return Double.compare(annotations.remove(item), noEntryValue)!=0;
	}

	public double getNoEntryValue() {
		return noEntryValue;
	}
}
