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
package de.ims.icarus2.model.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import de.ims.icarus2.ErrorCode;
import de.ims.icarus2.GlobalErrorCode;
import de.ims.icarus2.model.manifest.api.Manifest;
import de.ims.icarus2.model.manifest.api.ManifestException;
import de.ims.icarus2.model.manifest.api.ManifestLocation;
import de.ims.icarus2.model.manifest.api.ManifestRegistry;
import de.ims.icarus2.model.manifest.api.ManifestType;
import de.ims.icarus2.model.manifest.api.TypedManifest;
import de.ims.icarus2.model.manifest.types.DefaultIconLink;
import de.ims.icarus2.model.manifest.types.DefaultLink;
import de.ims.icarus2.model.manifest.types.DefaultUrlResource;
import de.ims.icarus2.model.manifest.types.Url;
import de.ims.icarus2.model.manifest.types.ValueType;
import de.ims.icarus2.test.TestUtils;
import de.ims.icarus2.util.function.ObjBoolConsumer;
import de.ims.icarus2.util.icon.IconWrapper;
import de.ims.icarus2.util.nio.ByteArrayChannel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * @author Markus Gärtner
 *
 */
@SuppressWarnings("boxing")
public class ManifestTestUtils {

	private static final Map<ValueType, TestInfo> testValues = new Object2ObjectOpenHashMap<>();

	public enum TestEnum {
		TEST1,
		TEST2,
		TEST3
	}

//	public static final ValueType EXTENSION_TYPE = spy(ValueType.EXTENSION);
//	static {
//		doReturn(extensions[0]).when(EXTENSION_TYPE).parse("extension1", null); //$NON-NLS-1$
//		doReturn(extensions[1]).when(EXTENSION_TYPE).parse("extension2", null); //$NON-NLS-1$
//		doReturn(extensions[2]).when(EXTENSION_TYPE).parse("extension3", null); //$NON-NLS-1$
//	}

	private static class TestInfo {
		Object illegalValue;
		Object[] legalValues;
		/**
		 * @param illegalValue
		 * @param legalValues
		 */
		public TestInfo(Object illegalValue, Object[] legalValues) {
			super();
			this.illegalValue = illegalValue;
			this.legalValues = legalValues;
		}
	}

	private static void addTestValues(ValueType type, Object illegalValue, Object...values) {
		testValues.put(type, new TestInfo(illegalValue, values));
	}
	static {
		addTestValues(ValueType.STRING, -1, "test1", "test2", "test3");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		addTestValues(ValueType.INTEGER, "illegal", 1, 20, 300);
		addTestValues(ValueType.LONG, "illegal", 1L, 20L, 300L);
		addTestValues(ValueType.FLOAT, "illegal", 1.1F, 2.5F, 3F);
		addTestValues(ValueType.DOUBLE, "illegal", 1.765324D, 2.56789D, -3D);
		addTestValues(ValueType.BOOLEAN, "illegal", true, false);
		addTestValues(ValueType.ENUM, "illegal", (Object[]) TestEnum.values());
		addTestValues(ValueType.IMAGE, "illegal",
				new IconWrapper("testIconName1"), //$NON-NLS-1$
				new IconWrapper("testIconName2"), //$NON-NLS-1$
				new IconWrapper("testIconName3")); //$NON-NLS-1$

		addTestValues(ValueType.BINARY_STREAM, 1,
				ByteArrayChannel.fromChars("this is a test"),
				ByteArrayChannel.fromChars("this is another slightly longer test...\n still a test"),
				ByteArrayChannel.fromChars("this is the third and final test"));

		try {
			addTestValues(ValueType.URL, "illegal",
					new Url("http://www.uni-stuttgart.de"), //$NON-NLS-1$
					new Url("http://www.uni-stuttgart.de/linguistik"), //$NON-NLS-1$
					new Url("http://www.dict.cc")); //$NON-NLS-1$
		} catch(MalformedURLException e) {
			// ignore
		}

		try {
			addTestValues(ValueType.URI, "illegal",
					new URI("mailto:xzy"), //$NON-NLS-1$
					new URI("/ref/some/relative/data"), //$NON-NLS-1$
					new URI("http://www.dict.cc#marker")); //$NON-NLS-1$
		} catch(URISyntaxException e) {
			// ignore
		}

		try {
			addTestValues(ValueType.URL_RESOURCE, "illegal",
					new DefaultUrlResource(new Url("http://www.uni-stuttgart.de"), "Url-Link 1", "Some test url link"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					new DefaultUrlResource(new Url("http://www.uni-stuttgart.de/linguistik"), "Url-Link 2", "Another url link"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					new DefaultUrlResource(new Url("http://www.dict.cc"), "Url-Link 3 (no desciption)")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch(MalformedURLException e) {
			// ignore
		}

		try {
			addTestValues(ValueType.LINK, "illegal",
					new DefaultLink(new URI("mailto:xzy"), "Link 1", "Some test link"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					new DefaultLink(new URI("/ref/some/relative/data"), "Link 2", "Another link"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					new DefaultLink(new URI("http://www.dict.cc#marker"), "Link 3 (no desciption)")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch(URISyntaxException e) {
			// ignore
		}

		addTestValues(ValueType.IMAGE_RESOURCE, "illegal",
				new DefaultIconLink(new IconWrapper("testIconName1"), "Icon-Link 1", "Some test icon link"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new DefaultIconLink(new IconWrapper("testIconName2"), "Icon-Link 2", "Some test icon link"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new DefaultIconLink(new IconWrapper("testIconName3"), "Icon-Link 3 (no description)")); //$NON-NLS-1$ //$NON-NLS-2$$

		addTestValues(ValueType.UNKNOWN, null, new Object(), new int[3], 456);
		addTestValues(ValueType.CUSTOM, null, new Dummy(), new Dummy(), new Dummy());

		addTestValues(ValueType.EXTENSION, -1,
				"my.plugin@extension1",
				"my.plugin@extension2",
				"my.plugin2@extension1");

		addTestValues(ValueType.FILE, "illegal",
				Paths.get("someFile"),
				Paths.get("anotherFile"),
				Paths.get("some","path","with","a","file.txt"));

		//FIXME add some test values for the other more complex types!
	}

	public static Set<ValueType> getAvailableTestTypes() {
		return Collections.unmodifiableSet(testValues.keySet());
	}

	public static Object[] getTestValues(ValueType type) {
		TestInfo info = testValues.get(type);
		if(info==null)
			throw new IllegalArgumentException("No test values for type: "+type);

		return info.legalValues;
	}

	public static Object getTestValue(ValueType type) {
		TestInfo info = testValues.get(type);
		if(info==null)
			throw new IllegalArgumentException("No test values for type: "+type);

		return info.legalValues[0];
	}

	public static Object getIllegalValue(ValueType type) {
		TestInfo info = testValues.get(type);
		if(info==null)
			throw new IllegalArgumentException("No test values for type: "+type);

		return info.illegalValue;
	}

	private static final String[] legalIdValues = {
			"abc",
			"abc123",
			"abcdef",
			"abc-def",
			"abc_def",
			"abc.def",
			"abc-def123",
			"abc_def123",
			"abc.def123"
	};

	private static final String[] illegalIdValues = {
			"",
			"a",
			"aa",
			"123",
			"123abc",
			"%$§!()",
			"abc:def",
			"abc/def",
			"abc@def",
			"abc+def",
			"abc~def",
			"abc#def",
			"abc'def",
			"abc*def",
			"abc=def",
			"abc&def",
			"abc%def",
			"abc$def",
			"abc§def",
			"abc\"def",
			"abc!def",
			"abc{def",
			"abc}def",
			"abc[def",
			"abc]def",
			"abc(def",
			"abc)def",
			"abc\\def",
			"abc`def",
			"abc´def",
			"abc°def",
			"abc^def",
			"abc<def",
			"abc>def",
			"abc|def",
			"abc;def",
			"abc"+TestUtils.EMOJI+"def",
	};

	public static String[] getIllegalIdValues() {
		return illegalIdValues.clone();
	}

	public static String[] getLegalIdValues() {
		return legalIdValues.clone();
	}

	private static class Dummy {
		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Dummy@"+hashCode(); //$NON-NLS-1$
		}
	}

	public static ManifestLocation mockManifestLocation(boolean template) {
		ManifestLocation location = mock(ManifestLocation.class);
		when(location.isTemplate()).thenReturn(template);
		return location;
	}

	public static ManifestLocation getOrMockManifestLocation(TypedManifest source, boolean template) {
		if(source instanceof Manifest) {
			return ((Manifest)source).getManifestLocation();
		}

		ManifestLocation location = mock(ManifestLocation.class);
		when(location.isTemplate()).thenReturn(template);
		return location;
	}

	public static ManifestRegistry mockManifestRegistry() {
		ManifestRegistry registry = mock(ManifestRegistry.class);
		AtomicInteger uuidGen = new AtomicInteger(0);
		when(registry.createUID()).then(invocation -> uuidGen.incrementAndGet());
		return registry;
	}

	public static ManifestRegistry getOrMockManifestRegistry(TypedManifest source) {
		if(source instanceof Manifest) {
			return ((Manifest)source).getRegistry();
		}

		ManifestRegistry registry = mock(ManifestRegistry.class);
		AtomicInteger uuidGen = new AtomicInteger(0);
		when(registry.createUID()).then(invocation -> uuidGen.incrementAndGet());
		return registry;
	}

	public static <M extends TypedManifest> M mockTypedManifest(ManifestType type) {
		Class<? extends TypedManifest> clazz = type.getBaseClass();
		if(clazz==null)
			throw new InternalError("Cannot create mock for manifest type: "+type);

		return mockTypedManifest(clazz);
	}

	public static <M extends TypedManifest> M mockTypedManifest(Class<? extends TypedManifest> clazz) {
		@SuppressWarnings("unchecked")
		M manifest = (M) mock(clazz);

		if(Manifest.class.isAssignableFrom(clazz)) {
			Manifest fullManifest = (Manifest) manifest;
			ManifestRegistry registry = Mockito.mock(ManifestRegistry.class);
			ManifestLocation location = Mockito.mock(ManifestLocation.class);

			when(fullManifest.getManifestLocation()).thenReturn(location);
			when(fullManifest.getRegistry()).thenReturn(registry);
		}

		return manifest;
	}

	// ASSERTIONS FOR CONTENT

	public static void assertMalformedId(Manifest manifest, String id) {
		ManifestException exception = assertThrows(ManifestException.class, () -> manifest.setId(id));
		assertEquals(ManifestErrorCode.MANIFEST_INVALID_ID, exception.getErrorCode());
	}

	public static void assertValidId(Manifest manifest, String id) {
		manifest.setId(id);
		assertEquals(id, manifest.getId());
	}

	/**
	 * {@link #assertManifestException(ManifestErrorCode, Executable) Assert} {@link ManifestErrorCode#MANIFEST_TYPE_CAST}
	 * @param executable
	 */
	public static void assertIllegalValue(Executable executable) {
		assertManifestException(ManifestErrorCode.MANIFEST_TYPE_CAST, executable);
	}

	/**
	 * {@link #assertManifestException(ManifestErrorCode, Executable) Assert} {@link ManifestErrorCode#MANIFEST_TYPE_CAST}
	 * @param executable
	 */
	public static void assertIllegalId(Executable executable) {
		assertManifestException(ManifestErrorCode.MANIFEST_INVALID_ID, executable);
	}

	public static void assertManifestException(ErrorCode errorCode, Executable executable) {
		ManifestException exception = assertThrows(ManifestException.class, executable);
		assertEquals(errorCode, exception.getErrorCode());
	}

	// ASSERTIONS FOR METHOD PATTERNS

	public static <T extends Object, K extends Object> void assertSetter(T instance, BiConsumer<T, K> setter, K value,
			boolean checkNPE, @SuppressWarnings("unchecked") K...illegalValues) {
		if(checkNPE) {
			TestUtils.assertNPE(() -> setter.accept(instance, null));
		} else {
			setter.accept(instance, null);
		}

		setter.accept(instance, value);

		for(K illegalValue : illegalValues) {
			assertIllegalValue(() -> setter.accept(instance, illegalValue));
		}
	}

	public static <T extends Object, K extends Object> void assertSetter(T instance, BiConsumer<T, K> setter, K[] values,
			boolean checkNPE, @SuppressWarnings("unchecked") K...illegalValues) {
		if(checkNPE) {
			TestUtils.assertNPE(() -> setter.accept(instance, null));
		} else {
			setter.accept(instance, null);
		}

		for(K value : values) {
			setter.accept(instance, value);
		}

		//TODO allow the type of assertion for illegal values to be customized

		for(K illegalValue : illegalValues) {
			assertIllegalValue(() -> setter.accept(instance, illegalValue));
		}
	}

	public static <T extends Object> void assertSetter(T instance, ObjBoolConsumer<T> setter) {
		setter.accept(instance, true);
		setter.accept(instance, false);
	}

	public static <T extends Object, K extends Object> void assertAccumulativeAdd(
			T instance, BiConsumer<T, K> adder,
			K[] illegalValues, boolean checkNPE, boolean checkDuplicate, @SuppressWarnings("unchecked") K...values) {

		if(checkNPE) {
			TestUtils.assertNPE(() -> adder.accept(instance, null));
		}

		for(int i=0; i<values.length; i++) {
			adder.accept(instance, values[i]);
		}

		if(checkDuplicate) {
			assertManifestException(GlobalErrorCode.INVALID_INPUT, () -> adder.accept(instance, values[0]));
		}

		if(illegalValues!=null) {
			for(K illegalValue : illegalValues) {
				assertIllegalValue(() -> adder.accept(instance, illegalValue));
			}
		}
	}

	public static <T extends Object, K extends Object, C extends Collection<K>> void assertAccumulativeRemove(
			T instance, BiConsumer<T, K> adder, BiConsumer<T, K> remover,
			Function<T, C> getter, boolean checkNPE, boolean checkInvalidRemove,
					@SuppressWarnings("unchecked") K...values) {

		for(K value : values) {
			adder.accept(instance, value);
		}

		TestUtils.assertNPE(() -> remover.accept(instance, null));

		TestUtils.assertCollectionEquals(getter.apply(instance), values);

		remover.accept(instance, values[0]);

		TestUtils.assertCollectionEquals(getter.apply(instance), Arrays.copyOfRange(values, 1, values.length));

		if(checkInvalidRemove) {
			assertManifestException(GlobalErrorCode.INVALID_INPUT,
					() -> remover.accept(instance, values[0]));
		}

		for(int i=1; i<values.length; i++) {
			remover.accept(instance, values[i]);
		}

		assertTrue(getter.apply(instance).isEmpty());


		adder.accept(instance, values[0]);
	}

	public static <T extends Object, K extends Object> void assertGetter(
			T instance, K value1, K value2, K defaultValue, Function<T,K> getter, BiConsumer<T, K> setter) {
		if(defaultValue==null) {
			assertNull(getter.apply(instance));
		} else {
			assertEquals(defaultValue, getter.apply(instance));
		}

		setter.accept(instance, value1);
		assertEquals(value1, getter.apply(instance));

		setter.accept(instance, value2);
		assertEquals(value2, getter.apply(instance));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object, K extends Object> void assertAccumulativeGetter(
			T instance, K value1, K value2, Function<T,? extends Collection<K>> getter, BiConsumer<T, K> adder) {
		assertTrue(getter.apply(instance).isEmpty());

		adder.accept(instance, value1);
		TestUtils.assertCollectionEquals(getter.apply(instance), value1);

		adder.accept(instance, value2);
		TestUtils.assertCollectionEquals(getter.apply(instance), value1, value2);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object, K extends Object> void assertAccumulativeFlagGetter(
			T instance, BiPredicate<T,K> getter, BiConsumer<T, K> adder, BiConsumer<T, K> remover, K...values) {

		for(K value : values) {
			assertFalse(getter.test(instance, value));

			adder.accept(instance, value);
			assertTrue(getter.test(instance, value));
		}

		for(K value : values) {
			assertTrue(getter.test(instance, value));

			remover.accept(instance, value);
			assertFalse(getter.test(instance, value));
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object, K extends Object> void assertAccumulativeLocalGetter(
			T instance, K value1, K value2, Function<T,? extends Collection<K>> getter, BiConsumer<T, K> adder) {
		assertTrue(getter.apply(instance).isEmpty());

		adder.accept(instance, value1);
		TestUtils.assertCollectionEquals(getter.apply(instance), value1);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object, K extends Object, A extends Consumer<? super K>> void assertForEach(
			T instance, K value1, K value2, Function<T,Consumer<A>> forEachGen, BiConsumer<T, K> adder) {

		TestUtils.assertForEachNPE(forEachGen.apply(instance));

		TestUtils.assertForEachEmpty(forEachGen.apply(instance));

		adder.accept(instance, value1);
		TestUtils.assertForEachUnsorted(forEachGen.apply(instance), value1);

		adder.accept(instance, value2);
		TestUtils.assertForEachUnsorted(forEachGen.apply(instance), value1, value2);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object, K extends Object, A extends Consumer<? super K>> void assertForEachLocal(
			T instance, K value1, K value2, Function<T,Consumer<A>> forEachLocalGen, BiConsumer<T, K> adder) {

		TestUtils.assertForEachNPE(forEachLocalGen.apply(instance));

		TestUtils.assertForEachEmpty(forEachLocalGen.apply(instance));

		adder.accept(instance, value1);
		TestUtils.assertForEachUnsorted(forEachLocalGen.apply(instance), value1);

		adder.accept(instance, value2);
		TestUtils.assertForEachUnsorted(forEachLocalGen.apply(instance), value1, value2);
	}

	/**
	 * @see #assertGetter(Class, Object, Object, Object, Function, BiConsumer)
	 *
	 * @param argType
	 * @param value1
	 * @param value2
	 * @param defaultValue
	 * @param getter
	 * @param setter
	 */
	public static <T extends Object, K extends Object> void assertIsLocal(
			T instance, K value1, K value2, Predicate<T> isLocalCheck, BiConsumer<T, K> setter) {
		assertFalse(isLocalCheck.test(instance));

		setter.accept(instance, value1);
		assertTrue(isLocalCheck.test(instance));
	}

	public static <T extends Object, K extends Object> void assertAccumulativeIsLocal(
			T instance, K value1, K value2, BiPredicate<T, K> isLocalCheck, BiConsumer<T, K> adder) {

		TestUtils.assertNPE(() -> isLocalCheck.test(instance, null));

		assertFalse(isLocalCheck.test(instance, value1));

		adder.accept(instance, value1);
		assertTrue(isLocalCheck.test(instance, value1));

		adder.accept(instance, value2);
		assertTrue(isLocalCheck.test(instance, value2));
	}

	public static <T extends Object> void assertFlagGetter(
			T instance, Boolean defaultValue, Predicate<T> getter, ObjBoolConsumer<T> setter) {
		if(defaultValue!=null) {
			assertEquals(defaultValue.booleanValue(), getter.test(instance));
		}

		setter.accept(instance, true);
		assertTrue(getter.test(instance));
		setter.accept(instance, false);
		assertFalse(getter.test(instance));
	}
}
