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
package de.ims.icarus2.query.api.engine;

import static java.util.Objects.requireNonNull;

import de.ims.icarus2.query.api.QueryErrorCode;
import de.ims.icarus2.query.api.QueryException;

/**
 * Allows to catch a thread and save it as the solely allowed point of
 * access for a resource or module. Any attempt to access a method that
 * uses a {@link ThreadVerifier} from any thread but the saved one will
 * throw a {@link QueryException} with code {@link QueryErrorCode#FOREIGN_THREAD_ACCESS}
 * if {@link Tripwire} is {@link Tripwire#ACTIVE active}.
 * <p>
 * Client code that uses {@link ThreadVerifier} should wrap any calls to
 * {@link #checkThread()} into a check on the activity status of {@link Tripwire}:<br>
 * <pre>
 * if(Tripwire.ACTIVE) {
 *     threadVerifier.checkThread();
 * }
 * </pre>
 *
 * @author Markus Gärtner
 *
 */
public final class ThreadVerifier {

	public static ThreadVerifier forCurrentThread(String id) {
		return new ThreadVerifier(Thread.currentThread(), id);
	}

	public static ThreadVerifier forThread(Thread thread, String id) {
		return new ThreadVerifier(thread, id);
	}

	private final Thread thread;
	private final String id;

	private ThreadVerifier(Thread thread, String id) {
		this.thread = requireNonNull(thread);
		this.id = requireNonNull(id);
	}


	public String getId() { return id; }

	public Thread getThread() { return thread; }

	public final void checkThread() {
		if(Thread.currentThread()!=thread)
			throw new QueryException(QueryErrorCode.FOREIGN_THREAD_ACCESS,
					String.format("Illegal access to '%s' by thread %s - only authorized for %s",
							id, Thread.currentThread().getName(), thread.getName()));
	}

	public final void checkNotThread() {
		if(Thread.currentThread()==thread)
			throw new QueryException(QueryErrorCode.FOREIGN_THREAD_ACCESS,
					String.format("Unespected access to '%s' by thread %s - any other thread should be used!",
							id, Thread.currentThread().getName()));
	}
}
