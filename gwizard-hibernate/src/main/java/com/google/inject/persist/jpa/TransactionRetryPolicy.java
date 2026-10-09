package com.google.inject.persist.jpa;

import org.gwizard.hibernate.TransactionRetryConfig;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Immutable, validated policy shared by all transactions in an injector. */
final class TransactionRetryPolicy {

	final int maxAttempts;
	private final long maxBackoffMillis;

	TransactionRetryPolicy(final TransactionRetryConfig config) {
		if (config == null)
			throw new IllegalArgumentException("transactionRetry must not be null");
		if (config.getMaxAttempts() < 1)
			throw new IllegalArgumentException("transactionRetry.maxAttempts must be at least 1");
		if (config.getMaxBackoffMillis() < 1)
			throw new IllegalArgumentException("transactionRetry.maxBackoffMillis must be at least 1");
		maxAttempts = config.getMaxAttempts();
		maxBackoffMillis = config.getMaxBackoffMillis();
	}

	/** The first retry uses 1/16 of the cap; the fifth and later retries use the cap. */
	long backoffCeilingMillis(final int retryNumber) {
		// Divide before rounding, avoiding cumulative rounding errors and multiplication overflow.
		return Math.max(1, maxBackoffMillis >> Math.max(0, 5 - retryNumber));
	}

	long backoffMillis(final int retryNumber) {
		final long ceiling = backoffCeilingMillis(retryNumber);
		final ThreadLocalRandom random = ThreadLocalRandom.current();
		return ceiling == Long.MAX_VALUE ? random.nextLong() & Long.MAX_VALUE : random.nextLong(ceiling + 1);
	}

	static boolean isSerializationFailure(final Throwable failure) {
		final var pending = new ArrayDeque<Throwable>();
		final var visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
		pending.add(failure);
		while (!pending.isEmpty()) {
			final Throwable current = pending.removeFirst();
			if (!visited.add(current))
				continue;
			if (current instanceof SQLException sql) {
				if ("40001".equals(sql.getSQLState()))
					return true;
				if (sql.getNextException() != null)
					pending.add(sql.getNextException());
			}
			if (current.getCause() != null)
				pending.add(current.getCause());
		}
		return false;
	}
}
