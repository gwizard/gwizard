package com.voodoodyne.gwizard.healthchecks;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.voodoodyne.gwizard.services.Services;

import java.util.concurrent.TimeUnit;

public class HealthChecksService extends AbstractScheduledService {

	private final HealthChecks healthChecks;
	private final HealthChecksConfig config;

	@Inject
	public HealthChecksService(Services services, HealthChecksConfig config, HealthChecks healthChecks) {
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
		return Scheduler.newFixedRateSchedule(0, config.getInterval().toSeconds(), TimeUnit.SECONDS);
	}
}
