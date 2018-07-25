/*
 * ICARUS2 Corpus Modeling Framework
 * Copyright (C) 2014-2018 Markus Gärtner <markus.gaertner@uni-stuttgart.de>
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
package de.ims.icarus2.model.standard.members.layers.annotation.packed;

import static de.ims.icarus2.util.Conditions.checkArgument;
import static de.ims.icarus2.util.Conditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ims.icarus2.model.api.layer.AnnotationLayer.AnnotationStorage;
import de.ims.icarus2.model.api.members.item.Item;
import de.ims.icarus2.model.manifest.api.AnnotationManifest;
import de.ims.icarus2.util.AbstractBuilder;
import de.ims.icarus2.util.IcarusUtils;
import de.ims.icarus2.util.Part;
import de.ims.icarus2.util.collections.LazyCollection;
import de.ims.icarus2.util.collections.LazyMap;
import de.ims.icarus2.util.collections.LookupList;
import de.ims.icarus2.util.mem.ByteAllocator;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * @author Markus Gärtner
 *
 * @param <E> type of elements that are used for mapping to byte chunks
 * @param <O> type of the owning context, such as an {@link AnnotationStorage}
 */
public class PackedDataManager<E extends Object, O extends Object> implements Part<O> {

	private static final Logger log = LoggerFactory.getLogger(PackedDataManager.class);

	private static final int NO_MAPPING_VALUE = IcarusUtils.UNSET_INT;

	/**
	 * Flag to indicate whether or not the chunk addressing should
	 * use weak references to {@link Item items} as keys for the mapping.
	 */
	private final boolean weakKeys;

	/**
	 * Hint on the initial capacity of the chunk address mapping structure.
	 */
	private final int initialCapacity;

	/**
	 * Flag to indicate whether the manager is allowed to cram
	 * multiple boolean values into a single byte.
	 */
	private final boolean allowBitPacking;

	/**
	 * Flag to indicate whether or not this manager supports
	 * dynamic add and remove of new annotation slots while
	 * the manager is active, i.e. when the underlying
	 * byte storage is live.
	 * <p>
	 * Note that this setting has serious influence on the
	 * expected performance! Modifying the current chunk
	 * composition results in a potentially very expensive
	 * duplication and reconstruction of the underlying byte
	 * storage and can block access for an extended period
	 * of time..
	 */
	private final boolean allowDynamicChunkComposition;

	/**
	 * Keeps track of the number of annotation storages using this manager.
	 * Allows lazy creation of the actual storage and to release the entire
	 * data once it is no longer needed.
	 */
	private final AtomicInteger useCounter = new AtomicInteger(0);

	/**
	 * Contains the actual raw data.
	 */
	private transient ByteAllocator rawStorage;

	/**
	 * For more efficient interaction with the raw storage
	 * in terms of locality.
	 */
	private transient ByteAllocator.Cursor cursor;

	/**
	 * Maps individual items to their associated chunks of
	 * allocated data in the {@link #rawStorage raw storage}.
	 */
	private transient Object2IntMap<E> chunkAddresses;

	/**
	 * Lock for accessing the raw annotation storage and chunk mapping.
	 */
	private final StampedLock lock = new StampedLock();

	private final IntFunction<ByteAllocator> storageSource;

	/**
	 * The lookup structure for defining
	 */
	private LookupList<PackageHandle> packageHandles = new LookupList<>();

	protected PackedDataManager(Builder<E,O> builder) {
		requireNonNull(builder);

		//TODO populate fields
		initialCapacity = builder.getInitialCapacity();
		storageSource = builder.getStorageSource();

		//TODO implement functionality and adjust
		weakKeys = false;
		allowBitPacking = true;
		allowDynamicChunkComposition = false;
	}

	public void registerHandles(Set<PackageHandle> handles) {
		requireNonNull(handles);
		checkArgument("No handles to register", !handles.isEmpty());
		checkState("Cannot register handles with a live storage", rawStorage!=null);

		//TODO merge new handles into our lookup
	}

	public void unregisterHandles(Set<PackageHandle> handles) {
		//TODO
	}

	/**
	 * Looks up the {@link PackageHandle handle} associated with the
	 * given {@link AnnotationManifest manifest}.
	 * <p>
	 * If no handle is available for the manifest, this method will
	 * return {@code null}.
	 *
	 * @param manifest
	 * @return
	 */
	public PackageHandle lookupHandle(AnnotationManifest manifest) {
		requireNonNull(manifest);

		for(PackageHandle handle : packageHandles) {
			if(manifest.equals(handle.manifest)) {
				return handle;
			}
		}

		return null;
	}

	/**
	 * Looks up the {@link PackageHandle handles} associated with the specified
	 * {@code manifests}.
	 *
	 * @param manifests
	 * @return
	 */
	public Map<AnnotationManifest, PackageHandle> lookupHandles(Set<AnnotationManifest> manifests) {
		requireNonNull(manifests);
		checkArgument("Set of manifests to look up must not be empty", !manifests.isEmpty());

		LazyMap<AnnotationManifest, PackageHandle> result = LazyMap.lazyHashMap(manifests.size());

		for(PackageHandle handle : packageHandles) {
			if(manifests.contains(handle.manifest)) {
				result.add(handle.manifest, handle);
			}
		}

		return result.getAsMap();
	}

	public boolean isWeakKeys() {
		return weakKeys;
	}

	private int getRequiredChunkSize() {
		// Required size in full bytes
		int size = 0;
		// Individual bits required for boolean data
		int bits = 0;

		for(PackageHandle handle : packageHandles) {
			BytePackConverter converter = handle.converter;

			if(converter.sizeInBytes()==0) {
				bits += converter.sizeInBits();
			} else {
				size += converter.sizeInBytes();
			}
		}

		// Add sufficient bytes to hold all the individual bits for
		// boolean data
		// TODO should we actually block bit packing when flag isn't set?
		if(bits>0) {
			size += Math.ceil(bits/8D);
		}

		return size;
	}

	/**
	 * Initializes the following components if the method is invoked
	 * for the first time after the manager has been created or after
	 * it has been reset:
	 * <ul>
	 * <li>The mapping facility for translating items to their chunk index values.</li>
	 * <li>The raw byte storage containing all the actual data.</li>
	 * <li>The cursor used to navigate the raw storage.</li>
	 * </ul>
	 *
	 * @see de.ims.icarus2.util.Part#addNotify(java.lang.Object)
	 */
	@Override
	public void addNotify(O owner) {
		if(useCounter.getAndIncrement()==0) {
			long stamp = lock.writeLock();
			try {
				chunkAddresses = buildMap();

				// Initialize a new storage based on our current chunk size
				rawStorage = storageSource.apply(getRequiredChunkSize());

				cursor = rawStorage.newCursor();
			} finally {
				lock.unlockWrite(stamp);
			}
		}

		//TODO
	}

	/**
	 * Releases the mapping facility, the raw data storage and
	 * the navigation cursor for that storage if this manager has
	 * no more {@link AnnotationStorage} instances linked to it.
	 *
	 * @see de.ims.icarus2.util.Part#removeNotify(java.lang.Object)
	 */
	@Override
	public void removeNotify(O owner) {
		if(useCounter.decrementAndGet()==0) {
			long stamp = lock.writeLock();
			try {
				chunkAddresses.clear();
				chunkAddresses = null;

				rawStorage.clear();
				rawStorage = null;

				cursor.clear();
				cursor = null;
			} finally {
				lock.unlockWrite(stamp);
			}
		}
	}

	/**
	 * @see de.ims.icarus2.util.Part#isAdded()
	 */
	@Override
	public boolean isAdded() {
		return useCounter.get()>0;
	}

	protected Object2IntMap<E> buildMap() {
		if(isWeakKeys()) {
			log.info("No implementation for weak keys available yet - defaulting to regular map implementation");
		}

		Object2IntMap<E> result = new Object2IntOpenHashMap<>(initialCapacity);
		result.defaultReturnValue(NO_MAPPING_VALUE);

		return result;
	}



	// Item management

	/**
	 * Reserves a chunk of byte buffer for the specified {@code item}
	 * if it hasn't been registered already.
	 *
	 * @param item the item for which a chunk of byte buffer should be reserved
	 * @return {@code true} iff the item hasn't been registered already
	 */
	public boolean register(E item) {
		requireNonNull(item);

		boolean isNewItem;

		long stamp = lock.writeLock();
		try {
			int id = chunkAddresses.getInt(item);

			isNewItem = id==NO_MAPPING_VALUE;

			if(isNewItem) {
				id = rawStorage.alloc();

				chunkAddresses.put(item, id);
			}
		} finally {
			lock.unlockWrite(stamp);
		}

		return isNewItem;
	}

	/**
	 * Releases the reserved chunk of byte buffer for the specified
	 * {@code item} if there has been one reserved previously.
	 *
	 * @param item
	 * @return
	 */
	public boolean unregister(E item) {
		requireNonNull(item);

		boolean wasKnownItem = false;;

		long stamp = lock.writeLock();
		try {
			int id = chunkAddresses.removeInt(item);

			if(id!=NO_MAPPING_VALUE) {
				rawStorage.free(id);
				wasKnownItem = true;
			}
		} finally {
			lock.unlockWrite(stamp);
		}

		return wasKnownItem;
	}

	/**
	 * Releases the reserved chunks of byte buffer for elements
	 * returned by the given {@link Supplier}. The method returns
	 * the total number of successfully unregistered elements.
	 *
	 * @param item
	 * @return
	 */
	public int unregister(Supplier<? extends E> source) {
		requireNonNull(source);

		int removedItems = 0;

		long stamp = lock.writeLock();
		try {
			E item;

			// Continue as long as we get new items supplied
			while((item = source.get()) !=null) {
				int id = chunkAddresses.removeInt(item);

				// If item had a valid address, deallocate and count
				if(id!=NO_MAPPING_VALUE) {
					rawStorage.free(id);
					removedItems++;
				}
			}
		} finally {
			lock.unlockWrite(stamp);
		}

		return removedItems;
	}

	public boolean isRegistered(E item) {
		requireNonNull(item);

		// Try optimistically first
		long stamp = lock.tryOptimisticRead();
		boolean registered = chunkAddresses.containsKey(item);

		// Run a real lock if needed
		if(!lock.validate(stamp)) {
			lock.readLock();
			try {
				registered = chunkAddresses.containsKey(item);
			} finally {
				lock.unlockRead(stamp);
			}
		}

		return registered;
	}

	/**
	 * Clears (i.e. sets to the respective {@code noEntryValue})
	 * all the annotations defined by the {@code handles} array
	 * for as long as the specified supplier produces {@code ids}
	 * that are different to {@link IcarusUtils#UNSET_INT -1}.
	 *
	 * @param ids
	 * @param handles
	 */
	public void clear(IntSupplier ids, PackageHandle[] handles) {
		requireNonNull(ids);
		requireNonNull(handles);
		checkArgument("Empty handles array", handles.length>0);

		long stamp = lock.writeLock();
		try {
			int id;
			while((id = ids.getAsInt()) != NO_MAPPING_VALUE) {
				cursor.moveTo(id);
				clearCurrent(handles);
			}
		} finally {
			lock.unlockWrite(stamp);
		}
	}

	/**
	 * Clears (i.e. sets to the respective {@code noEntryValue})
	 * all the annotations defined by the {@code handles} array
	 * for all currently registered items.
	 *
	 * @param handles
	 */
	public void clear(PackageHandle[] handles) {
		requireNonNull(handles);
		checkArgument("Empty handles array", handles.length>0);

		long stamp = lock.writeLock();
		try {

			for(IntIterator it = chunkAddresses.values().iterator(); it.hasNext();) {
				cursor.moveTo(it.nextInt());
				clearCurrent(handles);
			}
		} finally {
			lock.unlockWrite(stamp);
		}
	}

	/**
	 * Sets the byte chunks for the specified
	 * handles to their respective noEntryValue
	 * representations.
	 * <p>
	 * Must be called under write lock.
	 */
	private void clearCurrent(PackageHandle[] handles) {

		for(int i=handles.length-1; i>=0; i--) {
			PackageHandle handle = handles[i];
			handle.converter.setValue(handle, cursor, handle.noEntryValue);
		}
	}

	private int getChunkAddress(E item) {
		int id = chunkAddresses.getInt(item);

		/*
		 *  SPECIAL NOTE: We shouldn't throw an exception here since
		 *  it would break our optimistic locking:
		 *
		 *  I
		 */


//		if(id==NO_MAPPING_VALUE)
//			throw new ModelException(ModelErrorCode.MODEL_INVALID_REQUEST,
//					"Unregistered item: "+ModelUtils.toString(item));

		return id;
	}

	/**
	 * Shift cursor to the address of given item.
	 *
	 * @param item
	 * @return {@code true} iff given item had a valid address
	 */
	private boolean prepareCursor(E item) {
		int id = getChunkAddress(item);
		boolean validId =  id!=NO_MAPPING_VALUE;

		if(validId) {
			cursor.moveTo(id);
		}

		return validId;
	}

	// GetXXX methods

	/**
	 * Reads the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item} as a {@code boolean}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @return the annotation for {@code item} specified by {@code handle} as an {@code boolean}
	 *
	 * @see BytePackConverter#getBoolean(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor)
	 */
	public boolean getBoolean(E item, PackageHandle handle) {

		boolean result = ((Boolean)handle.noEntryValue).booleanValue();

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			result = handle.converter.getBoolean(handle, cursor);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					result = handle.converter.getBoolean(handle, cursor);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}

		return result;
	}

	/**
	 * Reads the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item} as an {@code int}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @return the annotation for {@code item} specified by {@code handle} as an {@code int}
	 *
	 * @see BytePackConverter#getInteger(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor)
	 */
	public int getInteger(E item, PackageHandle handle) {

		int result = ((Number)handle.noEntryValue).intValue();

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			result = handle.converter.getInteger(handle, cursor);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					result = handle.converter.getInteger(handle, cursor);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}

		return result;
	}

	/**
	 * Reads the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item} as a {@code long}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @return the annotation for {@code item} specified by {@code handle} as a {@code long}
	 *
	 * @see BytePackConverter#getLong(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor)
	 */
	public long getLong(E item, PackageHandle handle) {

		long result = ((Number)handle.noEntryValue).longValue();

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			result = handle.converter.getLong(handle, cursor);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					result = handle.converter.getLong(handle, cursor);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}

		return result;
	}

	/**
	 * Reads the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item} as a {@code float}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @return the annotation for {@code item} specified by {@code handle} as a {@code float}
	 *
	 * @see BytePackConverter#getFloat(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor)
	 */
	public float getFloat(E item, PackageHandle handle) {

		float result = ((Number)handle.noEntryValue).floatValue();

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			result = handle.converter.getFloat(handle, cursor);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					result = handle.converter.getFloat(handle, cursor);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}

		return result;
	}

	/**
	 * Reads the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item} as a {@code double}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @return the annotation for {@code item} specified by {@code handle} as a {@code double}
	 *
	 * @see BytePackConverter#getDouble(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor)
	 */
	public double getDouble(E item, PackageHandle handle) {

		double result = ((Number)handle.noEntryValue).doubleValue();

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			result = handle.converter.getDouble(handle, cursor);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					result = handle.converter.getDouble(handle, cursor);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}

		return result;
	}

	/**
	 * Reads the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @return the annotation for {@code item} specified by {@code handle}
	 *
	 * @see BytePackConverter#getValue(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor)
	 */
	public Object getValue(E item, PackageHandle handle) {

		Object result = handle.noEntryValue;

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			result = handle.converter.getValue(handle, cursor);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					result = handle.converter.getValue(handle, cursor);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}

		return result;
	}

	// SetXXX methods

	/**
	 * Changes the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @param value the new annotation value to use
	 *
	 * @see BytePackConverter#setBoolean(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor, boolean)
	 */
	public void setBoolean(E item, PackageHandle handle, boolean value) {

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			handle.converter.setBoolean(handle, cursor, value);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					handle.converter.setBoolean(handle, cursor, value);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}
	}

	/**
	 * Changes the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @param value the new annotation value to use
	 *
	 * @see BytePackConverter#setInteger(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor, int)
	 */
	public void setInteger(E item, PackageHandle handle, int value) {

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			handle.converter.setInteger(handle, cursor, value);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					handle.converter.setInteger(handle, cursor, value);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}
	}

	/**
	 * Changes the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @param value the new annotation value to use
	 *
	 * @see BytePackConverter#setLong(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor, long)
	 */
	public void setLong(E item, PackageHandle handle, long value) {

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			handle.converter.setLong(handle, cursor, value);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					handle.converter.setLong(handle, cursor, value);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}
	}

	/**
	 * Changes the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @param value the new annotation value to use
	 *
	 * @see BytePackConverter#setFloat(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor, float)
	 */
	public void setFloat(E item, PackageHandle handle, float value) {

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			handle.converter.setFloat(handle, cursor, value);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					handle.converter.setFloat(handle, cursor, value);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}
	}

	/**
	 * Changes the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @param value the new annotation value to use
	 *
	 * @see BytePackConverter#setDouble(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor, double)
	 */
	public void setDouble(E item, PackageHandle handle, double value) {

		long stamp = lock.tryOptimisticRead();

		// Try optimistically
		if(prepareCursor(item)) {
			handle.converter.setDouble(handle, cursor, value);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					handle.converter.setDouble(handle, cursor, value);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}
	}

	/**
	 * Changes the stored annotation value specified by the given {@code handle}
	 * for the selected {@code item}. This method internally
	 * calls the associated {@link BytePackConverter converter} and ensures
	 * proper synchronization.
	 *
	 * @param item target of the annotation
	 * @param handle the specification which annotation to access
	 * @param value the new annotation value to use
	 *
	 * @see BytePackConverter#setValue(PackageHandle, de.ims.icarus2.util.mem.ByteAllocator.Cursor, Object)
	 */
	public void setValue(E item, PackageHandle handle, Object value) {

		long stamp = lock.tryOptimisticRead();

		if(value==null) {
			value = handle.noEntryValue;
		}

		// Try optimistically
		if(prepareCursor(item)) {
			handle.converter.setValue(handle, cursor, value);
		}

		// Do a real locking in case we encountered parallel modifications
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				if(prepareCursor(item)) {
					handle.converter.setValue(handle, cursor, value);
				}
			} finally {
				lock.unlockRead(stamp);
			}
		}
	}

	// Utility

	/**
	 * Returns whether or not this manager contains any annotation values
	 * available. This method uses a somewhat simplistic approach and
	 * assumes that any registered item is meant to have at least one
	 * annotation associated with it.
	 * Therefore this implementation only checks whether or not the
	 * underlying lookup structure for mapping items to chunk ids
	 * contains at least one entry.
	 *
	 * @return
	 */
	public boolean hasValues() {
		return !chunkAddresses.isEmpty();
	}

	/**
	 * Forwards to the {@link #isRegistered(Item)} method to simply
	 * check if the given {@link Item} is known to this manager.
	 * Currently this method does not perform any real checks on the
	 * actual annotation values associated with the item!
	 *
	 * @param item
	 * @return
	 */
	public boolean hasValues(E item) {
		return isRegistered(item);
	}

	/**
	 * Checks whether the specified {@code item} has at least one
	 * valid annotation for any of the given {@link PackageHandle handles}.
	 * The optional {@code action} callback is used to report all the
	 * individual handles that were found to have annotations for
	 * the given {@code item}.
	 *
	 * @param item
	 * @param handles
	 * @param action
	 * @return
	 */
	public boolean collectHandles(E item, Collection<PackageHandle> handles, Consumer<? super PackageHandle> action) {

		boolean result;

		// Using lazy collection can prevent necessity of creating real buffer
		LazyCollection<PackageHandle> buffer = LazyCollection.lazySet();

		// Try to optimistically collect the information
		long stamp = lock.tryOptimisticRead();
		result = collectHandlesUnsafe(item, handles, buffer);

		// If we failed, go and properly lock before trying again
		if(!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				// Make sure the buffer doesn't hold duplicates or stale information
				buffer.clear();
				result = collectHandlesUnsafe(item, handles, buffer);
			} finally {
				lock.unlockRead(stamp);
			}
		}

		// Don't forget to actually report the handles for which we found annotation values
		if(result) {
			buffer.forEach(action);
		}

		return result;
	}

	private boolean collectHandlesUnsafe(E item, Collection<PackageHandle> handles, Consumer<? super PackageHandle> action) {
		boolean result = false;

		// Move to data chunk and then go through all the specified handles
		if(prepareCursor(item)) {
			for(PackageHandle handle : handles) {
				// Fetch actual and the "default" value
				Object value = handle.converter.getValue(handle, cursor);
				Object noEntryValue = handle.noEntryValue;

				// If current value is different to default, report it
				if(!handle.converter.equal(value, noEntryValue)) {
					result = true;
					if(action!=null) {
						action.accept(handle);
					}
				}
			}
		}

		return result;
	}

	/**
	 * Bundles all the information needed by the {@link PackedDataManager} to map
	 * annotation values for individual {@link Item items} to byte slots of a
	 * {@link ByteAllocator}.
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static final class PackageHandle {

		/**
		 * Annotation used for accessing this info
		 */
		private final AnnotationManifest manifest;

		private final Object noEntryValue;

		/**
		 * The converter used to translate between raw byte data
		 * and the actual annotation types in case of complex types.
		 */
		private final BytePackConverter converter;

		/**
		 * Utility objects used by the converter to
		 * process raw byte data. Also used for synchronization
		 * when present.
		 * <p>
		 * In case a converter does not {@link BytePackConverter#createContext() provide}
		 * his own context, it is assumed to be stateless.
		 */
		private final Object converterContext;

		/**
		 * Position in the lookup table.
		 */
		private int index = IcarusUtils.UNSET_INT;

		/**
		 * Byte offset within chunk of raw data.
		 */
		private int offset = IcarusUtils.UNSET_INT;

		/**
		 * For packed boolean values this indicates the bit within a single
		 * byte that is used for storing the annotation value for this handle.
		 */
		private int bit = IcarusUtils.UNSET_INT;

		/**
		 * @param key
		 * @param index
		 * @param converter
		 */
		private PackageHandle(AnnotationManifest manifest, BytePackConverter converter) {
			this.manifest = requireNonNull(manifest);
			this.converter = requireNonNull(converter);

			converterContext = converter.createContext();

			noEntryValue = manifest.getNoEntryValue();
		}

		public AnnotationManifest getManifest() {
			return manifest;
		}

		public BytePackConverter getConverter() {
			return converter;
		}

		public Object getConverterContext() {
			return converterContext;
		}

		public int getIndex() {
			return index;
		}

		public int getOffset() {
			return offset;
		}

		public int getBit() {
			return bit;
		}

		void setOffset(int offset) {
			this.offset = offset;
		}

		void setIndex(int index) {
			this.index = index;
		}

		void setBit(int bit) {
			this.bit = bit;
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 * @param <E>
	 */
	public static class Builder<E extends Object, O extends Object> extends AbstractBuilder<Builder<E,O>, PackedDataManager<E,O>> {

		private IntFunction<ByteAllocator> storageSource;

		private int initialCapacity = 0;

		public Builder<E,O> storageSource(IntFunction<ByteAllocator> storageSource) {
			requireNonNull(storageSource);
			checkState("Storage source already set", this.storageSource==null);

			this.storageSource = storageSource;

			return thisAsCast();
		}

		public IntFunction<ByteAllocator> getStorageSource() {
			return storageSource;
		}

		public Builder<E,O> initialCapacity(int initialCapacity) {
			checkArgument("Initial capacity must be greater than 0", initialCapacity>0);
			checkState("Initial capacity already set", this.initialCapacity==0);

			this.initialCapacity = initialCapacity;

			return thisAsCast();
		}

		public int getInitialCapacity() {
			return initialCapacity;
		}

		/**
		 * @see de.ims.icarus2.util.AbstractBuilder#validate()
		 */
		@Override
		protected void validate() {
			super.validate();

			checkState("Missing storage source", storageSource!=null);
			checkState("Missing initial capacity", initialCapacity>0);
		}

		/**
		 * @see de.ims.icarus2.util.AbstractBuilder#create()
		 */
		@Override
		protected PackedDataManager<E,O> create() {
			return new PackedDataManager<>(this);
		}

	}
}