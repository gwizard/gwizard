package org.gwizard.test;

import jakarta.inject.Inject;

import java.util.concurrent.Callable;

/**
 * Sets up and tears down state as if a request was made to a web container.
 */
class ScopeRequestFilter implements RequestFilter {

	private final RequestScope requestScope;

	@Inject
	public ScopeRequestFilter(final RequestScope requestScope) {
		this.requestScope = requestScope;
	}

	@Override
	public <T> Callable<T> filter(final Callable<T> callable) {
		return () -> {
			requestScope.enter();
			try {
				return callable.call();
			} finally {
				requestScope.exit();
			}
		};
	}
}
