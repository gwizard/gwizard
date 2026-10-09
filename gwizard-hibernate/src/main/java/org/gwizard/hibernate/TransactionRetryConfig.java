package org.gwizard.hibernate;

import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.concurrent.TimeUnit;

/** Configuration for automatic retries of transactions that fail with SQLSTATE 40001. */
@Data
public class TransactionRetryConfig {

	/** Total attempts, including the initial execution. Set to 1 to disable retries. */
	@Min(1)
	private int maxAttempts = 5;

	/** Maximum backoff. Backoff always uses exponential growth and full jitter. */
	@NotNull
	@MinDuration(value = 1, unit = TimeUnit.MILLISECONDS)
	private Duration maxBackoff = Duration.milliseconds(250);
}
