package org.gwizard.swagger;


import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.ServletModule;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.ServletContextListener;

@Slf4j
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class SwaggerModule extends ServletModule {

	@Override
	protected void configureServlets() {
		log.debug("Configuring swagger guice module...");
		Multibinder.newSetBinder(binder(), ServletContextListener.class).addBinding().to(SwaggerServletContextListener.class);
		bind(ApiListingResource.class);
		bind(SwaggerSerializers.class);
		filter("/*").through(ApiOriginFilter.class);
	}

}
