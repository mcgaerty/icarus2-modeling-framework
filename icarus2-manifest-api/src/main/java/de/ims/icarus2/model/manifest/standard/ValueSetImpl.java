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
package de.ims.icarus2.model.manifest.standard;

import static de.ims.icarus2.util.Conditions.checkIndex;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import de.ims.icarus2.model.manifest.api.ValueSet;
import de.ims.icarus2.model.manifest.types.ValueType;
import de.ims.icarus2.util.collections.CollectionUtils;

/**
 * @author Markus Gärtner
 *
 */
public class ValueSetImpl extends AbstractLockable implements ValueSet {

	private final ValueType valueType;
	private final List<Object> values = new ArrayList<>();

	public ValueSetImpl(ValueType valueType) {
		this.valueType = requireNonNull(valueType);
	}

	public ValueSetImpl addAll(Collection<?> items) {
		requireNonNull(items);

		valueType.checkValues(items);
		values.addAll(items);

		return this;
	}

	public ValueSetImpl addAll(Object...items) {
		requireNonNull(items);

		valueType.checkValues(items);
		CollectionUtils.feedItems(values, items);

		return this;
	}

	public ValueSetImpl addAll(Stream<?> items) {
		requireNonNull(items);

		items.forEach(this::addValue);

		return this;
	}

	public ValueSetImpl addAll(Class<?> enumClass) {
		requireNonNull(enumClass);

		if(!ValueType.ENUM.equals(valueType))
			throw new IllegalArgumentException("Cannot use the enum based constructor for other value types than "+ValueType.ENUM); //$NON-NLS-1$

		CollectionUtils.feedItems(values, (Object[]) enumClass.getEnumConstants());

		return this;
	}


	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hash = valueType.hashCode() * (1+values.size());

		for(int i=0; i<values.size(); i++) {
			hash *= (1+values.get(i).hashCode());
		}

		return hash;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		} if(obj instanceof ValueSet) {
			ValueSet other = (ValueSet) obj;

			if(!valueType.equals(other.getValueType())) {
				return false;
			}

			if(values.size()!=other.valueCount()) {
				return false;
			}

			for(int i=0; i<values.size(); i++) {
				if(!values.get(i).equals(other.getValueAt(i))) {
					return false;
				}
			}

			return true;
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ValueSet@"+valueType.getStringValue()+"["+values.size()+" items]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * @return the valueType
	 */
	@Override
	public ValueType getValueType() {
		return valueType;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.ValueSet#getValues()
	 */
	@Override
	public Object[] getValues() {
		return values.toArray();
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.ValueSet#valueCount()
	 */
	@Override
	public int valueCount() {
		return values.size();
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.ValueSet#getValueAt(int)
	 */
	@Override
	public Object getValueAt(int index) {
		return values.get(index);
	}

	@Override
	public void forEach(Consumer<? super Object> action) {
		values.forEach(action);
	}

	/**
	 * @see de.ims.icarus2.util.Searchable#forEachUntil(java.util.function.Predicate)
	 */
	@Override
	public void forEachUntil(Predicate<? super Object> check) {
		values.stream().sequential().anyMatch(check);
	}

	@Override
	public ValueSet addValue(Object value) {
		checkNotLocked();
		requireNonNull(value);

		addValue0(value, -1);

		return this;
	}

	@Override
	public ValueSet addValue(Object value, int index) {
		checkNotLocked();
		requireNonNull(value);
		checkIndex(index, 0, values.size()-1);

		addValue0(value, index);

		return this;
	}

	protected void addValue0(Object value, int index) {
		if(index==-1) {
			index = values.size();
		}

		valueType.checkValue(value);

		values.add(index, value);
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.ValueSet#removeValue(java.lang.Object)
	 */
	@Override
	public ValueSet removeValue(Object value) {
		checkNotLocked();
		ValueSet.super.removeValue(value);

		return this;
	}

	@Override
	public ValueSet removeValueAt(int index) {
		checkNotLocked();

		removeValueAt0(index);

		return this;
	}

	protected void removeValueAt0(int index) {
		values.remove(index);
	}/**
	 * @see de.ims.icarus2.model.manifest.api.ValueSet#removeAllValues()
	 */
	@Override
	public ValueSet removeAllValues() {
		checkNotLocked();

		removeAllValues0();

		return this;
	}

	protected void removeAllValues0() {
		values.clear();
	}

	@Override
	public int indexOfValue(Object value) {
		requireNonNull(value);

		return values.indexOf(value);
	}
}
