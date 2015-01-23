package com.voodoodyne.gwizard.healthchecks;

import io.dropwizard.util.Duration;
import lombok.Data;

@Data
public class PeriodicHealthCheckConfig {
	/**
	 * Run health checks periodically at this interval. Failing health checks will be logged at level WARN.
	 * This accepts a flexible format like "10 minutes" or "30s"
	 */
	private Duration interval = Duration.minutes(10);
}
