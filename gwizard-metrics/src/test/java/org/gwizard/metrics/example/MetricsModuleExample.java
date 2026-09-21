package org.gwizard.metrics.example;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.gwizard.metrics.MetricsModule;
import org.gwizard.services.Run;

public class MetricsModuleExample {

	public static class ExampleService {
		@Timed(value = "example.work", description = "Time spent doing example work")
		public String work() {
			return "done";
		}
	}

	public static void main(String[] args) {
		final Injector injector = Guice.createInjector(
				new MetricsModule(),
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(MeterRegistry.class).to(SimpleMeterRegistry.class).in(Scopes.SINGLETON);
					}
				}
		);

		final Run run = injector.getInstance(Run.class);
		run.start();
		try {
			injector.getInstance(ExampleService.class).work();
			final MeterRegistry registry = injector.getInstance(MeterRegistry.class);
			System.out.println("Completed calls: " + registry.get("example.work").timer().count());
		} finally {
			run.stop();
		}
	}

}
