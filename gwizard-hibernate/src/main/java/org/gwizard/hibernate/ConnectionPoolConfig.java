package org.gwizard.hibernate;

import jakarta.validation.constraints.Min;
import lombok.Data;

/** Settings for the default pool; explicit providers and application-owned pools keep their own settings. */
@Data
public class ConnectionPoolConfig {
	@Min(1)
	private int maxPoolSize = 20;

	/** Maximum wait for a pooled connection, not a query or transaction timeout. */
	@Min(250)
	private long connectionTimeoutMillis = 1_000;

	public void validate() {
		if (maxPoolSize < 1)
			throw new IllegalArgumentException("pool.maxPoolSize must be at least 1");
		if (connectionTimeoutMillis < 250)
			throw new IllegalArgumentException("pool.connectionTimeoutMillis must be at least 250");
	}
}
