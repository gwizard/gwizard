package org.gwizard.metrics;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.EqualsAndHashCode;
import org.gwizard.services.ServicesModule;

import java.lang.reflect.AnnotatedElement;

/**
 * Backend-neutral Micrometer integration. Applications must bind MeterRegistry as a singleton.
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class MetricsModule extends AbstractModule {
	@Override
	protected void configure() {
		install(new ServicesModule());

		requireBinding(MeterRegistry.class);

		final Matcher<AnnotatedElement> timed = element -> element.getAnnotationsByType(Timed.class).length > 0;
		final TimedInterceptor interceptor = new TimedInterceptor(getProvider(MeterRegistry.class));
		bindInterceptor(Matchers.any(), timed, interceptor);
		// A method annotation overrides class-level timing rather than recording twice.
		bindInterceptor(timed, method -> !timed.matches(method) && method.getDeclaringClass() != Object.class, interceptor);

		bind(MetricsService.class).asEagerSingleton();
	}
}
