package org.gwizard.web;

import com.google.inject.Injector;
import org.eclipse.jetty.server.Handler;

import javax.inject.Inject;

/**
 * Walks through the guice injector bindings, visiting each one that is a Handler.
 */
public class HandlerScanner extends Scanner<Handler> {
	@Inject
	public HandlerScanner(Injector injector) {
		super(injector, Handler.class);
	}
}
