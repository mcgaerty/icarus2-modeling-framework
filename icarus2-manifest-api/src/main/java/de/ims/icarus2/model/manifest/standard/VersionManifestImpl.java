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

import de.ims.icarus2.model.manifest.api.VersionManifest;

/**
 * @author Markus Gärtner
 *
 */
public class VersionManifestImpl extends AbstractLockable implements VersionManifest {

	private String formatId;
	private String versionString;

	/**
	 * @see de.ims.icarus2.model.manifest.api.VersionManifest#getFormatId()
	 */
	@Override
	public String getFormatId() {
		return formatId==null ? DEFAULT_VERSION_FORMAT_ID : formatId;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.VersionManifest#getVersionString()
	 */
	@Override
	public String getVersionString() {
		return versionString;
	}

	/**
	 * @param formatId the formatId to set
	 */
	@Override
	public void setFormatId(String formatId) {
		checkNotLocked();

		setFormatId0(formatId);
	}

	protected void setFormatId0(String formatId) {
		requireNonNull(formatId);

		this.formatId = formatId;
	}

	/**
	 * @param versionString the versionString to set
	 */
	@Override
	public void setVersionString(String versionString) {
		checkNotLocked();

		setVersionString0(versionString);
	}

	protected void setVersionString0(String versionString) {
		requireNonNull(versionString);

		this.versionString = versionString;
	}

	@Override
	public int hashCode() {
		int hash = versionString.hashCode();

		if(formatId!=null) {
			hash *= formatId.hashCode();
		}

		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		} if(obj instanceof VersionManifest) {
			VersionManifest other = (VersionManifest) obj;
			return versionString.equals(other.getVersionString())
					&& getFormatId().equals(other.getFormatId());
		}

		return false;
	}

	@Override
	public String toString() {
		String result = versionString;

		if(formatId!=null) {
			versionString += " ("+formatId+")"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		return result;
	}
}