/**
 *
 */
package de.ims.icarus2.query.api.engine.filter;

import static de.ims.icarus2.util.Conditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongFunction;

import de.ims.icarus2.GlobalErrorCode;
import de.ims.icarus2.IcarusApiException;
import de.ims.icarus2.IcarusRuntimeException;
import de.ims.icarus2.model.api.corpus.Context;
import de.ims.icarus2.model.api.members.container.Container;
import de.ims.icarus2.query.api.QueryException;
import de.ims.icarus2.query.api.engine.QueryInput;
import de.ims.icarus2.query.api.engine.ThreadVerifier;
import de.ims.icarus2.query.api.engine.Tripwire;
import de.ims.icarus2.query.api.iql.IqlQuery;
import de.ims.icarus2.query.api.iql.IqlStream;
import de.ims.icarus2.util.AbstractBuilder;

/**
 * @author Markus Gärtner
 *
 */
public abstract class AbstractFilterProcessor implements QueryInput {

	/** Used to delegate execution to another thread */
	private final ExecutorService executor;
	/** Convert index values from filters to actual candidate objects */
	private final LongFunction<Container> candidateLookup;
	/** Current lifecycle state */
	private final AtomicReference<State> state = new AtomicReference<>(State.WAITING);

	static enum State {
		WAITING,
		PREPARED,
		FINISHED,
		IGNORED,
		;

		boolean isFinished() { return ordinal()>=2; }
	}

	protected AbstractFilterProcessor(BuilderBase<?,?> builder) {
		builder.validate();
		executor = builder.getExecutor();
		candidateLookup = builder.getCandidateLookup();
	}

	protected ExecutorService getExecutor() {
		return executor;
	}

	protected LongFunction<Container> getCandidateLookup() {
		return candidateLookup;
	}

	protected final boolean trySetState(State expected, State next) {
		State current = state.get();
		if(current.isFinished())
			throw new IcarusRuntimeException(GlobalErrorCode.ILLEGAL_STATE, "Current state is already marked finished: "+current);
		return state.compareAndSet(expected, next);
	}

	protected final State getState() {
		return state.get();
	}

	protected final void setState(State next) {
		state.set(next);
	}

	static class FilterJob implements Runnable {
		private final QueryFilter filter;
		private final FilterContext context;
		private final int id;

		private ThreadVerifier threadVerifier;

		private Throwable exception;
		private boolean interrupted, finished;

		public FilterJob(int id, QueryFilter filter, FilterContext context) {
			this.id = id;
			this.filter = requireNonNull(filter);
			this.context = requireNonNull(context);
		}

		/** Signals that the filter process has been interrupted */
		public boolean isInterrupted() {
			return interrupted;
		}

		/** Returns the exception caught during filtering, if any. */
		public Throwable getException() {
			return exception;
		}

		/**
		 * Returns whether or not this job has already finished (either
		 * successfully or with an exception)
		 */
		public boolean isFinished() {
			return finished;
		}

		public boolean wasInterrupted() {
			return finished && exception!=null && InterruptedException.class.isInstance(exception);
		}

		/**
		 * Delegates to {@link ThreadVerifier#checkThread()} on the internal verifier.
		 * Before this call, client code should first check if {@link Tripwire} is actually
		 * {@link Tripwire#ACTIVE active} to ensure lower load on frequent calls.
		 */
		public final void checkThread() {
			checkState("hread verifier not initialized yet", threadVerifier!=null);
			threadVerifier.checkThread();
		}

		/**
		 * Execute filter and catch exceptions.
		 *
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			threadVerifier = ThreadVerifier.forCurrentThread("filter-job-"+id);
			try {
				filter.filter(context);
				finished = true;
			} catch (QueryException e) {
				exception = e;
			} catch (IcarusApiException e) {
				exception = e;
			} catch (InterruptedException e) {
				interrupted = true;
			} catch (Throwable t) {
				//TODO should we add a mechanism to mark unexpected errors?
				exception = t;
			}
		}
	}

	public static abstract class BuilderBase<B extends BuilderBase<B, P>, P extends AbstractFilterProcessor>
		extends AbstractBuilder<B, P> {

		private ExecutorService executor;
		private LongFunction<Container> candidateLookup;
		private Context context;
		private IqlStream stream;
		private IqlQuery query;

		//TODO add builder methods for filling

		public ExecutorService getExecutor() {
			return executor;
		}

		public LongFunction<Container> getCandidateLookup() {
			return candidateLookup;
		}

		public Context getContext() {
			return context;
		}

		public IqlStream getStream() {
			return stream;
		}

		public IqlQuery getQuery() {
			return query;
		}

		@Override
		protected void validate() {
			// TODO Auto-generated method stub
			super.validate();
		}
	}
}
