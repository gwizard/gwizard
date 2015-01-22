package com.voodoodyne.gwizard.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 */
@Slf4j
@Singleton
public class HealthChecks {
	private final HealthCheckRegistry healthCheckRegistry;

	@Inject
	public HealthChecks(HealthCheckRegistry healthCheckRegistry) {
		this.healthCheckRegistry = healthCheckRegistry;
	}

	public void register(String name, HealthCheck healthCheck) {
		healthCheckRegistry.register(name, healthCheck);
	}

	public void unregister(String name) {
		healthCheckRegistry.unregister(name);
	}

	public void run() {
		// TODO: use version of runHealthChecks which runs checks in parallel?
		for (Map.Entry<String, HealthCheck.Result> entry : healthCheckRegistry.runHealthChecks().entrySet()) {
			if (entry.getValue().isHealthy()) {
				log.trace("{} : OK {}", entry.getKey(), Strings.nullToEmpty(entry.getValue().getMessage()));
			} else {
				log.warn("{} : FAIL - {}", entry.getKey(), Strings.nullToEmpty(entry.getValue().getMessage()), entry.getValue().getError());
			}
		}
	}
}
