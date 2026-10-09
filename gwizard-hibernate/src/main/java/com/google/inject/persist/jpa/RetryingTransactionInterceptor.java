package com.google.inject.persist.jpa;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityTransaction;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.gwizard.hibernate.DatabaseConfig;
import org.gwizard.hibernate.TransactionRetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.inject.persist.jpa.TransactionRetryPolicy.isSerializationFailure;

/** Owns the complete transaction boundary, including commit and replacement of failed contexts. */
final class RetryingTransactionInterceptor implements MethodInterceptor {

	private static final Logger log = LoggerFactory.getLogger(RetryingTransactionInterceptor.class);

	@FunctionalInterface
	interface Sleeper {
		void sleep(long millis) throws InterruptedException;
	}

	private final JpaPersistService persistence;
	private final TransactionRetryPolicy policy;
	private final Sleeper sleeper;
	private final ThreadLocal<Boolean> executing = new ThreadLocal<>();
	private Counter attempts;
	private Counter retries;
	private Counter serializationFailures;
	private Counter exhausted;

	@Inject(optional = true)
	void bindMetrics(final MeterRegistry registry, @Jpa final String persistenceUnit) {
		try {
			attempts = Counter.builder("db.transaction.attempts").tag("persistence.unit", persistenceUnit)
					.description("Transaction attempts started by GWizard, including retries").register(registry);
			retries = Counter.builder("db.transaction.retries").tag("persistence.unit", persistenceUnit)
					.description("Additional transaction attempts actually started after SQLSTATE 40001").register(registry);
			serializationFailures = Counter.builder("db.transaction.serialization.failures").tag("persistence.unit", persistenceUnit)
					.description("Transaction attempts that failed with SQLSTATE 40001").register(registry);
			exhausted = Counter.builder("db.transaction.retry.exhausted").tag("persistence.unit", persistenceUnit)
					.description("Transactions that reached maxAttempts with SQLSTATE 40001").register(registry);
		} catch (RuntimeException failure) {
			log.warn("Unable to bind transaction metrics", failure);
		}
	}

	@Inject
	RetryingTransactionInterceptor(final JpaPersistService persistence, final DatabaseConfig config) {
		this(persistence, config.getTransactionRetry(), Thread::sleep);
	}

	RetryingTransactionInterceptor(final JpaPersistService persistence, final TransactionRetryConfig config,
			final Sleeper sleeper) {
		this.persistence = persistence;
		this.policy = new TransactionRetryPolicy(config);
		this.sleeper = sleeper;
	}

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		// Nested calls share the outer attempt, even if the database has already aborted it.
		if (executing.get() != null)
			return invocation.proceed();
		final boolean existingWork = persistence.isWorking();
		if (existingWork && persistence.get().getTransaction().isActive())
			return invocation.proceed(); // A manually started transaction belongs to its caller.

		final Transactional metadata = transactionMetadata(invocation);
		executing.set(true);
		Throwable failure = null;
		try {
			return retry(invocation, metadata);
		} catch (Throwable thrown) {
			failure = thrown;
			throw thrown;
		} finally {
			try {
				if (!existingWork)
					persistence.end();
				else if (!persistence.isWorking())
					persistence.begin(); // Keep a caller-owned request scope usable after an exception.
			} catch (Throwable cleanup) {
				if (failure == null)
					throw cleanup;
				addSuppressed(failure, cleanup);
			} finally {
				executing.remove();
			}
		}
	}

	private Object retry(final MethodInvocation invocation, final Transactional metadata) throws Throwable {
		for (int attempt = 1; ; attempt++) {
			increment(attempts);
			if (attempt > 1)
				increment(retries);
			EntityTransaction transaction = null;
			try {
				if (!persistence.isWorking())
					persistence.begin();
				transaction = persistence.get().getTransaction();
				transaction.begin();
				return execute(invocation, metadata, transaction);
			} catch (Throwable failure) {
				final boolean cleanedUp = rollbackAndClose(transaction, failure);
				final boolean serializationFailure = !(failure instanceof Error) && isSerializationFailure(failure);
				if (serializationFailure) {
					increment(serializationFailures);
					if (attempt >= policy.maxAttempts)
						increment(exhausted);
				}
				if (!cleanedUp || !serializationFailure
						|| attempt >= policy.maxAttempts || Thread.currentThread().isInterrupted())
					throw failure;

				final long delay = policy.backoffMillis(attempt);
				log.debug("Retrying {} after SQLSTATE 40001 (attempt {}/{}, backoff {} ms)",
						invocation.getMethod(), attempt + 1, policy.maxAttempts, delay);
				try {
					sleeper.sleep(delay);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					addSuppressed(failure, interrupted);
					throw failure;
				}
			}
		}
	}

	private static void increment(final Counter counter) {
		if (counter != null) {
			try {
				counter.increment();
			} catch (RuntimeException failure) {
				log.warn("Unable to record transaction metric", failure);
			}
		}
	}

	private Object execute(final MethodInvocation invocation, final Transactional metadata,
			final EntityTransaction transaction) throws Throwable {
		final Object result;
		try {
			result = invocation.proceed();
		} catch (Throwable failure) {
			// A serialization failure always aborts the transaction, regardless of rollback metadata.
			if (!isSerializationFailure(failure) && !shouldRollback(metadata, failure)) {
				try {
					finish(transaction);
				} catch (Throwable commitFailure) {
					addSuppressed(commitFailure, failure);
					throw commitFailure;
				}
			}
			throw failure;
		}
		finish(transaction);
		return result;
	}

	private static void finish(final EntityTransaction transaction) {
		if (transaction.isActive()) {
			if (transaction.getRollbackOnly())
				transaction.rollback();
			else
				transaction.commit();
		}
	}

	private boolean rollbackAndClose(final EntityTransaction transaction, final Throwable failure) {
		boolean cleanedUp = true;
		try {
			if (transaction != null && transaction.isActive())
				transaction.rollback();
		} catch (Throwable cleanup) {
			addSuppressed(failure, cleanup);
			cleanedUp = false;
		}
		try {
			persistence.end();
		} catch (Throwable cleanup) {
			addSuppressed(failure, cleanup);
			cleanedUp = false;
		}
		return cleanedUp;
	}

	private static Transactional transactionMetadata(final MethodInvocation invocation) {
		final Transactional method = invocation.getMethod().getAnnotation(Transactional.class);
		return method != null ? method : invocation.getThis().getClass().getAnnotation(Transactional.class);
	}

	private static boolean shouldRollback(final Transactional metadata, final Throwable failure) {
		if (failure instanceof Error)
			return true;
		for (Class<? extends Exception> rollback : metadata.rollbackOn()) {
			if (rollback.isInstance(failure)) {
				for (Class<? extends Exception> ignore : metadata.ignore()) {
					if (ignore.isInstance(failure))
						return false;
				}
				return true;
			}
		}
		return false;
	}

	private static void addSuppressed(final Throwable failure, final Throwable cleanup) {
		if (failure != cleanup)
			failure.addSuppressed(cleanup);
	}
}
