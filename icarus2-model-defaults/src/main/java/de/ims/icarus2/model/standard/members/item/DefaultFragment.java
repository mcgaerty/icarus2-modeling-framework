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

import de.ims.icarus2.model.api.layer.FragmentLayer;
import de.ims.icarus2.model.api.members.MemberType;
import de.ims.icarus2.model.api.members.item.Fragment;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.api.raster.Position;
import de.ims.icarus2.model.util.ModelUtils;
import de.ims.icarus2.util.mem.Assessable;
import de.ims.icarus2.util.mem.Reference;
import de.ims.icarus2.util.mem.ReferenceType;

/**
 * @author Markus Gärtner
 *
 */
@Assessable
public class DefaultFragment extends DefaultItem implements Fragment {

	@Reference(ReferenceType.UPLINK)
	private Item item;
	@Reference(ReferenceType.DOWNLINK)
	private Position fragmentBegin;
	@Reference(ReferenceType.DOWNLINK)
	private Position fragmentEnd;

	/**
	 * @param item the item to set
	 */
	public void setItem(Item item) {
		if (item == null)
			throw new NullPointerException("Invalid item"); //$NON-NLS-1$
		this.item = item;
	}

	@Override
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		} else if(obj instanceof Fragment) {
			Fragment other = (Fragment) obj;
			return item==other.getItem()
					&& fragmentBegin.equals(other.getFragmentBegin())
					&& fragmentEnd.equals(other.getFragmentEnd());
		}
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return fragmentBegin.hashCode()*fragmentEnd.hashCode()+1;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.Item#getBeginOffset()
	 */
	@Override
	public long getBeginOffset() {
		return item.getBeginOffset();
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.Item#getEndOffset()
	 */
	@Override
	public long getEndOffset() {
		return item.getEndOffset();
	}

	/**
	 * @see de.ims.icarus2.model.api.members.CorpusMember#getMemberType()
	 */
	@Override
	public MemberType getMemberType() {
		return MemberType.FRAGMENT;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.Fragment#getItem()
	 */
	@Override
	public Item getItem() {
		return item;
	}

	/**
	 * @see de.ims.icarus2.util.Recyclable#recycle()
	 */
	@Override
	public void recycle() {
		super.recycle();

		item = null;
		fragmentBegin = fragmentEnd = null;
	}

	/**
	 * @see de.ims.icarus2.util.Recyclable#revive()
	 */
	@Override
	public boolean revive() {
		return super.revive() && item!=null
				&& fragmentBegin!=null && fragmentEnd!=null;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.Fragment#getLayer()
	 */
	@Override
	public FragmentLayer getLayer() {
		return (FragmentLayer) getContainer().getLayer();
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.Fragment#getFragmentBegin()
	 */
	@Override
	public Position getFragmentBegin() {
		return fragmentBegin;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.Fragment#getFragmentEnd()
	 */
	@Override
	public Position getFragmentEnd() {
		return fragmentEnd;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.Fragment#setFragmentBegin(de.ims.icarus2.model.api.raster.Position)
	 */
	@Override
	public void setFragmentBegin(Position begin) {
		if (begin == null)
			throw new NullPointerException("Invalid begin");  //$NON-NLS-1$

		ModelUtils.checkFragmentPositions(this, begin, null);

		fragmentBegin = begin;
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.Fragment#setFragmentEnd(de.ims.icarus2.model.api.raster.Position)
	 */
	@Override
	public void setFragmentEnd(Position end) {
		if (end == null)
			throw new NullPointerException("Invalid end");  //$NON-NLS-1$

		fragmentEnd = end;
	}

	public void setSpan(Position begin, Position end) {
		if (begin == null)
			throw new NullPointerException("Invalid begin");  //$NON-NLS-1$
		if (end == null)
			throw new NullPointerException("Invalid end");  //$NON-NLS-1$

		ModelUtils.checkFragmentPositions(this, begin, end);

		fragmentBegin = begin;
		fragmentEnd = end;
	}
}