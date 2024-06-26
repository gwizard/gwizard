package org.gwizard.healthchecks;

import io.dropwizard.util.Duration;

public interface HealthChecksConfig {
	/**
	 * health checks which extend AbstractMetricsHealthCheck will register
	 * metrics with this prefix.
	 */
	default String getMetricsPrefix() { return "gwizard.healthChecks"; }

	/**
	 * <p>Run health checks periodically at this interval. Failing health checks will be logged at level WARN.
	 * This accepts a flexible format like "10 minutes" or "30s".</p>
	 *
	 * Default is null, which disables periodic health checks.
	 */
	default Duration getInterval() { return Duration.minutes(10); }
}
