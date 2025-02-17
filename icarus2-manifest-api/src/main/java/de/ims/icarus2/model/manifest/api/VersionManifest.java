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
package de.ims.icarus2.model.manifest.api;

import javax.annotation.Nullable;

/**
 * @author Markus Gärtner
 *
 */
public interface VersionManifest extends Lockable, TypedManifest {

	public static final String DEFAULT_VERSION_FORMAT_ID = "major-minor-patch";

	/**
	 * Returns the format id that serves as a URI for a certain type
	 * of version format.
	 *
	 * @see #DEFAULT_VERSION_FORMAT_ID
	 */
	String getFormatId();

	@Nullable String getVersionString();

	/**
	 * Equality of a version manifest requires equality of both the
	 * format id and the actual version string.
	 */
	@Override
	boolean equals(Object obj);

	/**
	 * @see de.ims.icarus2.model.manifest.api.TypedManifest#getManifestType()
	 */
	@Override
	default public ManifestType getManifestType() {
		return ManifestType.VERSION;
	}

	// Modification methods

	VersionManifest setFormatId(String formatId);

	VersionManifest setVersionString(String versionString);
}
