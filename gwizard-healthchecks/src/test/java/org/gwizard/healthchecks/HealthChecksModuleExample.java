package org.gwizard.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import io.dropwizard.util.Duration;
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
	 * dumb example that shows we can also expose health via JMX
	 */
	public static class JmxHealthCheck extends AbstractMetricReportingHealthCheck {
		public static final String name = "example";
		@Inject
		public JmxHealthCheck(HealthChecks healthChecks) {
			super(healthChecks, name);
		}

		@Override
		protected Result check() throws Exception {
			return Result.healthy();
		}
	}

	/**
	 * dumb example that shows we can also expose health via JMX
	 */
	@Slf4j
	public static class CachedJmxHealthCheck extends AbstractMetricReportingHealthCheck {
		public static final String name = "exampleCached";
		@Inject
		public CachedJmxHealthCheck(HealthChecks healthChecks) {
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
			bind(ChronicallyUnhealthy.class).asEagerSingleton();
			bind(JmxHealthCheck.class).asEagerSingleton();
			bind(CachedJmxHealthCheck.class).asEagerSingleton();
			bind(DeadlockHcWrapper.class).asEagerSingleton();
		}

		@Provides
		public HealthChecksConfig periodicHealthCheckConfig() {
			HealthChecksConfig cfg = new HealthChecksConfig();
			cfg.setInterval(Duration.seconds(30));
			return cfg;
		}
	}

	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(
				new ExampleModule(),
				new MetricsModule(), // to show checks also exposed as metrics via JMX
				new HealthChecksModule() // binding for HealthChecks
		);

		// start services
		injector.getInstance(Run.class).start();
	}

}
