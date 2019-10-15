/**
 *
 */
package de.ims.icarus2.util.stat;

import static de.ims.icarus2.test.TestTags.RANDOMIZED;
import static de.ims.icarus2.test.TestUtils.RUNS;
import static de.ims.icarus2.test.TestUtils.displayString;
import static de.ims.icarus2.test.TestUtils.random;
import static de.ims.icarus2.test.TestUtils.randomIntStream;
import static de.ims.icarus2.util.lang.Primitives._int;
import static de.ims.icarus2.util.lang.Primitives.strictToInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import de.ims.icarus2.test.GenericTest;
import de.ims.icarus2.test.TestSettings;
import de.ims.icarus2.test.annotations.Provider;
import de.ims.icarus2.util.stat.Histogram.ArrayHistogram;

/**
 * @author Markus Gärtner
 *
 */
class HistogramTest {

	/**
	 * Test method for {@link de.ims.icarus2.util.stat.Histogram#fixedHistogram(int)}.
	 */
	@Test
	void testFixedHistogram() {
		Histogram hist = Histogram.fixedHistogram(100);
		assertNotNull(hist);
		assertEquals(100, hist.bins());
	}

	interface BaseTest<H extends Histogram> extends GenericTest<H> {

		/**
		 * @see de.ims.icarus2.test.Testable#createTestInstance(de.ims.icarus2.test.TestSettings)
		 */
		@Provider
		@Override
		default H createTestInstance(TestSettings settings) {
			return settings.process(createForBins(10));
		}

		@Provider
		H createForBins(int bins);

		@Provider
		H createForRange(long from, long to);

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#bins()}.
		 */
		@Tag(RANDOMIZED)
		@TestFactory
		default Stream<DynamicTest> testBins() {
			return randomIntStream(100)
					.distinct()
					.limit(RUNS)
					.map(i -> i+1)
					.mapToObj(bins -> dynamicTest(String.valueOf(bins), () -> {
						H histogram = createForBins(bins);
						assertEquals(bins, histogram.bins());
					}));
		}

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#lowerBound(int)}.
		 */
		@Tag(RANDOMIZED)
		@TestFactory
		default Stream<DynamicTest> testLowerBound() {
			return random()
					.longs(Long.MIN_VALUE/2, Long.MAX_VALUE/2)
					.distinct()
					.limit(RUNS)
					.mapToObj(lowerBound -> dynamicTest(displayString(lowerBound), () -> {
						H histogram = createForRange(lowerBound, lowerBound+100);
						assertEquals(lowerBound, histogram.lowerBound(0));
						assertEquals(lowerBound+99, histogram.lowerBound(99));
					}));
		}

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#higherBound(int)}.
		 */
		@Tag(RANDOMIZED)
		@TestFactory
		default Stream<DynamicTest> testHigherBound() {
			return random()
					.longs(Long.MIN_VALUE/2, Long.MAX_VALUE/2)
					.distinct()
					.limit(RUNS)
					.mapToObj(higherBound -> dynamicTest(displayString(higherBound), () -> {
						H histogram = createForRange(higherBound-100, higherBound);
						assertEquals(higherBound, histogram.higherBound(100));
						assertEquals(higherBound-100, histogram.higherBound(0));
					}));
		}

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#entries()}.
		 */
		@Test
		default void testEntriesEmpty() {
			assertEquals(0, createForBins(100).entries());
		}

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#entries()}.
		 */
		@RepeatedTest(RUNS)
		default void testEntries() {
			Histogram hist = createForBins(100);

			int count = random(1, 1000);
			for (int i = 0; i < count; i++) {
				hist.accept(10);
			}

			assertEquals(count, hist.entries());
		}

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#freq(int)}.
		 */
		@Test
		default void testFreqEmpty() {
			Histogram hist = createForBins(100);
			for (int i = 0; i < 100; i++) {
				assertEquals(0, hist.freq(i));
			}
		}

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#freq(int)}.
		 */
		@TestFactory
		default Stream<DynamicNode> testFreq() {
			return Stream.of(
					dynamicContainer("single bin", IntStream.range(0, 100)
							.distinct()
							.limit(RUNS)
							.mapToObj(bin -> dynamicTest(String.valueOf(bin), () -> {
								H hist = createForBins(100);
								int count = random(10, 1000);
								long value = hist.lowerBound(bin);
								// Fill histogram
								for (int i = 0; i < count; i++) {
									hist.accept(value);
								}
								// Verify
								for (int i = 0; i < hist.bins(); i++) {
									int target = i==bin ? count : 0;
									assertEquals(target, hist.freq(i));
								}
							}))),
					dynamicContainer("equal distribution", IntStream.range(1, 10)
							.distinct()
							.limit(RUNS)
							.mapToObj(count -> dynamicTest(String.valueOf(count), () -> {
								H hist = createForBins(100);
								// Fill histogram
								for (int i = 0; i < hist.bins(); i++) {
									long value = hist.lowerBound(i);
									for (int j = 0; j < count; j++) {
										hist.accept(value);
									}
								}
								// Verify
								for (int i = 0; i < hist.bins(); i++) {
									assertEquals(count, hist.freq(i));
								}
							}))),
					dynamicContainer("random distribution", IntStream.rangeClosed(1, RUNS)
							.mapToObj(run -> dynamicTest(String.format("run %d of %d",
									_int(run), _int(RUNS)), () -> {
								H hist = createForBins(100);
								int[] counts = new int[100];
								// Fill histogram
								for (int i = 0; i < hist.bins(); i++) {
									int count = random(0, 100);
									int value = strictToInt(hist.lowerBound(i));
									for (int j = 0; j < count; j++) {
										hist.accept(value);
									}
									counts[i] = count;
								}
								// Verify
								for (int i = 0; i < hist.bins(); i++) {
									assertEquals(counts[i], hist.freq(i));
								}
							})))
			);
		}

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#bin(long)}.
		 */
		@TestFactory
		default Stream<DynamicNode> testBin() {
			return Stream.of(
						dynamicTest("fixed 0 offset", () -> {
							int bins = random(10, 100);
							H hist = createForBins(bins);
							for (int i = 0; i < bins; i++) {
								assertEquals(i, hist.bin(i));
							}
						}),
						dynamicTest("random offset", () -> {
							int bins = random(10, 100);
							long offset = random(Long.MIN_VALUE/2, Long.MAX_VALUE/2);
							H hist = createForRange(offset, offset+bins-1);
							for (int i = 0; i < bins; i++) {
								assertEquals(i, hist.bin(i+offset));
							}
						})
			);
		}

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#average()}.
		 */
		@TestFactory
		default Stream<DynamicNode> testAverage() {
			return Stream.of(
					dynamicTest("empty", () -> {
						assertEquals(Double.NaN, createForBins(100).average());
					}),
					dynamicContainer("single bin", IntStream.range(0, 100)
							.distinct()
							.limit(RUNS)
							.mapToObj(bin -> dynamicTest("bin "+bin, () -> {
								H hist = createForBins(100);
								int count = random(10, 1000);
								long value = hist.lowerBound(bin);
								// Fill histogram
								for (int i = 0; i < count; i++) {
									hist.accept(value);
								}
								// Verify
								assertEquals(value, hist.average());
							}))),
					dynamicContainer("equal distribution", IntStream.range(0, 100)
							.distinct()
							.limit(RUNS)
							.mapToObj(run -> dynamicTest(String.format("run %d of %d",
									_int(run), _int(RUNS)), () -> {
								H hist = createForBins(100);
								int count = random(10, 1000);
								long sum = 0L;
								// Fill histogram
								for (int i = 0; i < hist.bins(); i++) {
									int value = strictToInt(hist.lowerBound(i));
									for (int j = 0; j < count; j++) {
										hist.accept(value);
									}
									sum += value*count;
								}
								// Verify
								assertEquals((double)sum/hist.entries(), hist.average());
							}))),
					dynamicContainer("random distribution", IntStream.range(0, 100)
							.distinct()
							.limit(RUNS)
							.mapToObj(run -> dynamicTest(String.format("run %d of %d",
									_int(run), _int(RUNS)), () -> {
								H hist = createForBins(100);
								long sum = 0L;
								// Fill histogram
								for (int bin = 0; bin < hist.bins(); bin++) {
									int count = random(10, 1000);
									int value = strictToInt(hist.lowerBound(bin));
									for (int j = 0; j < count; j++) {
										hist.accept(value);
									}
									sum += value*count;
								}
								// Verify
								assertEquals((double)sum/hist.entries(), hist.average());
							})))
			);
		}

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#min()}.
		 */
		@TestFactory
		default Stream<DynamicNode> testMin() {
			return Stream.of(
					dynamicTest("no offset", () -> {
						int bins = random(10, 100);
						H hist = createForBins(bins);
						assertEquals(0, hist.min());
					}),
					dynamicTest("negative offset", () -> {
						int bins = random(10, 100);
						long offset = random(Long.MIN_VALUE, 0);
						H hist = createForRange(offset, offset+bins-1);
						assertEquals(offset, hist.min());
					}),
					dynamicTest("positive offset", () -> {
						int bins = random(10, 100);
						long offset = random(1, Long.MAX_VALUE/2);
						H hist = createForRange(offset, offset+bins-1);
						assertEquals(offset, hist.min());
					})
			);
		}

		/**
		 * Test method for {@link de.ims.icarus2.util.stat.Histogram#max()}.
		 */
		@TestFactory
		default Stream<DynamicNode> testMax() {
			return Stream.of(
					dynamicTest("no offset", () -> {
						int bins = random(10, 100);
						H hist = createForBins(bins);
						assertEquals(bins-1, hist.max());
					}),
					dynamicTest("negative offset", () -> {
						int bins = random(10, 100);
						long offset = random(Long.MIN_VALUE, -100);
						H hist = createForRange(offset, offset+bins-1);
						assertEquals(offset+bins-1, hist.max());
					}),
					dynamicTest("positive offset", () -> {
						int bins = random(10, 100);
						long offset = random(1, Long.MAX_VALUE/2);
						H hist = createForRange(offset, offset+bins-1);
						assertEquals(offset+bins-1, hist.max());
					})
			);
		}

	}

	@Nested
	class ForArrayHistogram implements BaseTest<ArrayHistogram> {

		/**
		 * @see de.ims.icarus2.test.TargetedTest#getTestTargetClass()
		 */
		@Override
		public Class<?> getTestTargetClass() {
			return ArrayHistogram.class;
		}

		/**
		 * @see de.ims.icarus2.util.stat.HistogramTest.BaseTest#createForBins(int)
		 */
		@Override
		public ArrayHistogram createForBins(int bins) {
			return ArrayHistogram.fixed(bins);
		}

		/**
		 * @see de.ims.icarus2.util.stat.HistogramTest.BaseTest#createForRange(int, int)
		 */
		@Override
		public ArrayHistogram createForRange(long from, long to) {
			return ArrayHistogram.range(from, to);
		}

	}
}