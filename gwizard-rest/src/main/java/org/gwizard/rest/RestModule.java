package org.gwizard.rest;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.servlet.ServletModule;
import lombok.EqualsAndHashCode;
import org.gwizard.web.WebModule;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import javax.inject.Singleton;
import java.util.Map;

/**
 */
@EqualsAndHashCode(callSuper=false, of={})	// makes installation of this module idempotent
public class RestModule extends ServletModule {
	private final String path;

	public RestModule() {
		this.path = null;
	}

	/**
	 * @param path    a prefix for all REST requests, eg "/api"
	 */
	public RestModule(final String path) {
		Preconditions.checkArgument(path.length() == 0 || path.startsWith("/"), "Path must begin with '/'");
		Preconditions.checkArgument(!path.endsWith("/"), "Path must not have a trailing '/'");
		this.path = path;
	}

	@Override
	protected void configureServlets() {
		install(new WebModule());
		install(new JaxrsModule());

		// Binding this will cause it to be picked up by gwizard-web
		bind(GuiceResteasyBootstrapServletContextListener.class);

		// Make sure RESTEasy picks this up so we get our ObjectMapper from guice
		bind(ObjectMapperContextResolver.class);

		bind(HttpServletDispatcher.class).in(Singleton.class);

		if (path == null) {
			serve("/*").with(HttpServletDispatcher.class);
		} else {
			final Map<String, String> initParams = ImmutableMap.of("resteasy.servlet.mapping.prefix", path);
			serve(path + "/*").with(HttpServletDispatcher.class, initParams);
		}
	}
}
