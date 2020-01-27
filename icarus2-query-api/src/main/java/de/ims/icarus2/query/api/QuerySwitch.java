/*
 * ICARUS2 Corpus Modeling Framework
 * Copyright (C) 2014-2020 Markus Gärtner <markus.gaertner@ims.uni-stuttgart.de>
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
package de.ims.icarus2.query.api;

import static de.ims.icarus2.util.Conditions.checkNotEmpty;
import static java.util.Objects.requireNonNull;

import de.ims.icarus2.query.api.iql.IqlConstants;

/**
 * Defines the native constants used to declare switch statements in IQL queries.
 *
 * @author Markus Gärtner
 *
 */
public enum QuerySwitch {

	STRING_CASE_OFF("string.case.off", "Turns of case sensitivity when performing string operations such as equality checks."),
	STRING_CASE_LOWER("string.case.lower", "Another approach to case insensitivity, this switch turns all strings into lower case."),
	EXPANSION_OFF("expansion.off", "Effectively shuts down value expansion."),
	STRING_TO_BOOLEAN_OFF("string2bool.off", "Deactivates the interpretation of strings as boolean values."),
	INT_TO_BOOLEAN_OFF("int2bool.off", "Deactivates the interpretation of integers as boolean values."),
	FLOAT_TO_BOOLEAN("float2bool.off", "Deactivates the interpretation of floating point numbers as boolean values."),
	OBJECT_TO_BOOLEAN("obj2bool.off", "Deactivates the interpretation of arbitrary objects as boolean values."),
	ANY_TO_BOOLEAN("any2bool.off", "Deactivates the interpretation of anything non-boolean as boolean value. "
			+ "This is a combination of 'iql.string2bool.off', 'iql.int2bool.off', 'iql.float2bool.off' and  'iql.obj2bool.off'+*."),
	DIRECTION_REVERSE("direction.reverse", "Reverses the direction used to traverse corpus data for a search."),
	ARRAY_ZERO("array.zero", "Change array access to be 0-based."),
	WARNINGS_OFF("warnings.off", "Deactivates all warnings, potentially resulting in confusing results if there are mistakes in the query."),

	;
	private final String key, description;

	private QuerySwitch(String key, String description) {
		this.key = IqlConstants.IQL_PREFIX+'.'+checkNotEmpty(key);
		this.description = requireNonNull(description);
	}

	public String getKey() {
		return key;
	}

	public String getDescription() {
		return description;
	}
}
