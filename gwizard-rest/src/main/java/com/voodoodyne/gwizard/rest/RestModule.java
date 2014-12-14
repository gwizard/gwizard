package com.voodoodyne.gwizard.rest;

import com.google.inject.servlet.ServletModule;
import com.voodoodyne.gwizard.web.WebModule;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import javax.inject.Singleton;

/**
 */
public class RestModule extends ServletModule {
	@Override
	protected void configureServlets() {
		install(new WebModule());
		install(new JaxrsModule());

		// Binding this will cause it to be picked up by gwizard-web
		bind(GuiceResteasyBootstrapServletContextListener.class);

		bind(HttpServletDispatcher.class).in(Singleton.class);
		serve("/*").with(HttpServletDispatcher.class);
	}
}
