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
package de.ims.icarus2.query.api.eval;

import static de.ims.icarus2.model.util.ModelUtils.getName;
import static de.ims.icarus2.util.Conditions.checkNotEmpty;
import static de.ims.icarus2.util.Conditions.checkState;
import static de.ims.icarus2.util.collections.CollectionUtils.list;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

import javax.annotation.Nullable;

import de.ims.icarus2.GlobalErrorCode;
import de.ims.icarus2.model.api.corpus.Corpus;
import de.ims.icarus2.model.api.layer.AnnotationLayer;
import de.ims.icarus2.model.api.layer.ItemLayer;
import de.ims.icarus2.model.api.layer.Layer;
import de.ims.icarus2.model.api.layer.annotation.AnnotationStorage;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.api.view.Scope;
import de.ims.icarus2.model.manifest.ManifestErrorCode;
import de.ims.icarus2.model.manifest.api.AnnotationLayerManifest;
import de.ims.icarus2.model.manifest.api.AnnotationManifest;
import de.ims.icarus2.model.manifest.types.ValueType;
import de.ims.icarus2.model.manifest.util.ManifestUtils;
import de.ims.icarus2.model.util.Graph;
import de.ims.icarus2.model.util.ModelUtils;
import de.ims.icarus2.query.api.QueryErrorCode;
import de.ims.icarus2.query.api.QueryException;
import de.ims.icarus2.query.api.QuerySwitch;
import de.ims.icarus2.query.api.engine.ext.EngineConfigurator;
import de.ims.icarus2.query.api.iql.IqlBinding;
import de.ims.icarus2.query.api.iql.IqlElement.IqlProperElement;
import de.ims.icarus2.query.api.iql.IqlLane;
import de.ims.icarus2.query.api.iql.IqlReference;
import de.ims.icarus2.util.AbstractBuilder;
import de.ims.icarus2.util.collections.set.DataSet;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Utility class(es) to store the final configuration and bindings for a query evaluation
 * on a single stream/corpus.
 *
 * @author Markus Gärtner
 *
 */
public abstract class EvaluationContext {

	public static RootContextBuilder rootBuilder() {
		return new RootContextBuilder();
	}

	private static class RootContext extends EvaluationContext {

		/** The corpus this context refers to. */
		private final Corpus corpus;

		/**
		 * Maps all the usable raw names and/or aliases to layer entries.
		 * Note that a single layer can end up twice in this lookup map if
		 * it has been assigned an alias!
		 */
		private final Map<String, Layer> layers;

		/** Stores all the alias names assigned to layers by the query */
		private final Set<String> aliases;

		private final Scope scope;

		/** Efficient lookup for collecting  */
		private final Graph<Layer> layerGraph;

		/** Flags that have been switched on for the query. */
		private final Set<String> switches;

		/** Additional properties that have been set for the query. */
		private final Map<String, Object> properties;

		private RootContext(RootContextBuilder builder) {
			super(builder);

			corpus = builder.corpus;
			scope = builder.scope;
			switches = new ObjectOpenHashSet<>(builder.switches);
			properties = new Object2ObjectOpenHashMap<>(builder.properties);

			layers = new Object2ObjectOpenHashMap<>(builder.namedLayers);
			aliases = new ObjectOpenHashSet<>(builder.namedLayers.keySet());

			for(Layer layer : scope.getLayers()) {
				String key = key(layer);
				Layer previous = layers.putIfAbsent(key, layer);

				// Make sure users can't (accidentally) shadow layers with new aliases
				if(previous!=null && previous!=layer)
					throw new QueryException(ManifestErrorCode.MANIFEST_DUPLICATE_ID,
							String.format("Reference to layer '%s' is shadowed by (aliased) entry '%s' for id: %s",
									getName(layer), getName(previous), key));
			}

			// Create a complete dependency graph of all (and only those) the layers included in the scope
			layerGraph = Graph.layerGraph(scope.getLayers(), scope::containsLayer);
		}

		private static String key(Layer layer) {
			return ManifestUtils.requireId(layer.getManifest());
		}

		@Override
		protected Graph<Layer> getLayerGraph() { return layerGraph; }

		@Override
		public Optional<Layer> findLayer(String name) {
			return Optional.ofNullable(layers.get(checkNotEmpty(name)));
		}

		@Override
		public boolean isSwitchSet(String name) {
			return switches.contains(checkNotEmpty(name));
		}

		@Override
		public Optional<?> getProperty(String name) {
			return Optional.ofNullable(properties.get(checkNotEmpty(name)));
		}

		@Override
		public Corpus getCorpus() { return corpus; }

		@Override
		public Scope getScope() { return scope; }
	}

	private static class AnnotationLink {
		private final AnnotationManifest manifest;
		private final AnnotationLayer layer;
		private final boolean isAlias;

		public AnnotationLink(AnnotationManifest manifest, AnnotationLayer layer, boolean isAlias) {
			this.manifest = requireNonNull(manifest);
			this.layer = requireNonNull(layer);
			this.isAlias = isAlias;
		}
	}

	private static class LaneInfo {
		private final IqlLane lane;
		private final ItemLayer layer;

		public LaneInfo(IqlLane lane, ItemLayer layer) {
			this.lane = requireNonNull(lane);
			this.layer = requireNonNull(layer);
		}

		public IqlLane getLane() { return lane; }
		public ItemLayer getLayer() { return layer; }
	}

	private static class ElementInfo {
		private final IqlProperElement element;
		private final List<ItemLayer> layers;

		public ElementInfo(IqlProperElement element, List<ItemLayer> layers) {
			this.element = requireNonNull(element);
			this.layers = requireNonNull(layers);
		}

		public IqlProperElement getElement() { return element; }
		public List<ItemLayer> getLayers() { return layers; }
	}

	private static ItemLayer ensureItemLayer(Layer layer) {
		if(!ModelUtils.isItemLayer(layer))
			throw new QueryException(QueryErrorCode.INCOMPATIBLE_REFERENCE,
					"Lane name must reference an item or structure layer: "+getName(layer));
		return (ItemLayer) layer;
	}

	private static class SubContext extends EvaluationContext {

		/** Uplink to another SubContext or an instance of RootContext */
		private final EvaluationContext parent;

		private final LaneInfo lane;
		private final ElementInfo element;

		/** Maps member labels to layers */
		private final Map<String, ItemLayer> bindings;

		/** Lazily constructed lookup to map from annotation keys to actual sources */
		private Map<String, AnnotationLink> annotationLookup;

		/** Layers that allow unknown keys for annotations */
		private List<AnnotationLayer> catchAllLayers;

		private SubContext(SubContextBuilder builder) {
			super(builder);

			// First assign the fields we need for resolving other parts
			parent = builder.parent;
			bindings = new Object2ObjectOpenHashMap<>(builder.bindings);


			lane = resolve(builder.lane);
			element = resolve(builder.element, lane);
		}

		private LaneInfo resolve(IqlLane lane) {
			if(lane==null) {
				return null;
			}

			String name = lane.getName();
			Layer layer = requireLayer(name);

			return new LaneInfo(lane, ensureItemLayer(layer));
		}

		private ElementInfo resolve(IqlProperElement element, LaneInfo lane) {
			if(element==null) {
				return null;
			}

			String label = element.getLabel().orElse(null);
			if(label!=null) {
				// Explicitly bound element -> can only have 1 source layer
				return new ElementInfo(element, list(forMemberLabel(label)));
			}

			// Unbound element can stem from any base layer of current lane
			DataSet<ItemLayer> baseLayers = lane.getLayer().getBaseLayers();
			if(baseLayers.isEmpty())
				throw new QueryException(QueryErrorCode.INCORRECT_USE,
						"Cannot use non-aggregating layer as lane: "+getName(lane.getLayer()));

			return new ElementInfo(element, baseLayers.toList());
		}

		private ItemLayer forMemberLabel(String label) {
			ItemLayer layer = bindings.get(label);
			if(layer==null)
				throw new QueryException(QueryErrorCode.UNKNOWN_IDENTIFIER,
						"No layer bound to label: "+label);
			return layer;
		}

		private void ensureLayerLookups() {
			if(catchAllLayers==null || annotationLookup==null) {

			}
		}

		private LaneInfo requireLaneInfo() {
			return getInheritable(this, ctx -> ctx.lane).orElseThrow(
					() -> new QueryException(GlobalErrorCode.INTERNAL_ERROR,
					"No lane available"));
		}

		private ElementInfo requireElementInfo() {
			return getInheritable(this, ctx -> ctx.element).orElseThrow(
					() -> new QueryException(GlobalErrorCode.INTERNAL_ERROR,
					"No element available"));
		}

		private static @Nullable <T> Optional<T> getInheritable(SubContext ctx,
				Function<SubContext, T> getter) {
			while(ctx!=null) {
				T value = getter.apply(ctx);
				if(value!=null) {
					return Optional.of(value);
				}
				ctx = ctx.parent instanceof SubContext ? (SubContext)ctx.parent : null;
			}
			return Optional.empty();
		}

		@Override
		public Optional<EvaluationContext> getParent() { return Optional.of(parent); }

		@Override
		public Optional<IqlLane> getLane() {
			return getInheritable(this, ctx -> ctx.lane).map(LaneInfo::getLane);
		}

		@Override
		public Optional<IqlProperElement> getElement() {
			return getInheritable(this, ctx -> ctx.element).map(ElementInfo::getElement);
		}

		@Override
		public Optional<AnnotationInfo> findAnnotation(String key) {
			// TODO Auto-generated method stub
			return super.findAnnotation(key);
		}
	}

	private final Stack<Class<?>> trace = new ObjectArrayList<>();

	/** Flag to signal that this context shouldn't be used any further */
	private volatile boolean disposed = false;

	private final List<Environment> environments;

	//TODO add mechanisms to obtain root namespace and to navigate namespace hierarchies

	//TODO add mechanism to register callbacks for stages of matching process?

	private EvaluationContext(BuilderBase<?,?> builder) {
		requireNonNull(builder);

		this.environments = new ArrayList<>(builder.environments);

		//TODO
	}

	public boolean isRoot() {
		return !getParent().isPresent();
	}

	public @Nullable EvaluationContext getRootContext() {
		EvaluationContext ctx = this;
		while(!ctx.isRoot()) {
			ctx = getParent().orElse(null);
		}
		return ctx;
	}

	/**
	 * Creates a builder that can be used to derive a new context from this
	 * instance, inheriting all the settings, but with e.g. new bound item
	 * contexts etc...
	 * @return
	 */
	public SubContextBuilder derive() {
		return new SubContextBuilder(this);
	}

	protected void cleanup() { /* no-op */ }

	/**
	 * Effectively disables this context. After invoking this method, further calling
	 * any of method on this instance can cause an exception.
	 */
	public void dispose() {
		disposed = true;

		// Ensure our basic state is reset
		environments.clear();
		while(!trace.isEmpty()) trace.pop();

		// Let subclasses do their housekeeping
		cleanup();
	}

	public Optional<IqlLane> getLane() { return Optional.empty(); }

	public Optional<IqlProperElement> getElement() { return Optional.empty(); }

	public Optional<EvaluationContext> getParent() { return Optional.empty(); }

	public Optional<?> getProperty(String name) { return getRootContext().getProperty(name); }
	public boolean isSwitchSet(String name) { return getRootContext().isSwitchSet(name); }
	public Corpus getCorpus() { return getRootContext().getCorpus(); }
	public Scope getScope() { return getRootContext().getScope(); }

	public Optional<Layer> findLayer(String name) { return getRootContext().findLayer(name); }

	public Layer requireLayer(String name) {
		return findLayer(name).orElseThrow(
				() -> new QueryException(QueryErrorCode.UNKNOWN_IDENTIFIER,
						"Cannot resolve name to layer: "+name));
	}

	protected Graph<Layer> getLayerGraph() { return getRootContext().getLayerGraph(); }

	public boolean isSwitchSet(QuerySwitch qs) {
		return isSwitchSet(qs.getKey());
	}

	/**
	 * Return all the currently active and available environments that can be
	 * used to resolve identifiers to fields or methods. The environments are
	 * ordered based on their priority: the environment on position {@code 0}
	 * is considered the most important one for any resolution process, with
	 * the importance of subsequent entries decreasing.
	 * @return
	 */
	public List<Environment> getActiveEnvironments() {
		//TODO implement
		throw new UnsupportedOperationException();
	}

	/**
	 * Enter the scope of the given {@code context} class and translate it into an available
	 * {@link Environment}.
	 *
	 * @param context
	 * @return
	 */
	public void enter(Class<?> context) {
		//TODO implement
		throw new UnsupportedOperationException();
	}

	public void exit(Class<?> context) {
		//TODO implement
		throw new UnsupportedOperationException();
	}

	/**
	 * Tries to resolve the given {@code name} to a field or no-args method
	 * equivalent. Using the {@code resultFilter} argument, returned expressions
	 * can be restricted to be return type compatible to a desired target type.
	 *
	 * @throws QueryException of type {@link QueryErrorCode#UNKNOWN_IDENTIFIER} iff
	 * the specified {@code name} could not be resolved to a target that satisfies
	 * the given {@code filter} (if present).
	 */
	public Expression<?> resolve(String name, @Nullable TypeFilter filter) {
		//TODO implement
		throw new UnsupportedOperationException();
	}

	/**
	 * Tries to resolve the given {@code name} to a method that takes the
	 * specified {@code arguments} as input.
	 * If the {@code resultFilter} argument is provided, it will be used to
	 * restrict the pool of methods to be considered to those that return
	 * a compatible value.
	 *
	 * @throws QueryException of type {@link QueryErrorCode#UNKNOWN_IDENTIFIER} iff
	 * the specified {@code name} could not be resolved to a method that satisfies
	 * the given {@code argument} specification and {@code resultFilter} (if present).
	 */
	public Expression<?> resolve(String name, @Nullable TypeFilter resultFilter,
			Expression<?>[] arguments) {
		//TODO implement
		throw new UnsupportedOperationException();
	}

	public Optional<AnnotationInfo> findAnnotation(String key) { return Optional.empty(); }

	/**
	 * Tries to resolve the specified annotation {@code key} to a unique source of
	 * annotation values and all associated info data.
	 * <p>
	 * Note that this method will only consider those annotation sources that are
	 * available through the scope defined by the current
	 *
	 * @param key
	 * @return
	 */
	public AnnotationInfo findAnnotation0(String key) {
		requireNonNull(key);

		//TODO do we need to use the Optional.orElseThrow() method here?
		Map<String, Layer> layers = Collections.emptyMap(); //TODO

		// Option 1: aliased or directly referenced layer
		Layer layer = layers.get(key);
		if(layer!=null) {
			if(!ModelUtils.isAnnotationLayer(layer))
				throw new QueryException(QueryErrorCode.INCOMPATIBLE_REFERENCE, String.format(
						"Not an annotation layer for key '%s': %s", key, getName(layer)));

			return fromDefault(key, (AnnotationLayer) layer);
		}

		List<AnnotationManifest> manifests = new ArrayList<>();
		for (Layer l : layers.values()) {
			if(ModelUtils.isAnnotationLayer(l)) {
				AnnotationLayer annotationLayer = (AnnotationLayer)l;

			}
		}

		return null; //TODO
	}

	private AnnotationInfo fromDefault(String key, AnnotationLayer layer) {
		AnnotationLayerManifest layerManifest = layer.getManifest();
		Optional<String> defaultKey = layerManifest.getDefaultKey();

		String actualKey;

		if(defaultKey.isPresent()) {
			actualKey = defaultKey.get();
		} else {
			Set<String> keys = layerManifest.getAvailableKeys();
			if(keys.size()>1)
				throw new QueryException(QueryErrorCode.INCORRECT_USE, String.format(
						"Annotation key '%s' points to layer with more than 1 annotation manifest: %s",
								key, getName(layer)));

			actualKey = keys.iterator().next();
		}

		return fromManifest(key,
				ManifestUtils.require(layerManifest, m -> m.getAnnotationManifest(actualKey), "annotation manifest"),
				layer.getAnnotationStorage());
	}

	/**
	 * Produces a type-aware {@link AnnotationInfo} that will be able to access
	 * annotations for the given manifest from the specified storage.
	 *
	 * @param rawKey
	 * @param manifest
	 * @param storage
	 * @return
	 */
	private AnnotationInfo fromManifest(String rawKey, AnnotationManifest manifest,
			AnnotationStorage storage) {
		final String key = ManifestUtils.require(manifest, AnnotationManifest::getKey, "key");
		AnnotationInfo info = new AnnotationInfo(rawKey, key, manifest.getValueType(),
				EvaluationUtils.typeFor(manifest.getValueType()));

		if(TypeInfo.isInteger(info.type)) {
			info.integerSource = item -> storage.getLong(item, key);
		} else if(TypeInfo.isFloatingPoint(info.type)) {
			info.floatingPointSource = item -> storage.getDouble(item, key);
		} else if(TypeInfo.isBoolean(info.type)) {
			info.booleanSource = item -> storage.getBoolean(item, key);
		} else if(TypeInfo.isText(info.type)) {
			info.objectSource = item -> storage.getString(item, key);
		} else {
			info.objectSource = item -> storage.getValue(item, key);
		}

		return info;
	}

	public static class AnnotationInfo {
		private final String rawKey;
		private final String key;
		private final ValueType valueType;
		private final TypeInfo type;

		// HARD BINDING
		private AnnotationManifest manifest;
		private AnnotationStorage storage;
		// END HARD BINDING

		Function<Item, Object> objectSource;
		ToLongFunction<Item> integerSource;
		ToDoubleFunction<Item> floatingPointSource;
		Predicate<Item> booleanSource;

		private AnnotationInfo(String rawKey, String key, ValueType valueType, TypeInfo type) {
			this.rawKey = requireNonNull(rawKey);
			this.key = requireNonNull(key);
			this.valueType = requireNonNull(valueType);
			this.type = requireNonNull(type);
		}

		public String getRawKey() { return rawKey; }

		public String getKey() { return key; }

		public ValueType getValueType() { return valueType; }

		public TypeInfo getType() { return type; }


		public Function<Item, Object> getObjectSource() {
			checkState("No object source defined", objectSource!=null);
			return objectSource;
		}

		public ToLongFunction<Item> getIntegerSource() {
			checkState("No integer source defined", integerSource!=null);
			return integerSource;
		}

		public ToDoubleFunction<Item> getFloatingPointSource() {
			checkState("No floating point source defined", floatingPointSource!=null);
			return floatingPointSource;
		}

		public Predicate<Item> getBooleanSource() {
			checkState("No boolean source defined", booleanSource!=null);
			return booleanSource;
		}

	}

	private static final class ContextInfo {
		private final Class<?> type;
		private final Environment environment;

		ContextInfo(Class<?> type, Environment environment) {
			this.type = requireNonNull(type);
			this.environment = environment;
		}
	}

	public static abstract class BuilderBase<B extends BuilderBase<B, C>, C extends EvaluationContext>
			extends AbstractBuilder<B, C> {
		//TODO

		private final List<Environment> environments = new ArrayList<>();

		public B registerEnvironment(Environment environment) {
			// TODO Auto-generated method stub

			return thisAsCast();
		}

		@Override
		protected void validate() {
			// TODO Auto-generated method stub
			super.validate();
		}
	}

	public static final class RootContextBuilder extends BuilderBase<RootContextBuilder, RootContext>
			implements EngineConfigurator{

		private Corpus corpus;

		/** Maps the usable raw names or aliases to layer entries. */
		private final Map<String, Layer> namedLayers = new Object2ObjectOpenHashMap<>();

		/**
		 *  Contains additional layers that have not received a dedicated reference in the
		 *  original query definition. This includes for instance those layers that got
		 *  added transitively via dependencies on other (named) layers.
		 */
		private Scope scope;

		/** Flags that have been switched on for the query. */
		private final Set<String> switches = new ObjectOpenHashSet<>();

		/** Additional properties that have been set for this query. */
		private final Map<String, Object> properties = new Object2ObjectOpenHashMap<>();

		public RootContextBuilder namedLayer(String alias, Layer layer) {
			requireNonNull(alias);
			requireNonNull(layer);
			checkState("Alias already used: "+alias, !namedLayers.containsKey(alias));
			namedLayers.put(alias, layer);
			return this;
		}

		public RootContextBuilder scope(Scope scope) {
			requireNonNull(scope);
			checkState("Scope already set", this.scope==null);
			this.scope = scope;
			return this;
		}

		public RootContextBuilder corpus(Corpus corpus) {
			requireNonNull(corpus);
			checkState("Corpus already set", this.corpus==null);
			this.corpus = corpus;
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public RootContextBuilder setProperty(String key, Object value) {
			requireNonNull(key);
			if(value==null) {
				properties.remove(key);
			} else {
				properties.put(key, value);
			}
			return this;
		}

		/** {@inheritDoc} */
		@Override
		public RootContextBuilder setSwitch(String name, boolean active) {
			requireNonNull(name);
			if(active) {
				switches.add(name);
			} else {
				switches.remove(name);
			}
			return this;
		}

		@Override
		protected void validate() {
			// TODO Auto-generated method stub
			super.validate();
		}

		@Override
		protected RootContext create() { return new RootContext(this); }

	}

	public static final class SubContextBuilder extends BuilderBase<SubContextBuilder, SubContext> {

		private final EvaluationContext parent;

		private IqlLane lane;
		private IqlProperElement element;
		private Map<String, ItemLayer> bindings = new Object2ObjectOpenHashMap<>();

		private SubContextBuilder(EvaluationContext parent) {
			this.parent = requireNonNull(parent);
		}

		public SubContextBuilder lane(IqlLane lane) {
			requireNonNull(lane);
			checkState("Lane already set", this.lane==null);
			this.lane = lane;
			return this;
		}

		public SubContextBuilder element(IqlProperElement element) {
			requireNonNull(element);
			checkState("Element already set", this.element==null);
			this.element = element;
			return this;
		}

		public SubContextBuilder bind(String name, ItemLayer layer) {
			requireNonNull(name);
			requireNonNull(layer);
			checkNotEmpty(name);
			checkState("Name already bound: "+name, !bindings.containsKey(name));
			bindings.put(name, layer);
			return this;
		}

		public SubContextBuilder bind(IqlBinding binding) {
			requireNonNull(binding);
			ItemLayer targetLayer = ensureItemLayer(parent.requireLayer(binding.getTarget()));

			for(IqlReference ref : binding.getMembers()) {
				String name = ref.getName();
				checkState("Name already bound: "+name, !bindings.containsKey(name));
				bindings.put(name, targetLayer);
			}
			return this;
		}

		@Override
		protected void validate() {
			// TODO Auto-generated method stub
			super.validate();
		}

		@Override
		protected SubContext create() { return new SubContext(this); }

	}
}
