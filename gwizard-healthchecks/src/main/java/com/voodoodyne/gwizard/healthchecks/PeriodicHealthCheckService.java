package com.voodoodyne.gwizard.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.voodoodyne.gwizard.services.Services;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This service periodically runs health checks at a configured interval and
 * reports any health issues via slf4j
 *
 */
@Singleton
@Slf4j
public class PeriodicHealthCheckService extends AbstractScheduledService {

	private final HealthChecks healthChecks;
	private final PeriodicHealthCheckConfig config;

	@Inject
	public PeriodicHealthCheckService(Services services, PeriodicHealthCheckConfig config, HealthChecks healthChecks) {
		services.add(this);
		this.config = config;
		this.healthChecks = healthChecks;
	}

	@Override
	protected void runOneIteration() throws Exception {

		for (Map.Entry<String, HealthCheck.Result> entry : healthChecks.run().entrySet()) {
			if (entry.getValue().isHealthy()) {
				log.trace("{} : OK {}", entry.getKey(), Strings.nullToEmpty(entry.getValue().getMessage()));
			} else {
				log.warn("{} : FAIL - {}", entry.getKey(), Strings.nullToEmpty(entry.getValue().getMessage()), entry.getValue().getError());
			}
		}
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedRateSchedule(0, config.getInterval().toSeconds(), TimeUnit.SECONDS);
	}
}
