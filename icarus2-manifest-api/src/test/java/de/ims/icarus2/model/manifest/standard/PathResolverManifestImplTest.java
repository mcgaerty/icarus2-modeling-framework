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
/**
 *
 */
package de.ims.icarus2.model.manifest.standard;

import de.ims.icarus2.model.manifest.api.LocationManifest;
import de.ims.icarus2.model.manifest.api.ManifestLocation;
import de.ims.icarus2.model.manifest.api.ManifestRegistry;
import de.ims.icarus2.model.manifest.api.PathResolverManifestTest;
import de.ims.icarus2.model.manifest.api.TypedManifest;
import de.ims.icarus2.test.TestSettings;

/**
 * @author Markus Gärtner
 *
 */
class PathResolverManifestImplTest implements PathResolverManifestTest<PathResolverManifestImpl> {

	/**
	 * @see de.ims.icarus2.test.GenericTest#getTestTargetClass()
	 */
	@Override
	public Class<? extends PathResolverManifestImpl> getTestTargetClass() {
		return PathResolverManifestImpl.class;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.EmbeddedMemberManifestTest#createHosted(de.ims.icarus2.test.TestSettings, de.ims.icarus2.model.manifest.api.ManifestLocation, de.ims.icarus2.model.manifest.api.ManifestRegistry, de.ims.icarus2.model.manifest.api.TypedManifest)
	 */
	@Override
	public PathResolverManifestImpl createHosted(TestSettings settings, ManifestLocation manifestLocation,
			ManifestRegistry registry, TypedManifest host) {
		return new PathResolverManifestImpl(manifestLocation, registry, (LocationManifest) host);
	}

}
