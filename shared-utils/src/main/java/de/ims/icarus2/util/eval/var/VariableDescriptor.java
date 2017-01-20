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
 *
 */
package de.ims.icarus2.util.eval.var;

import static java.util.Objects.requireNonNull;

/**
 * @author Markus Gärtner
 *
 */
public class VariableDescriptor {

	public static enum Mode {
		IN(true, false),
		OUT(false, true),
		IN_OUT(true, true),
		;

		private final boolean in, out;

		private Mode(boolean in, boolean out) {
			this.in = in;
			this.out = out;
		}

		public boolean isIn() {
			return in;
		}

		public boolean isOut() {
			return out;
		}
	}

	private final String name;
	private final boolean nullable;
	private final Mode mode;

	private final Class<?> namespaceClass;


	public VariableDescriptor(String name, Class<?> namespaceClass, Mode mode, boolean nullable) {
		requireNonNull(name);
		requireNonNull(namespaceClass);
		requireNonNull(mode);

		this.name = name;
		this.namespaceClass = namespaceClass;
		this.mode = mode;
		this.nullable = nullable;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the namespaceClass
	 */
	public Class<?> getNamespaceClass() {
		return namespaceClass;
	}

	/**
	 * @return the mode
	 */
	public Mode getMode() {
		return mode;
	}

	/**
	 * @return the nullable
	 */
	public boolean isNullable() {
		return nullable;
	}


	/**
	 * Assigns this variable's internal value to given one.
	 * If the provided {@code value} is {@code null} it will first
	 * be cast to an instance of the appropriate  {@link #getNamespaceClass() namespace}.
	 *
	 * @param value the value to set
	 *
	 * @throws ClassCastException if the given {@code value} is not assignment compatible with
	 * the {@link #getNamespaceClass() namespace} for this variable.
	 * @throws IcarusException iff the given {@code value} is {@code null} and this variable is
	 * not declared to be {@link #isNullable() nullable}.
	 *
	 * @see Class#cast(Object)
	 */
//	public void setValue(Object value) {
//
//		if(value==null && !nullable)
//			throw new IcarusException(GlobalErrorCode.INVALID_INPUT, "VariableDescriptor does not support null value: "+getName());
//
//		this.value = namespaceClass.cast(value);
//	}
}
