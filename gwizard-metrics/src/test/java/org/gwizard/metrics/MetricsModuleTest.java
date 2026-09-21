package org.gwizard.metrics;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricsModuleTest {
	@Test
	void requiresAnExplicitRegistryBinding() {
		assertThatThrownBy(() -> Guice.createInjector(new MetricsModule()))
				.isInstanceOf(CreationException.class)
				.hasMessageContaining("No implementation for MeterRegistry was bound");
	}

	@Test
	void acceptsAnExplicitSimpleRegistryBinding() {
		final AbstractModule application = new AbstractModule() {
			@Override
			protected void configure() {
				bind(MeterRegistry.class).to(SimpleMeterRegistry.class).in(Scopes.SINGLETON);
			}
		};
		final Injector first = Guice.createInjector(new MetricsModule(), application);
		final Injector second = Guice.createInjector(new MetricsModule(), application);
		final MeterRegistry registry = first.getInstance(MeterRegistry.class);
		final MeterRegistry other = second.getInstance(MeterRegistry.class);
		try {
			assertThat(registry).isInstanceOf(SimpleMeterRegistry.class);
			assertThat(first.getInstance(MeterRegistry.class)).isSameAs(registry);
			assertThat(other).isNotSameAs(registry);
		} finally {
			registry.close();
			other.close();
		}
	}

	@Test
	void bindsJvmMetricsAndClosesTheProvidedRegistryOnce() throws Exception {
		final TrackingRegistry registry = new TrackingRegistry();
		registry.config().commonTags("application", "example");
		final Injector injector = Guice.createInjector(new MetricsModule(), new MetricsModule(), new AbstractModule() {
			@Override
			protected void configure() {
				bind(MeterRegistry.class).toInstance(registry);
			}
		});
		assertThat(injector.getInstance(MeterRegistry.class)).isSameAs(registry);
		final ServiceManager services = injector.getInstance(ServiceManager.class);
		assertThat(services.servicesByState().values()).hasSize(1);
		try {
			services.startAsync().awaitHealthy(5, TimeUnit.SECONDS);
			assertThat(registry.get("jvm.memory.used").gauges()).isNotEmpty();
			assertThat(registry.get("jvm.buffer.memory.used").gauges()).isNotEmpty();
			assertThat(registry.get("jvm.threads.live").gauge().value()).isPositive();
			assertThat(registry.get("jvm.classes.loaded").gauge().value()).isPositive();
			assertThat(registry.get("system.cpu.count").gauge().value()).isPositive();
			assertThat(registry.get("jvm.gc.memory.allocated").counter()).isNotNull();
			assertThat(registry.getMeters()).allSatisfy(meter ->
					assertThat(meter.getId().getTag("application")).isEqualTo("example"));
		} finally {
			services.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
		}
		assertThat(registry.isClosed()).isTrue();
		assertThat(registry.closes).isEqualTo(1);
	}

	private static class TrackingRegistry extends SimpleMeterRegistry {
		private int closes;

		@Override
		public void close() {
			closes++;
			super.close();
		}
	}
}
