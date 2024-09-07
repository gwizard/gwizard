package org.gwizard.test.web;

import com.google.inject.Provider;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Value;
import org.gwizard.test.RequestFilter;

import java.util.concurrent.Callable;

/**
 * Allows a servlet Filter to be used as a RequestFilter
 */
@Value
public class ServletFilterAdapter implements RequestFilter {

	/** */
	private static class Holder<T> {
		public T value;
	}

	/** */
	Filter filter;
	Provider<HttpServletRequest> requestProvider;
	Provider<HttpServletResponse> responseProvider;

	@Override
	public <T> Callable<T> filter(final Callable<T> callable) {
		return () -> {
			final Holder<T> holder = new Holder<>();

			filter.doFilter(requestProvider.get(), responseProvider.get(), (request, response) -> {
				try {
					holder.value = callable.call();
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			return holder.value;
		};
	}
}
