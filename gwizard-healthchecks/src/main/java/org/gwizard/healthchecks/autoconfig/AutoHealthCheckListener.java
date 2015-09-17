package org.gwizard.healthchecks.autoconfig;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.gwizard.healthchecks.HealthChecks;

import javax.inject.Provider;

public class AutoHealthCheckListener implements TypeListener {

	private final Provider<HealthChecks> healthChecksProvider;

	public AutoHealthCheckListener(Provider<HealthChecks> servicesProvider) {
		this.healthChecksProvider = servicesProvider;
	}

	@Override
	public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
		final Class<?> clazz = typeLiteral.getRawType();
		if (HealthCheck.class.isAssignableFrom(clazz)) {
			String healthCheckName = clazz.getAnnotation(AutoHealthCheck.class).name();
			typeEncounter.register((InjectionListener<I>) i ->
					healthChecksProvider.get().add(healthCheckName, (HealthCheck) i));
		}
	}
}
