package org.gwizard.web;

import com.google.inject.Binding;
import com.google.inject.Injector;
import org.eclipse.jetty.server.Handler;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.EventListener;

/**
 * Walks through the guice injector bindings, visiting each one that is a Handler.
 */
public class HandlerScanner {
	public interface Visitor {
		void visit(Handler handler);
	}

	private final Injector injector;

	@Inject
	public HandlerScanner(Injector injector) {
		this.injector = injector;
	}

	/** Start the process, visiting each Handler bound in the injector or any parents */
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

			if (type instanceof Class && Handler.class.isAssignableFrom((Class)type)) {
				visitor.visit((Handler)binding.getProvider().get());
			}
		}
	}
}
