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
package de.ims.icarus2.model.standard.corpus;

import static de.ims.icarus2.util.Conditions.checkArgument;
import static de.ims.icarus2.util.Conditions.checkState;
import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import de.ims.icarus2.apiguard.Api;
import de.ims.icarus2.apiguard.Api.ApiType;
import de.ims.icarus2.apiguard.Guarded;
import de.ims.icarus2.apiguard.Guarded.MethodType;
import de.ims.icarus2.apiguard.Mandatory;
import de.ims.icarus2.model.api.ModelErrorCode;
import de.ims.icarus2.model.api.ModelException;
import de.ims.icarus2.model.api.corpus.Corpus;
import de.ims.icarus2.model.api.corpus.GenerationControl;
import de.ims.icarus2.model.api.edit.CorpusEditEvents;
import de.ims.icarus2.model.manifest.api.CorpusManifest;
import de.ims.icarus2.model.manifest.util.Messages;
import de.ims.icarus2.util.AbstractBuilder;
import de.ims.icarus2.util.annotations.TestableImplementation;
import de.ims.icarus2.util.events.EventObject;
import de.ims.icarus2.util.events.SimpleEventListener;
import de.ims.icarus2.util.io.IOUtil;
import it.unimi.dsi.fastutil.Stack;

/**
 * @author Markus Gärtner
 *
 */
@TestableImplementation(GenerationControl.class)
public class DefaultGenerationControl implements GenerationControl, SimpleEventListener {

	public static Builder builder() {
		return new Builder();
	}

	public static final String KEY_STAGE = "generation.stage";
	public static final String KEY_MAX_STAGE = "generation.maxStage";

	private final Corpus corpus;

	private final Supplier<UUID> uuidGenerator;

	private final Stack<String> storage;

	private transient Stage currentStage;

	protected DefaultGenerationControl(Builder builder) {
		corpus = builder.getCorpus();
		uuidGenerator = builder.getUuidGenerator();
		storage = builder.getStorage();

		// Access back-end storage to load history
		currentStage = loadStage(TOP);

		// Make sure we have a properly initialized current stage
		if(currentStage==null) {
			advance();
		}
	}

	/**
	 * Position designating the current stage on the stack
	 */
	protected static final int TOP = 0;

	/**
	 * Position designating the last (i.e. immediately prior
	 * to the current one) stage.
	 */
	protected static final int LAST = 1;

	protected Stage loadStage(int position) {
		String s = null;

		try {
			s = storage.peek(position);
		} catch(IndexOutOfBoundsException e) {
			// ignore
		}

		return s==null ? null : parseStage(s);
	}

//	/**
//	 * Persists the current stage in the corpus' {@link MetadataRegistry}.
//	 * <p>
//	 * Note that this method tries to acquire the global lock for the corpus
//	 * to ensure consistency of the two saved values.
//	 *
//	 * @throws ModelException in case the corpus lock is held by a foreign thread
//	 */
//	protected void storeStage() {
//
//		Lock lock = getCorpus().getLock();
//
//		// Check for lock free or held by current thread
//		if(lock.tryLock()) {
//			try {
//				MetadataRegistry registry = getCorpus().getMetadataRegistry();
//				registry.beginUpdate();
//				try {
//					registry.setLongValue(KEY_STAGE, getStage());
//					registry.setLongValue(KEY_MAX_STAGE, maxStage);
//				} finally {
//					registry.endUpdate();
//				}
//			} finally {
//				lock.unlock();
//			}
//		} else
//			throw new ModelException(ModelErrorCode.EDIT_UNSYNCHRONIZED_ACCESS,
//				"Unable to persist generation stage - corpus lock held by foreign thread");
//	}

	/**
	 * @see de.ims.icarus2.model.api.corpus.GenerationControl#getCorpus()
	 */
	@Override
	public Corpus getCorpus() {
		return corpus;
	}

	/**
	 * @see de.ims.icarus2.model.api.corpus.GenerationControl#getStage()
	 */
	@Override
	public Stage getStage() {
		return currentStage;
	}

	protected Stage newStage() {
		return new UniqueStage(uuidGenerator.get());
	}

	private void ensureMonotonicStageProgress(Stage now, Stage next) {
		if(now==null) {
			return;
		}

		if(next.compareTo(now)<1)
			throw new ModelException(getCorpus(), ModelErrorCode.MODEL_CORRUPTED_STATE,
					Messages.mismatch("Nonmonotonic stage detected", now, next));
	}

	/**
	 * @see de.ims.icarus2.model.api.corpus.GenerationControl#advance()
	 */
	@Override
	public Stage advance() {
		Lock lock = getCorpus().getLock();

		// Check for lock free or held by current thread
		if(lock.tryLock()) {
			try {
				Stage newStage = newStage();

				ensureMonotonicStageProgress(currentStage, newStage);

				storage.push(newStage.getStringValue());

				currentStage = newStage;

				return newStage;
			} finally {
				lock.unlock();
			}
		}

		throw new ModelException(ModelErrorCode.EDIT_UNSYNCHRONIZED_ACCESS,
				"Unable to increment generation stage - corpus lock held by foreign thread");
	}

	@Override
	public boolean step(Stage expectedStage, Stage newStage) {
		requireNonNull(expectedStage);
		requireNonNull(newStage);

		Lock lock = getCorpus().getLock();

		// Check for lock free or held by current thread
		if(lock.tryLock()) {
			try {
				if(!currentStage.equals(expectedStage)) {
					return false;
				}

				Stage lastStage = loadStage(LAST);
				if(!newStage.equals(lastStage)) {
					return false;
				}

				storage.pop();
				currentStage = lastStage;

				return true;
			} finally {
				lock.unlock();
			}
		}

		throw new ModelException(ModelErrorCode.EDIT_UNSYNCHRONIZED_ACCESS,
				"Unable to change generation stage - corpus lock held by foreign thread");
	}

	/**
	 * @see de.ims.icarus2.model.api.corpus.GenerationControl#close()
	 */
	@Override
	public void close() {
		if(storage instanceof Closeable) {
			IOUtil.closeSilently((Closeable) storage);
		}
	}

	/**
	 * @see de.ims.icarus2.model.api.corpus.GenerationControl#parseStage(java.lang.String)
	 */
	@Override
	public Stage parseStage(String s) {
		return new UniqueStage(UUID.fromString(s));
	}

	/**
	 * @see de.ims.icarus2.util.events.SimpleEventListener#invoke(java.lang.Object, de.ims.icarus2.util.events.EventObject)
	 */
	@Override
	public void invoke(Object sender, EventObject event) {
		switch (event.getName()) {

		//FIXME currently we assume every atomic change to be a full grown stage change. is this intended or too fine-grained?

		// If an atomic change occurs for the first time we simple do a generation step forward
		case CorpusEditEvents.EXECUTE:
			advance();
			break;

		default:
			break;
		}
	}

	private static final class UniqueStage implements Stage {

		private final UUID uuid;

		UniqueStage(UUID uuid) {
			this.uuid = requireNonNull(uuid);
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Stage@"+getStringValue();
		}

		@Override
		public String getStringValue() {
			return uuid.toString();
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if(obj==this) {
				return true;
			} else if(obj instanceof UniqueStage) {
				return uuid.equals(((UniqueStage)obj).uuid);
			}

			return false;
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return uuid.hashCode();
		}

		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Stage other) {
			return uuid.compareTo(((UniqueStage)other).uuid);
		}

	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	@Api(type=ApiType.BUILDER)
	public static class Builder extends AbstractBuilder<Builder, DefaultGenerationControl> {
		private Corpus corpus;
		private Supplier<UUID> uuidGenerator;
		private Stack<String> storage;

		protected Builder() {
			// no-op
		}

		/**
		 * Sets the corpus to be used with the generation control.
		 * Note that the corpus must be {@link CorpusManifest#isEditable() editable}!
		 *
		 * @param corpus
		 * @return
		 */
		@Guarded(methodType=MethodType.BUILDER)
		@Mandatory
		public Builder corpus(Corpus corpus) {
			requireNonNull(corpus);
			checkState("Corpus alread set", this.corpus == null);
			checkArgument("Corpus must be editable", corpus.getManifest().isEditable());

			this.corpus = corpus;

			return thisAsCast();
		}

		/**
		 * @return the corpus
		 */
		@Guarded(methodType=MethodType.GETTER)
		@Nullable
		public Corpus getCorpus() {
			return corpus;
		}

		/**
		 * Sets the generator used for creating new {@link UUID} objects for
		 * internal use. Note that ideally the generator should return
		 * time-based UUIDs.
		 *
		 * @param uuidGenerator
		 * @return
		 */
		@Guarded(methodType=MethodType.BUILDER)
		@Mandatory
		public Builder uuidGenerator(Supplier<UUID> uuidGenerator) {
			requireNonNull(uuidGenerator);
			checkState("UUID generator alread set", this.uuidGenerator == null);

			this.uuidGenerator = uuidGenerator;

			return thisAsCast();
		}

		/**
		 * @return the uuidGenerator
		 */
		@Guarded(methodType=MethodType.GETTER)
		@Nullable
		public Supplier<UUID> getUuidGenerator() {
			return uuidGenerator;
		}

		/**
		 * Sets the back-end storage where stage objects are serialized
		 * from and to in string form.
		 * <p>
		 * Note that if this storage also implements the {@link Closeable}
		 * interface, it will be {@link Closeable#close() closed}
		 * when the generation control's {@link GenerationControl#close()}
		 * method is invoked.
		 *
		 *
		 * @param storage
		 * @return
		 */
		@Guarded(methodType=MethodType.BUILDER)
		@Mandatory
		public Builder storage(Stack<String> storage) {
			requireNonNull(storage);
			checkState("UUID storage alread set", this.storage == null);

			this.storage = storage;

			return thisAsCast();
		}

		/**
		 * @return the storage
		 */
		@Guarded(methodType=MethodType.GETTER)
		@Nullable
		public Stack<String> getStorage() {
			return storage;
		}

		/**
		 * @see de.ims.icarus2.util.AbstractBuilder#validate()
		 */
		@Override
		protected void validate() {
			super.validate();

			checkState("Corpus not set", corpus!=null);
			checkState("UUID generator not set", uuidGenerator!=null);
			checkState("UUID storage not set", storage!=null);
		}

		/**
		 * @see de.ims.icarus2.util.AbstractBuilder#create()
		 */
		@Override
		protected DefaultGenerationControl create() {
			return new DefaultGenerationControl(this);
		}
	}
}
