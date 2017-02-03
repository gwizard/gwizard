package org.gwizard.web;

import javax.inject.Inject;

import com.google.inject.Injector;

/**
 * Walks through the guice injector bindings, visiting each one that is a Handler.
 */
public class ServletContextConfiguratorScanner extends Scanner<ServletContextConfigurator> {

	@Inject
	public ServletContextConfiguratorScanner(Injector injector) {
		super(injector, ServletContextConfigurator.class);
	}
}
