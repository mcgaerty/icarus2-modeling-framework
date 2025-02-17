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
package de.ims.icarus2.model.api.registry;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import de.ims.icarus2.apiguard.OptionalMethod;
import de.ims.icarus2.model.api.ModelException;
import de.ims.icarus2.model.api.corpus.Corpus;
import de.ims.icarus2.model.api.driver.Driver;
import de.ims.icarus2.model.api.events.CorpusLifecycleListener;
import de.ims.icarus2.model.api.io.FileManager;
import de.ims.icarus2.model.api.view.paged.CorpusModel;
import de.ims.icarus2.model.api.view.paged.PagedCorpusView;
import de.ims.icarus2.model.manifest.api.ContextManifest;
import de.ims.icarus2.model.manifest.api.CorpusManifest;
import de.ims.icarus2.model.manifest.api.ImplementationManifest;
import de.ims.icarus2.model.manifest.api.ManifestRegistry;
import de.ims.icarus2.util.AccumulatingException;
import de.ims.icarus2.util.id.Identity;
import de.ims.icarus2.util.io.resource.ResourceProvider;

/**
 * The {@code CorpusManager} is responsible for creating and managing live {@link Corpus corpora}
 * during their respective lifecycle. A corpus is in exactly one of the following states and can
 * only transition between them in a well-defined order:
 * <ul>
 * <li>The corpus is <b>unconnected</b> but valid for being connected. This is the basic state for every
 * new corpus manifest when added to this manager's {@link CorpusRegistry registry}.</li>
 * <li>Once requested to connect, a corpus transitions to the <b>connecting</b> state. This will either last
 * until it is properly connected or the manager encountered an error in which case the corpus will be marked as <b>bad</b>.</li>
 * <li>A <b>connected</b> corpus can finally be interacted with in terms of its content, which means client code
 * can query its {@link Driver drivers} or request {@link PagedCorpusView views} to inspect data.</li>
 * <li>After a live corpus is no longer required it can be requested to start <b>disconnecting</b> which will
 * cause it to close all open views and release previously held data. Again, errors during this phase will result
 * in the corpus marked as bad by the manager.</li>
 * <li>When a request to either connect or disconnect a corpus is terminated prematurely by means of an error,
 * the manager marks the respective corpus as being <b>bad</b>, meaning it is prevented from further such attempts.
 * Note that there is a mechanism in place that allows a client to {@link #resetBadCorpus(CorpusManifest) recover}
 * a corpus from its bad state. However, this should be used carefully as explained in the method's documentation.</li>
 * <li>Besides getting automatically locked down by the manager, a corpus can also be <b>disabled</b> by the client
 * to prevent it from any attempts to dis-/connect it. Unlike the bad state that stems from erroneous behavior of a
 * corpus during a transition phase, {@link #disableCorpus(CorpusManifest) disabling} and again
 * {@link #enableCorpus(CorpusManifest) enabling} a corpus can only happen when it is fully unconnected and in a
 * clean state. This mechanism exists for situations where a global lock down functionality besides typical fine-grained
 * access control patterns is required.</li>
 * </ul>
 *
 * Possible transitions are as follows:
 *
 * <table border="1">
 * <tr align="center">
 * 	<th>State</th>
 *  <th>unconnected/enabled</th>
 *  <th>connecting</th>
 *  <th>connected</th>
 *  <th>disconnecting</th>
 *  <th>bad</th>
 *  <th>disabled</th>
 * </tr>
 * <tr align="center">
 *  <td>unconnected/enabled</td><td>&nbsp;</td><td>X</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>X</td>
 * </tr>
 * <tr align="center">
 *  <td>connecting</td><td>&nbsp;</td><td>&nbsp;</td><td>X</td><td>&nbsp;</td><td>X</td><td>&nbsp;</td>
 * </tr>
 * <tr align="center">
 *  <td>connected</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>X</td><td>&nbsp;</td><td>&nbsp;</td>
 * </tr>
 * <tr align="center">
 *  <td>disconnecting</td><td>X</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>X</td><td>&nbsp;</td>
 * </tr>
 * <tr align="center">
 *  <td>bad</td><td>(X)</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td>
 * </tr>
 * <tr align="center">
 *  <td>disabled</td><td>X</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td>
 * </tr>
 * </table>
 *
 * <p>
 * The corpus manager uses {@link CorpusLifecycleListener lifecycle listeners} to publish information about
 * a corpus' state transitions. Note that only when a corpus is connected successfully will listeners
 * be passed the reference to a live corpus. In all other instances only the corresponding {@link CorpusManifest}
 * object will be provided!
 *
 * @author Markus Gärtner
 *
 * @see ManifestRegistry
 */
public interface CorpusManager {

	/**
	 * Obtains the global setting denoted by the {@code key} parameter.
	 *
	 * @param key
	 * @return
	 */
	String getProperty(String key);

	//TODO keep the properties interface read-only or allow some way of programmatically setting new values?

	//TODO create some interface to gather statistics and situational snapshots of a corpus' state (lifetime, views created, etc...)

	/**
	 *
	 * @return the registry that hosts legal manifests for this manager
	 */
	ManifestRegistry getManifestRegistry();

	/**
	 * Returns the global client wide registry for storing of metadata information.
	 *
	 * @return
	 */
	MetadataRegistry getMetadataRegistry();

	/**
	 * Returns the optional file manager that can be used to provide a unified
	 * access to shared file resources.
	 *
	 * @return
	 */
	FileManager getFileManager();

	/**
	 * Returns the optional resource provider that is used to access the content of
	 * resources associated with a corpus or the framework in general.
	 *
	 * @return
	 */
	ResourceProvider getResourceProvider();

	/**
	 * Returns the {@link MetadataStoragePolicy policy} to be used for creating metadata
	 * registries for corpora. The default implementation simply returns an
	 * {@link MetadataStoragePolicy#emptyCorpusMetadataStoragePolicy empty} implementation.
	 *
	 * @return
	 */
	default MetadataStoragePolicy<CorpusManifest> getCorpusMetadataPolicy() {
		return MetadataStoragePolicy.emptyCorpusMetadataStoragePolicy;
	}

	/**
	 * Returns the {@link MetadataStoragePolicy policy} to be used for creating metadata
	 * registries for contexts. The default implementation simply returns an
	 * {@link MetadataStoragePolicy#emptyContextMetadataStoragePolicy empty} implementation.
	 *
	 * @return
	 */
	default MetadataStoragePolicy<ContextManifest> getContextMetadataPolicy() {
		return MetadataStoragePolicy.emptyContextMetadataStoragePolicy;
	}

	/**
	 * Creates a factory that can be used to instantiate a variety of corpus members
	 * suitable for the default corpus implementation this manager uses.
	 * <p>
	 * Note that although this method is called {@code newFactory}, the manager implementation
	 * is allowed to recycle factory instances and is not required to return a new factory
	 * instance each time the method is called. This is of cause only true as long as the
	 * returned factory object is thread safe!
	 *
	 * @return
	 */
	CorpusMemberFactory newFactory();

	/**
	 * Connects to the specified corpus, potentially instantiating a new {@code Corpus}
	 * instance when there is not already an active one. When an attempt to connect to the
	 * corpus is already being made, this method will simply wait till it completes and then
	 * act accordingly. In case the corpus is in the process of being disconnected then this
	 * method should return {@code null} to signal the client code to wait and make another
	 * request once the corpus has been properly disconnected.
	 *
	 * @param manifest
	 * @return
	 * @throws InterruptedException
	 * @throws ModelException in case the given {@code corpus} has been {@link #disableCorpus(CorpusManifest) disabled}
	 * or is marked as {@link #isBadCorpus(CorpusManifest) bad} or for any internal error encountered during execution.
	 */
	@Nullable
	Corpus connect(CorpusManifest corpus) throws InterruptedException;

	/**
	 * Returns the live corpus created from the given {@code manifest} if present. If the specified
	 * corpus has not yet been connected, this method returns {@code null}. This method is a mechanism to
	 * atomically check the state of a corpus and return its live version if possible.
	 *
	 * @param corpus
	 * @return the current live version of the specified corpus or {@code null} if the corpus has
	 * not yet been connected
	 */
	Corpus getLiveCorpus(CorpusManifest manifest);

	/**
	 * Disconnects from the specified {@code corpus}, closing and releasing all {@link PagedCorpusView views}
	 * and {@link CorpusModel models} it is currently holding open. After successful disconnect the corpus
	 * will be removed from the internal list of <i>active</i> corpora. Note that the actual release of
	 * currently used data is delegated to the active corpus instance by invoking its {@link Corpus#close() close}
	 * method.
	 * <p>
	 * This method does nothing if the {@code corpus} in question is already in the process of being closed as
	 * determined by the {@link #isCorpusDisconnecting(CorpusManifest)} method.
	 *
	 * @param corpus
	 * @throws InterruptedException
	 * @throws ModelException if the specified {@code corpus} is not currently connected
	 */
	void disconnect(CorpusManifest corpus) throws InterruptedException, AccumulatingException;

	/**
	 * Disconnects all currently connected corpora, in reverse to the order they were connected to.
	 *
	 * @throws InterruptedException
	 */
	void shutdown() throws InterruptedException, AccumulatingException;

	/**
	 * Returns {@code true} if the given {@code corpus} has been connected and is not yet
	 * in the process of being disconnected.
	 *
	 * @param corpus
	 * @return
	 */
	boolean isCorpusConnected(CorpusManifest corpus);

	/**
	 * Returns {@code true} if an attempt has been started to connect to the given {@code corpus}.
	 *
	 * @param corpus
	 * @return
	 */
	boolean isCorpusConnecting(CorpusManifest corpus);

	/**
	 * Returns {@code true} if an attempt has been started to disconnect the given {@code corpus}.
	 *
	 * @param corpus
	 * @return
	 */
	boolean isCorpusDisconnecting(CorpusManifest corpus);

	/**
	 * Returns whether the given {@code corpus} is enabled, determining whether
	 * it is possible to connect to it at all. Note that all corpora are enabled by default
	 * and need to be manually {@link #disableCorpus(CorpusManifest) disabled}!
	 *
	 * @param corpus
	 * @return
	 */
	boolean isCorpusEnabled(CorpusManifest corpus);

	/**
	 * Returns {@code true} if the given {@code corpus} has been added to this manager's internal
	 * list of bad corpora. A bad corpus is a corpus that previously experienced problems during
	 * an connection or disconnection attempt and is deemed unstable or unreliable by the manager.
	 * Note that a corpus once in {@link #isCorpusConnected(CorpusManifest) connected state} can
	 * never be marked as bad until an attempt to disconnect it is being made.
	 *
	 * @param corpus
	 * @return
	 */
	boolean isBadCorpus(CorpusManifest corpus);

	/**
	 * Enables the given {@code corpus}, making it possible to connect to it. A corpus' {@code enabled}
	 * state can only be changed when it is not connected or marked as bad and no attempts to either
	 * connect or disconnect it are currently being made.
	 *
	 * @param corpus
	 * @return {@code true} iff the corpus was not in {@link #isCorpusConnected(CorpusManifest) connected state}
	 * and its {@link #isCorpusEnabled(CorpusManifest) enabled state} changed as a result of this method call.
	 * @throws ModelException if the corpus is not in a clean state (meaning it is either in the process of being
	 * dis-/connected, has already been connected or is marked as bad)
	 */
	boolean enableCorpus(CorpusManifest corpus);

	/**
	 * Disables the given {@code corpus}, making it impossible to connect to it. A corpus' {@code enabled}
	 * state can only be changed when it is not connected or marked as bad and no attempts to either
	 * connect or disconnect it are currently being made.
	 *
	 * @param corpus
	 * @return {@code true} iff the corpus was not in {@link #isCorpusConnected(CorpusManifest) connected state}
	 * and its {@link #isCorpusEnabled(CorpusManifest) enabled state} changed as a result of this method call.
	 * @throws ModelException if the corpus is not in a clean state (meaning it is either in the process of being
	 * dis-/connected, has already been connected or is marked as bad)
	 */
	boolean disableCorpus(CorpusManifest corpus);

	/**
	 * Allows for client code to clear the 'bad' flag for a corpus when the problem could be resolved outside
	 * the manager's area of influence. This method should be treated with care since clearing a corpus' bad state
	 * automatically transitions it to being {@code unconnected}. This can lead to resources still being locked by
	 * a <i>phantom</i> corpus instance when the 'bad' flag was assigned during a failed attempt to disconnect the
	 * corpus. Therefore a client should really only use this method when it can be absolutely sure that the issue
	 * has been resolved and the corpus in question is in a clean state, not compromising any resources.
	 *
	 * @param corpus
	 * @return {@code true} iff the corpus was marked as bad and this state changed as a result of calling this method
	 * @throws ModelException if the corpus in question is not marked as being bad.
	 */
	@OptionalMethod
	boolean resetBadCorpus(CorpusManifest corpus);

	void addCorpusLifecycleListener(CorpusLifecycleListener listener);
	void removeCorpusLifecycleListener(CorpusLifecycleListener listener);

	/**
	 * Returns a collection of all {@code Corpus} instances this manager has created and
	 * which are not yet completely disconnected. Note that only the manifests for
	 * those corpora are returned!
	 * <p>
	 * The returned collection is for all practical purposes equal to the one returned by
	 * {@link #getConnectedCorpora()}, but it is implementation specific, whether the latter
	 * method is actually overridden to not use the default {@link #getCorpora(CorpusState)}
	 * method in the background.
	 *
	 * @return
	 */
	List<CorpusManifest> getLiveCorpora();

	List<CorpusManifest> getCorpora(Predicate<? super CorpusManifest> p);

	List<CorpusManifest> getCorpora(CorpusState state);

	default List<CorpusManifest> getConnectedCorpora() {
		return getCorpora(CorpusState.CONNECTED);
	}

	default List<CorpusManifest> getConnectingCorpora() {
		return getCorpora(CorpusState.CONNECTING);
	}

	default List<CorpusManifest> getDisconnectingCorpora() {
		return getCorpora(CorpusState.DISCONNECTING);
	}

	default List<CorpusManifest> getBadCorpora() {
		return getCorpora(CorpusState.BAD);
	}

	default List<CorpusManifest> getDisabledCorpora() {
		return getCorpora(CorpusState.DISABLED);
	}

	// Optional "plugin" mechanics

	default boolean isSupportPlugins() {
		return false;
	}

	@OptionalMethod
	Collection<String> availableExtensions(String extensionPointUid);

	@OptionalMethod
	Class<?> resolveExtension(String extensionUid) throws ClassNotFoundException;

	@OptionalMethod
	ClassLoader getPluginClassLoader(String pluginUid);

	default ClassLoader getImplementationClassLoader(ImplementationManifest manifest) {
		return manifest.getManifestLocation().getClassLoader();
	}

	Identity getExtensionIdentity(String extensionuid);

	public static enum CorpusState {
		ENABLED,
		CONNECTING,
		CONNECTED,
		DISCONNECTING,
		BAD,
		DISABLED,
		;

		private EnumSet<CorpusState> preconditions;

		private void setPreconditions(CorpusState...preconditions) {
			this.preconditions = EnumSet.noneOf(CorpusState.class);

			if(preconditions!=null) {
				for(CorpusState state : preconditions) {
					this.preconditions.add(state);
				}
			}
		}

		public boolean isValidPrecondition(CorpusState state) {
			return preconditions.contains(state);
		}

		static {
			ENABLED.setPreconditions(ENABLED, DISABLED, DISCONNECTING, BAD);
			CONNECTING.setPreconditions(ENABLED);
			CONNECTED.setPreconditions(CONNECTING);
			DISCONNECTING.setPreconditions(CONNECTED);
			BAD.setPreconditions(CONNECTING, DISCONNECTING);
			DISABLED.setPreconditions(ENABLED, DISABLED);
		}
	}

}
