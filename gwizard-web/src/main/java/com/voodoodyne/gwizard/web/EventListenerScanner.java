package com.voodoodyne.gwizard.web;

import com.google.inject.Binding;
import com.google.inject.Injector;
import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.EventListener;

/**
 * Walks through the guice injector bindings, visiting each one that is an EventListener.
 */
public class EventListenerScanner {
	public static interface Visitor {
		void visit(EventListener listener);
	}

	private final Injector injector;

	@Inject
	public EventListenerScanner(Injector injector) {
		this.injector = injector;
	}

	/** Start the process, visiting each ServletContextListener bound in the injector or any parents */
	public void accept(Visitor visitor) {
		accept(injector, visitor);
	}

	/** Recursive impl that walks up the parent injectors first */
	private void accept(Injector inj, Visitor visitor) {
		if (inj == null)
			return;

		accept(inj.getParent(), visitor);

		for (final Binding<?> binding: inj.getBindings().values()) {
			final Type type = binding.getKey().getTypeLiteral().getType();

			if (type instanceof Class && EventListener.class.isAssignableFrom((Class)type)) {
				visitor.visit((EventListener)binding.getProvider().get());
			}
		}
	}
}
