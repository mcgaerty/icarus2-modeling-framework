/**
 *
 */
package de.ims.icarus2.model.api.registry;

import static de.ims.icarus2.SharedTestUtils.mockSet;
import static de.ims.icarus2.model.api.ModelTestUtils.mockContainer;
import static de.ims.icarus2.model.api.ModelTestUtils.mockItem;
import static de.ims.icarus2.model.api.ModelTestUtils.mockPosition;
import static de.ims.icarus2.test.TestUtils.randomId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import de.ims.icarus2.model.api.members.container.Container;
import de.ims.icarus2.model.api.members.item.Edge;
import de.ims.icarus2.model.api.members.item.Fragment;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.api.members.structure.Structure;
import de.ims.icarus2.model.api.raster.Position;
import de.ims.icarus2.model.manifest.api.ContainerFlag;
import de.ims.icarus2.model.manifest.api.ContainerManifest;
import de.ims.icarus2.model.manifest.api.ContainerType;
import de.ims.icarus2.model.manifest.api.StructureManifest;
import de.ims.icarus2.model.manifest.api.StructureType;
import de.ims.icarus2.test.ApiGuardedTest;
import de.ims.icarus2.test.guard.ApiGuard;
import de.ims.icarus2.util.IcarusUtils;
import de.ims.icarus2.util.collections.set.DataSet;

/**
 * @author Markus Gärtner
 *
 */
public interface LayerMemberFactoryTest<F extends LayerMemberFactory>
		extends ApiGuardedTest<F>{

	/**
	 * @see de.ims.icarus2.test.ApiGuardedTest#configureApiGuard(de.ims.icarus2.test.guard.ApiGuard)
	 */
	@Override
	default void configureApiGuard(ApiGuard<F> apiGuard) {
		ApiGuardedTest.super.configureApiGuard(apiGuard);

		apiGuard.nullGuard(true);
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.registry.LayerMemberFactory#newContainer(de.ims.icarus2.model.manifest.api.ContainerManifestBase, de.ims.icarus2.model.api.members.container.Container, long)}.
	 */
	@SuppressWarnings("boxing")
	@TestFactory
	default Stream<DynamicTest> testNewContainer() {
		return Stream.of(ContainerType.values())
			.map(containerType -> dynamicTest(containerType.name(), () -> {
				ContainerManifest manifest = mock(ContainerManifest.class);
				when(manifest.getContainerType()).thenReturn(containerType);

				Container host = mock(Container.class);
				when((ContainerManifest)host.getManifest()).thenReturn(manifest);

				// Make sure we have a single static base container (some implementations might need that)
				ContainerManifest baseManifest = mock(ContainerManifest.class);
				when(baseManifest.isContainerFlagSet(ContainerFlag.NON_STATIC)).thenReturn(Boolean.FALSE);
				Container baseContainer = mockContainer();
				when((ContainerManifest)baseContainer.getManifest()).thenReturn(baseManifest);
				DataSet<Container> baseContainers = mockSet(baseContainer);

				long id = randomId();

				Container container = create().newContainer(manifest, host, baseContainers, null, id);
				assertNotNull(container);

				assertTrue(containerType.isCompatibleWith(container.getContainerType()));
				assertSame(manifest, container.getManifest());
				assertSame(host, container.getContainer());
				assertSame(baseContainers, container.getBaseContainers());
				assertNull(container.getBoundaryContainer());
				assertEquals(id, container.getId());
			}));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.registry.LayerMemberFactory#newStructure(de.ims.icarus2.model.manifest.api.StructureManifest, de.ims.icarus2.model.api.members.container.Container, long)}.
	 */
	@SuppressWarnings("boxing")
	@TestFactory
	default Stream<DynamicTest> testNewStructure() {
		return Stream.of(StructureType.values())
				.flatMap(structureType -> Stream.of(ContainerType.values())
						.map(containerType -> dynamicTest(containerType+"+"+structureType, () -> {
					StructureManifest manifest = mock(StructureManifest.class);
					when(manifest.getContainerType()).thenReturn(containerType);
					when(manifest.getStructureType()).thenReturn(structureType);

					Container host = mock(Container.class);
					when((StructureManifest)host.getManifest()).thenReturn(manifest);

					// Make sure we have a single static base container (some implementations might need that)
					ContainerManifest baseManifest = mock(ContainerManifest.class);
					when(baseManifest.isContainerFlagSet(ContainerFlag.NON_STATIC)).thenReturn(Boolean.FALSE);
					Container baseContainer = mockContainer();
					when((ContainerManifest)baseContainer.getManifest()).thenReturn(baseManifest);
					DataSet<Container> baseContainers = mockSet(baseContainer);

					long id = randomId();

					Structure structure = create().newStructure(manifest, host, baseContainers, null, id);
					assertNotNull(structure);

					assertTrue(structureType.isCompatibleWith(structure.getStructureType()));
					assertSame(manifest, structure.getManifest());
					assertSame(host, structure.getContainer());
					assertSame(baseContainers, structure.getBaseContainers());
					assertNull(structure.getBoundaryContainer());
					assertEquals(id, structure.getId());
				})));
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.registry.LayerMemberFactory#newItem(de.ims.icarus2.model.api.members.container.Container, long)}.
	 */
	@Test
	default void testNewItem() {
		Container host = mock(Container.class);
		long id = randomId();

		Item item = create().newItem(host, id);

		assertSame(host, item.getContainer());
		assertEquals(id, item.getId());
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.registry.LayerMemberFactory#newEdge(de.ims.icarus2.model.api.members.structure.Structure, long)}.
	 */
	@Test
	default void testNewEdgeStructureLong() {
		Structure host = mock(Structure.class);
		long id = randomId();

		Edge edge = create().newEdge(host, id);

		assertSame(host, edge.getContainer());
		assertSame(host, edge.getStructure());
		assertEquals(IcarusUtils.UNSET_LONG, edge.getId());
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.registry.LayerMemberFactory#newEdge(de.ims.icarus2.model.api.members.structure.Structure, long, de.ims.icarus2.model.api.members.item.Item, de.ims.icarus2.model.api.members.item.Item)}.
	 */
	@Test
	default void testNewEdgeStructureLongItemItem() {
		Structure host = mock(Structure.class);
		Item source = mockItem();
		Item target = mockItem();
		long id = randomId();

		Edge edge = create().newEdge(host, id, source, target);

		assertSame(host, edge.getContainer());
		assertSame(host, edge.getStructure());
		assertSame(source, edge.getSource());
		assertSame(target, edge.getTarget());
		assertEquals(IcarusUtils.UNSET_LONG, edge.getId());
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.registry.LayerMemberFactory#newFragment(de.ims.icarus2.model.api.members.container.Container, long, de.ims.icarus2.model.api.members.item.Item)}.
	 */
	@Test
	default void testNewFragmentContainerLongItem() {
		Container host = mock(Container.class);
		long id = randomId();
		Item item = mockItem();

		Fragment fragment = create().newFragment(host, id, item);

		assertSame(host, fragment.getContainer());
		assertSame(item, fragment.getItem());
		assertEquals(id, fragment.getId());
	}

	/**
	 * Test method for {@link de.ims.icarus2.model.api.registry.LayerMemberFactory#newFragment(de.ims.icarus2.model.api.members.container.Container, long, de.ims.icarus2.model.api.members.item.Item, de.ims.icarus2.model.api.raster.Position, de.ims.icarus2.model.api.raster.Position)}.
	 */
	@Test
	default void testNewFragmentContainerLongItemPositionPosition() {
		Container host = mock(Container.class);
		long id = randomId();
		Item item = mockItem();
		Position position1 = mockPosition();
		Position position2 = mockPosition();

		Fragment fragment = create().newFragment(host, id, item, position1, position2);

		assertSame(host, fragment.getContainer());
		assertSame(item, fragment.getItem());
		assertEquals(id, fragment.getId());
		assertSame(position1, fragment.getFragmentBegin());
		assertSame(position2, fragment.getFragmentEnd());
	}

}
