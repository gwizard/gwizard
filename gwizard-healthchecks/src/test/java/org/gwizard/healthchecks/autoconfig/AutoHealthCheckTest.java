package org.gwizard.healthchecks.autoconfig;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.gwizard.healthchecks.HealthChecks;
import org.gwizard.healthchecks.HealthChecksModule;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AutoHealthCheckTest {

	@Singleton
	@AutoHealthCheck(name = "example health check")
	static class ExampleHealthCheck extends HealthCheck {
		@Override
		protected Result check() throws Exception {
			return Result.healthy();
		}
	}

	@Test
	public void shouldRegisterHealthChecks() {
		// given
		HealthChecks healthChecks = mock(HealthChecks.class);

		// when
		Injector injector = Guice.createInjector(
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(HealthChecks.class).toInstance(healthChecks);
						bind(ExampleHealthCheck.class).asEagerSingleton();
					}
				},
				new HealthChecksModule()
		);

		// then
		verify(healthChecks).add("example health check", injector.getInstance(ExampleHealthCheck.class));
	}
}