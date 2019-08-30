/**
 *
 */
package de.ims.icarus2.model.standard.driver.virtual;

import static de.ims.icarus2.SharedTestUtils.assertIcarusException;
import static de.ims.icarus2.model.api.ModelTestUtils.mockItems;
import static de.ims.icarus2.test.TestUtils.random;
import static de.ims.icarus2.util.IcarusUtils.MAX_INTEGER_INDEX;
import static de.ims.icarus2.util.collections.ArrayUtils.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.ims.icarus2.GlobalErrorCode;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.api.members.item.manager.ItemListTest;
import de.ims.icarus2.test.TestSettings;

/**
 * @author Markus Gärtner
 *
 */
class DefaultItemListTest implements ItemListTest<DefaultItemList> {

	@Override
	public DefaultItemList createFilled(Item... items) {
		return new DefaultItemList(Arrays.asList(items));
	}

	@Override
	public Class<?> getTestTargetClass() {
		return DefaultItemList.class;
	}

	@Override
	public DefaultItemList createTestInstance(TestSettings settings) {
		return settings.process(new DefaultItemList());
	}

	@Nested
	class Constructors {

		/**
		 * Test method for {@link de.ims.icarus2.model.standard.driver.virtual.DefaultItemList#DefaultItemList()}.
		 */
		@Test
		void testDefaultItemList() {
			assertNotNull(new DefaultItemList());
		}

		/**
		 * Test method for {@link de.ims.icarus2.model.standard.driver.virtual.DefaultItemList#DefaultItemList(java.util.Collection)}.
		 */
		@Test
		void testDefaultItemListCollectionOfQextendsItem() {
			Item[] items = mockItems(random(10, 20));
			DefaultItemList list = new DefaultItemList(asList(items));

			assertEquals(items.length, list.getItemCount());
			for (int i = 0; i < items.length; i++) {
				assertSame(items[i], list.getItemAt(i));
			}
		}

		/**
		 * Test method for {@link de.ims.icarus2.model.standard.driver.virtual.DefaultItemList#DefaultItemList(int)}.
		 */
		@ParameterizedTest
		@ValueSource(ints = {1, 10, 10_000})
		void testDefaultItemListInt(int capacity) {
			DefaultItemList list = new DefaultItemList(capacity);
			assertEquals(capacity, list.capacity());
		}

		/**
		 * Test method for {@link de.ims.icarus2.model.standard.driver.virtual.DefaultItemList#DefaultItemList(int)}.
		 */
		@ParameterizedTest
		@ValueSource(ints = {-1, 0, MAX_INTEGER_INDEX+1})
		void testDefaultItemListIntInvalidCapacity(int capacity) {
			assertIcarusException(GlobalErrorCode.INVALID_INPUT,
					() -> new DefaultItemList(capacity));
		}

	}

}
