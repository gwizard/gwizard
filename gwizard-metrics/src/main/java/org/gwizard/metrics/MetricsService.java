package org.gwizard.metrics;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.gwizard.services.Services;
import lombok.extern.slf4j.Slf4j;
import java.lang.management.ManagementFactory;

/**
 * a Service that starts Metrics JmxReporter
 */
@Slf4j
public class MetricsService extends AbstractIdleService {
	private final MetricRegistry metricRegistry;

	private JmxReporter jmxReporter;

	@Inject
	public MetricsService(Services services, MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;

		metricRegistry.register("jvm.buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
		metricRegistry.register("jvm.gc", new GarbageCollectorMetricSet());
		metricRegistry.register("jvm.memory", new MemoryUsageGaugeSet());
		metricRegistry.register("jvm.threads", new ThreadStatesGaugeSet());

		services.add(this);
	}

	@Override
	protected void startUp() throws Exception {
		jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
		jmxReporter.start();
	}

	@Override
	protected void shutDown() throws Exception {
		jmxReporter.stop();
	}
}
