package org.gwizard.services.interceptor;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import org.gwizard.services.Services;

public class ServicesInterceptorModule extends AbstractModule{
	protected void configure() {
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(Service.class),
				new ServiceInterceptor(getProvider(Services.class)));
	}
}
