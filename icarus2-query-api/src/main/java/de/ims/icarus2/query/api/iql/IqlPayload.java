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
package de.ims.icarus2.query.api.iql;

import static de.ims.icarus2.util.Conditions.checkNotEmpty;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import de.ims.icarus2.util.collections.CollectionUtils;

/**
 * @author Markus Gärtner
 *
 */
public class IqlPayload extends IqlUnique {

	@JsonProperty(value=IqlTags.QUERY_TYPE, required=true)
	private QueryType queryType;

	@JsonProperty(IqlTags.NAME)
	@JsonInclude(Include.NON_ABSENT)
	private Optional<String> name = Optional.empty();

	/**
	 * All the bindings to be usable for this query, if defined.
	 */
	@JsonProperty(IqlTags.BINDINGS)
	@JsonInclude(Include.NON_EMPTY)
	private final List<IqlBinding> bindings = new ArrayList<>();

	@JsonProperty(IqlTags.LANES)
	@JsonInclude(Include.NON_EMPTY)
	private final List<IqlLane> lanes = new ArrayList<>();

	// Pre-search filter (not compatible with plain constraints!!)
	@JsonProperty(IqlTags.FILTER)
	@JsonInclude(Include.NON_ABSENT)
	private Optional<IqlConstraint> filter = Optional.empty();

	// Either plain or global constraints
	@JsonProperty(IqlTags.CONSTRAINT)
	@JsonInclude(Include.NON_ABSENT)
	private Optional<IqlConstraint> constraint = Optional.empty();

	@Override
	public IqlType getType() { return IqlType.PAYLOAD; }

	@Override
	public void checkIntegrity() {
		super.checkIntegrity();
		checkNotNull(queryType, IqlTags.QUERY_TYPE);
		checkOptionalStringNotEmpty(name, IqlTags.NAME);

		if(queryType==QueryType.ALL) {
			checkCondition(!constraint.isPresent(), IqlTags.CONSTRAINT,
					"must not define a global 'constraint' entry if query type is 'all'");
			checkCondition(lanes.isEmpty(), IqlTags.LANES,
					"must not define a global 'lanes' entry if query type is 'all'");
		} else {
			checkCondition(constraint.isPresent() || !lanes.isEmpty(), "constraint/lanes",
					"must either define a global 'constraint' or at least one 'lanes' entry");
		}

		checkCollection(bindings);
		checkOptionalNested(constraint);
		checkCollection(lanes);
	}

	public QueryType getQueryType() { return queryType; }

	public Optional<String> getName() { return name; }

	public List<IqlBinding> getBindings() { return CollectionUtils.unmodifiableListProxy(bindings); }

	public List<IqlLane> getLanes() { return CollectionUtils.unmodifiableListProxy(lanes); }

	public Optional<IqlConstraint> getFilter() { return filter; }

	public Optional<IqlConstraint> getConstraint() { return constraint; }


	public void setQueryType(QueryType queryType) { this.queryType = requireNonNull(queryType); }

	public void setName(String name) { this.name = Optional.of(checkNotEmpty(name)); }

	public void addBinding(IqlBinding binding) { bindings.add(requireNonNull(binding)); }

	public void addLane(IqlLane lane) { lanes.add(requireNonNull(lane)); }

	public void setFilter(IqlConstraint filter) { this.filter = Optional.of(filter); }

	public void setConstraint(IqlConstraint constraint) { this.constraint = Optional.of(constraint); }

	public enum QueryType {
		/**
		 * Return all elements of the corpus for specified primary layer.
		 * Cannot have any constraints defined at all!
		 * (Only filtering will be donw in the result section)
		 */
		ALL("all", false, false),
		/** Define basic (global) constraints for bound members */
		PLAIN("plain", true, false),
		/** Contains a single lane to match */
		SINGLE_LANE("singleLane", true, true),
		/** Multiple lanes (e.g. concurrent parses), optionally linked via global constraints. */
		MULTI_LANE("multiLane", true, true),
		;

		private final String label;
		private final boolean allowConstraints, allowElements;

		private QueryType(String label, boolean allowConstraints, boolean allowElements) {
			this.label = label;
			this.allowConstraints = allowConstraints;
			this.allowElements = allowElements;
		}

		@JsonValue
		public String getLabel() {
			return label;
		}

		public boolean isAllowConstraints() { return allowConstraints; }

		public boolean isAllowElements() { return allowElements; }
	}
}
