package com.voodoodyne.gwizard.metrics;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;
import com.voodoodyne.gwizard.services.ServicesModule;
import lombok.EqualsAndHashCode;

/**
 * Add the metrics-guice MetricsInstrumentationModule to scan for metrics annotations.
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class MetricsModule extends AbstractModule {
	private final MetricRegistry metricRegistry = new MetricRegistry();

	@Override
	protected void configure() {
		install(new ServicesModule());

		bind(MetricRegistry.class).toInstance(metricRegistry);

		install(new MetricsInstrumentationModule(metricRegistry));

		bind(MetricsService.class).asEagerSingleton();
	}
}
