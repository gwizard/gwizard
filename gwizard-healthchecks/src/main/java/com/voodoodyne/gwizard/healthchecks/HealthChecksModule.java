package com.voodoodyne.gwizard.healthchecks;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(of={})	// makes installation of this module idempotent
public class HealthChecksModule extends AbstractModule {

	@Override
	protected void configure() {
	}

	@Provides
	@Singleton
	public HealthCheckRegistry healthCheckRegistry() {
		return new HealthCheckRegistry();
	}
}
