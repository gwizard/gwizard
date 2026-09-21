package org.gwizard.metrics;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.gwizard.services.Services;

/**
 * Registers JVM meters and closes the application registry and GC listeners on shutdown.
 * Publishing, if any, is handled by the application's chosen MeterRegistry implementation.
 */
public class MetricsService extends AbstractIdleService {
	private final MeterRegistry registry;
	private final JvmGcMetrics gcMetrics = new JvmGcMetrics();

	@Inject
	public MetricsService(final Services services, final MeterRegistry registry) {
		this.registry = registry;
		services.add(this);
	}

	@Override
	protected void startUp() {
		new ClassLoaderMetrics().bindTo(registry);
		new JvmMemoryMetrics().bindTo(registry);
		new JvmThreadMetrics().bindTo(registry);
		new ProcessorMetrics().bindTo(registry);
		gcMetrics.bindTo(registry);
	}

	@Override
	protected void shutDown() {
		try {
			gcMetrics.close();
		} finally {
			registry.close();
		}
	}
}
