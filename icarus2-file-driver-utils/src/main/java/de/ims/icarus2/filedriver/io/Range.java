/**
 *
 */
package de.ims.icarus2.filedriver.io;

import static de.ims.icarus2.util.IcarusUtils.UNSET_LONG;

/**
 * @author Markus Gärtner
 *
 */
public class Range {

	private long min = UNSET_LONG;
	private long max = UNSET_LONG;

	public Range update(long value) {
		if(min==UNSET_LONG || value<min) {
			min = value;
		}
		if(max==UNSET_LONG || value>max) {
			max = value;
		}
		return this;
	}

	public long getMin() {
		return min;
	}

	public long getMax() {
		return max;
	}

	public Range set(long min, long max) {
		this.min = min;
		this.max = max;
		return this;
	}

	public boolean contains(long value) {
		return min!=UNSET_LONG && value>=min && value <=max;
	}

	public boolean isUnset() {
		return min==UNSET_LONG;
	}

	public Range reset() {
		min = max = UNSET_LONG;
		return this;
	}

	/**
	 * Limits this range to the intersection of {@code this} and {@code other}.
	 *
	 * @param other
	 */
	public void limit(Range other) {
		// Nothing changes if this or other range is empty
		if(isUnset() || other.isUnset()) {
			return;
		}
		// From here on we can assume both ranges to be non-empty

		if(max<other.min || min>other.max) {
			// disjoint ranges, so we have to clear our local range
			reset();
		} else {
			min = Math.max(min, other.min);
			max = Math.min(max, other.max);
		}
	}
}