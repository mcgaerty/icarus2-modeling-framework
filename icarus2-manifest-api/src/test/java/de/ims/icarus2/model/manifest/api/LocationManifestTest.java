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
package de.ims.icarus2.model.manifest.api;

import static de.ims.icarus2.model.manifest.ManifestTestUtils.assertManifestException;
import static de.ims.icarus2.model.manifest.ManifestTestUtils.mockTypedManifest;
import static de.ims.icarus2.test.TestUtils.DEFAULT;
import static de.ims.icarus2.test.TestUtils.NO_CHECK;
import static de.ims.icarus2.test.TestUtils.NO_DEFAULT;
import static de.ims.icarus2.test.TestUtils.NO_ILLEGAL;
import static de.ims.icarus2.test.TestUtils.NO_NPE_CHECK;
import static de.ims.icarus2.test.TestUtils.NPE_CHECK;
import static de.ims.icarus2.test.TestUtils.assertAccumulativeGetter;
import static de.ims.icarus2.test.TestUtils.assertForEach;
import static de.ims.icarus2.test.TestUtils.assertGetter;
import static de.ims.icarus2.test.TestUtils.assertOptGetter;
import static de.ims.icarus2.test.TestUtils.settings;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import de.ims.icarus2.model.manifest.ManifestErrorCode;
import de.ims.icarus2.model.manifest.api.LocationManifest.PathEntry;
import de.ims.icarus2.model.manifest.api.LocationManifest.PathType;
import de.ims.icarus2.test.TestUtils;

/**
 * @author Markus Gärtner
 *
 */
public interface LocationManifestTest<M extends LocationManifest> extends ManifestTest<M> {

	public static PathEntry mockEntry(PathType type, String value) {
		PathEntry entry = mock(PathEntry.class);
		when(entry.getType()).thenReturn(type);
		when(entry.getValue()).thenReturn(value);
		return entry;
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#isInline()}.
	 */
	@SuppressWarnings("boxing")
	@Test
	default void testIsInline() {
		assertGetter(createUnlocked(),
				Boolean.FALSE, Boolean.TRUE,
				DEFAULT(LocationManifest.DEFAULT_IS_INLINE),
				LocationManifest::isInline,
				LocationManifest::setIsInline);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#getInlineData()}.
	 */
	@Test
	default void testGetInlineData() {
		M instance = createUnlocked();
		instance.setIsInline(true);

		assertOptGetter(instance,
				"data1", "data2",
				NO_DEFAULT(),
				LocationManifest::getInlineData,
				LocationManifest::setInlineData);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#getRootPath()}.
	 */
	@Test
	default void testGetRootPath() {
		assertOptGetter(createUnlocked(),
				"path1", "path2", NO_DEFAULT(),
				LocationManifest::getRootPath,
				LocationManifest::setRootPath);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#getRootPathType()}.
	 */
	@Test
	default void testGetRootPathType() {
		for(PathType pathType : PathType.values()) {
			assertOptGetter(createUnlocked(),
					pathType, TestUtils.other(pathType),
					DEFAULT(LocationManifest.DEFAULT_ROOT_PATH_TYPE),
					LocationManifest::getRootPathType,
					LocationManifest::setRootPathType);
		}
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#getPathResolverManifest()}.
	 */
	@Test
	default void testGetPathResolverManifest() {
		assertOptGetter(createUnlocked(),
				mockTypedManifest(ManifestType.PATH_RESOLVER_MANIFEST),
				mockTypedManifest(ManifestType.PATH_RESOLVER_MANIFEST),
				NO_DEFAULT(),
				LocationManifest::getPathResolverManifest,
				LocationManifest::setPathResolverManifest);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#forEachPathEntry(java.util.function.Consumer)}.
	 */
	@Test
	default void testForEachPathEntry() {
		assertForEach(createUnlocked(),
				mockEntry(PathType.FILE, "file1"),
				mockEntry(PathType.FILE, "file2"),
				(Function<M, Consumer<Consumer<? super PathEntry>>>)m -> m::forEachPathEntry,
				LocationManifest::addPathEntry);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#getPathEntries()}.
	 */
	@Test
	default void testGetPathEntries() {
		assertAccumulativeGetter(createUnlocked(),
				mockEntry(PathType.FILE, "file1"),
				mockEntry(PathType.FILE, "file2"),
				LocationManifest::getPathEntries,
				LocationManifest::addPathEntry);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#setIsInline(boolean)}.
	 */
	@Test
	default void testSetIsInline() {
		assertLockableSetter(settings(), LocationManifest::setIsInline);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#setInlineData(java.lang.CharSequence)}.
	 */
	@Test
	default void testSetInlineData() {
		M instance = createUnlocked();
		assertManifestException(ManifestErrorCode.MANIFEST_ERROR,
				() -> instance.setInlineData("data"),
				"Expecting manifest error when setting inline data without setting inline flag");

		assertLockableSetter(
				settings().<LocationManifest>processor((s,m) -> m.setIsInline(true)),
				LocationManifest::setInlineData,
				"data", NPE_CHECK, NO_CHECK);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#setRootPath(java.lang.String)}.
	 */
	@Test
	default void testSetRootPath() {
		M instance = createUnlocked();
		instance.setIsInline(true);
		assertManifestException(ManifestErrorCode.MANIFEST_ERROR,
				() -> instance.setRootPath("path"),
				"Expecting manifest error when setting root path with active inline flag");

		assertLockableSetter(
				settings(),
				LocationManifest::setRootPath,
				"path", NPE_CHECK, INVALID_INPUT_CHECK, "");
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#setRootPathType(de.ims.icarus2.model.manifest.api.LocationManifest.PathType)}.
	 */
	@Test
	default void testSetRootPathType() {
		M instance = createUnlocked();
		instance.setIsInline(true);
		assertManifestException(ManifestErrorCode.MANIFEST_ERROR,
				() -> instance.setRootPathType(PathType.FILE),
				"Expecting manifest error when setting root path type with active inline flag");

		for(PathType pathType : PathType.values()) {
			assertLockableSetter(
					settings(),
					LocationManifest::setRootPathType,
					pathType, NPE_CHECK, NO_CHECK);
		}
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#setPathResolverManifest(de.ims.icarus2.model.manifest.api.PathResolverManifest)}.
	 */
	@Test
	default void testSetPathResolverManifest() {
		M instance = createUnlocked();
		instance.setIsInline(true);
		assertManifestException(ManifestErrorCode.MANIFEST_ERROR,
				() -> instance.setPathResolverManifest(mockTypedManifest(ManifestType.PATH_RESOLVER_MANIFEST)),
				"Expecting manifest error when setting path resolver manifest with active inline flag");

		assertLockableSetter(
				settings(),
				LocationManifest::setPathResolverManifest,
				mockTypedManifest(ManifestType.PATH_RESOLVER_MANIFEST),
				NO_NPE_CHECK, NO_CHECK);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#addPathEntry(de.ims.icarus2.model.manifest.api.LocationManifest.PathEntry)}.
	 */
	@Test
	default void testAddPathEntry() {
		M instance = createUnlocked();
		instance.setIsInline(true);
		assertManifestException(ManifestErrorCode.MANIFEST_ERROR,
				() -> instance.addPathEntry(mockEntry(PathType.FILE, "path")),
				"Expecting manifest error when adding path entry with active inline flag");

		assertLockableAccumulativeAdd(settings(),
				LocationManifest::addPathEntry,
				NO_ILLEGAL(), NO_CHECK, NPE_CHECK, INVALID_INPUT_CHECK,
				mockEntry(PathType.FILE, "path1"),
				mockEntry(PathType.FILE, "path2"));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LocationManifest#removePathEntry(de.ims.icarus2.model.manifest.api.LocationManifest.PathEntry)}.
	 */
	@Test
	default void testRemovePathEntry() {
		M instance = createUnlocked();
		instance.setIsInline(true);
		assertManifestException(ManifestErrorCode.MANIFEST_ERROR,
				() -> instance.removePathEntry(mockEntry(PathType.FILE, "path")),
				"Expecting manifest error when removing path entry with active inline flag");

		assertLockableAccumulativeRemove(settings(),
				LocationManifest::addPathEntry,
				LocationManifest::removePathEntry,
				LocationManifest::getPathEntries,
				NPE_CHECK, INVALID_INPUT_CHECK,
				mockEntry(PathType.FILE, "path1"),
				mockEntry(PathType.FILE, "path2"));
	}

}
