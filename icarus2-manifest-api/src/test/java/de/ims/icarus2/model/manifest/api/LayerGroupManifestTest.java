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

import static de.ims.icarus2.model.manifest.ManifestTestUtils.getIllegalIdValues;
import static de.ims.icarus2.model.manifest.ManifestTestUtils.getLegalIdValues;
import static de.ims.icarus2.model.manifest.ManifestTestUtils.mockTypedManifest;
import static de.ims.icarus2.model.manifest.ManifestTestUtils.transform_id;
import static de.ims.icarus2.test.TestUtils.NO_DEFAULT;
import static de.ims.icarus2.test.TestUtils.NO_ILLEGAL;
import static de.ims.icarus2.test.TestUtils.assertOptionalEquals;
import static de.ims.icarus2.test.TestUtils.settings;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import de.ims.icarus2.test.TestUtils;
import de.ims.icarus2.test.annotations.OverrideTest;

/**
 * @author Markus Gärtner
 *
 */
public interface LayerGroupManifestTest<M extends LayerGroupManifest>
		extends ModifiableIdentityTest<M>, ManifestFragmentTest<M>, EmbeddedTest<M> {

	public static LayerManifest mockLayerManifest(String id) {
		return LayerManifestTest.mockLayerManifest(id);
	}

	public static <M extends LayerGroupManifest, L extends LayerManifest> BiConsumer<M, L> inject_addLayer(
			BiConsumer<M, L> setter) {
		return (m, layerManifest) -> {

			m.addLayerManifest(layerManifest);

			setter.accept(m, layerManifest);
		};
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.TypedManifestTest#getExpectedType()
	 */
	@Override
	default ManifestType getExpectedType() {
		return ManifestType.LAYER_GROUP_MANIFEST;
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.EmbeddedTest#getAllowedHostTypes()
	 */
	@Override
	default Set<ManifestType> getAllowedHostTypes() {
		return Collections.singleton(ManifestType.CONTEXT_MANIFEST);
	}

	/**
	 * @see de.ims.icarus2.model.manifest.api.ModifiableIdentityTest#testGetId()
	 */
	@Override
	@OverrideTest
	@Test
	default void testGetId() {
		ModifiableIdentityTest.super.testGetId();
		ManifestFragmentTest.super.testGetId();
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#getContextManifest()}.
	 * @throws Exception
	 */
	@Test
	default void testGetContextManifest() throws Exception {
		// Layer group can never exist without an enclosing context manifest!
		assertNotNull(createEmpty().getContextManifest());

		ContextManifest host = mockTypedManifest(ManifestType.CONTEXT_MANIFEST);
		assertOptionalEquals(host, createEmbedded(settings(), host).getContextManifest());
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#layerCount()}.
	 */
	@Test
	default void testLayerCount() {
		TestUtils.assertAccumulativeCount(createUnlocked(),
				LayerGroupManifest::addLayerManifest,
				LayerGroupManifest::removeLayerManifest,
				LayerGroupManifest::layerCount,
				mockLayerManifest("layer1"),
				mockLayerManifest("layer2"));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#forEachLayerManifest(java.util.function.Consumer)}.
	 */
	@Test
	default void testForEachLayerManifest() {
		TestUtils.assertForEach(createUnlocked(),
				mockLayerManifest("layer1"),
				mockLayerManifest("layer2"),
				(Function<M, Consumer<Consumer<? super LayerManifest>>>)m -> m::forEachLayerManifest,
				LayerGroupManifest::addLayerManifest);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#getLayerManifests()}.
	 */
	@Test
	default void testGetLayerManifests() {
		TestUtils.assertAccumulativeGetter(createUnlocked(),
				mockLayerManifest("layer1"),
				mockLayerManifest("layer2"),
				LayerGroupManifest::getLayerManifests,
				LayerGroupManifest::addLayerManifest);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#getPrimaryLayerManifest()}.
	 */
	@Test
	default void testGetPrimaryLayerManifest() {
		TestUtils.assertOptGetter(createUnlocked(),
				LayerManifestTest.mockItemLayerManifest("layer1"),
				LayerManifestTest.mockItemLayerManifest("layer2"),
				NO_DEFAULT(),
				LayerGroupManifest::getPrimaryLayerManifest,
				inject_addLayer(TestUtils.inject_genericSetter(
						LayerGroupManifest::setPrimaryLayerId, transform_id())));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#isIndependent()}.
	 */
	@Test
	default void testIsIndependent() {
		assertLockableSetter(settings(), LayerGroupManifest::setIndependent);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#getLayerManifest(java.lang.String)}.
	 */
	@Test
	default void testGetLayerManifest() {
		TestUtils.assertAccumulativeOptLookup(createUnlocked(),
				mockLayerManifest("layer1"),
				mockLayerManifest("layer2"),
				(m, id) -> m.getLayerManifest(id),
				true,
				LayerGroupManifest::addLayerManifest,
				transform_id());
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#addLayerManifest(de.ims.icarus2.model.manifest.api.LayerManifest)}.
	 */
	@Test
	default void testAddLayerManifest() {
		assertLockableAccumulativeAdd(settings(),
				LayerManifestTest.inject_layerLookup(
						LayerGroupManifest::addLayerManifest,
						LayerGroupManifest::getContextManifest),
				NO_ILLEGAL(), TestUtils.NO_CHECK, true,
				DUPLICATE_ID_CHECK,
				LayerManifestTest.mockItemLayerManifest("layer1"),
				LayerManifestTest.mockItemLayerManifest("layer2"));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#removeLayerManifest(de.ims.icarus2.model.manifest.api.LayerManifest)}.
	 */
	@Test
	default void testRemoveLayerManifest() {
		assertLockableAccumulativeRemove(settings(),
				LayerManifestTest.inject_layerLookup(
						LayerGroupManifest::addLayerManifest,
						LayerGroupManifest::getContextManifest),
				LayerGroupManifest::removeLayerManifest,
				LayerGroupManifest::getLayerManifests,
				true, UNKNOWN_ID_CHECK,
				LayerManifestTest.mockItemLayerManifest("layer1"),
				LayerManifestTest.mockItemLayerManifest("layer2"));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#setPrimaryLayerId(java.lang.String)}.
	 */
	@Test
	default void testSetPrimaryLayerId() {
		assertLockableSetterBatch(settings(),
				LayerGroupManifest::setPrimaryLayerId,
				getLegalIdValues(), true,
				INVALID_ID_CHECK, getIllegalIdValues());
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.manifest.api.LayerGroupManifest#setIndependent(boolean)}.
	 */
	@Test
	default void testSetIndependent() {
		assertLockableSetter(settings(), LayerGroupManifest::setIndependent);
	}

}
