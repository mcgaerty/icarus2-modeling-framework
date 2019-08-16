/**
 *
 */
package de.ims.icarus2.model.standard.members.layers.annotation.fixed;

import static de.ims.icarus2.model.api.ModelTestUtils.assertModelException;
import static de.ims.icarus2.test.TestUtils.random;
import static de.ims.icarus2.util.IcarusUtils.UNSET_INT;
import static de.ims.icarus2.util.collections.CollectionUtils.set;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.ims.icarus2.GlobalErrorCode;
import de.ims.icarus2.model.api.layer.AnnotationLayer;
import de.ims.icarus2.model.api.layer.annotation.ManagedAnnotationStorageTest;
import de.ims.icarus2.model.api.layer.annotation.MultiKeyAnnotationStorageTest;
import de.ims.icarus2.model.manifest.types.ValueType;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

/**
 * @author Markus Gärtner
 *
 */
class FixedKeysLongStorageTest implements MultiKeyAnnotationStorageTest<FixedKeysLongStorage>,
		ManagedAnnotationStorageTest<FixedKeysLongStorage> {

	/** Maps keys to noEntryValues */
	private Object2LongMap<String> setup = new Object2LongOpenHashMap<>();
	private List<String> keys = new ArrayList<>();

	@BeforeEach
	void setUp() {
		for (int i = 0; i < 20; i++) {
			String key = "key_" + i;
			setup.put(key, random().nextLong());
			keys.add(key);
		}
	}

	@AfterEach
	void tearDown() {
		setup.clear();
		keys.clear();
	}

	@Override
	public Set<ValueType> typesForSetters(String key) {
		return set(ValueType.INTEGER, ValueType.LONG, ValueType.FLOAT, ValueType.DOUBLE);
	}

	@Override
	public Set<ValueType> typesForGetters(String key) {
		return set(ValueType.INTEGER, ValueType.LONG, ValueType.FLOAT, ValueType.DOUBLE);
	}

	@Override
	public Object testValue(String key) {
		long noEntryValue = setup.getLong(key);
		long value;
		do {
			value = random().nextLong();
		} while (value == noEntryValue);
		return Long.valueOf(value);
	}

	/**
	 * @see de.ims.icarus2.model.api.layer.annotation.ManagedAnnotationStorageTest#supportsAutoRemoval()
	 */
	@Override
	public boolean supportsAutoRemoval() {
		return false;
	}

	/**
	 * @see de.ims.icarus2.model.api.layer.annotation.MultiKeyAnnotationStorageTest#supportsAutoRemoveAnnotations()
	 */
	@Override
	public boolean supportsAutoRemoveAnnotations() {
		return false;
	}

	/**
	 * @see de.ims.icarus2.model.api.layer.annotation.AnnotationStorageTest#noEntryValue(java.lang.String)
	 */
	@Override
	public Object noEntryValue(String key) {
		return Long.valueOf(setup.getLong(key));
	}

	@Override
	public ValueType valueType(String key) {
		return ValueType.LONG;
	}

	@Override
	public FixedKeysLongStorage createForLayer(AnnotationLayer layer) {
		return new FixedKeysLongStorage();
	}

	@Override
	public Class<? extends FixedKeysLongStorage> getTestTargetClass() {
		return FixedKeysLongStorage.class;
	}

	@Override
	public List<String> keys() {
		return keys;
	}

	@Override
	public String key() {
		return keys.get(0);
	}

	@Nested
	class Constructors {

		/**
		 * Test method for
		 * {@link de.ims.icarus2.model.standard.members.layers.annotation.fixed.FixedKeysLongStorage#FixedKeysLongStorage()}.
		 */
		@Test
		void testFixedKeysLongStorage() {
			assertNotNull(new FixedKeysLongStorage());
		}

		/**
		 * Test method for
		 * {@link de.ims.icarus2.model.standard.members.layers.annotation.fixed.FixedKeysLongStorage#FixedKeysLongStorage(int)}.
		 */
		@ParameterizedTest
		@ValueSource(ints = { UNSET_INT, 1, 10, 100, 10_000 })
		void testFixedKeysLongStorageInt(int capacity) {
			assertNotNull(new FixedKeysLongStorage(capacity));
		}

		/**
		 * Test method for
		 * {@link de.ims.icarus2.model.standard.members.layers.annotation.fixed.FixedKeysLongStorage#FixedKeysLongStorage(int)}.
		 */
		@ParameterizedTest
		@ValueSource(ints = { 0, -2 })
		void testFixedKeysLongStorageIntInvalidCapacity(int capacity) {
			assertModelException(GlobalErrorCode.INVALID_INPUT, () -> new FixedKeysLongStorage(capacity));
		}

		/**
		 * Test method for
		 * {@link de.ims.icarus2.model.standard.members.layers.annotation.fixed.FixedKeysLongStorage#FixedKeysLongStorage(boolean, int)}.
		 */
		@ParameterizedTest
		@ValueSource(ints = { UNSET_INT, 1, 10, 100, 10_000 })
		void testFixedKeysLongStorageBooleanInt(int capacity) {
			assertNotNull(new FixedKeysLongStorage(true, capacity));
			assertNotNull(new FixedKeysLongStorage(false, capacity));
		}

		/**
		 * Test method for
		 * {@link de.ims.icarus2.model.standard.members.layers.annotation.fixed.FixedKeysLongStorage#FixedKeysLongStorage(boolean, int)}.
		 */
		@ParameterizedTest
		@ValueSource(ints = { 0, -2 })
		void testFixedKeysLongStorageBooleanIntInvalidCapacity(int capacity) {
			assertModelException(GlobalErrorCode.INVALID_INPUT, () -> new FixedKeysLongStorage(true, capacity));
			assertModelException(GlobalErrorCode.INVALID_INPUT, () -> new FixedKeysLongStorage(false, capacity));
		}

	}

}
