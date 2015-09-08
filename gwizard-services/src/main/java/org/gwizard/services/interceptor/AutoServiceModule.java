package org.gwizard.services.interceptor;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import org.gwizard.services.Services;

public class AutoServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bindListener(new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> typeLiteral) {
				return typeLiteral.getRawType().isAnnotationPresent(AutoService.class);
			}
		},new AutoServiceListener(getProvider(Services.class)));
	}
}
