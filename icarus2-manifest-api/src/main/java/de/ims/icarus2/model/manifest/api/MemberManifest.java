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
package de.ims.icarus2.model.manifest.api;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import de.ims.icarus2.model.manifest.ManifestErrorCode;
import de.ims.icarus2.model.manifest.api.OptionsManifest.Option;
import de.ims.icarus2.model.manifest.types.ValueType;
import de.ims.icarus2.util.Options;
import de.ims.icarus2.util.access.AccessControl;
import de.ims.icarus2.util.access.AccessMode;
import de.ims.icarus2.util.access.AccessPolicy;
import de.ims.icarus2.util.access.AccessRestriction;
import de.ims.icarus2.util.collections.LazyCollection;


/**
 * A manifest is a kind of descriptor for parts of a corpus.
 * It stores information relevant to localization and identification
 * of the item it describes. Manifests for the most part are immutable
 * storage objects created by the model framework. They normally derive
 * from a static xml definition and the only thing the user can modify
 * in certain cases is the identifier used to present them in the GUI.
 *
 * When saving the current state of a corpus, the framework converts the
 * manifests back into a physical xml-based representation.
 *
 * @author Markus Gärtner
 *
 */
@AccessControl(AccessPolicy.DENY)
public interface MemberManifest<M extends MemberManifest<M>>
		extends ModifiableIdentity, Categorizable<M>, Documentable<M>, Manifest {

	/**
	 * Returns the manifest that describes possible options the
	 * user can assign to this manifest. If the manifest does not
	 * support additional properties assignable by the user, this
	 * method returns an empty {@link Optional}.
	 *
	 * @return the manifest describing options for this manifest
	 * or an empty {@link Optional}
	 */
	@AccessRestriction(AccessMode.READ)
	Optional<OptionsManifest> getOptionsManifest();

	/**
	 * Returns the property assigned to this manifest for the given
	 * name. If there is no property with the given name available
	 * this method should return an empty {@link Optional}. Note that multi-value
	 * properties will typically return a collection of values.
	 *
	 * @param <V> type of the returned value.
	 * @param name The name of the property in question
	 * @return The value of the property with the given name or an empty {@link Optional}
	 * if no such property exists.
	 * @throws ClassCastException in case the actual property value is not
	 * assignment compatible with the type parameter {@code V}
	 */
	@AccessRestriction(AccessMode.READ)
	<V extends Object> Optional<V> getPropertyValue(String name);

	/**
	 *
	 * @param name
	 * @param valueType
	 * @param multiValue
	 * @param value
	 * @return
	 *
	 * @throws ManifestException of type {@link ManifestErrorCode#MANIFEST_DUPLICATE_ID} if
	 * a property for the given {@code name} already exists
	 */
	Property addProperty(String name, ValueType valueType, boolean multiValue, Object value);

	void addProperty(Property property);

	/**
	 *
	 * @param name identifier of the property to be retrieved
	 * @return the property mapped to {@code name} or an empty {@link Optional}
	 */
	Optional<Property> getProperty(String name);

	boolean hasProperty(String name);

	/**
	 * Returns a {@link Set} view of all the available property names
	 * in this manifest. If there are no properties in the manifest
	 * available then this method should return an empty {@code Set}!
	 * <p>
	 * The returned {@code Set} should be immutable.
	 *
	 * @return A {@code Set} view on all the available property names
	 * for this manifest or the empty {@code Set} if this manifest does
	 * not contain any properties.
	 */
	@AccessRestriction(AccessMode.READ)
	default Set<String> getPropertyNames() {
		LazyCollection<String> result = LazyCollection.lazySet();

		forEachProperty(m -> result.add(m.getName()));

		return result.getAsSet();
	}

	@AccessRestriction(AccessMode.READ)
	void forEachProperty(Consumer<? super Property> action);

	@AccessRestriction(AccessMode.READ)
	default void forEachLocalProperty(Consumer<? super Property> action) {
		forEachProperty(p -> {
			if(isLocalProperty(p.getName())) {
				action.accept(p);
			}
		});
	}

	@AccessRestriction(AccessMode.READ)
	boolean isLocalProperty(String name);

	@AccessRestriction(AccessMode.READ)
	default Set<Property> getProperties() {
		LazyCollection<Property> result = LazyCollection.lazySet();

		forEachProperty(result);

		return result.getAsSet();
	}

	@AccessRestriction(AccessMode.READ)
	default Set<Property> getLocalProperties() {
		LazyCollection<Property> result = LazyCollection.lazySet();

		forEachLocalProperty(result);

		return result.getAsSet();
	}

	default Options getPropertiesAsOptions() {
		Options options = new Options();

		forEachProperty(p -> options.put(p.getName(), p.getValue()));

		return options;
	}

	// Modification methods

	/**
	 * Changes the value of the property specified by {@code name} to
	 * the new {@code value}. Note that only for multi-value properties
	 * it is allowed to pass collections of values!
	 *
	 * @param name The name of the property to be changed
	 * @param value The new value for the property, allowed to be {@code null}
	 * if stated so in the {@code OptionsManifest} for this manifest
	 * @throws NullPointerException if the {@code name} argument is {@code null}
	 * @throws ManifestException of type {@link ManifestErrorCode#MANIFEST_TYPE_CAST}
	 * if the {@code value} argument does not fulfill the contract described in the
	 * {@code OptionsManifest} of this manifest.
	 * @throws UnsupportedOperationException if the manifest does not declare
	 * any properties the user can modify.
	 */
	M setPropertyValue(String name, @Nullable Object value);

	/**
	 * Attaches a new {@link OptionsManifest} to this manifest or
	 * removes the current one when supplying a {@code null} parameter.
	 * @param optionsManifest
	 */
	M setOptionsManifest(@Nullable OptionsManifest optionsManifest);

	// Since we inherit the following Identity related methods from two pathes, we need to unify return types
	@Override
	M setId(String id);
	@Override
	M setName(@Nullable String name);
	@Override
	M setDescription(@Nullable String description);

	// Extension of Categorizable

//	boolean isLocalCategory(Category category);
//
//	default void forEachLocalCategory(Consumer<? super Category> action) {
//		forEachCategory(c -> {
//			if(isLocalCategory(c)) {
//				action.accept(c);
//			}
//		});
//	}
//
//	default Set<Category> getLocalCategories() {
//		return LazyCollection.<Category>lazySet()
//				.addFromForEach(this::forEachLocalCategory)
//				.getAsSet();
//	}

	// Modification methods

	public interface Property extends Cloneable, Lockable {

		public static final boolean DEFAULT_MULTI_VALUE = false;

		ValueType getValueType();

		/**
		 * Returns the current value of this property. If no value is set
		 * and the property is backed by an {@link Option} declaration,
		 * returns the {@link Option#getDefaultValue() default value} of that
		 * option.
		 *
		 * @return
		 */
		<V extends Object> Optional<V> getValue();

		String getName();

		Optional<Option> getOption();

		boolean isMultiValue();

		// Modification methods

		/**
		 * Changes this property's value to the given one.
		 * Note that client code should rarely use this method for anything other
		 * that initializing a new property instance. The proper way to modify a
		 * property at runtime is by using the {@link MemberManifest#setPropertyValue(String, Object)}
		 * method that takes care of checking for a locked manifest or verification of
		 * the value in case it is restricted by a present {@link OptionsManifest options manifest}.
		 *
		 * @param value
		 */
		Property setValue(@Nullable Object value);

		// Cloning

		/**
		 * Clones this {@code Property} including the current {@link #getValue() value} if one is set.
		 *
		 * @return
		 */
		Property clone();

		Property setMultiValue(boolean multiValue);
	}
}
