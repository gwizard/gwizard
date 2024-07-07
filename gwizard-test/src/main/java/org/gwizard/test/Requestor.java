package org.gwizard.test;

import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * <p>In tests we often need to model the idea of a "request". Webapps have requests, rpc servers
 * have requests, queue workers have requests, etc. This class manages that behavior for you.
 * In particular, it allows us to register a stack of "filters" which execute around requests.
 * The obvious application here is adding servlet Filters to simulated webapp requests.</p>
 *
 * <p>Works in concert with the GuiceExtension.</p>
 */
@Singleton
public class Requestor {

	/** They will be run in order */
	private final List<RequestFilter> filters = new ArrayList<>();

	public void addFilter(final RequestFilter filter) {
		this.filters.add(filter);
	}

	/** Execute within the context of a request */
	public <T> T req(final Callable<T> callable) throws Exception {
		return wrap(callable, filters.size()-1).call();
	}

	/** Execute within the context of a request; just a wrapper for the Callable version */
	final public void req(final Runnable runnable) throws Exception {
		req(() -> {
			runnable.run();
			return null;
		});
	}

	/** Recursively wrap them, but do it in reverse order by decrementing the index */
	private <T> Callable<T> wrap(final Callable<T> callable, int index) {
		if (index < 0)
			return callable;

		final Callable<T> filtered = filters.get(index).filter(callable);
		return wrap(filtered, index - 1);
	}
}
