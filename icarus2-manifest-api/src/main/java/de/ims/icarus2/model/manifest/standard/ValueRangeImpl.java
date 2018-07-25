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
package de.ims.icarus2.model.manifest.standard;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import de.ims.icarus2.model.manifest.api.ManifestErrorCode;
import de.ims.icarus2.model.manifest.api.ManifestException;
import de.ims.icarus2.model.manifest.api.ValueRange;
import de.ims.icarus2.model.manifest.types.UnsupportedValueTypeException;
import de.ims.icarus2.model.manifest.types.ValueType;
import de.ims.icarus2.util.lang.ClassUtils;

public class ValueRangeImpl extends AbstractLockable implements ValueRange {

	private final ValueType valueType;
	private Object lower, upper, stepSize;
	private boolean lowerIncluded = DEFAULT_LOWER_INCLUSIVE_VALUE;
	private boolean upperIncluded = DEFAULT_UPPER_INCLUSIVE_VALUE;

	private static final Set<ValueType> supportedValueTypes = ValueType.filterIncluding(
			v -> (v==ValueType.STRING || Comparable.class.isAssignableFrom(v.getBaseClass())));

	public ValueRangeImpl(ValueType valueType) {
		requireNonNull(valueType);

		if(!supportedValueTypes.contains(valueType))
			throw new UnsupportedValueTypeException(valueType);

		this.valueType = valueType;
	}

	public ValueRangeImpl(ValueType valueType, boolean lowerIncluded, boolean upperIncluded) {
		this(valueType);

		this.lowerIncluded = lowerIncluded;
		this.upperIncluded = upperIncluded;
	}

	public ValueRangeImpl(ValueType valueType, Object lower, Object upper, boolean lowerIncluded, boolean upperIncluded) {
		this(valueType, lowerIncluded, upperIncluded);

		setLowerBound(lower);
		setUpperBound(upper);
	}

	public ValueRangeImpl(ValueType valueType, Object lower, Object upper) {
		this(valueType, lower, upper, true, true);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hash = valueType.hashCode();

		if(lowerIncluded!=DEFAULT_LOWER_INCLUSIVE_VALUE) {
			hash *= -1;
		}

		if(upperIncluded!=DEFAULT_UPPER_INCLUSIVE_VALUE) {
			hash *= -2;
		}

		if(lower!=null) {
			hash *= lower.hashCode();
		}

		if(upper!=null) {
			hash *= upper.hashCode();
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
		} if(obj instanceof ValueRange) {
			ValueRange other = (ValueRange)obj;

			return valueType.equals(other.getValueType())
					&& lowerIncluded==other.isLowerBoundInclusive()
					&& upperIncluded==other.isUpperBoundInclusive()
					&& ClassUtils.equals(lower, other.getLowerBound())
					&& ClassUtils.equals(upper, other.getUpperBound());
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ValueRange@").append(valueType.getStringValue()); //$NON-NLS-1$

		if(lowerIncluded) {
			sb.append('[');
		} else {
			sb.append('(');
		}

		if(lower==null && upper==null) {
			sb.append('-');
		} else {
			sb.append(lower).append(',').append(upper);
		}

		if(upperIncluded) {
			sb.append('[');
		} else {
			sb.append(')');
		}


		return sb.toString();
	}

	/**
	 * @return the valueType
	 */
	@Override
	public ValueType getValueType() {
		return valueType;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.ValueRange#getLowerBound()
	 */
	@Override
	public Object getLowerBound() {
		return lower;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.ValueRange#getUpperBound()
	 */
	@Override
	public Object getUpperBound() {
		return upper;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.ValueRange#getStepSize()
	 */
	@Override
	public Object getStepSize() {
		return stepSize;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.ValueRange#isLowerBoundInclusive()
	 */
	@Override
	public boolean isLowerBoundInclusive() {
		return lowerIncluded;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.ValueRange#isUpperBoundInclusive()
	 */
	@Override
	public boolean isUpperBoundInclusive() {
		return upperIncluded;
	}

	protected void checkValue(Object value) {
		Class<?> type = valueType.checkValue(value);

		if(!Comparable.class.isAssignableFrom(type))
			throw new ManifestException(ManifestErrorCode.MANIFEST_TYPE_CAST,
					"Provided value for value range does not implement java.lang.Comparable: "+type.getName()); //$NON-NLS-1$
	}

	/**
	 * @param lower the lower to set
	 */
	@Override
	public void setLowerBound(Object lower) {
		checkNotLocked();

		setLowerBound0(lower);
	}

	protected void setLowerBound0(Object lower) {
		requireNonNull(lower);

		checkValue(lower);

		this.lower = lower;
	}

	/**
	 * @param upper the upper to set
	 */
	@Override
	public void setUpperBound(Object upper)  {
		checkNotLocked();

		setUpperBound0(upper);
	}

	protected void setUpperBound0(Object upper) {
		requireNonNull(upper);

		checkValue(upper);

		this.upper = upper;
	}

	/**
	 * @param upper the upper to set
	 */
	@Override
	public void setStepSize(Object stepSize) {
		checkNotLocked();

		setStepSize0(stepSize);
	}

	protected void setStepSize0(Object stepSize) {
		requireNonNull(stepSize);

		checkValue(stepSize);

		this.stepSize = stepSize;
	}

	/**
	 * @param lowerIncluded the lowerIncluded to set
	 */
	@Override
	public void setLowerBoundInclusive(boolean lowerIncluded) {
		checkNotLocked();

		setLowerBoundIncluded0(lowerIncluded);
	}

	protected void setLowerBoundIncluded0(boolean lowerIncluded) {
		this.lowerIncluded = lowerIncluded;
	}

	/**
	 * @param upperIncluded the upperIncluded to set
	 */
	@Override
	public void setUpperBoundInclusive(boolean upperIncluded) {
		checkNotLocked();

		setUpperBoundIncluded0(upperIncluded);
	}

	protected void setUpperBoundIncluded0(boolean upperIncluded) {
		this.upperIncluded = upperIncluded;
	}

}