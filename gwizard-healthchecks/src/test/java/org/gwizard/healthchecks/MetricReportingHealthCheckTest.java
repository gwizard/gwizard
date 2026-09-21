package org.gwizard.healthchecks;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.dropwizard.util.Duration;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.gwizard.metrics.MetricsModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MetricReportingHealthCheckTest {
	private final HealthChecksConfigProperties config = new HealthChecksConfigProperties();
	private Injector injector;
	private MeterRegistry registry;

	@BeforeEach
	void setUp() {
		config.setInterval(null);
		injector = Guice.createInjector(new MetricsModule(), new HealthChecksModule(), new AbstractModule() {
			@Override
			protected void configure() {
				bind(HealthChecksConfig.class).toInstance(config);
				bind(MeterRegistry.class).to(SimpleMeterRegistry.class).in(Scopes.SINGLETON);
			}
		});
		registry = injector.getInstance(MeterRegistry.class);
	}

	@AfterEach
	void close() {
		registry.close();
	}

	@Test
	void exposesHealthAndFailuresThroughMicrometer() {
		final Example check = injector.getInstance(Example.class);
		final Gauge gauge = registry.get("gwizard.healthChecks.example").gauge();
		assertThat(check.calls).isZero();
		assertThat(gauge.value()).isEqualTo(1);
		check.healthy = false;
		assertThat(gauge.value()).isZero();
		check.fail = true;
		assertThat(gauge.value()).isNaN();
		assertThat(check.calls).isEqualTo(3);
	}

	@Test
	void supportsAnEmptyPrefix() {
		config.setMetricsPrefix("");
		injector.getInstance(Example.class);
		assertThat(registry.get("example").gauge().value()).isEqualTo(1);
	}

	@Test
	void cachesGaugeSamplesButNotExplicitHealthChecks() {
		final CachedExample check = injector.getInstance(CachedExample.class);
		final Gauge gauge = registry.get("gwizard.healthChecks.cached").gauge();
		assertThat(gauge.value()).isEqualTo(1);
		assertThat(gauge.value()).isEqualTo(1);
		assertThat(check.calls).isEqualTo(1);
		assertThat(injector.getInstance(HealthChecks.class).run().get("cached").isHealthy()).isTrue();
		assertThat(check.calls).isEqualTo(2);
	}

	public static class Example extends AbstractMetricReportingHealthCheck {
		private int calls;
		private boolean healthy = true;
		private boolean fail;

		@Inject
		public Example(final HealthChecks checks) {
			super(checks, "example");
		}

		@Override
		protected Result check() throws IOException {
			calls++;
			if (fail) {
				throw new IOException("Cannot connect");
			}
			return healthy ? Result.healthy() : Result.unhealthy("Not ready");
		}
	}

	public static class CachedExample extends AbstractMetricReportingHealthCheck {
		private int calls;

		@Inject
		public CachedExample(final HealthChecks checks) {
			super(checks, "cached", Duration.seconds(30));
		}

		@Override
		protected Result check() {
			calls++;
			return Result.healthy();
		}
	}
}
