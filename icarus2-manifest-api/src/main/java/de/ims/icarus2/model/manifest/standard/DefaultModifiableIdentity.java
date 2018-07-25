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

import javax.swing.Icon;

import de.ims.icarus2.model.manifest.api.ModifiableIdentity;
import de.ims.icarus2.model.manifest.util.ManifestUtils;
import de.ims.icarus2.util.id.Identity;

/**
 * @author Markus Gärtner
 *
 */
public class DefaultModifiableIdentity extends AbstractLockable implements ModifiableIdentity {

	private String id;
	private String name;
	private String description;
	private Icon icon;

	public DefaultModifiableIdentity() {
		// default constructor
	}

	public DefaultModifiableIdentity(String id, String name, String description, Icon icon) {
		setId(id);
		setName(name);
		setDescription(description);
		setIcon(icon);
	}

	public DefaultModifiableIdentity(String id, String description, Icon icon) {
		this(id, null, description, icon);
	}

	public DefaultModifiableIdentity(String id, String description) {
		this(id, null, description, null);
	}

	/**
	 * @return the id
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @return the description
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * @return the icon
	 */
	@Override
	public Icon getIcon() {
		return icon;
	}

	/**
	 * @param id the id to set
	 */
	@Override
	public void setId(String id) {
		checkNotLocked();

		setId0(id);
	}

	protected void setId0(String id) {
		requireNonNull(id);
		if(!ManifestUtils.isValidId(id))
			throw new IllegalArgumentException("Id format not supported: "+id); //$NON-NLS-1$

		this.id = id;
	}

	/**
	 * @param name the name to set
	 */
	@Override
	public void setName(String name) {
		checkNotLocked();

		setName0(name);
	}

	protected void setName0(String name) {
		this.name = name;
	}

	/**
	 * @param description the description to set
	 */
	@Override
	public void setDescription(String description) {
		checkNotLocked();

		setDescription0(description);
	}

	protected void setDescription0(String description) {
		this.description = description;
	}

	/**
	 * @param icon the icon to set
	 */
	@Override
	public void setIcon(Icon icon) {
		checkNotLocked();

		setIcon0(icon);
	}

	protected void setIcon0(Icon icon) {
		this.icon = icon;
	}

	/**
	 * @see de.ims.icarus2.util.id.Identity#getOwner()
	 */
	@Override
	public Object getOwner() {
		return this;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return id==null ? 0 : id.hashCode();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		} if(obj instanceof Identity) {
			Identity other = (Identity) obj;
			return id==null ? other.getId()==null :
				id.equals(other.getId());
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ModifiableIdentity@"+(id==null ? "<unnamed>" : id); //$NON-NLS-1$ //$NON-NLS-2$
	}
}