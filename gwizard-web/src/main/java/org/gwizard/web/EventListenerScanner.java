package org.gwizard.web;

import com.google.inject.Injector;

import javax.inject.Inject;
import java.util.EventListener;

/**
 * Walks through the guice injector bindings, visiting each one that is an EventListener.
 */
public class EventListenerScanner extends Scanner<EventListener> {
	@Inject
	public EventListenerScanner(Injector injector) {
		super(injector, EventListener.class);
	}
}
