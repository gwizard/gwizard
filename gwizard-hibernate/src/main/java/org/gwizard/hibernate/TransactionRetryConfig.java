package org.gwizard.hibernate;

import jakarta.validation.constraints.Min;
import lombok.Data;

/** Configuration for automatic retries of transactions that fail with SQLSTATE 40001. */
@Data
public class TransactionRetryConfig {

	/** Total attempts, including the initial execution. Set to 1 to disable retries. */
	@Min(1)
	private int maxAttempts = 5;

	/** Maximum backoff in milliseconds. Backoff always uses exponential growth and full jitter. */
	@Min(1)
	private long maxBackoffMillis = 250;
}
