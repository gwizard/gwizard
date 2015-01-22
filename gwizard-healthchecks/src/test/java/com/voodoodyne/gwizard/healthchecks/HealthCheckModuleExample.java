package com.voodoodyne.gwizard.healthchecks;

import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.voodoodyne.gwizard.metrics.MetricsModule;
import com.voodoodyne.gwizard.services.Run;

public class HealthCheckModuleExample {

	/**
	 * dumb example that's always unhealthy
	 */
	public static class ChronicallyUnhealthy extends HealthCheck {
		@Inject
		public ChronicallyUnhealthy(HealthChecks healthChecks) {
			healthChecks.register("chronicfailure", this);
		}

		@Override
		protected Result check() throws Exception {
			return Result.unhealthy("this health check is a terrible disappointment");
		}
	}

	/**
	 * dumb example that shows we can also expose 'health' via JMX
	 */
	public static class HealthCheckInJmxToo extends  HealthCheck {
		@Inject
		public HealthCheckInJmxToo(HealthChecks healthChecks) {
			healthChecks.register("example", this);
		}

		/**
		 *
		 * the MetricsModule adds the metrics-guice instrumentation module,
		 * which will see this @Gauge annotation and create a metric with
		 * the ObjectName : "metrics:name=example.healthy"
		 *
		 * @return 1 if healthy, zero if unhealthy (null if exception occurred)
		 */
		@Gauge(name="example.healthy", absolute = true)
		public Integer healthGaugeMethod() {
			try {
				return check().isHealthy() ? 1 : 0;
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected Result check() throws Exception {
			return Result.healthy();
		}
	}

	/**
	 * dumb singleton that wraps the ThreadDeadlockHealthCheck that's in metrics-healthcheck
	 */
	public static class DeadlockHcWrapper {
		@Inject
		public DeadlockHcWrapper(HealthChecks healthChecks, ThreadDeadlockHealthCheck healthCheck) {
			healthChecks.register("deadlocks", healthCheck);
		}
	}


	public static class ExampleModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(ChronicallyUnhealthy.class).asEagerSingleton();
			bind(HealthCheckInJmxToo.class).asEagerSingleton();
			bind(DeadlockHcWrapper.class).asEagerSingleton();
		}
	}

	public static void main(String[] args) throws Exception {
		final Injector injector = Guice.createInjector(
				new ExampleModule(),
				new MetricsModule(), // to show checks also exposed as metrics via JMX
				new HealthCheckModule()
		);

		// start services
		injector.getInstance(Run.class).start();
	}

}
