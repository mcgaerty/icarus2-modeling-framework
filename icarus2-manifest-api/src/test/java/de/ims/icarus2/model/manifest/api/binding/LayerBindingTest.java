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
 */
package de.ims.icarus2.model.manifest.api.binding;

import static de.ims.icarus2.util.TestUtils.assertHashContract;
import static de.ims.icarus2.util.TestUtils.assertObjectContract;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.ims.icarus2.model.manifest.api.binding.LayerBinding.Builder;
import de.ims.icarus2.util.Multiplicity;

/**
 * @author Markus Gärtner
 *
 */
public class LayerBindingTest {

	private static final String LAYER_ID_1 = "layerId1";
	private static final String LAYER_ID_2 = "layerId2";
	private static final String LAYER_ID_3 = "layerId3";
	private static final String LAYER_ID_4 = "layerId4";

	private static final String CONTEX_ID_1 = "contextId1";
	private static final String CONTEX_ID_2 = "contextId2";
	private static final String CONTEX_ID_3 = "contextId3";

	private static final String ALIAS_1 = "alias1";
	private static final String ALIAS_2 = "alias2";
	private static final String ALIAS_3 = "alias3";
	private static final String ALIAS_4 = "alias4";

	private static final String LAYER_TYPE_ID_1 = "layerTypeId1";
	private static final String LAYER_TYPE_ID_2 = "layerTypeId2";

	private static final String DESCRIPTION_1 = "This is some silly description...";
	private static final String DESCRIPTION_2 = "This is another\nsilly\nmultiline\ndescription...";

	private static final String CORPUS_ID = "corpusId";

	@Test
	public void testMissingCorpusId() throws Exception {
		assertThrows(IllegalStateException.class, () -> {
			LayerBinding.Builder builder = LayerBinding.newBuilder();

			builder.addPointer(ALIAS_1, CONTEX_ID_1, LAYER_ID_1);

			builder.build();
		});
	}

	@Test
	public void testEmptyBuilder() throws Exception {
		assertThrows(IllegalStateException.class, () -> {
			LayerBinding.Builder builder = LayerBinding.newBuilder();

			builder.build();
		});
	}

	@Test
	public void testLayerPointerImpl() throws Exception {
		LayerPointer pointer1 = new LayerBinding.LayerPointerImpl(CONTEX_ID_1, LAYER_ID_1);
		LayerPointer pointer2 = new LayerBinding.LayerPointerImpl(CONTEX_ID_2, LAYER_ID_2);

		LayerPointer pointer3 = new LayerBinding.LayerPointerImpl(CONTEX_ID_1, LAYER_ID_1);
		LayerPointer pointer4 = new LayerBinding.LayerPointerImpl(CONTEX_ID_2, LAYER_ID_2);

		assertEquals(pointer1, pointer1);
		assertEquals(pointer1, pointer3);
		assertEquals(pointer2, pointer2);
		assertEquals(pointer2, pointer4);

		assertEquals(CONTEX_ID_1, pointer1.getContextId());
		assertEquals(LAYER_ID_1, pointer1.getLayerId());

		assertEquals(CONTEX_ID_2, pointer2.getContextId());
		assertEquals(LAYER_ID_2, pointer2.getLayerId());

		assertNotEquals(pointer1, pointer2);

		assertObjectContract(pointer1);

		assertHashContract(pointer1, pointer2); // not equal
		assertHashContract(pointer1, pointer3); // equal
	}

	@Test
	public void testLayerPrerequisiteImpl() throws Exception {
		LayerPrerequisite prerequisite1 = new LayerBinding.LayerPrerequisiteImpl(LAYER_TYPE_ID_1, ALIAS_1, Multiplicity.ONE, DESCRIPTION_1);
		LayerPrerequisite prerequisite2 = new LayerBinding.LayerPrerequisiteImpl(LAYER_TYPE_ID_2, ALIAS_2, Multiplicity.NONE_OR_ONE, DESCRIPTION_2);

		LayerPrerequisite prerequisite3 = new LayerBinding.LayerPrerequisiteImpl(CONTEX_ID_1, LAYER_ID_1, ALIAS_1, Multiplicity.ONE, DESCRIPTION_1);
		LayerPrerequisite prerequisite4 = new LayerBinding.LayerPrerequisiteImpl(CONTEX_ID_2, LAYER_ID_2, ALIAS_2, Multiplicity.ONE_OR_MORE, DESCRIPTION_2);

		LayerPrerequisite prerequisite5 = new LayerBinding.LayerPrerequisiteImpl(LAYER_TYPE_ID_1, ALIAS_1, Multiplicity.ONE, DESCRIPTION_1);

		assertEquals(prerequisite1, prerequisite1);
		assertEquals(prerequisite2, prerequisite2);
		assertEquals(prerequisite3, prerequisite3);
		assertEquals(prerequisite4, prerequisite4);

		assertEquals(prerequisite1, prerequisite5);

		assertNotEquals(prerequisite1, prerequisite2);
		assertNotEquals(prerequisite2, prerequisite3);
		assertNotEquals(prerequisite3, prerequisite4);
		assertNotEquals(prerequisite4, prerequisite5);

		assertHashContract(prerequisite1, prerequisite1); // equal
		assertHashContract(prerequisite1, prerequisite2); // not equal
		assertHashContract(prerequisite1, prerequisite5); // equal

		assertNull(prerequisite1.getContextId());
		assertNull(prerequisite1.getLayerId());

		assertNull(prerequisite3.getTypeId());
	}

	/**
	 * @see Builder#addPrerequisite(LayerPrerequisite)
	 */
	@Test
	public void testSinglePredefinedPrereq() throws Exception {
		LayerBinding.Builder builder = LayerBinding.newBuilder(CORPUS_ID);

		LayerPrerequisite prerequisite = new LayerBinding.LayerPrerequisiteImpl(LAYER_TYPE_ID_1, ALIAS_1, Multiplicity.ONE, null);

		builder.addPrerequisite(prerequisite);

		LayerBinding binding = builder.build();

		assertTrue(binding.containsAlias(ALIAS_1));

		LayerPrerequisite prerequisite2 = binding.getLayerPrerequisite(ALIAS_1);

		assertSame(prerequisite, prerequisite2);
	}

	/**
	 * @see Builder#addPrerequisite(String, String, String)
	 */
	@Test
	public void testSinglePrereq1() throws Exception {
		LayerBinding.Builder builder = LayerBinding.newBuilder(CORPUS_ID);

		builder.addPrerequisite(ALIAS_1, LAYER_TYPE_ID_1, DESCRIPTION_1);

		LayerBinding binding = builder.build();

		assertTrue(binding.containsAlias(ALIAS_1));

		LayerPrerequisite prerequisite = binding.getLayerPrerequisite(ALIAS_1);

		assertEquals(ALIAS_1, prerequisite.getAlias());
		assertEquals(LAYER_TYPE_ID_1, prerequisite.getTypeId());
		assertEquals(DESCRIPTION_1, prerequisite.getDescription());
		assertEquals(Multiplicity.ONE, prerequisite.getMultiplicity());

		assertNull(prerequisite.getLayerId());
		assertNull(prerequisite.getContextId());
	}

	/**
	 * @see Builder#addPrerequisite(String, String, Multiplicity, String)
	 */
	@Test
	public void testSinglePrereq2() throws Exception {
		LayerBinding.Builder builder = LayerBinding.newBuilder(CORPUS_ID);

		Multiplicity multiplicity = Multiplicity.ANY;

		builder.addPrerequisite(ALIAS_1, LAYER_TYPE_ID_1, multiplicity, DESCRIPTION_1);

		LayerBinding binding = builder.build();

		assertTrue(binding.containsAlias(ALIAS_1));

		LayerPrerequisite prerequisite = binding.getLayerPrerequisite(ALIAS_1);

		assertEquals(ALIAS_1, prerequisite.getAlias());
		assertEquals(LAYER_TYPE_ID_1, prerequisite.getTypeId());
		assertEquals(DESCRIPTION_1, prerequisite.getDescription());
		assertEquals(multiplicity, prerequisite.getMultiplicity());

		assertNull(prerequisite.getLayerId());
		assertNull(prerequisite.getContextId());
	}

	/**
	 * @see Builder#addPrerequisite(String, String, String, String)
	 */
	@Test
	public void testSinglePrereq3() throws Exception {
		LayerBinding.Builder builder = LayerBinding.newBuilder(CORPUS_ID);

		builder.addPrerequisite(ALIAS_1, CONTEX_ID_1, LAYER_ID_1, DESCRIPTION_1);

		LayerBinding binding = builder.build();

		assertTrue(binding.containsAlias(ALIAS_1));

		LayerPrerequisite prerequisite = binding.getLayerPrerequisite(ALIAS_1);

		assertEquals(ALIAS_1, prerequisite.getAlias());
		assertEquals(LAYER_ID_1, prerequisite.getLayerId());
		assertEquals(CONTEX_ID_1, prerequisite.getContextId());
		assertEquals(DESCRIPTION_1, prerequisite.getDescription());
		assertEquals(Multiplicity.ONE, prerequisite.getMultiplicity());

		assertNull(prerequisite.getTypeId());
	}

	/**
	 * @see Builder#addPrerequisite(String, String, String, Multiplicity, String)
	 */
	@Test
	public void testSinglePrereq4() throws Exception {
		LayerBinding.Builder builder = LayerBinding.newBuilder(CORPUS_ID);

		Multiplicity multiplicity = Multiplicity.ANY;

		builder.addPrerequisite(ALIAS_1, CONTEX_ID_1, LAYER_ID_1, multiplicity, DESCRIPTION_1);

		LayerBinding binding = builder.build();

		assertTrue(binding.containsAlias(ALIAS_1));

		LayerPrerequisite prerequisite = binding.getLayerPrerequisite(ALIAS_1);

		assertEquals(ALIAS_1, prerequisite.getAlias());
		assertEquals(LAYER_ID_1, prerequisite.getLayerId());
		assertEquals(CONTEX_ID_1, prerequisite.getContextId());
		assertEquals(DESCRIPTION_1, prerequisite.getDescription());
		assertEquals(multiplicity, prerequisite.getMultiplicity());

		assertNull(prerequisite.getTypeId());
	}

	/**
	 * @see Builder#addPointer(String, LayerPointer)
	 */
	@Test
	public void testSinglePredefinedPointer() throws Exception {
		LayerBinding.Builder builder = LayerBinding.newBuilder(CORPUS_ID);

		LayerPointer pointer = new LayerBinding.LayerPointerImpl(CONTEX_ID_1, LAYER_ID_1);

		builder.addPointer(ALIAS_1, pointer);

		LayerBinding binding = builder.build();

		assertTrue(binding.containsAlias(ALIAS_1));

		Set<LayerPointer> pointers = binding.getLayerPointers(ALIAS_1);

		assertEquals(1, pointers.size());

		LayerPointer pointer2 = pointers.iterator().next();

		assertSame(pointer, pointer2);
	}

	/**
	 * @see Builder#addPointer(String, String, String)
	 */
	@Test
	public void testSinglePointer() throws Exception {
		LayerBinding.Builder builder = LayerBinding.newBuilder(CORPUS_ID);

		builder.addPointer(ALIAS_1, CONTEX_ID_1, LAYER_ID_1);

		LayerBinding binding = builder.build();

		assertTrue(binding.containsAlias(ALIAS_1));

		Set<LayerPointer> pointers = binding.getLayerPointers(ALIAS_1);

		assertEquals(1, pointers.size());

		LayerPointer pointer = pointers.iterator().next();

		assertEquals(CONTEX_ID_1, pointer.getContextId());
		assertEquals(LAYER_ID_1, pointer.getLayerId());
	}

	/**
	 * @see Builder#addPointers(String, java.util.Collection)
	 */
	@Test
	public void testBatchPointers() throws Exception {
		LayerBinding.Builder builder = LayerBinding.newBuilder(CORPUS_ID);

		Collection<LayerPointer> pointers = new ArrayList<LayerPointer>();
		pointers.add(new LayerBinding.LayerPointerImpl(CONTEX_ID_1, LAYER_ID_1));
		pointers.add(new LayerBinding.LayerPointerImpl(CONTEX_ID_1, LAYER_ID_2));
		pointers.add(new LayerBinding.LayerPointerImpl(CONTEX_ID_2, LAYER_ID_3));
		pointers.add(new LayerBinding.LayerPointerImpl(CONTEX_ID_2, LAYER_ID_4));
		pointers.add(new LayerBinding.LayerPointerImpl(CONTEX_ID_3, LAYER_ID_1));

		builder.addPointers(ALIAS_1, pointers);

		LayerBinding binding = builder.build();

		assertTrue(binding.containsAlias(ALIAS_1));

		Set<LayerPointer> pointers2 = binding.getLayerPointers(ALIAS_1);

		assertEquals(pointers.size(), pointers.size());

		for(LayerPointer pointer : pointers) {
			assertTrue(pointers2.contains(pointer));
		}
	}
}
