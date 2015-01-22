package com.voodoodyne.gwizard.healthchecks;

import lombok.Data;

import java.time.Duration;

@Data
public class HealthChecksConfig {
	/**
	 * run health checks periodically at this interval
	 */
	private Duration interval = Duration.ofSeconds(10);
}
