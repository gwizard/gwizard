package org.gwizard.eventcast;

import com.lexicalscope.eventcast.EventCastUnhandledListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * if no handlers were instantiated via Guice for an event, this listener will log a warning
 */
@Slf4j
public class UnhandledEventListener implements EventCastUnhandledListener {
	@Override
	public void unhandledEventCast(Type listenerType, Method listenerMethod, Object[] args) {
		log.warn("Unhandled event: {}, {}, {}", listenerType, listenerMethod, args);
	}
}
