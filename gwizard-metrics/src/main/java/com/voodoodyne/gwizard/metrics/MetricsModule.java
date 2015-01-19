package com.voodoodyne.gwizard.metrics;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;

/**
 * add the metrics-guice MetricsInstrumentationModule to scan for metrics annotations
 */
public class MetricsModule extends AbstractModule {
    private final MetricRegistry metricRegistry = new MetricRegistry();

    @Override
    protected void configure() {

        bind(MetricRegistry.class).toInstance(metricRegistry);

        install(new MetricsInstrumentationModule(metricRegistry));

        Multibinder.newSetBinder(binder(), Service.class)
                .addBinding().to(MetricsService.class);
    }
}
