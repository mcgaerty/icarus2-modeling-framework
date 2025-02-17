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
package de.ims.icarus2.model.api.layer.annotation;

import static de.ims.icarus2.model.api.ModelTestUtils.mockItem;
import static de.ims.icarus2.model.manifest.ManifestTestUtils.mockTypedManifest;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import de.ims.icarus2.model.api.layer.AnnotationLayer;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.manifest.ManifestTestUtils;
import de.ims.icarus2.model.manifest.api.AnnotationLayerManifest;
import de.ims.icarus2.model.manifest.api.AnnotationManifest;
import de.ims.icarus2.model.manifest.types.ValueType;
import de.ims.icarus2.test.annotations.Provider;
import de.ims.icarus2.test.annotations.RandomizedTest;
import de.ims.icarus2.test.random.RandomGenerator;

/**
 * @author Markus Gärtner
 *
 */
public interface MultiKeyAnnotationStorageTest<S extends AnnotationStorage>
		extends AnnotationStorageTest<S> {

	/** Creates a list of distinct keys */
	List<String> keys();

	@SuppressWarnings("unchecked")
	default AnnotationLayerManifest createManifest(List<String> keys) {
		AnnotationLayerManifest manifest = mockTypedManifest(AnnotationLayerManifest.class);
		when(manifest.getAvailableKeys()).thenReturn(new HashSet<>(keys));

		Set<AnnotationManifest> annotationManifests = new HashSet<>();

		for(String key : keys) {
			AnnotationManifest annotationManifest = createAnnotationManifest(key);
			when(manifest.getAnnotationManifest(key)).thenReturn(
					Optional.of(annotationManifest));

			annotationManifests.add(annotationManifest);
		}

		when(manifest.getAnnotationManifests()).thenReturn(annotationManifests);
		doAnswer(invoc -> {
			annotationManifests.forEach((Consumer<? super AnnotationManifest>)invoc.getArgument(0));
			return null;
		}).when(manifest).forEachAnnotationManifest(any());

		return manifest;
	}

	/**
	 * @see de.ims.icarus2.model.api.layer.annotation.AnnotationStorageTest#createManifest()
	 */
	@Override
	default AnnotationLayerManifest createManifest() {
		return createManifest(keys());
	}

	@Provider
	default S createForKeys(List<String> keys) {
		AnnotationLayer layer = createLayer(createManifest(keys));
		S storage = createForLayer(layer);
		if(storage instanceof ManagedAnnotationStorage) {
			((ManagedAnnotationStorage)storage).addNotify(layer);
		}
		return storage;
	}

	/** Allows the test class to produce multiple alternate instances to be tested */
	default Stream<Config<S>> createConfigurations() {
		return Stream.of(new Config<>("default", this::create, keys()));
	}

	/**
	 * Signals that the implementation under test can automatically remove annotations
	 * from storage when assigned a {@code noEntryValue}.
	 *
	 * @return
	 */
	default boolean supportsAutoRemoveAnnotations() {
		return true;
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#collectKeys(de.ims.icarus2.model.api.members.item.Item, java.util.function.Consumer)}.
	 */
	@TestFactory
	default Stream<DynamicTest> testCollectKeys() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				storage.setValue(item, key, testValue(key));
			}

			Consumer<String> collector = mock(Consumer.class);

			storage.collectKeys(item, collector);

			for(String key : config.keys()) {
				verify(collector).accept(key);
			}

			Consumer<String> collector2 = mock(Consumer.class);
			Supplier<Item> supplier = mock(Supplier.class);
			when(supplier.get()).thenReturn(item, null, null);
			storage.removeAllValues(supplier);
			storage.collectKeys(item, collector2);

			verify(collector2, never()).accept(any());
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#getValue(de.ims.icarus2.model.api.members.item.Item, java.lang.String)}.
	 */
	@TestFactory
	default Stream<DynamicTest> testGetValueEmptyMulti() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				assertEquals(noEntryValue(key), storage.getValue(item, key));
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#getString(de.ims.icarus2.model.api.members.item.Item, java.lang.String)}.
	 */
	@TestFactory
	default Stream<DynamicTest> testGetStringEmptyMulti() {
		return Config.expand(this, ValueType.STRING).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForGetters(key).contains(ValueType.STRING)) {
					assertEquals(noEntryValue(key), storage.getString(item, key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.getString(item, key),
							"Fetching string from "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#getInteger(de.ims.icarus2.model.api.members.item.Item, java.lang.String)}.
	 */
	@TestFactory
	default Stream<DynamicTest> testGetIntegerEmptyMulti() {
		return Config.expand(this, ValueType.INTEGER).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForGetters(key).contains(ValueType.INTEGER)) {
					assertEquals(((Number)noEntryValue(key)).intValue(), storage.getInteger(item, key),
							"Mismatch for key "+key+" of type "+valueType(key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.getInteger(item, key),
							"Fetching int from "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#getFloat(de.ims.icarus2.model.api.members.item.Item, java.lang.String)}.
	 */
	@TestFactory
	default Stream<DynamicTest> testGetFloatEmptyMulti() {
		return Config.expand(this, ValueType.FLOAT).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForGetters(key).contains(ValueType.FLOAT)) {
					assertEquals(((Number)noEntryValue(key)).floatValue(), storage.getFloat(item, key),
							"Mismatch for key "+key+" of type "+valueType(key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.getFloat(item, key),
							"Fetching float from "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#getDouble(de.ims.icarus2.model.api.members.item.Item, java.lang.String)}.
	 */
	@TestFactory
	default Stream<DynamicTest> testGetDoubleEmptyMulti() {
		return Config.expand(this, ValueType.DOUBLE).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForGetters(key).contains(ValueType.DOUBLE)) {
					assertEquals(((Number)noEntryValue(key)).doubleValue(), storage.getDouble(item, key),
							"Mismatch for key "+key+" of type "+valueType(key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.getDouble(item, key),
							"Fetching double from "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#getLong(de.ims.icarus2.model.api.members.item.Item, java.lang.String)}.
	 */
	@TestFactory
	default Stream<DynamicTest> testGetLongEmptyMulti() {
		return Config.expand(this, ValueType.LONG).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForGetters(key).contains(ValueType.LONG)) {
					assertEquals(((Number)noEntryValue(key)).longValue(), storage.getLong(item, key),
							"Mismatch for key "+key+" of type "+valueType(key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.getLong(item, key),
							"Fetching long from "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#getBoolean(de.ims.icarus2.model.api.members.item.Item, java.lang.String)}.
	 */
	@SuppressWarnings("boxing")
	@TestFactory
	default Stream<DynamicTest> testGetBooleanEmptyMulti() {
		return Config.expand(this, ValueType.BOOLEAN).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForGetters(key).contains(ValueType.BOOLEAN)) {
					assertEquals(noEntryValue(key), storage.getBoolean(item, key),
							"Mismatch for key "+key+" of type "+valueType(key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.getBoolean(item, key),
							"Fetching boolean from "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#setValue(de.ims.icarus2.model.api.members.item.Item, java.lang.String, java.lang.Object)}.
	 */
	@TestFactory
	default Stream<DynamicTest> testSetValueMulti() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();
			List<String> keys = config.keys();
			Object[] values = keys.stream().map(this::testValue).toArray();

			// Set all the values
			for (int i = 0; i < values.length; i++) {
				storage.setValue(item, keys.get(i), values[i]);
			}

			// Now verify all the values
			for (int i = 0; i < values.length; i++) {
				assertEquals(values[i], storage.getValue(item, keys.get(i)));
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#setString(de.ims.icarus2.model.api.members.item.Item, java.lang.String, java.lang.String)}.
	 */
	@TestFactory
	@RandomizedTest
	default Stream<DynamicTest> testSetStringMulti() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForSetters(key).contains(ValueType.STRING)) {
					String value = config.rand().randomString(10); // ValueType.CUSTOM would yield Object
					storage.setString(item, key, value);
					assertEquals(value, storage.getString(item, key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.setString(item, key, config.rand().randomString(10)));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#setInteger(de.ims.icarus2.model.api.members.item.Item, java.lang.String, int)}.
	 */
	@TestFactory
	@RandomizedTest
	default Stream<DynamicTest> testSetIntegerMulti() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForSetters(key).contains(ValueType.INTEGER)) {
					int value = ((Number)testValue(key)).intValue();
					storage.setInteger(item, key, value);
					assertEquals(value, storage.getInteger(item, key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.setInteger(item, key, config.rand().nextInt()),
							"Expecting error when setting int on "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#setLong(de.ims.icarus2.model.api.members.item.Item, java.lang.String, long)}.
	 */
	@TestFactory
	@RandomizedTest
	default Stream<DynamicTest> testSetLongMulti() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForSetters(key).contains(ValueType.LONG)) {
					long value = ((Number)testValue(key)).longValue();
					storage.setLong(item, key, value);
					assertEquals(value, storage.getLong(item, key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.setLong(item, key, config.rand().nextLong()),
							"Expecting error when setting long on "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#setFloat(de.ims.icarus2.model.api.members.item.Item, java.lang.String, float)}.
	 */
	@TestFactory
	@RandomizedTest
	default Stream<DynamicTest> testSetFloatMulti() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForSetters(key).contains(ValueType.FLOAT)) {
					float value = ((Number)testValue(key)).floatValue();
					storage.setFloat(item, key, value);
					assertEquals(value, storage.getFloat(item, key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.setFloat(item, key, config.rand().nextFloat()),
							"Expecting error when setting float on "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#setDouble(de.ims.icarus2.model.api.members.item.Item, java.lang.String, double)}.
	 */
	@TestFactory
	@RandomizedTest
	default Stream<DynamicTest> testSetDoubleMulti() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForSetters(key).contains(ValueType.DOUBLE)) {
					double value = ((Number)testValue(key)).doubleValue();
					storage.setDouble(item, key, value);
					assertEquals(value, storage.getDouble(item, key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.setDouble(item, key, config.rand().nextDouble()),
							"Expecting error when setting double on "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#setBoolean(de.ims.icarus2.model.api.members.item.Item, java.lang.String, boolean)}.
	 */
	@SuppressWarnings("boxing")
	@TestFactory
	@RandomizedTest
	default Stream<DynamicTest> testSetBooleanMulti() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				if(typesForSetters(key).contains(ValueType.BOOLEAN)) {
					boolean value = ((Boolean)testValue(key)).booleanValue();
					storage.setBoolean(item, key, value);
					assertEquals(value, storage.getBoolean(item, key));
				} else {
					ManifestTestUtils.assertUnsupportedType(() -> storage.setBoolean(item, key, config.rand().nextBoolean()),
							"Expecting error when setting boolean on "+valueType(key));
				}
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#hasAnnotations()}.
	 */
	@TestFactory
	default Stream<DynamicTest> testHasAnnotationsMulti() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				storage.setValue(item, key, testValue(key));
			}

			assertTrue(storage.hasAnnotations());

			if(supportsAutoRemoveAnnotations()) {
				for(String key : config.keys()) {
					storage.setValue(item, key, noEntryValue(key));
				}
				assertFalse(storage.hasAnnotations());
			}
		}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage#hasAnnotations(de.ims.icarus2.model.api.members.item.Item)}.
	 */
	@TestFactory
	default Stream<DynamicTest> testHasAnnotationsItemMulti() {
		return Config.expand(this).map(config -> dynamicTest(config.label(), () -> {
			S storage = config.storage();
			Item item = mockItem();

			for(String key : config.keys()) {
				storage.setValue(item, key, testValue(key));
			}

			assertTrue(storage.hasAnnotations(item));

			if(supportsAutoRemoveAnnotations()) {
				for(String key : config.keys()) {
					storage.setValue(item, key, noEntryValue(key));
				}
				assertFalse(storage.hasAnnotations(item));
			}

			// Verify foreign item yields no annotations
			assertFalse(storage.hasAnnotations(mockItem()));
		}));
	}

	static class Config<S extends AnnotationStorage> {
		private final String label;
		private final Supplier<S> source;
		private final List<String> keys;
		private RandomGenerator rand;

		private static final RandomGenerator defaultRand = RandomGenerator.forClass(Config.class);

		public Config(String label, Supplier<S> source, List<String> keys) {
			this.label = requireNonNull(label);
			this.source = requireNonNull(source);
			this.keys = requireNonNull(keys);
		}

		public Config<S> rand(RandomGenerator rand) {
			this.rand = requireNonNull(rand);
			return this;
		}

		S storage() {
			return source.get();
		}

		String label() {
			return label +"[" + keys.size() +"]";
		}

		List<String> keys() {
			return keys;
		}

		String keyForType(MultiKeyAnnotationStorageTest<S> test, ValueType valueType) {
			for(String key : keys) {
				if(test.valueType(key)==valueType) {
					return key;
				}
			}
			throw new IllegalStateException("No key in config for type: "+valueType);
		}

		RandomGenerator rand() {
			return rand == null ? defaultRand : rand;
		}

		/**
		 * Create a stream of configurations that represent the combinations
		 * of the basic configurations {@link MultiKeyAnnotationStorageTest#createConfigurations() provided}
		 * by the given {@code test} and the permutations of key sub-lists.
		 */
		static <S extends AnnotationStorage> Stream<Config<S>> expand(
				MultiKeyAnnotationStorageTest<S> test) {
			return test.createConfigurations()
					.flatMap(config -> config.rand().randomSubLists(config.keys(), 0.5)
							.map(subKeys -> new Config<S>(config.label, config.source, subKeys)));

		}

		/**
		 * Create a stream of configurations that represent the combinations
		 * of the basic configurations {@link MultiKeyAnnotationStorageTest#createConfigurations() provided}
		 * by the given {@code test} and the permutations of key sub-lists.
		 * In addition this method will make sure that all the returned configurations
		 * will contain key lists that cover all the specified {@code types}.
		 *
		 * @see #expand(MultiKeyAnnotationStorageTest)
		 */
		static <S extends AnnotationStorage> Stream<Config<S>> expand(
				MultiKeyAnnotationStorageTest<S> test, ValueType...types) {
			String[] sentinels = Stream.of(types)
					.map(test::keyForType)
					.distinct()
					.toArray(String[]::new);

			return test.createConfigurations()
					.flatMap(config -> config.rand().randomSubLists(config.keys(), 0.5, sentinels)
							.map(subKeys -> new Config<S>(config.label, config.source, subKeys)));

		}
	}
}
