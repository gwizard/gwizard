package org.gwizard.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import io.dropwizard.util.Duration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.gwizard.metrics.MetricsModule;
import org.gwizard.services.Run;

public class HealthChecksModuleExample {

	/**
	 * dumb example that's always unhealthy
	 */
	public static class ChronicallyUnhealthy extends HealthCheck {
		@Inject
		public ChronicallyUnhealthy(HealthChecks healthChecks) {
			healthChecks.add("chronicfailure", this);
		}

		@Override
		protected Result check() throws Exception {
			return Result.unhealthy("this health check is a terrible disappointment");
		}
	}

	/**
	 * dumb example that shows we can also expose health via Micrometer
	 */
	public static class MetricHealthCheck extends AbstractMetricReportingHealthCheck {
		public static final String name = "example";
		@Inject
		public MetricHealthCheck(HealthChecks healthChecks) {
			super(healthChecks, name);
		}

		@Override
		protected Result check() throws Exception {
			return Result.healthy();
		}
	}

	/**
	 * dumb example that caches health-check gauge samples
	 */
	@Slf4j
	public static class CachedMetricHealthCheck extends AbstractMetricReportingHealthCheck {
		public static final String name = "exampleCached";
		@Inject
		public CachedMetricHealthCheck(HealthChecks healthChecks) {
			super(healthChecks, name, Duration.seconds(30));
		}

		@Override
		protected Result check() throws Exception {
			log.trace("called!");
			return Result.healthy();
		}
	}

	/**
	 * dumb singleton that wraps the ThreadDeadlockHealthCheck that's in metrics-healthcheck
	 */
	public static class DeadlockHcWrapper {
		@Inject
		public DeadlockHcWrapper(HealthChecks healthChecks, ThreadDeadlockHealthCheck healthCheck) {
			healthChecks.add("deadlocks", healthCheck);
		}
	}


	public static class ExampleModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(MeterRegistry.class).to(SimpleMeterRegistry.class).in(Scopes.SINGLETON);
			bind(ChronicallyUnhealthy.class).asEagerSingleton();
			bind(MetricHealthCheck.class).asEagerSingleton();
			bind(CachedMetricHealthCheck.class).asEagerSingleton();
			bind(DeadlockHcWrapper.class).asEagerSingleton();
		}

		@Provides
		public HealthChecksConfig periodicHealthCheckConfig() {
			final HealthChecksConfigProperties cfg = new HealthChecksConfigProperties();
			cfg.setInterval(Duration.seconds(30));
			return cfg;
		}
	}

	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(
				new ExampleModule(),
				new MetricsModule(), // to show checks also exposed as Micrometer gauges
				new HealthChecksModule() // binding for HealthChecks
		);

		// start services
		injector.getInstance(Run.class).start();
	}

}
