package org.gwizard.web;

import com.google.inject.Binding;
import com.google.inject.Injector;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Type;

/**
 * Walks through the guice injector bindings, visiting each one that is of the specified type.
 */
@RequiredArgsConstructor
public class Scanner<T> {
	public interface Visitor<V> {
		void visit(V thing);
	}

	private final Injector injector;
	private final Class<T> scanFor;

	/** Start the process, visiting each ServletContextListener bound in the injector or any parents */
	public void accept(final Visitor<T> visitor) {
		accept(injector, visitor);
	}

	/** Recursive impl that walks up the parent injectors first */
	private void accept(final Injector inj, final Visitor visitor) {
		if (inj == null)
			return;

		accept(inj.getParent(), visitor);

		for (final Binding<?> binding: inj.getBindings().values()) {
			final Type type = binding.getKey().getTypeLiteral().getType();

			if (type instanceof Class && scanFor.isAssignableFrom((Class)type)) {
				//noinspection unchecked
				visitor.visit((T)binding.getProvider().get());
			}
		}
	}
}
