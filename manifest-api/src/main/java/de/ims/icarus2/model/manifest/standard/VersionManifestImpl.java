/*
 *  ICARUS 2 -  Interactive platform for Corpus Analysis and Research tools, University of Stuttgart
 *  Copyright (C) 2015-2016 Markus Gärtner
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
 *
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
