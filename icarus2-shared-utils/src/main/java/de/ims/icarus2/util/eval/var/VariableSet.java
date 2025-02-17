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
package de.ims.icarus2.util.eval.var;

import java.util.List;
import java.util.function.Consumer;

import de.ims.icarus2.IcarusRuntimeException;
import de.ims.icarus2.util.collections.LazyCollection;

/**
 * Models a storage for reading and writing named variables.
 *
 * @author Markus Gärtner
 *
 */
public interface VariableSet {


	void forEachVariable(Consumer<? super VariableDescriptor> action);

	default List<VariableDescriptor> getVariables() {
		LazyCollection<VariableDescriptor> result = LazyCollection.lazyList();

		forEachVariable(result);

		return result.getAsList();
	}

	VariableDescriptor getVariable(String name);

	Object getValue(String name);

	default int getInteger(VariableDescriptor variableDescriptor) {
		return getInteger(variableDescriptor.getName());
	}

	default int getInteger(String name) {
		return ((Number) getValue(name)).intValue();
	}

	default short getShort(VariableDescriptor variableDescriptor) {
		return getShort(variableDescriptor.getName());
	}

	default short getShort(String name) {
		return ((Number) getValue(name)).shortValue();
	}

	default long getLong(VariableDescriptor variableDescriptor) {
		return getLong(variableDescriptor.getName());
	}

	default long getLong(String name) {
		return ((Number) getValue(name)).longValue();
	}

	default byte getByte(VariableDescriptor variableDescriptor) {
		return getByte(variableDescriptor.getName());
	}

	default byte getByte(String name) {
		return ((Number) getValue(name)).byteValue();
	}

	default char getChar(VariableDescriptor variableDescriptor) {
		return getChar(variableDescriptor.getName());
	}

	default char getChar(String name) {
		return ((Character) getValue(name)).charValue();
	}

	default float getFloat(VariableDescriptor variableDescriptor) {
		return getFloat(variableDescriptor.getName());
	}

	default float getFloat(String name) {
		return ((Number) getValue(name)).floatValue();
	}

	default double getDouble(VariableDescriptor variableDescriptor) {
		return getDouble(variableDescriptor.getName());
	}

	default double getDouble(String name) {
		return ((Number) getValue(name)).doubleValue();
	}

	default boolean getBoolean(VariableDescriptor variableDescriptor) {
		return getBoolean(variableDescriptor.getName());
	}

	default boolean getBoolean(String name) {
		return ((Boolean) getValue(name)).booleanValue();
	}

	/**
	 * Assigns a new value to the variable specified by {@code variableName}.
	 * <p>
	 * Note that this method is heavily overloaded with alternatives for each
	 * primitive type. Per default all the versions for primitive values delegate
	 * to this general method by using the appropriate static {@code XXX.valueOf(x)}
	 * method of the respective wrapper class.
	 * <p>
	 * Actual expression implementations should try to override the primitive methods
	 * to provide more efficient handling of primitive values.
	 *
	 * @param variableName
	 * @param value
	 *
	 * @throws ClassCastException if the given {@code value} is not assignment compatible with
	 * the {@link VariableDescriptor#getNamespaceClass() namespace} for the variable.
	 * @throws IcarusRuntimeException iff the given {@code value} is {@code null} and the variable is
	 * not declared to be {@link VariableDescriptor#isNullable() nullable}.
	 */
	void setValue(String variableName, Object value);

	default void setValue(VariableDescriptor variableDescriptor, Object value) {
		setValue(variableDescriptor.getName(), value);
	}

	default void setByte(String variableName, byte value) {
		setValue(variableName, Byte.valueOf(value));
	}

	default void setByte(VariableDescriptor variableDescriptor, byte value) {
		setByte(variableDescriptor.getName(), value);
	}

	default void setShort(String variableName, short value) {
		setValue(variableName, Short.valueOf(value));
	}

	default void setShort(VariableDescriptor variableDescriptor, short value) {
		setShort(variableDescriptor.getName(), value);
	}

	default void setInteger(String variableName, int value) {
		setValue(variableName, Integer.valueOf(value));
	}

	default void setInteger(VariableDescriptor variableDescriptor, int value) {
		setInteger(variableDescriptor.getName(), value);
	}

	default void setLong(String variableName, long value) {
		setValue(variableName, Long.valueOf(value));
	}

	default void setLong(VariableDescriptor variableDescriptor, long value) {
		setLong(variableDescriptor.getName(), value);
	}

	default void setBoolean(String variableName, boolean value) {
		setValue(variableName, Boolean.valueOf(value));
	}

	default void setBoolean(VariableDescriptor variableDescriptor, boolean value) {
		setBoolean(variableDescriptor.getName(), value);
	}

	default void setChar(String variableName, char value) {
		setValue(variableName, Character.valueOf(value));
	}

	default void setChar(VariableDescriptor variableDescriptor, char value) {
		setChar(variableDescriptor.getName(), value);
	}

	default void setFloat(String variableName, float value) {
		setValue(variableName, Float.valueOf(value));
	}

	default void setFloat(VariableDescriptor variableDescriptor, float value) {
		setFloat(variableDescriptor.getName(), value);
	}

	default void setDouble(String variableName, double value) {
		setValue(variableName, Double.valueOf(value));
	}

	default void setDouble(VariableDescriptor variableDescriptor, double value) {
		setDouble(variableDescriptor.getName(), value);
	}
}
