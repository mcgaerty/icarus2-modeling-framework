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
/**
 *
 */
package de.ims.icarus2.query.api.exp;

import static de.ims.icarus2.util.lang.Primitives._int;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.function.Executable;

import de.ims.icarus2.ErrorCode;
import de.ims.icarus2.query.api.QueryException;
import de.ims.icarus2.query.api.exp.Expression.BooleanListExpression;
import de.ims.icarus2.query.api.exp.Expression.FloatingPointListExpression;
import de.ims.icarus2.query.api.exp.Expression.IntegerListExpression;
import de.ims.icarus2.query.api.exp.Expression.ListExpression;
import de.ims.icarus2.util.MutablePrimitives.MutableBoolean;
import de.ims.icarus2.util.MutablePrimitives.MutableDouble;
import de.ims.icarus2.util.MutablePrimitives.MutableLong;
import de.ims.icarus2.util.MutablePrimitives.Primitive;

/**
 * @author Markus Gärtner
 *
 */
public class ExpressionTestUtils {

	public static QueryException assertQueryException(ErrorCode errorCode, Executable executable) {
		QueryException exception = assertThrows(QueryException.class, executable);
		assertEquals(errorCode, exception.getErrorCode());
		return exception;
	}

	public static EvaluationContext mockContext() {
		EvaluationContext ctx = mock(EvaluationContext.class);
		when(ctx.duplicate((Expression<?>)any(Expression.class)))
			.thenAnswer(invoc -> ((Expression<?>)invoc.getArgument(0)).duplicate(ctx));
		when(ctx.duplicate((Assignable<?>)any(Assignable.class)))
			.thenAnswer(invoc -> ((Assignable<?>)invoc.getArgument(0)).duplicate(ctx));
		when(ctx.optimize((Expression<?>)any(Expression.class)))
			.thenAnswer(invoc -> ((Expression<?>)invoc.getArgument(0)).optimize(ctx));

		return ctx;
	}

	// Simple expression assertions

	@SuppressWarnings("unchecked")
	public static <T> void assertExpression(Expression<?> exp, EvaluationContext ctx, T expected,
			BiPredicate<T, T> equality) {
		assertThat(exp.compute()).satisfies(result -> equality.test((T) result, expected));

		assertThat(ctx.duplicate(exp).compute()).satisfies(result -> equality.test((T) result, expected));

		assertThat(ctx.optimize(exp).compute()).satisfies(result -> equality.test((T) result, expected));
	}

	public static void assertExpression(Expression<?> exp, EvaluationContext ctx, long expected) {
		assertThat(exp.computeAsLong()).isEqualTo(expected);

		assertThat(ctx.duplicate(exp).computeAsLong()).isEqualTo(expected);

		assertThat(ctx.optimize(exp).computeAsLong()).isEqualTo(expected);
	}

	public static void assertExpression(Expression<?> exp, EvaluationContext ctx, double expected) {
		assertThat(exp.computeAsDouble()).isEqualTo(expected);

		assertThat(ctx.duplicate(exp).computeAsDouble()).isEqualTo(expected);

		assertThat(ctx.optimize(exp).computeAsDouble()).isEqualTo(expected);
	}

	public static void assertExpression(Expression<?> exp, EvaluationContext ctx, boolean expected) {
		assertThat(exp.computeAsBoolean()).isEqualTo(expected);

		assertThat(ctx.duplicate(exp).computeAsBoolean()).isEqualTo(expected);

		assertThat(ctx.optimize(exp).computeAsBoolean()).isEqualTo(expected);
	}

	// Liste expression assertions

	@SuppressWarnings("unchecked")
	public static <T> void assertListExpression(ListExpression<?,T> exp, EvaluationContext ctx,
			BiPredicate<T, T> equality, T...expected) {
		ListExpression<?,?> optimized = (ListExpression<?, ?>) ctx.optimize(exp);
		ListExpression<?,?> duplicated = (ListExpression<?, ?>) ctx.duplicate(exp);

		assertThat(exp.size()).isEqualTo(expected.length);
		assertThat(optimized.size()).isEqualTo(exp.size());
		assertThat(duplicated.size()).isEqualTo(exp.size());

		for (int i = 0; i < expected.length; i++) {
			T target = expected[i];
			assertThat(exp.get(i))
				.as("Mismatch in original expression at %d", _int(i))
				.satisfies(item -> equality.test(item, target));
			assertThat(duplicated.get(i))
				.as("Mismatch in duplicate at %d", _int(i))
				.satisfies(item -> equality.test((T)item, target));
			assertThat(optimized.get(i))
				.as("Mismatch in optimized expression at %d", _int(i))
				.satisfies(item -> equality.test((T)item, target));
		}
	}

	public static void assertListExpression(IntegerListExpression<?> exp, EvaluationContext ctx, long...expected) {
		IntegerListExpression<?> optimized = (IntegerListExpression<?>) ctx.optimize(exp);
		IntegerListExpression<?> duplicated = (IntegerListExpression<?>) ctx.duplicate(exp);

		assertThat(exp.size()).isEqualTo(expected.length);
		assertThat(optimized.size()).isEqualTo(exp.size());
		assertThat(duplicated.size()).isEqualTo(exp.size());

		for (int i = 0; i < expected.length; i++) {
			assertThat(exp.getAsLong(i))
				.as("Mismatch in original expression at %d", _int(i))
				.isEqualTo(expected[i]);
			assertThat(duplicated.getAsLong(i))
				.as("Mismatch in duplicate at %d", _int(i))
				.isEqualTo(expected[i]);
			assertThat(optimized.getAsLong(i))
				.as("Mismatch in optimized expression at %d", _int(i))
				.isEqualTo(expected[i]);
		}
	}

	public static void assertListExpression(FloatingPointListExpression<?> exp, EvaluationContext ctx, double...expected) {
		FloatingPointListExpression<?> optimized = (FloatingPointListExpression<?>) ctx.optimize(exp);
		FloatingPointListExpression<?> duplicated = (FloatingPointListExpression<?>) ctx.duplicate(exp);

		assertThat(exp.size()).isEqualTo(expected.length);
		assertThat(optimized.size()).isEqualTo(exp.size());
		assertThat(duplicated.size()).isEqualTo(exp.size());

		for (int i = 0; i < expected.length; i++) {
			assertThat(exp.getAsDouble(i))
				.as("Mismatch in original expression at %d", _int(i))
				.isEqualTo(expected[i]);
			assertThat(duplicated.getAsDouble(i))
				.as("Mismatch in duplicate at %d", _int(i))
				.isEqualTo(expected[i]);
			assertThat(optimized.getAsDouble(i))
				.as("Mismatch in optimized expression at %d", _int(i))
				.isEqualTo(expected[i]);
		}
	}

	public static void assertListExpression(BooleanListExpression<?> exp, EvaluationContext ctx, boolean...expected) {
		BooleanListExpression<?> optimized = (BooleanListExpression<?>) ctx.optimize(exp);
		BooleanListExpression<?> duplicated = (BooleanListExpression<?>) ctx.duplicate(exp);

		assertThat(exp.size()).isEqualTo(expected.length);
		assertThat(optimized.size()).isEqualTo(exp.size());
		assertThat(duplicated.size()).isEqualTo(exp.size());

		for (int i = 0; i < expected.length; i++) {
			assertThat(exp.getAsBoolean(i))
				.as("Mismatch in original expression at %d", _int(i))
				.isEqualTo(expected[i]);
			assertThat(duplicated.getAsBoolean(i))
				.as("Mismatch in duplicate at %d", _int(i))
				.isEqualTo(expected[i]);
			assertThat(optimized.getAsBoolean(i))
				.as("Mismatch in optimized expression at %d", _int(i))
				.isEqualTo(expected[i]);
		}
	}

	static Object dummy(String toStringValue) {
		return new Object() {
			@Override
			public String toString() { return toStringValue; }
		};
	}

	static Expression<Object> generic(String toStringValue) {
		Object dummy = dummy(toStringValue);
		return new Expression<Object>() {

			@Override
			public TypeInfo getResultType() { return TypeInfo.GENERIC; }

			@Override
			public Object compute() { return dummy; }

			@Override
			public Expression<Object> duplicate(EvaluationContext context) { return this; }

			@Override
			public boolean isConstant() { return true; }
		};
	}

	static Expression<CharSequence> fixed(String text) {
		return new Expression<CharSequence>() {
			final CharSequence value = text;

			@Override
			public Expression<CharSequence> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public CharSequence compute() { return value; }

			@Override
			public TypeInfo getResultType() { return TypeInfo.TEXT; }
		};
	}

	static Expression<CharSequence> optimizable(String text) {
		return new Expression<CharSequence>() {
			final CharSequence value = text;

			@Override
			public Expression<CharSequence> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public CharSequence compute() { return value; }

			@Override
			public Expression<CharSequence> optimize(EvaluationContext context) {
				return Literals.of(value);
			}

			@Override
			public TypeInfo getResultType() { return TypeInfo.TEXT; }
		};
	}

	static Expression<?> optimizable(long value) {
		return new Expression<Primitive<Long>>() {
			final MutableLong buffer = new MutableLong(value);

			@Override
			public Expression<Primitive<Long>> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public Primitive<Long> compute() { return buffer; }

			@Override
			public TypeInfo getResultType() { return TypeInfo.INTEGER; }

			@Override
			public long computeAsLong() { return value; }

			@Override
			public double computeAsDouble() { return value; }

			@Override
			public Expression<Primitive<Long>> optimize(EvaluationContext context) {
				return Literals.of(value);
			}
		};
	}

	static Expression<Primitive<Boolean>> optimizable(boolean value) {
		return new Expression<Primitive<Boolean>>() {
			final MutableBoolean buffer = new MutableBoolean(value);

			@Override
			public Expression<Primitive<Boolean>> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public Primitive<Boolean> compute() { return buffer; }

			@Override
			public TypeInfo getResultType() { return TypeInfo.BOOLEAN; }

			@Override
			public boolean computeAsBoolean() { return value; }

			@Override
			public Expression<Primitive<Boolean>> optimize(EvaluationContext context) {
				return Literals.of(value);
			}
		};
	}

	static Expression<?> optimizable(double value) {
		return new Expression<Primitive<Double>>() {
			final MutableDouble buffer = new MutableDouble(value);

			@Override
			public Expression<Primitive<Double>> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public Primitive<Double> compute() { return buffer; }

			@Override
			public TypeInfo getResultType() { return TypeInfo.FLOATING_POINT; }

			@Override
			public long computeAsLong() { throw EvaluationUtils.forUnsupportedCast(
					TypeInfo.FLOATING_POINT, TypeInfo.INTEGER); }

			@Override
			public double computeAsDouble() { return value; }

			@Override
			public Expression<Primitive<Double>> optimize(EvaluationContext context) {
				return Literals.of(value);
			}
		};
	}

	static IntegerListExpression<long[]> optimizableLongs(long...array) {
		return new IntegerListExpression<long[]>() {
			private final MutableLong value = new MutableLong();

			@Override
			public IntegerListExpression<long[]> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public long[] compute() { return array; }

			@Override
			public boolean isFixedSize() { return true; }

			@Override
			public TypeInfo getResultType() { return TypeInfo.of(long[].class, true); }

			@Override
			public IntegerListExpression<long[]> optimize(EvaluationContext context) {
				return ArrayLiterals.of(array);
			}

			@Override
			public int size() { return array.length; }

			@Override
			public Primitive<Long> get(int index) {
				value.setLong(array[index]);
				return value;
			}

			@Override
			public long getAsLong(int index) { return array[index]; }
		};
	}

	static FloatingPointListExpression<double[]> optimizableDoubles(double...array) {
		return new FloatingPointListExpression<double[]>() {
			private final MutableDouble value = new MutableDouble();

			@Override
			public FloatingPointListExpression<double[]> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public double[] compute() { return array; }

			@Override
			public boolean isFixedSize() { return true; }

			@Override
			public TypeInfo getResultType() { return TypeInfo.of(long[].class, true); }

			@Override
			public FloatingPointListExpression<double[]> optimize(EvaluationContext context) {
				return ArrayLiterals.of(array);
			}

			@Override
			public int size() { return array.length; }

			@Override
			public Primitive<Double> get(int index) {
				value.setDouble(array[index]);
				return value;
			}

			@Override
			public double getAsDouble(int index) { return array[index]; }
		};
	}

	static <T> Expression<CharSequence> dynamicText(Supplier<T> dummy) {
		Expression<T> expression = new Expression<T>() {

			@Override
			public TypeInfo getResultType() { return TypeInfo.of(dummy.getClass()); }

			@Override
			public Expression<T> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public T compute() { return dummy.get(); }
		};

		return Conversions.toText(expression);
	}

	@SuppressWarnings("unchecked")
	static <T> Expression<T> raw(Object dummy) {
		return (Expression<T>) new Expression<Object>() {

			@Override
			public TypeInfo getResultType() { return TypeInfo.of(dummy.getClass()); }

			@Override
			public Expression<Object> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public Object compute() { return dummy; }
		};
	}

	static Expression<?> dynamic(LongSupplier source) {
		return new Expression<Primitive<? extends Number>>() {

			final MutableLong value = new MutableLong();

			@Override
			public TypeInfo getResultType() { return TypeInfo.INTEGER; }

			@Override
			public Expression<Primitive<? extends Number>> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public Primitive<? extends Number> compute() {
				value.setLong(computeAsLong());
				return value;
			}

			@Override
			public long computeAsLong() { return source.getAsLong(); }

			@Override
			public double computeAsDouble() { return computeAsLong(); }
		};
	}

	static Expression<?> dynamic(DoubleSupplier source) {
		return new Expression<Primitive<? extends Number>>() {

			final MutableDouble value = new MutableDouble();

			@Override
			public TypeInfo getResultType() { return TypeInfo.FLOATING_POINT; }

			@Override
			public Expression<Primitive<? extends Number>> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public Primitive<? extends Number> compute() {
				value.setDouble(computeAsDouble());
				return value;
			}

			@Override
			public long computeAsLong() { throw EvaluationUtils.forUnsupportedCast(
					TypeInfo.FLOATING_POINT, TypeInfo.INTEGER); }

			@Override
			public double computeAsDouble() { return source.getAsDouble(); }
		};
	}

	static Expression<Primitive<Boolean>> dynamic(BooleanSupplier source) {
		return new Expression<Primitive<Boolean>>() {

			final MutableBoolean value = new MutableBoolean();

			@Override
			public TypeInfo getResultType() { return TypeInfo.BOOLEAN; }

			@Override
			public Expression<Primitive<Boolean>> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public Primitive<Boolean> compute() {
				value.setBoolean(computeAsBoolean());
				return value;
			}

			@Override
			public boolean computeAsBoolean() { return source.getAsBoolean(); }
		};
	}

	static Expression<CharSequence> dynamic(Supplier<? extends CharSequence> source) {
		return new Expression<CharSequence>() {
			@Override
			public Expression<CharSequence> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public CharSequence compute() { return source.get(); }

			@Override
			public TypeInfo getResultType() { return TypeInfo.TEXT; }
		};
	}

	static <T> Expression<T> dynamicGeneric(Supplier<T> source) {
		return new Expression<T>() {
			@Override
			public Expression<T> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public T compute() { return source.get(); }

			@Override
			public TypeInfo getResultType() { return TypeInfo.GENERIC; }
		};
	}

	static IntegerListExpression<long[]> dynamicLongs(Supplier<long[]> source) {
		return new IntegerListExpression<long[]>() {
			private final MutableLong value = new MutableLong();

			@Override
			public IntegerListExpression<long[]> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public long[] compute() { return source.get(); }

			@Override
			public boolean isFixedSize() { return false; }

			@Override
			public TypeInfo getResultType() { return TypeInfo.of(long[].class, true); }

			@Override
			public int size() { return source.get().length; }

			@Override
			public Primitive<Long> get(int index) {
				value.setLong(getAsLong(index));
				return value;
			}

			@Override
			public long getAsLong(int index) { return source.get()[index]; }
		};
	}

	static FloatingPointListExpression<double[]> dynamicDoubles(Supplier<double[]> source) {
		return new FloatingPointListExpression<double[]>() {
			private final MutableDouble value = new MutableDouble();

			@Override
			public FloatingPointListExpression<double[]> duplicate(EvaluationContext context) {
				return this;
			}

			@Override
			public double[] compute() { return source.get(); }

			@Override
			public boolean isFixedSize() { return false; }

			@Override
			public TypeInfo getResultType() { return TypeInfo.of(double[].class, true); }

			@Override
			public int size() { return source.get().length; }

			@Override
			public Primitive<Double> get(int index) {
				value.setDouble(getAsDouble(index));
				return value;
			}

			@Override
			public double getAsDouble(int index) { return source.get()[index]; }
		};
	}
}
