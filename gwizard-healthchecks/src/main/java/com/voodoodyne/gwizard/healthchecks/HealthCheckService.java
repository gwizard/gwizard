package com.voodoodyne.gwizard.healthchecks;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.voodoodyne.gwizard.services.Services;

import java.util.concurrent.TimeUnit;

public class HealthCheckService extends AbstractScheduledService {

	private final HealthChecks healthChecks;
	private final HealthCheckConfig config;

	@Inject
	public HealthCheckService(Services services,
							  HealthCheckConfig config,
							  HealthChecks healthChecks) {
		services.add(this);
		this.config = config;
		this.healthChecks = healthChecks;
	}

	@Override
	protected void runOneIteration() throws Exception {
		healthChecks.run();
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedRateSchedule(0, // run immediately
				config.getInterval().getSeconds(), TimeUnit.SECONDS); // and every <config> seconds
	}
}
