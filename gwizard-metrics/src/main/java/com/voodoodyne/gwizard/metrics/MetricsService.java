package com.voodoodyne.gwizard.metrics;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;

/**
 * a Service that starts Metrics JmxReporter
 */
@Slf4j
public class MetricsService extends AbstractExecutionThreadService {
	private final CountDownLatch doneSignal = new CountDownLatch(1);
	private final JmxReporter jmxReporter;
	private final MetricRegistry metricRegistry;


	@Inject
	public MetricsService(MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;
		jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
	}

	@Override
	protected void run() throws Exception {
		jmxReporter.start();

		metricRegistry.register("jvm.buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
		metricRegistry.register("jvm.gc", new GarbageCollectorMetricSet());
		metricRegistry.register("jvm.memory", new MemoryUsageGaugeSet());
		metricRegistry.register("jvm.threads", new ThreadStatesGaugeSet());

		doneSignal.await();

		jmxReporter.stop();
	}

	@Override
	protected void triggerShutdown() {
		doneSignal.countDown();
	}
}
