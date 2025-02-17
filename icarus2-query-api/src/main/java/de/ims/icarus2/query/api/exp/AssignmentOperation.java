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

import static java.util.Objects.requireNonNull;

import de.ims.icarus2.util.MutablePrimitives.MutableBoolean;
import de.ims.icarus2.util.MutablePrimitives.Primitive;

/**
 * @author Markus Gärtner
 *
 */
public class AssignmentOperation {

	public static Expression<Primitive<Boolean>> assignment(
			Expression<?> source, Assignable<?> target, boolean optional) {
		//TODO make switch on result type later to forward to primitive specializations
		return new ObjectAssignment(source, target, optional);
	}

	static final class ObjectAssignment implements Expression<Primitive<Boolean>> {

		private final MutableBoolean value = new MutableBoolean();
		private final Expression<?> source;
		private final Assignable<?> target;
		private final boolean optional;

		ObjectAssignment(Expression<?> source, Assignable<?> target, boolean optional) {
			this.source = requireNonNull(source);
			this.target = requireNonNull(target);
			this.optional = optional;
		}

		@Override
		public TypeInfo getResultType() { return target.getResultType(); }

		@Override
		public Primitive<Boolean> compute() {
			value.setBoolean(computeAsBoolean());
			return value;
		}

		@Override
		public boolean computeAsBoolean() {
			Object value = source.compute();
			target.assign(value);
			return optional || value!=null;
		}

		@Override
		public Expression<Primitive<Boolean>> duplicate(EvaluationContext context) {
			return new ObjectAssignment(context.duplicate(source), context.duplicate(target), optional);
		}

		@Override
		public Expression<Primitive<Boolean>> optimize(EvaluationContext context) {
			Expression<?> newSource = context.optimize(source);

			/*
			 *  If we produce a permanent null assignment, bail and wrap
			 *  the 'optional' flag as result literal.
			 */
			if(newSource.isConstant() && newSource.compute()==null) {
				return Literals.of(optional);
			}

			return new ObjectAssignment(newSource, target, optional);
		}
	}
}
