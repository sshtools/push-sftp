package com.sshtools.pushsftp.jfx;

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.client.PasswordAuthenticator.PasswordPrompt;

import javafx.concurrent.Task;

public abstract class TargetJob<V, T extends Target> extends Task<V> implements Callable<V> {
	final static ResourceBundle RESOURCES = ResourceBundle.getBundle(SshConnectionJob.class.getName());
	final static Logger LOG = LoggerFactory.getLogger(TargetJob.class);

	public static abstract class AbstractTargetJobBuilder<T extends Target, J extends TargetJob<?, T>, B extends AbstractTargetJobBuilder<T, J, B>> {
		private Optional<Supplier<T>> target = Optional.empty();
		private boolean verbose;
		private Optional<PasswordPrompt> password = Optional.empty();
		private Optional<Consumer<T>> serializer = Optional.empty();
		
		@SuppressWarnings("unchecked")
		public final B withSerializer(Consumer<T> serializer) {
			this.serializer = Optional.of(serializer);
			return (B)this;
		}

		public final B withPassword(String password) {
			return withPassword(() -> password);
		}

		public final B withPassword(PasswordPrompt password) {
			return withPassword(Optional.of(password));
		}

		@SuppressWarnings("unchecked")
		public final B withPassword(Optional<PasswordPrompt> password) {
			this.password = password;
			return (B) this;
		}

		public final B withVerbose() {
			return withVerbose(true);
		}

		@SuppressWarnings("unchecked")
		public final B withVerbose(boolean verbose) {
			this.verbose = verbose;
			return (B) this;
		}

		public final B withTarget(T target) {
			return withTarget(() -> target);
		}

		@SuppressWarnings("unchecked")
		public final B withTarget(Supplier<T> target) {
			this.target = Optional.of(target);
			return (B) this;
		}

		public abstract J build();
	}

	protected final Supplier<T> target;
	protected final boolean verbose;
	protected final Optional<Consumer<T>> serializer;
	protected final Optional<PasswordPrompt> password;
	
	protected TargetJob(AbstractTargetJobBuilder<T, ?, ?> builder) {
		this.target = builder.target.orElseThrow(() -> new IllegalStateException("Target must be provided.")); //$NON-NLS-1$
		this.verbose = builder.verbose;
		this.password = builder.password;
		this.serializer = builder.serializer;
	}


	@Deprecated
    public V resultNow() {
		/* NOTE: This is a patch to get some methods available in Java 19 (we 
		 * build with 17 currently). Will be removed when built and deployed with
		 * Java 19+
		 */
        if (!isDone())
            throw new IllegalStateException("Task has not completed");
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return get();
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (ExecutionException e) {
                    throw new IllegalStateException("Task completed with exception");
                } catch (CancellationException e) {
                    throw new IllegalStateException("Task was cancelled");
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

	@Deprecated
    public Throwable exceptionNow() {
		/* NOTE: This is a patch to get some methods available in Java 19 (we 
		 * build with 17 currently). Will be removed when built and deployed with
		 * Java 19+
		 */
        if (!isDone())
            throw new IllegalStateException("Task has not completed");
        if (isCancelled())
            throw new IllegalStateException("Task was cancelled");
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    get();
                    throw new IllegalStateException("Task completed with a result");
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (ExecutionException e) {
                    return e.getCause();
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
}
