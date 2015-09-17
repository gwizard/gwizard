package org.gwizard.healthchecks.autoconfig;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import org.gwizard.healthchecks.HealthChecks;

public class AutoHealthChecksModule extends AbstractModule {

	@Override
	protected void configure() {
		bindListener(new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return typeLiteral.getRawType().isAnnotationPresent(AutoHealthCheck.class);
			}
		},new AutoHealthCheckListener(getProvider(HealthChecks.class)));
	}
}
