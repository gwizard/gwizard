package org.gwizard.rest;

import com.google.inject.servlet.ServletModule;
import org.gwizard.web.WebModule;
import lombok.EqualsAndHashCode;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import javax.inject.Singleton;

/**
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class RestModule extends ServletModule {
	@Override
	protected void configureServlets() {
		install(new WebModule());
		install(new JaxrsModule());

		// Binding this will cause it to be picked up by gwizard-web
		bind(GuiceResteasyBootstrapServletContextListener.class);

		// Make sure RESTEasy picks this up so we get our ObjectMapper from guice
		bind(ObjectMapperContextResolver.class);

		bind(HttpServletDispatcher.class).in(Singleton.class);
		serve("/*").with(HttpServletDispatcher.class);
	}
}
