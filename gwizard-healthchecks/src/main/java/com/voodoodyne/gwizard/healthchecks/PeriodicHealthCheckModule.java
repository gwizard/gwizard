package com.voodoodyne.gwizard.healthchecks;

import com.google.inject.AbstractModule;
import com.voodoodyne.gwizard.services.ServicesModule;
import lombok.EqualsAndHashCode;

/**
 * binding for a periodic service that runs health checks and logs results
 *
 * depends on gwizard-services
 */
@EqualsAndHashCode(of={})	// makes installation of this module idempotent
public class PeriodicHealthCheckModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new ServicesModule());

		bind(PeriodicHealthCheckService.class).asEagerSingleton();
	}
}
