/*
 *  ICARUS 2 -  Interactive platform for Corpus Analysis and Research tools, University of Stuttgart
 *  Copyright (C) 2015 Markus Gärtner
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

 * $Revision: 419 $
 * $Date: 2015-07-23 22:36:36 +0200 (Do, 23 Jul 2015) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus2Core/core/de.ims.icarus2.model/source/de/ims/icarus2/model/standard/driver/virtual/VirtualDriver.java $
 *
 * $LastChangedDate: 2015-07-23 22:36:36 +0200 (Do, 23 Jul 2015) $
 * $LastChangedRevision: 419 $
 * $LastChangedBy: mcgaerty $
 */
package de.ims.icarus2.model.standard.driver.virtual;

import static de.ims.icarus2.model.standard.util.CorpusUtils.getName;
import static de.ims.icarus2.model.util.Conditions.checkArgument;
import static de.ims.icarus2.model.util.Conditions.checkNotNull;
import static de.ims.icarus2.model.util.Conditions.checkState;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import de.ims.icarus2.model.api.ModelErrorCode;
import de.ims.icarus2.model.api.ModelException;
import de.ims.icarus2.model.api.driver.ChunkInfo;
import de.ims.icarus2.model.api.driver.Driver;
import de.ims.icarus2.model.api.driver.DriverListener;
import de.ims.icarus2.model.api.driver.indices.IndexSet;
import de.ims.icarus2.model.api.driver.mods.DriverModule;
import de.ims.icarus2.model.api.layer.ItemLayer;
import de.ims.icarus2.model.api.layer.Layer;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.api.members.item.ItemLayerManager;
import de.ims.icarus2.model.manifest.api.ItemLayerManifest;
import de.ims.icarus2.model.standard.driver.AbstractDriver;

/**
 * Wraps an {@link ItemLayerManager} implementation and decorates it with the additional
 * features of a {@link Driver}.
 *
 * @author Markus Gärtner
 * @version $Id: VirtualDriver.java 419 2015-07-23 20:36:36Z mcgaerty $
 *
 */
public class VirtualDriver extends AbstractDriver {

	private ItemLayerManager itemLayerManager;

	private final TIntObjectMap<ItemLayer> idMap = new TIntObjectHashMap<>();

	private final Collection<DriverModule> modules = new ArrayList<>();

	private transient VirtualDriverBuilder builder;

	/**
	 * @param manifest
	 * @param corpus
	 * @throws ModelException
	 */
	protected VirtualDriver(VirtualDriverBuilder builder)
			throws ModelException {
		super(builder);

		modules.addAll(builder.getModules());

		this.builder = builder;
	}

	/**
	 * Creates a new {@link ItemLayerManager} for this driver by trying to use
	 * the following optional settings from the {@link VirtualDriverBuilder}
	 * supplied at constructor time (in the given order):
	 * <ol>
	 * <li>If a finished {@link ItemLayerManager} instance has been supplied, it will be used</li>
	 * <li>If a {@link Class} implementing the {@code ItemLayerManager} interface has been specified,
	 * it will be instantiated using the default no-args constructor. Note that this effectively makes
	 * the manager ignore any other settings on the builder!</li>
	 * <li>If a {@link Supplier} has been specified, it will be used to {@link Supplier#get() obtain} a single {@link ItemLayerManager}
	 * instance. Note that in this case builder settings can only be applied if the supplier argument is using
	 * an external reference to the builder!</li>
	 * <li>If a {@link Function} was provided that takes a {@link Driver} instance and returns an {@link ItemLayerManager}
	 * it will be called will be {@link Function#apply(Object) called} with this driver as argument. This is one of the only ways to
	 * have <i>some</i> access to settings of the original builder that have already been applied to this driver instance.</li>
	 * <li>If all the above attempts fail then the protected method {@link #defaultCreateItemLayerManager()} is called which allows
	 * the driver implementation to decide on the actual instantiation of a new manager.</li>
	 * </ol>
	 *
	 * @return
	 */
	protected ItemLayerManager createItemLayerManager() {

		// Try finished item layer manager
		final ItemLayerManager itemLayerManager = builder.getItemLayerManager();
		if(itemLayerManager!=null) {
			return itemLayerManager;
		}

		// Try explicit class definition
		Class<? extends ItemLayerManager> managerClass = builder.getManagerClass();
		if(managerClass!=null) {
			try {
				return managerClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new ModelException(ModelErrorCode.DELEGATION_FAILED,
						"Failed to instantiate item layer manager from class", e);
			}
		}

		// Try supplier
		Supplier<ItemLayerManager> supplier = builder.getSupplier();
		if(supplier!=null) {
			return supplier.get();
		}

		// Try Creator
		final Function<Driver, ItemLayerManager> creator = builder.getCreator();
		if(creator!=null) {
			return creator.apply(VirtualDriver.this);
		}

		return defaultCreateItemLayerManager();
	}

	/**
	 * Fallback method in the process of creating a new {@link ItemLayerManager} for this driver.
	 * This method is called only from within {@link #createItemLayerManager()} and then only in
	 * case all other ways of creating the manager have failed. For those alternatives check the
	 * setter methods in the {@link VirtualDriverBuilder} class.
	 *
	 * @see VirtualDriverBuilder
	 *
	 * @return
	 */
	protected ItemLayerManager defaultCreateItemLayerManager() {
		VirtualItemLayerManager itemLayerManager = new VirtualItemLayerManager();

		//FIXME change so that layers get copied over lazily
		itemLayerManager.addLayers(getContext());

		return itemLayerManager;
	}

	@Override
	public void forEachModule(Consumer<? super DriverModule> action) {
		modules.forEach(action);
	}

	public ItemLayerManager getItemLayerManager() {
		checkConnected();

		return itemLayerManager;
	}

	public ItemLayer getLayerForManifest(ItemLayerManifest manifest) {
		ensureLayerMap();

		return idMap.get(manifest.getUID());
	}

	protected final void ensureLayerMap() {
		if(idMap.isEmpty()) {
			for(Layer layer : getItemLayerManager().getLayers()) {
				idMap.put(layer.getManifest().getUID(), (ItemLayer)layer);
			}
		}
	}

	protected final void registerLayer(ItemLayer layer) {
		int id = layer.getManifest().getUID();
		if(idMap.containsKey(id))
			throw new ModelException(ModelErrorCode.ILLEGAL_STATE, "Layer already registered: "+getName(layer));
		idMap.put(id, layer);
	}

	protected final ItemLayer getLayerForId(int id) {
		return idMap.get(id);
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.ItemLayerManager#getItemCount(de.ims.icarus2.model.api.layer.ItemLayer)
	 */
	@Override
	public long getItemCount(ItemLayer layer) {
		return getItemLayerManager().getItemCount(layer);
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.ItemLayerManager#getItem(de.ims.icarus2.model.api.layer.ItemLayer, long)
	 */
	@Override
	public Item getItem(ItemLayer layer, long index) {
		return getItemLayerManager().getItem(layer, index);
	}

	/**
	 * @see de.ims.icarus2.model.api.members.item.ItemLayerManager#release(de.ims.icarus2.model.api.driver.indices.IndexSet[], de.ims.icarus2.model.api.layer.ItemLayer)
	 */
	@Override
	public void release(IndexSet[] indices, ItemLayer layer)
			throws InterruptedException {
		getItemLayerManager().release(indices, layer);
	}

	/**
	 * First creates the {@link ItemLayerManager} instance to be used for this driver
	 * and then {@link DriverModule#addNotify(Driver) notifies} all modules
	 *
	 * @see de.ims.icarus2.model.standard.driver.AbstractDriver#doConnect()
	 */
	@Override
	protected void doConnect() throws InterruptedException {

		super.doConnect();

		setItemLayerManager(createItemLayerManager());

		for(DriverModule module : modules) {
			module.addNotify(this);
		}
	}

	protected final void setItemLayerManager(ItemLayerManager itemLayerManager) {
		checkNotNull(itemLayerManager);
		checkState(this.itemLayerManager==null);

		this.itemLayerManager = itemLayerManager;
	}

	@Override
	protected void verifyInternals() {
		super.verifyInternals();

		checkState("Missing item layer manager", itemLayerManager!=null);
	}

	/**
	 * First {@link DriverModule#removeNotify(Driver) notifies} all modules and
	 * then deletes the {@link ItemLayerManager} of this driver.
	 * Note that in case subclasses wish to perform maintenance work on the
	 * item layer manager while disconnecting, they should do it <b>prior</b> to
	 * calling this super method!
	 *
	 * @see de.ims.icarus2.model.standard.driver.AbstractDriver#doDisconnect()
	 */
	@Override
	protected void doDisconnect() throws InterruptedException {

		for(DriverModule module : modules) {
			module.removeNotify(this);
		}

		itemLayerManager = null;
	}

	@Override
	public long getItemCount(ItemLayerManifest layer) {
		return (isReady() && isConnected()) ? getItemCount(getLayerForManifest(layer)) : NO_INDEX;
	}

	/**
	 * The default implementation delegates to the {@link ItemLayerManager} that was specified
	 * at construction time. In addition it ensures that all registered
	 * {@link DriverListener driver listeners} are notified in addition to the given {@code action}
	 * argument (note however, that the {@code action} callback will always be called prior to
	 * the listener notification).
	 * <p>
	 * If subclasses decide to perform actual data loading in this method, they should either make sure
	 * to call registered listeners themselves or simply delegate to the super method after the
	 * actual loading of data chunks.
	 *
	 * @see de.ims.icarus2.model.standard.driver.AbstractDriver#loadPrimaryLayer(de.ims.icarus2.model.api.driver.indices.IndexSet[], de.ims.icarus2.model.api.layer.ItemLayer, java.util.function.Consumer)
	 * @see ItemLayerManager#load(IndexSet[], ItemLayer, Consumer)
	 */
	@Override
	protected long loadPrimaryLayer(IndexSet[] indices, ItemLayer layer,
			Consumer<ChunkInfo> action) throws InterruptedException {

		// Unlike the ItemLayerManager a driver also has independent listeners to satisfy
		Consumer<ChunkInfo> compoundAction = c -> {
			fireChunksLoaded(layer, c);
		};

		if(action!=null) {
			// Priority: the supplied action should always come before registered listeners
			compoundAction = action.andThen(compoundAction);
		}

		// Delegate to wrapped ItemLayerManager for actual loading
		return getItemLayerManager().load(indices, layer, compoundAction);
	}

	public static class VirtualDriverBuilder extends DriverBuilder<VirtualDriverBuilder, VirtualDriver> {

		private ItemLayerManager itemLayerManager;
		private Supplier<ItemLayerManager> supplier;
		private Function<Driver, ItemLayerManager> creator;
		private Class<? extends ItemLayerManager> managerClass;

		private final Collection<DriverModule> modules = new ArrayList<>();

		public VirtualDriverBuilder itemLayerManager(ItemLayerManager itemLayerManager) {
			checkNotNull(itemLayerManager);
			checkState(this.itemLayerManager==null);
			checkState(this.supplier==null);
			checkState(this.creator==null);
			checkState(this.managerClass==null);

			this.itemLayerManager = itemLayerManager;

			return thisAsCast();
		}

		public ItemLayerManager getItemLayerManager() {
			return itemLayerManager;
		}

		public VirtualDriverBuilder supplier(Supplier<ItemLayerManager> supplier) {
			checkNotNull(supplier);
			checkState(this.supplier==null);
			checkState(this.itemLayerManager==null);
			checkState(this.creator==null);
			checkState(this.managerClass==null);

			this.supplier = supplier;

			return thisAsCast();
		}

		public Supplier<ItemLayerManager> getSupplier() {
			return supplier;
		}

		public VirtualDriverBuilder creator(Function<Driver, ItemLayerManager> creator) {
			checkNotNull(creator);
			checkState(this.creator==null);
			checkState(this.itemLayerManager==null);
			checkState(this.supplier==null);
			checkState(this.managerClass==null);

			this.creator = creator;

			return thisAsCast();
		}

		public Function<Driver, ItemLayerManager> getCreator() {
			return creator;
		}

		public VirtualDriverBuilder managerClass(Class<? extends ItemLayerManager> managerClass) {
			checkNotNull(managerClass);
			checkState(this.managerClass==null);
			checkState(this.creator==null);
			checkState(this.itemLayerManager==null);
			checkState(this.supplier==null);

			this.managerClass = managerClass;

			return thisAsCast();
		}

		public Class<? extends ItemLayerManager> getManagerClass() {
			return managerClass;
		}

		public void module(DriverModule module) {
			checkNotNull(module);
			checkState(!modules.contains(module));

			modules.add(module);
		}

		public void modules(Collection<? extends DriverModule> modules) {
			checkNotNull(modules);
			checkArgument(!modules.isEmpty());

			this.modules.addAll(modules);
		}

		public Collection<DriverModule> getModules() {
			return modules;
		}

		@Override
		public VirtualDriver create() {
			return new VirtualDriver(this);
		}
	}
}
