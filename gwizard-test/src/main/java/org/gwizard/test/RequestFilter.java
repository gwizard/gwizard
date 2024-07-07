package org.gwizard.test;

import java.util.concurrent.Callable;

/**
 * A more abstract concept than a servlet filter; in fact, it can wrap an
 * actual servlet filter and execute it as part of a simulated request.
 *
 * @see Requestor
 */
public interface RequestFilter {

	<T> Callable<T> filter(final Callable<T> callable);

}
