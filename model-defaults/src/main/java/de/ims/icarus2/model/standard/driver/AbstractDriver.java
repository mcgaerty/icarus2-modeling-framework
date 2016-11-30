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
package de.ims.icarus2.model.standard.driver;

import static de.ims.icarus2.model.util.ModelUtils.getName;
import static de.ims.icarus2.util.Conditions.checkNotNull;
import static de.ims.icarus2.util.Conditions.checkState;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;

import de.ims.icarus2.GlobalErrorCode;
import de.ims.icarus2.model.api.ModelErrorCode;
import de.ims.icarus2.model.api.ModelException;
import de.ims.icarus2.model.api.corpus.Context;
import de.ims.icarus2.model.api.corpus.Corpus;
import de.ims.icarus2.model.api.driver.ChunkInfo;
import de.ims.icarus2.model.api.driver.Driver;
import de.ims.icarus2.model.api.driver.DriverListener;
import de.ims.icarus2.model.api.driver.mapping.Mapping;
import de.ims.icarus2.model.api.driver.mapping.MappingStorage;
import de.ims.icarus2.model.api.driver.mods.DriverModule;
import de.ims.icarus2.model.api.layer.ItemLayer;
import de.ims.icarus2.model.api.layer.Layer;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.api.meta.AnnotationValueDistribution;
import de.ims.icarus2.model.api.meta.AnnotationValueSet;
import de.ims.icarus2.model.api.registry.CorpusMemberFactory;
import de.ims.icarus2.model.api.registry.LayerMemberFactory;
import de.ims.icarus2.model.manifest.api.AnnotationLayerManifest;
import de.ims.icarus2.model.manifest.api.ContextManifest;
import de.ims.icarus2.model.manifest.api.DriverManifest;
import de.ims.icarus2.model.manifest.api.DriverManifest.ModuleManifest;
import de.ims.icarus2.model.manifest.api.ImplementationLoader;
import de.ims.icarus2.model.manifest.api.ItemLayerManifest;
import de.ims.icarus2.model.standard.members.DefaultLayerMemberFactory;
import de.ims.icarus2.model.standard.members.item.DefaultItem;
import de.ims.icarus2.model.standard.members.structure.DefaultEdge;
import de.ims.icarus2.util.AbstractBuilder;
import de.ims.icarus2.util.Options;

/**
 *
 * For the most part not thread safe!
 *
 * @author Markus Gärtner
 *
 */
public abstract class AbstractDriver implements Driver {

	private final DriverManifest manifest;

	private volatile boolean dead = false;

	/**
	 *  Helper flag to disable connection based errors during the connect() method
	 */
	private volatile boolean allowUncheckedAccess = false;
	private final AtomicBoolean connected = new AtomicBoolean(false);
	private Corpus corpus;

	private final List<DriverListener> driverListeners = new CopyOnWriteArrayList<>();

	private Context context;

	/**
	 * Driver global lock to synchronize critical operations like {@link #connect(Corpus)}
	 */
	private final Lock lock = new ReentrantLock();

	private MappingStorage mappings;

	protected AbstractDriver(DriverBuilder<?,?> builder) {
		this(builder.getManifest());
	}

	protected AbstractDriver(DriverManifest manifest) {
		checkNotNull(manifest);

		this.manifest = manifest;
	}

	protected Lock getGlobalLock() {
		return lock;
	}

	/**
	 * Returns a fresh instance of {@link DefaultLayerMemberFactory} which
	 * in turn returns implementations for all layer members based on the
	 * {@code de.ims.icarus2.standard.members} package and its subpackages.
	 *
	 * @see de.ims.icarus2.model.api.driver.Driver#newMemberFactory()
	 * @see DefaultLayerMemberFactory
	 * @see DefaultItem
	 * @see DefaultEdge
	 */
	@Override
	public LayerMemberFactory newMemberFactory() {
		return new DefaultLayerMemberFactory();
	}

	@Override
	public String toString() {
		return getClass().getName()+"Driver["+manifest.getId()+"]";
	}

	@Override
	public boolean isConnected() {
		return connected.get();
	}

	protected boolean isDead() {
		return dead;
	}

	@Override
	public void connect(Corpus target) throws InterruptedException {

		Lock lock = getGlobalLock();

		lock.lock();
		try {
			if(isDead())
				throw new ModelException(ModelErrorCode.DRIVER_CONNECTION, "Driver previously disconencted - considered dead: "+manifest.getId());
			if(isConnected())
				throw new ModelException(ModelErrorCode.DRIVER_CONNECTION, "Driver already connected: "+manifest.getId());

			this.corpus = target;

			// From here on subclasses should be able to access critical components without problems
			allowUncheckedAccess = true;

			// Delegate initialization work
			doConnect();

			verifyInternals();

			connected.set(true);

			// Connect notify AFTER this driver's 'connected' flag has been set!
			context.connectNotify(this);
		} finally {
			allowUncheckedAccess = false;
			lock.unlock();
		}
	}

	/**
	 * Callback for subclasses to perform maintenance work or additional  initialization
	 * after the main connection process has been finished. This is to allow subclasses
	 * to inject custom initialization steps without overriding and compromising the
	 * main connection routine.
	 * <p>
	 * This method will be called outside the global lock!
	 */
	protected void afterConnect() {
		// no-op
	}

	private void setMappings(MappingStorage mappings) {
		checkNotNull(mappings);
		checkState(this.mappings==null);

		this.mappings = mappings;
	}

	private void setContext(Context context) {
		checkNotNull(context);
		checkState(this.context==null);

		this.context = context;
	}

	protected void verifyInternals() {

		checkState("Missing context", context!=null);
		checkState("Missing mappings", mappings!=null);
	}

	/**
	 * Performs maintenance work during the connection process.
	 * For virtual drivers for example, this is the place to create their content.
	 * Other implementations might use this opportunity to prepare {@link DriverModule modules}
	 * or do sanity checks on external resources.
	 * <p>
	 * Note that this method will be called <b>first</b> from {@link #connect(Corpus)}.
	 * <p>
	 * The default implementation creates the context and mapping storage to be used later.
	 * <p>
	 * For proper nesting of method calls a subclass should make sure to call {@code super.doConnect()}
	 * <b>before</b> any setup work.
	 *
	 * @throws InterruptedException
	 */
	protected void doConnect() throws InterruptedException {
		setContext(createContext());
		setMappings(createMappings());
	}

	/**
	 * Creates the {@link Context} instance of this driver.
	 * <p>
	 * Note that this method should only perform initialization and proper
	 * linking of all the involved layers and such, but not {@link Context#connectNotify(Driver) connect}
	 * the context, which is the responsibility of the calling code!
	 *
	 * @see #connect(Corpus)
	 *
	 * @return
	 */
	protected Context createContext() {

		CorpusMemberFactory factory = corpus.getManager().newFactory();
		ContextManifest contextManifest = manifest.getContextManifest();

		// Allow custom layer implementations defined by the driver
		Options options = createCustomLayers(contextManifest);
		//TODO maybe introduce some general options or verify custom layers?

		return factory.createContext(corpus, contextManifest, options);
	}

	/**
	 * The default implementation does not provide custom layer implementations and therefore
	 * always returns {@code null}.
	 *
	 * @see de.ims.icarus2.model.api.driver.Driver#createCustomLayers(de.ims.icarus2.model.manifest.api.ContextManifest)
	 */
	protected Options createCustomLayers(ContextManifest manifest) {
		return null;
	}

	/**
	 * Creates the basic mappings for this driver implementation.
	 * <p>
	 * This method is called exactly once by this driver implementation during the connection phase.
	 *
	 * @param manifest
	 * @return
	 */
	protected MappingStorage createMappings() {
		MappingStorage.Builder builder = new MappingStorage.Builder();

		// Allow for subclasses to provide a fallback function
		BiFunction<ItemLayerManifest, ItemLayerManifest, Mapping> fallback = getRegularMappingFallback();
		if(fallback!=null) {
			builder.fallback(fallback);
		}

		return builder.build();
	}

	/**
	 * Hook for subclasses to modify default behavior when creating the mapping storage.
	 *
	 * @return
	 */
	protected BiFunction<ItemLayerManifest, ItemLayerManifest, Mapping> getRegularMappingFallback() {
		return null;
	}

	/**
	 * Hook for subclasses to modify default behavior when creating the mapping storage.
	 *
	 * @return
	 */
	protected Function<ItemLayerManifest, Mapping> getRootMappingFallback() {
		return null;
	}

	@Override
	public void disconnect(Corpus target) throws InterruptedException {
		checkNotNull(target);

		Lock lock = getGlobalLock();

		lock.lock();
		try {
			checkConnected();
			if(isDead())
				throw new ModelException(ModelErrorCode.DRIVER_CONNECTION, "Driver previously disconencted - considered dead: "+manifest.getId());
			if(this.corpus!=target)
				throw new ModelException(GlobalErrorCode.ILLEGAL_STATE,
						"Driver not connected to given corpus: "+manifest.getId());

			allowUncheckedAccess = true;

			/* Enable fail fast behavior for all parts of this driver by disabling
			 * it early. This way each attempt to access mappings or such things
			 * during the duration of this method call is bound to fail.
			 */
			connected.set(false);

			// Delegate cleanup work
			doDisconnect();

			context.disconnectNotify(this);

			// Let gc do the rest
			context = null;
			corpus = null;
			mappings = null;

			dead = true;
		} finally {
			allowUncheckedAccess = false;
			lock.unlock();
		}
	}

	/**
	 * Performs maintenance work <b>before</b> any other steps in the process of disconnecting
	 * from a corpus.
	 * <p>
	 * The default implementation just {@link #closeMappings() shuts down} all the mappings for
	 * this driver.
	 * <p>
	 * For proper nesting of method calls a subclass should make sure to call {@code super.doConnect()}
	 * <b>after</b> any cleanup work.
	 *
	 * @throws InterruptedException
	 */
	protected void doDisconnect() throws InterruptedException {
		closeMappings();
	}

	/**
	 * Iterates over all mappings this driver manages and calls {@link Mapping#close()} on them.
	 * <p>
	 * If a subclass wishes to perform individual cleanup jobs for mappings, it should override this method.
	 */
	protected void closeMappings() {
		mappings.forEachMapping(Mapping::close);
	}

	/**
	 * Throws {@link ModelException} with code {@link GlobalErrorCode#DRIVER_CONNECTION} in case
	 * the driver is not currently connected. Does nothing if the driver is in the process
	 * of connecting and was marked to allow unchecked access to connection critical code.
	 */
	protected void checkConnected() {
		if(!allowUncheckedAccess && !isConnected())
			throw new ModelException(ModelErrorCode.DRIVER_CONNECTION, "Driver not yet connected: "+manifest.getId());
	}

	/**
	 * Throws {@link ModelException} with code {@link GlobalErrorCode#DRIVER_READY} in case
	 * the driver is not currently ready.
	 */
	protected void checkReady() {
		if(!isReady())
			throw new ModelException(ModelErrorCode.DRIVER_READY, "Driver is not ready: "+manifest.getId());
	}

	/**
	 * @see de.ims.icarus2.model.api.driver.Driver#getContext()
	 */
	@Override
	public Context getContext() {
		checkConnected();

		return context;
	}

	public Corpus getCorpus() {
		checkConnected();
		return corpus;
	}

	/**
	 *
	 * @see de.ims.icarus2.model.api.members.item.ItemLayerManager#getLayers()
	 *
	 * @throws ModelException in case the driver is currently not connected to any live corpus
	 */
	@Override
	public Collection<Layer> getLayers() {
		return getContext().getLayers(ItemLayer.class);
	}

	/**
	 * @see de.ims.icarus2.model.api.driver.Driver#getManifest()
	 */
	@Override
	public DriverManifest getManifest() {
		return manifest;
	}

	/**
	 * @see de.ims.icarus2.model.api.driver.Driver#getMappings()
	 */
	@Override
	public MappingStorage getMappings() {
		checkConnected();

		return mappings;
	}

	/**
	 * @see de.ims.icarus2.model.api.driver.Driver#getMapping(ItemLayerManifest, ItemLayerManifest)
	 */
	@Override
	public Mapping getMapping(ItemLayerManifest sourceLayer, ItemLayerManifest targetLayer) {
		return getMappings().getMapping(sourceLayer, targetLayer);
	}

	@Override
	public void addDriverListener(DriverListener listener) {
		checkNotNull(listener);

		if(!driverListeners.contains(listener)) {
			driverListeners.add(listener);
		}
	}

	@Override
	public void removeDriverListener(DriverListener listener) {
		checkNotNull(listener);

		driverListeners.remove(listener);
	}

	protected void fireChunksLoaded(ItemLayer layer, ChunkInfo info) {
		if(driverListeners.isEmpty() || info==null || info.chunkCount()==0) {
			return;
		}

		for(DriverListener listener : driverListeners) {
			listener.chunksLoaded(layer, info);
		}
	}

	protected void fireChunksSkipped(ItemLayer layer, ChunkInfo info) {
		if(driverListeners.isEmpty() || info==null || info.chunkCount()==0) {
			return;
		}

		for(DriverListener listener : driverListeners) {
			listener.chunksSkipped(layer, info);
		}
	}

	/**
	 * Instantiates a new {@link DriverModule module} for this driver based on the
	 * provided manifest. This method will first obtain a new {@link ImplementationLoader}
	 * via the {@link Corpus} it is connected with and then set it up with additional
	 * information:
	 * <br>
	 * The {@link ImplementationLoader#environment(Object) environment} will be set to this driver instance.
	 * <br>
	 * When calling the {@link ImplementationLoader#instantiate(Class) instantiation} method, the
	 * given {@code resultClass} argument will be passed on.
	 *
	 * @param resultClass
	 * @param manifest
	 * @return
	 */
	protected <T extends Object> T defaultInstantiateModule(Class<T> resultClass, ModuleManifest manifest) {
		checkNotNull(resultClass);
		checkNotNull(manifest);

		checkConnected();

		CorpusMemberFactory factory = corpus.getManager().newFactory();

		return factory.newImplementationLoader()
				.manifest(manifest.getImplementationManifest())
				.environment(this)
				.message("Module manifest "+getName(manifest))
				.instantiate(resultClass);
	}

	/**
	 * Default implementation throws {@link GlobalErrorCode#UNSUPPORTED_OPERATION ModelException}.
	 *
	 * @see de.ims.icarus2.model.api.driver.Driver#addItem(de.ims.icarus2.model.api.layer.ItemLayer, de.ims.icarus2.model.api.members.item.Item, long)
	 */
	@Override
	public void addItem(ItemLayer layer, Item item, long index) {
		throw new ModelException(GlobalErrorCode.UNSUPPORTED_OPERATION,
				"Driver implementation does not support addition of items");
	}

	/**
	 * Default implementation throws {@link GlobalErrorCode#UNSUPPORTED_OPERATION ModelException}.
	 *
	 * @see de.ims.icarus2.model.api.driver.Driver#removeItem(de.ims.icarus2.model.api.layer.ItemLayer, de.ims.icarus2.model.api.members.item.Item, long)
	 */
	@Override
	public void removeItem(ItemLayer layer, Item item, long index) {
		throw new ModelException(GlobalErrorCode.UNSUPPORTED_OPERATION,
				"Driver implementation does not support removal of items");
	}

	/**
	 * Default implementation throws {@link GlobalErrorCode#UNSUPPORTED_OPERATION ModelException}.
	 *
	 * @see de.ims.icarus2.model.api.driver.Driver#moveItem(de.ims.icarus2.model.api.layer.ItemLayer, de.ims.icarus2.model.api.members.item.Item, long, long)
	 */
	@Override
	public void moveItem(ItemLayer layer, Item item, long fromIndex,
			long toIndex) {
		throw new ModelException(GlobalErrorCode.UNSUPPORTED_OPERATION,
				"Driver implementation does not support moving of items");
	}

	/**
	 * Default implementation always returns {@code false}.
	 * <p>
	 * Subclasses that implement a synchronous link to their backend storage
	 * should use this method to signal client code about unfinished maintenance
	 * work.
	 *
	 * @see de.ims.icarus2.model.api.driver.Driver#hasPendingChanges()
	 */
	@Override
	public boolean hasPendingChanges() {
		return false;
	}

	/**
	 * Default implementation does nothing.
	 * <p>
	 * Subclasses that implement a synchronous link to their backend storage
	 * should use this method to finish maintenance work.
	 *
	 * @see de.ims.icarus2.model.api.driver.Driver#flush()
	 */
	@Override
	public void flush() throws IOException {
		// no-op
	}

	/**
	 * Default implementation does nothing and returns {@code null}.
	 *
	 * @see de.ims.icarus2.model.api.driver.Driver#lookupValues(de.ims.icarus2.model.api.layer.AnnotationLayer, java.lang.String)
	 */
	@Override
	public AnnotationValueSet lookupValues(AnnotationLayerManifest layer, String key)
			throws InterruptedException {
		return null;
	}

	/**
	 * Default implementation does nothing and returns {@code null}.
	 *
	 * @see de.ims.icarus2.model.api.driver.Driver#lookupDistribution(de.ims.icarus2.model.api.layer.AnnotationLayer, java.lang.String)
	 */
	@Override
	public AnnotationValueDistribution lookupDistribution(
			AnnotationLayerManifest layer, String key) throws InterruptedException {
		return null;
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 * @param <B>
	 * @param <D>
	 */
	public static abstract class DriverBuilder<B extends DriverBuilder<B, D>, D extends Driver> extends AbstractBuilder<B, D> {

		private DriverManifest manifest;

		public B manifest(DriverManifest manifest) {
			checkNotNull(manifest);
			checkState(this.manifest==null);

			this.manifest = manifest;

			return thisAsCast();
		}

		public DriverManifest getManifest() {
			return manifest;
		}

		@Override
		protected void validate() {
			checkState("No manifest defined", manifest!=null);
		}
	}
}
