package org.gwizard.hibernate;

import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.concurrent.TimeUnit;

/** Settings for the default pool; explicit providers and application-owned pools keep their own settings. */
@Data
public class ConnectionPoolConfig {
	@Min(1)
	private int maxPoolSize = 20;

	/** Maximum wait for a pooled connection, not a query or transaction timeout. */
	@NotNull
	@MinDuration(value = 250, unit = TimeUnit.MILLISECONDS)
	private Duration connectionTimeout = Duration.seconds(1);

	public void validate() {
		if (maxPoolSize < 1)
			throw new IllegalArgumentException("pool.maxPoolSize must be at least 1");
		if (connectionTimeout == null || connectionTimeout.toMilliseconds() < 250)
			throw new IllegalArgumentException("pool.connectionTimeout must be at least 250 ms");
	}
}
